package org.getalp.dbnary;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;
import org.getalp.dbnary.tools.CounterSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.regex.Pattern;

public class OntolexBasedRDFDataHandler extends DbnaryModel implements IWiktionaryDataHandler {

    protected static class PosAndType {
        protected Resource pos;
        protected Resource type;

        public PosAndType(Resource p, Resource t) {
            this.pos = p;
            this.type = t;
        }
    }

    private Logger log = LoggerFactory.getLogger(OntolexBasedRDFDataHandler.class);

    protected Model aBox;
    protected Map<Feature, Model> featureBoxes;

    // States used for processing
    protected Resource currentLexEntry;
    protected Resource currentLexinfoPos;
    protected String currentWiktionaryPos;

    protected Resource currentSense;
    protected int currentSenseNumber;
    protected int currentSubSenseNumber;
    protected CounterSet translationCount = new CounterSet();
    private CounterSet reifiedNymCount = new CounterSet();
    protected String wktLanguageEdition;

    protected Resource lexvoLanguageEdition;
    protected Resource lexvoExtractedLanguage;

    private Set<Statement> heldBackStatements = new HashSet<Statement>();

    protected int nbEntries = 0;
    private String NS;
    protected String WIKT;
    protected String currentEncodedPageName;
    protected String currentWiktionaryPageName;
    protected CounterSet currentLexieCount = new CounterSet();
    protected Resource currentMainLexEntry;
    protected Resource currentCanonicalForm;

    protected Set<PronunciationPair> currentSharedPronunciations;
//	private String currentSharedPronunciation;
//	private String currentSharedPronunciationLang;

    private HashMap<SimpleImmutableEntry<String, String>, HashSet<HashSet<PropertyObjectPair>>> heldBackOtherForms = new HashMap<SimpleImmutableEntry<String, String>, HashSet<HashSet<PropertyObjectPair>>>();

    private static HashMap<String, Property> nymPropertyMap = new HashMap<String, Property>();
    protected static HashMap<String, PosAndType> posAndTypeValueMap = new HashMap<String, PosAndType>();

    static {

        nymPropertyMap.put("syn", DBnaryOnt.synonym);
        nymPropertyMap.put("ant", DBnaryOnt.antonym);
        nymPropertyMap.put("hypo", DBnaryOnt.hyponym);
        nymPropertyMap.put("hyper", DBnaryOnt.hypernym);
        nymPropertyMap.put("mero", DBnaryOnt.meronym);
        nymPropertyMap.put("holo", DBnaryOnt.holonym);
        nymPropertyMap.put("qsyn", DBnaryOnt.approximateSynonym);
        nymPropertyMap.put("tropo", DBnaryOnt.troponym);

        //	posAndTypeValueMap.put("", new PosAndType(null, LemonOnt.LexicalEntry)); // other Part of Speech

    }

    // Map of the String to lexvo language entity
    private HashMap<String, Resource> languages = new HashMap<String, Resource>();

    public OntolexBasedRDFDataHandler(String lang) {
        super();

        NS = DBNARY_NS_PREFIX + "/" + lang + "/";

        wktLanguageEdition = LangTools.getPart1OrId(lang);
	WIKT = "https://" + wktLanguageEdition + ".wiktionary.org/wiki/";
        lexvoExtractedLanguage = tBox.createResource(LEXVO + lang);

        // Create aBox
        aBox = ModelFactory.createDefaultModel();
        aBox.setNsPrefix(lang, NS);
        aBox.setNsPrefix("dbnary", DBnaryOnt.getURI());
        aBox.setNsPrefix("dbetym", DBnaryEtymologyOnt.getURI());
        // aBox.setNsPrefix("lemon", LemonOnt.getURI());
        aBox.setNsPrefix("lexinfo", LexinfoOnt.getURI());
        aBox.setNsPrefix("rdfs", RDFS.getURI());
        aBox.setNsPrefix("dcterms", DCTerms.getURI());
        aBox.setNsPrefix("lexvo", LEXVO);
        aBox.setNsPrefix("rdf", RDF.getURI());
        aBox.setNsPrefix("olia", OliaOnt.getURI());
        aBox.setNsPrefix("ontolex", OntolexOnt.getURI());
        aBox.setNsPrefix("vartrans", VarTransOnt.getURI());
        aBox.setNsPrefix("synsem", SynSemOnt.getURI());
        aBox.setNsPrefix("lime", LimeOnt.getURI());
        aBox.setNsPrefix("decomp", DecompOnt.getURI());
        aBox.setNsPrefix("skos", SkosOnt.getURI());
        aBox.setNsPrefix("xs", XSD.getURI());
        aBox.setNsPrefix("wikt", WIKT);

        featureBoxes = new HashMap<>();
        featureBoxes.put(Feature.MAIN, aBox);
    }

    /**
     * returns the language of the current Entry
     * @return a language code
     */
    @Override
    public String getCurrentEntryLanguage() {
        return wktLanguageEdition;
    }

    @Override
    public void enableFeature(Feature f) {
        Model box = ModelFactory.createDefaultModel();
        fillInPrefixes(aBox, box);
        featureBoxes.put(f, box);
    }

    @Override
    public boolean isEnabled(Feature f) {
        return featureBoxes.containsKey(f);
    }

    @Override
    public void initializePageExtraction(String wiktionaryPageName) {
        currentLexieCount.resetAll();
    }

    @Override
    public void finalizePageExtraction() {

    }

    private void fillInPrefixes(Model aBox, Model morphoBox) {
        for (Map.Entry<String, String> e : aBox.getNsPrefixMap().entrySet()) {
            morphoBox.setNsPrefix(e.getKey(), e.getValue());
        }
    }

    @Override
    public void initializeEntryExtraction(String wiktionaryPageName) {
        currentSense = null;
        currentSenseNumber = 0;
        currentSubSenseNumber = 0;
        currentWiktionaryPageName = wiktionaryPageName;
        currentLexinfoPos = null;
        currentWiktionaryPos = null;
        translationCount.resetAll();
        reifiedNymCount.resetAll();
        currentCanonicalForm = null;
        currentSharedPronunciations = new HashSet<PronunciationPair>();

        // Create a dummy lexical entry that points to the one that corresponds to a part of speech
        currentMainLexEntry = getVocableResource(wiktionaryPageName, true);


        // Retain these statements to be inserted in the model when we know that the entry corresponds to a proper part of speech
        heldBackStatements.add(aBox.createStatement(currentMainLexEntry, RDF.type, DBnaryOnt.Page));

        currentEncodedPageName = null;
        currentLexEntry = null;
    }

    @Override
    public void finalizeEntryExtraction() {
        // Clear currentStatements. If statemenents do exist-s in it, it is because, there is no extractable part of speech in the entry.
        heldBackStatements.clear();
        promoteNymProperties();
    }

    public static String getEncodedPageName(String pageName, String pos, int defNumber) {
        return uriEncode(pageName, pos) + "__" + defNumber;
    }

    public Resource getLexEntry(String languageCode, String pageName, String pos, int defNumber) {
        //FIXME this doesn't use its languageCode parameter
        return getLexEntry(
                getEncodedPageName(pageName, pos, defNumber),
                typeResource(pos)
        );
    }

    public Resource getLexEntry(String encodedPageName, Resource typeResource) {
        return aBox.createResource(getPrefix() + encodedPageName, typeResource);
    }

    public int currentDefinitionNumber() {
        return currentLexieCount.get(currentWiktionaryPos);
    }

    @Override
    public String currentWiktionaryPos() {
        return currentWiktionaryPos;
    }

    @Override
    public Resource currentLexinfoPos() {
        return currentLexinfoPos;
    }

    public Resource addPartOfSpeech(String originalPOS, Resource normalizedPOS, Resource normalizedType) {
        // DONE: create a LexicalEntry for this part of speech only and attach info to it.
        currentWiktionaryPos = originalPOS;
        currentLexinfoPos = normalizedPOS;

        nbEntries++;

        currentEncodedPageName = getEncodedPageName(currentWiktionaryPageName, originalPOS, currentLexieCount.incr(currentWiktionaryPos));
        currentLexEntry = getLexEntry(currentEncodedPageName, normalizedType);

        if (!normalizedType.equals(OntolexOnt.LexicalEntry)) {
            // Add the Lexical Entry type so that users may refer to all entries using the top hierarchy without any reasoner.
            aBox.add(aBox.createStatement(currentLexEntry, RDF.type, OntolexOnt.LexicalEntry));
        }

        // import other forms
        SimpleImmutableEntry<String, String> keyOtherForms = new SimpleImmutableEntry<String, String>(currentWiktionaryPageName, originalPOS);
        HashSet<HashSet<PropertyObjectPair>> otherForms = heldBackOtherForms.get(keyOtherForms);

        // TODO: check that other forms point to valid entries and log faulty entries for wiktionary correction.
        if (otherForms != null) {
            for (HashSet<PropertyObjectPair> otherForm : otherForms) {
                addOtherFormPropertiesToLexicalEntry(currentLexEntry, otherForm);
            }
        }

        // All translation numbers are local to a lexEntry
        translationCount.resetAll();
        reifiedNymCount.resetAll();

        currentCanonicalForm = aBox.createResource(getPrefix() + "__cf_" + currentEncodedPageName, OntolexOnt.Form);

        // If a pronunciation was given before the first part of speech, it means that it is shared amoung pos/etymologies
        for (PronunciationPair p : currentSharedPronunciations) {
            if (null != p.lang && p.lang.length() > 0) {
                aBox.add(currentCanonicalForm, OntolexOnt.phoneticRep, p.pron, p.lang);
            } else {
                aBox.add(currentCanonicalForm, OntolexOnt.phoneticRep, p.pron);
            }
        }

        aBox.add(currentLexEntry, OntolexOnt.canonicalForm, currentCanonicalForm);
        aBox.add(currentCanonicalForm, OntolexOnt.writtenRep, currentWiktionaryPageName, LangTools.threeLettersCode(getCurrentEntryLanguage()));
        aBox.add(currentCanonicalForm, RDFS.label, currentWiktionaryPageName, LangTools.threeLettersCode(getCurrentEntryLanguage()));
        aBox.add(currentLexEntry, DBnaryOnt.partOfSpeech, currentWiktionaryPos);
        if (null != currentLexinfoPos)
            aBox.add(currentLexEntry, LexinfoOnt.partOfSpeech, currentLexinfoPos);

        aBox.add(currentLexEntry, LimeOnt.language, getCurrentEntryLanguage());
        aBox.add(currentLexEntry, DCTerms.language, lexvoExtractedLanguage);

        // Register the pending statements.
        for (Statement s : heldBackStatements) {
            aBox.add(s);
        }
        heldBackStatements.clear();
        aBox.add(currentMainLexEntry, DBnaryOnt.describes, currentLexEntry);
        return currentLexEntry;
    }

    public Resource posResource(PosAndType pat) {
        return (null == pat) ? null : pat.pos;
    }

    public Resource typeResource(PosAndType pat) {
        return (pat == null) ? OntolexOnt.LexicalEntry : pat.type;
    }

    public Resource posResource(String pos) {
        return posResource(posAndTypeValueMap.get(pos));
    }

    public Resource typeResource(String pos) {
        return typeResource(posAndTypeValueMap.get(pos));
    }

    @Override
    public void addPartOfSpeech(String pos) {
        PosAndType pat = posAndTypeValueMap.get(pos);
        addPartOfSpeech(pos, posResource(pat), typeResource(pat));
    }

    @Override
    public void registerPropertyOnCanonicalForm(Property p, RDFNode r) {
        if (null == currentLexEntry) {
            log.debug("Registering property when lex entry is null in \"{}\".", this.currentMainLexEntry);
            return; // Don't register anything if current lex entry is not known.
        }

        Resource canonicalForm = currentLexEntry.getPropertyResourceValue(OntolexOnt.canonicalForm);

        if (canonicalForm == null) {
            log.debug("Registering property when lex entry's canonicalForm is null in \"{}\".", this.currentMainLexEntry);
            return;
        }

        aBox.add(canonicalForm, p, r);
    }


    @Override
    public void registerPropertyOnLexicalEntry(Property p, RDFNode r) {
        if (null == currentLexEntry) {
            log.debug("Registering property on null lex entry in \"{}\".", this.currentMainLexEntry);
            return; // Don't register anything if current lex entry is not known.
        }

        aBox.add(currentLexEntry, p, r);
    }


    // TODO : Alternate spelling or lexical Variant ?
    // In Ontolex, orthographic variants are supposed to be given as a second writtenRep in the same Form
    // lexicalVariant should link 2 Lexical entries, same with varTrans lexicalRel
    @Override
    public void registerAlternateSpelling(String alt) {
        if (null == currentLexEntry) {
            log.debug("Registering Alternate Spelling when lex entry is null in \"{}\".", this.currentMainLexEntry);
            return; // Don't register anything if current lex entry is not known.
        }

        log.debug("Registering lexical Variant: {} for entry: {}", alt, currentEncodedPageName);
        Resource altlemma = aBox.createResource();
        aBox.add(currentLexEntry, VarTransOnt.lexicalRel, altlemma);
        aBox.add(altlemma, OntolexOnt.writtenRep, alt, wktLanguageEdition);
    }

    @Override
    public void registerNewDefinition(String def) {
        this.registerNewDefinition(def, 1);
    }

    @Override
    public void registerNewDefinition(String def, int lvl) {
        if (null == currentLexEntry) {
            log.debug("Registering Word Sense when lex entry is null in \"{}\".", this.currentMainLexEntry);
            return; // Don't register anything if current lex entry is not known.
        }
        if (lvl > 1) {
            log.trace("registering sub sense for {}", currentEncodedPageName);
            currentSubSenseNumber++;
        } else {
            currentSenseNumber++;
            currentSubSenseNumber = 0;
        }
        registerNewDefinition(def, computeSenseNum());
    }

    public void registerNewDefinition(String def, String senseNumber) {
        if (def == null || def.length() == 0) return;
        if (null == currentLexEntry) {
            log.debug("Registering Word Sense when lex entry is null in \"{}\".", this.currentMainLexEntry);
            return; // Don't register anything if current lex entry is not known.
        }

        // Create new word sense + a definition element
        currentSense = aBox.createResource(computeSenseId(senseNumber), OntolexOnt.LexicalSense);
        aBox.add(currentLexEntry, OntolexOnt.sense, currentSense);
        aBox.add(aBox.createLiteralStatement(currentSense, DBnaryOnt.senseNumber, aBox.createTypedLiteral(senseNumber)));
        // pos is not usefull anymore for word sense as they should be correctly linked to an entry with only one pos.
        // if (currentPos != null && ! currentPos.equals("")) {
        //	aBox.add(currentSense, LexinfoOnt.partOfSpeech, currentPos);
        //}

        Resource defNode = aBox.createResource();
        // TODO: no definition relation in Ontolex, Lexical Concepts use skos:definition, but not lexical senses, or do they ?
        aBox.add(currentSense, SkosOnt.definition, defNode);
        // Keep a human readable version of the definition, removing all links annotations.
        aBox.add(defNode, RDF.value, AbstractWiktionaryExtractor.cleanUpMarkup(def, true), wktLanguageEdition);

        // TODO: Extract domain/usage field from the original definition.

    }

    private String computeSenseId(String senseNumber) {
        return getPrefix() + "__ws_" + senseNumber + "_" + currentEncodedPageName;
    }

    protected String computeSenseNum() {
        return "" + currentSenseNumber + ((currentSubSenseNumber == 0) ? "" : ("." + currentSubSenseNumber));
    }

    protected Resource registerTranslationToEntity(Resource entity, String lang, Resource currentGlose, String usage, String word) {
        if (null == entity) {
            log.debug("Registering Translation when lex entry is null in \"{}\".", this.currentMainLexEntry);
            return null; // Don't register anything if current lex entry is not known.
        }
        word = word.trim();
        // Do not register empty translations
        if (word.length() == 0 && (usage == null || usage.length() == 0)) {
            return null;
        }
        // Ensure language is in its standard form.
        String tl = LangTools.getPart1OrId(lang);
        lang = LangTools.normalize(lang);

        Resource trans = aBox.createResource(computeTransId(lang, entity), DBnaryOnt.Translation);
        aBox.add(trans, DBnaryOnt.isTranslationOf, entity);
        aBox.add(createTargetLanguageProperty(trans, lang));

        if (null == tl) {
            aBox.add(trans, DBnaryOnt.writtenForm, word);
        } else {
            aBox.add(trans, DBnaryOnt.writtenForm, word, tl);
        }

        if (currentGlose != null && !currentGlose.equals("")) {
            aBox.add(trans, DBnaryOnt.gloss, currentGlose);
        }

        if (usage != null && !usage.equals("")) {
            aBox.add(trans, DBnaryOnt.usage, usage);
        }
        return trans;
    }

    @Override
    public void registerTranslation(String lang, Resource currentGlose, String usage, String word) {
        registerTranslationToEntity(currentLexEntry, lang, currentGlose, usage, word);
    }

    public String getVocableResourceName(String vocable) {
        return getPrefix() + uriEncode(vocable);
    }

    public Resource getVocableResource(String vocable, boolean dontLinkWithType) {
        if (dontLinkWithType) {
            return aBox.createResource(getVocableResourceName(vocable));
        }
        return aBox.createResource(getVocableResourceName(vocable), DBnaryOnt.Page);
    }

    public Resource getVocableResource(String vocable) {
        return getVocableResource(vocable, false);
    }

    protected void mergePropertiesIntoResource(HashSet<PropertyObjectPair> properties, Resource res) {
        for (PropertyObjectPair p : properties) {
            if (!res.getModel().contains(res, p.getKey(), p.getValue())) {
                res.getModel().add(res, p.getKey(), p.getValue());
            }
        }
    }

    private boolean incompatibleProperties(Property p1, Property p2, boolean applyCommutativity) {
        return (
                p1 == LexinfoOnt.mood && p2 == LexinfoOnt.gender
        ) || (applyCommutativity && incompatibleProperties(p2, p1, false));
    }

    private boolean incompatibleProperties(Property p1, Property p2) {
        return incompatibleProperties(p1, p2, true);
    }

    private boolean isResourceCompatible(Resource r, HashSet<PropertyObjectPair> properties) {
        for (PropertyObjectPair pr : properties) {
            Property p = pr.getKey();

            Statement roStat = r.getProperty(p);

            if (roStat != null) {
                RDFNode ro = roStat.getObject();

                if (ro != null && !ro.equals(pr.getValue())) {
                    return false;
                }

                StmtIterator i = r.listProperties();
                while (i.hasNext()) {
                    if (incompatibleProperties(p, i.nextStatement().getPredicate())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    protected void addOtherFormPropertiesToLexicalEntry(Resource lexEntry, HashSet<PropertyObjectPair> properties) {
        boolean foundCompatible = false;
        Model morphoBox = featureBoxes.get(Feature.MORPHOLOGY);

        if (null == morphoBox) return;

        lexEntry = lexEntry.inModel(morphoBox);

        // DONE: Add other forms to a morphology dedicated model.
        StmtIterator otherForms = lexEntry.listProperties(OntolexOnt.otherForm);

        while (otherForms.hasNext() && !foundCompatible) {
            Resource otherForm = otherForms.next().getResource();
            if (isResourceCompatible(otherForm, properties)) {
                foundCompatible = true;
                mergePropertiesIntoResource(properties, otherForm);
            }
        }

        if (!foundCompatible) {
            String otherFormNodeName = computeOtherFormResourceName(lexEntry, properties);
            Resource otherForm = morphoBox.createResource(getPrefix() + otherFormNodeName, OntolexOnt.Form);
            morphoBox.add(lexEntry, OntolexOnt.otherForm, otherForm);
            mergePropertiesIntoResource(properties, otherForm);
        }
    }

    protected String computeOtherFormResourceName(Resource lexEntry, HashSet<PropertyObjectPair> properties) {
        String lexEntryLocalName = currentEncodedPageName;
        String compactProperties = DatatypeConverter.printBase64Binary(BigInteger.valueOf(properties.hashCode()).toByteArray()).replaceAll("[/=\\+]", "-");

        return "__wf_" + compactProperties + "_" + lexEntryLocalName;
    }

    public void registerInflection(String languageCode,
                                   String pos,
                                   String inflection,
                                   String canonicalForm,
                                   int defNumber,
                                   HashSet<PropertyObjectPair> props,
                                   HashSet<PronunciationPair> pronunciations) {

        if (pronunciations != null) {
            for (PronunciationPair pronunciation : pronunciations) {
                props.add(PropertyObjectPair.get(LexinfoOnt.pronunciation, aBox.createLiteral(pronunciation.pron, pronunciation.lang)));
            }
        }

        registerInflection(languageCode, pos, inflection, canonicalForm, defNumber, props);
    }

    public void registerInflection(String languageCode,
                                   String pos,
                                   String inflection,
                                   String canonicalForm,
                                   int defNumber,
                                   HashSet<PropertyObjectPair> props) {

        Resource posResource = posResource(pos);

        PropertyObjectPair p = PropertyObjectPair.get(OntolexOnt.writtenRep, aBox.createLiteral(inflection, getCurrentEntryLanguage()));

        props.add(p);

        if (defNumber == 0) {
            // the definition number was not specified, we have to register this
            // inflection for each entry.

            // First, we store the other form for all the existing entries
            Resource vocable = getVocableResource(canonicalForm, true);

            StmtIterator entries = vocable.listProperties(DBnaryOnt.describes);

            while (entries.hasNext()) {
                Resource lexEntry = entries.next().getResource();
                if (aBox.contains(lexEntry, LexinfoOnt.partOfSpeech, posResource)) {
                    addOtherFormPropertiesToLexicalEntry(lexEntry, props);
                }
            }

            // Second, we store the other form for future possible matching entries
            SimpleImmutableEntry<String, String> key = new SimpleImmutableEntry<String, String>(canonicalForm, pos);

            HashSet<HashSet<PropertyObjectPair>> otherForms = heldBackOtherForms.get(key);

            if (otherForms == null) {
                otherForms = new HashSet<HashSet<PropertyObjectPair>>();
                heldBackOtherForms.put(key, otherForms);
            }

            otherForms.add(props);
        } else {
            // the definition number was specified, this makes registration easy.
            addOtherFormPropertiesToLexicalEntry(
                    getLexEntry(languageCode, canonicalForm, pos, defNumber),
                    props
            );
        }
    }

    private Statement createTargetLanguageProperty(Resource trans, String lang) {
        lang = lang.trim();
        if (isAnISO639_3Code(lang)) {
            return aBox.createStatement(trans, DBnaryOnt.targetLanguage, getLexvoLanguageResource(lang));
        } else {
            return aBox.createStatement(trans, DBnaryOnt.targetLanguageCode, lang);
        }
    }

    private final static Pattern iso3letters = Pattern.compile("\\w{3}");

    private boolean isAnISO639_3Code(String lang) {
        // TODO For the moment, only check if the code is a 3 letter code...
        return iso3letters.matcher(lang).matches();
    }

    private String computeTransId(String lang, Resource entity) {
        lang = uriEncode(lang);
        return getPrefix() + "__tr_" + lang + "_" + translationCount.incr(lang) + "_" + entity.getURI().substring(getPrefix().length());
    }

    private Resource getLexvoLanguageResource(String lang) {
        Resource res = languages.get(lang);
        if (res == null) {
            res = tBox.createResource(LEXVO + lang);
            languages.put(lang, res);
        }
        return res;
    }

    public void registerNymRelationToEntity(String target, String synRelation, Resource entity) {
        if (null == entity) {
            log.debug("Registering Lexical Relation when lex entry is null in \"{}\".", this.currentMainLexEntry);
            return; // Don't register anything if current lex entry is not known.
        }
        // Some links point to Annex pages or Images, just ignore these.
        int colon = target.indexOf(':');
        if (colon != -1) {
            return;
        }
        int hash = target.indexOf('#');
        if (hash != -1) {
            // The target contains an intra page href. Remove it from the target uri and keep it in the relation.
            target = target.substring(0, hash);
            // TODO: keep additional intra-page href
            // aBox.add(nym, isAnnotatedBy, target.substring(hash));
        }

        Property nymProperty = nymPropertyMap.get(synRelation);

        Resource targetResource = getVocableResource(target);

        aBox.add(entity, nymProperty, targetResource);
    }

    @Override
    public void registerNymRelation(String target, String synRelation) {
        registerNymRelationToEntity(target, synRelation, currentLexEntry);
    }

    @Override
    public Resource createGlossResource(StructuredGloss gloss) {
        return createGlossResource(gloss, -1);
    }

    @Override
    public Resource createGlossResource(StructuredGloss gloss, int rank) {
        if (gloss == null || (
                        (gloss.getGloss() == null || gloss.getGloss().length() == 0) &&
                        (gloss.getSenseNumber() == null || gloss.getSenseNumber().length() == 0))
                ) return null;

        Resource glossResource = aBox.createResource(getGlossResourceName(gloss), DBnaryOnt.Gloss);
        if (null != gloss.getGloss() && gloss.getGloss().trim().length() > 0)
            aBox.add(aBox.createStatement(glossResource, RDF.value, gloss.getGloss(), wktLanguageEdition));
        if (gloss.getSenseNumber() != null)
            aBox.add(aBox.createStatement(glossResource, DBnaryOnt.senseNumber, gloss.getSenseNumber()));
        if (rank > 0)
            aBox.add(aBox.createLiteralStatement(glossResource, DBnaryOnt.rank, rank));
        return glossResource;
    }

    protected String getGlossResourceName(StructuredGloss gloss) {
        String key = gloss.getGloss() + gloss.getSenseNumber();
        key = DatatypeConverter.printBase64Binary(BigInteger.valueOf(key.hashCode()).toByteArray()).replaceAll("[/=\\+]", "-");
        return getPrefix() + "__" + wktLanguageEdition + "_gloss_" + key + "_" + currentEncodedPageName ;
    }

    @Override
    public void registerNymRelation(String target, String synRelation, Resource gloss) {
        registerNymRelation(target, synRelation, gloss, null);
    }

    @Override
    public void registerNymRelation(String target, String synRelation, Resource gloss, String usage) {
        if (null == currentLexEntry) {
            log.debug("Registering Lexical Relation when lex entry is null in \"{}\".", this.currentMainLexEntry);
            return; // Don't register anything if current lex entry is not known.
        }
        // Some links point to Annex pages or Images, just ignore these.
        int colon = target.indexOf(':');
        if (colon != -1) {
            return;
        }
        int hash = target.indexOf('#');
        if (hash != -1) {
            // The target contains an intra page href. Remove it from the target uri and keep it in the relation.
            target = target.substring(0, hash);
            // TODO: keep additional intra-page href
            // aBox.add(nym, isAnnotatedBy, target.substring(hash));
        }
        Property nymProperty = nymPropertyMap.get(synRelation);

        Resource targetResource = getVocableResource(target);

        Statement nymR = aBox.createStatement(currentLexEntry, nymProperty, targetResource);
        aBox.add(nymR);

        if (gloss == null && usage == null)
            return;

        ReifiedStatement rnymR = nymR.createReifiedStatement(computeNymId(synRelation));
        if (gloss != null)
            rnymR.addProperty(DBnaryOnt.gloss, gloss);
        if (usage != null)
            rnymR.addProperty(DBnaryOnt.usage, usage);


    }

    private String computeNymId(String nym) {
        return getPrefix() + "__" + nym + "_" + reifiedNymCount.incr(nym) + "_" + currentEncodedPageName;
    }

    @Override
    public void registerNymRelationOnCurrentSense(String target, String synRelation) {
        if (null == currentSense) {
            log.debug("Registering Lexical Relation when current sense is null in \"{}\".", this.currentMainLexEntry);
            registerNymRelation(target, synRelation);
            return; // Don't register anything if current lex entry is not known.
        }
        // Some links point to Annex pages or Images, just ignore these.
        int colon = target.indexOf(':');
        if (colon != -1) {
            return;
        }
        int hash = target.indexOf('#');
        if (hash != -1) {
            // The target contains an intra page href. Remove it from the target uri and keep it in the relation.
            target = target.substring(0, hash);
            // TODO: keep additional intra-page href
            // aBox.add(nym, isAnnotatedBy, target.substring(hash));
        }

        Property nymProperty = nymPropertyMap.get(synRelation);

        Resource targetResource = getVocableResource(target);

        aBox.add(currentSense, nymProperty, targetResource);
    }

    @Override
    public void registerPronunciation(String pron, String lang) {
        if (null == currentCanonicalForm) {
            currentSharedPronunciations.add(new PronunciationPair(pron, lang));
        } else {
            registerPronunciation(currentCanonicalForm, pron, lang);
        }
    }

    protected void registerPronunciation(Resource writtenRepresentation, String pron, String lang) {
        if (null != lang && lang.length() > 0) {
            aBox.add(writtenRepresentation, LexinfoOnt.pronunciation, pron, lang);
        } else {
            aBox.add(writtenRepresentation, LexinfoOnt.pronunciation, pron);
        }
    }

    private void promoteNymProperties() {
        StmtIterator entries = currentMainLexEntry.listProperties(DBnaryOnt.describes);
        HashSet<Statement> toBeRemoved = new HashSet<Statement>();
        while (entries.hasNext()) {
            Resource lu = entries.next().getResource();
            List<Statement> senses = lu.listProperties(OntolexOnt.sense).toList();
            if (senses.size() == 1) {
                Resource s = senses.get(0).getResource();
                HashSet<Property> alreadyProcessedNyms = new HashSet<Property>();
                for (Property nymProp : nymPropertyMap.values()) {
                    if (alreadyProcessedNyms.contains(nymProp)) continue;
                    alreadyProcessedNyms.add(nymProp);
                    StmtIterator nyms = lu.listProperties(nymProp);
                    while (nyms.hasNext()) {
                        Statement nymRel = nyms.next();
                        aBox.add(s, nymProp, nymRel.getObject());
                        toBeRemoved.add(nymRel);
                    }
                }
            }
        }
        for (Statement s : toBeRemoved) {
            s.remove();
        }
    }

    @Override
    public void dump(Feature f, OutputStream out, String format) {
        Model box = featureBoxes.get(f);
        if (null != box) {
            box.write(out, format);
        }
    }

    @Override
    public int nbEntries() {
        return nbEntries;
    }

    @Override
    public String currentLexEntry() {
        // TODO Auto-generated method stub
        return currentWiktionaryPageName;
    }

    public String getPrefix() {
        return NS;
    }

    @Override
    public void initializeEntryExtraction(String wiktionaryPageName, String lang) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Cannot initialize a foreign language entry.");
    }

    @Override
    public Resource registerExample(String ex, Map<Property, String> context) {
        if (null == currentSense) {
            log.debug("Registering example when lex sense is null in \"{}\".", this.currentMainLexEntry);
            return null; // Don't register anything if current lex entry is not known.
        }

        // Create new word sense + a definition element
        Resource example = aBox.createResource();
        aBox.add(aBox.createStatement(example, RDF.value, ex, getCurrentEntryLanguage()));
        if (null != context) {
            for (Map.Entry<Property, String> c : context.entrySet()) {
                aBox.add(aBox.createStatement(example, c.getKey(), c.getValue(), wktLanguageEdition));
            }
        }
        aBox.add(aBox.createStatement(currentSense, SkosOnt.example, example));
        return example;

    }
}
