package org.getalp.dbnary.pol;

import com.hp.hpl.jena.rdf.model.Resource;
import org.getalp.dbnary.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WiktionaryDataHandler extends OntolexBasedRDFDataHandler {

    private Logger log = LoggerFactory.getLogger(WiktionaryDataHandler.class);

    private Map<String, Resource[]> currentWordsenses = new HashMap<String, Resource[]>();
    private Set<Resource> currentLexEntries = new HashSet<Resource>();

    class DecodedPOS {
        class PropValPair {
            Resource prop, val;

            public PropValPair(Resource p, Resource v) {
                this.prop = p;
                this.val = v;
            }
        }

        String simplePOSName;
        Resource lexinfoPOS;
        Resource entryType;

        ArrayList<PropValPair> additionalProps = new ArrayList<PropValPair>();

        public DecodedPOS(String sn, Resource pos, Resource type) {
            this.simplePOSName = sn;
            this.lexinfoPOS = pos;
            this.entryType = type;
        }

        public void addAnnotation(Resource prop, Resource val) {
            additionalProps.add(new PropValPair(prop, val));
        }
    }

    public WiktionaryDataHandler(String lang) {
        super(lang);
    }

    @Override
    public void initializeEntryExtraction(String wiktionaryPageName) {
        super.initializeEntryExtraction(wiktionaryPageName);

        currentWordsenses.clear();
    }


    @Override
    public void registerNewDefinition(String def, String senseNumber) {
        super.registerNewDefinition(def, senseNumber);
        if (null != this.currentLexEntry) {
            currentWordsenses.put(senseNumber, new Resource[]{this.currentSense, this.currentLexEntry});
            currentLexEntries.add(this.currentLexEntry);
        }
    }

    public void registerNymRelation(String target, String synRelation, String senseNum) {
        // parse the gloss to get the sense number(s)
        ArrayList<String> numlist = getSenseNumbers(senseNum);
        for (String n : numlist) {
            Resource[] senseAndEntry = currentWordsenses.get(n);
            if (null == senseAndEntry) {
                log.debug("Could not fetch sense resource for nym property of {} in {}", n, currentLexEntry());
                StructuredGloss sg = new StructuredGloss(n, "");
                Resource g = super.createGlossResource(sg);
                super.registerNymRelation(target, synRelation, g);
            } else {
                Resource ws = senseAndEntry[0];
                registerNymRelationToEntity(target, synRelation, ws);
            }
        }
    }


    private DecodedPOS decodePOS(String group) {
        String orig = group;

        if (group.startsWith("rzeczownik")
                || group.startsWith("przymiotnik")
                || group.startsWith("czasownik")
                || group.startsWith("przys????wek")
                || group.startsWith("fraza")
                || group.startsWith("zwi??zek frazeologiczny"))
            group = group.split("''|\\(|<")[0];

        DecodedPOS dpos = computeDecodedPOS(group);
        if (dpos != null) {
            if (group.contains("rodzaj ??e??ski/m??ski") || group.contains("rodzaj m??ski/??e??ski")
                    || group.contains("rodzaj m??ski lub ??e??ski") || group.contains("rodzaj ??e??ski, m??ski")) {
                dpos.addAnnotation(LexinfoOnt.gender, LexinfoOnt.masculine);
                dpos.addAnnotation(LexinfoOnt.gender, LexinfoOnt.feminine);
                group = group.replace("rodzaj ??e??ski/m??ski", "");
                group = group.replace("rodzaj m??ski/??e??ski", "");
            }
            if (group.contains("rodzaj ??e??ski") || group.contains("rodzaju ??e??skiego") || group.contains("lub ??e??ski") || group.contains("i ??e??ski")) {
                dpos.addAnnotation(LexinfoOnt.gender, LexinfoOnt.feminine);
                group = group.replace("rodzaj ??e??ski", "");
                group = group.replace("rodzaju ??e??skiego", "");
                group = group.replace("lub ??e??ski", "");
                group = group.replace("i ??e??ski", "");
            }
            if (group.contains("rodzaj nijaki") || group.contains("rodzaju nijakiego") || group.contains("lub nijaki")) {
                dpos.addAnnotation(LexinfoOnt.gender, LexinfoOnt.neuter);
                group = group.replace("rodzaj nijaki", "");
                group = group.replace("rodzaju nijakiego", "");
                group = group.replace("lub nijaki", "");
            }
            if (group.contains("rodzaj m??ski") || group.contains("rodzaju m??skiego") || group.contains("lub m??ski") || group.contains(", m??ski")) {
                dpos.addAnnotation(LexinfoOnt.gender, LexinfoOnt.masculine);
                group = group.replace("rodzaj m??ski", "");
                group = group.replace("rodzaju m??skiego", "");
                group = group.replace("lub m??ski", "");
            }
            if (group.contains("rodzaj m??skorzeczowy") || group.contains("rodzaju m??skorzeczowego") || group.contains("lub m??skorzeczowy")) {
                dpos.addAnnotation(LexinfoOnt.gender, LexinfoOnt.masculine);
                dpos.addAnnotation(LexinfoOnt.animacy, LexinfoOnt.inanimate);
                group = group.replace("rodzaj m??skorzeczowy", "");
                group = group.replace("rodzaju m??skorzeczowego", "");
                group = group.replace("lub m??skorzeczowy", "");
            }
            if (group.contains("rodzaj m??skozwierz??cy") || group.contains("lub m??skozwierz??cy")) {
                dpos.addAnnotation(LexinfoOnt.gender, LexinfoOnt.masculine);
                dpos.addAnnotation(LexinfoOnt.animacy, LexinfoOnt.animate);
                group = group.replace("rodzaj m??skozwierz??cy", "");
                group = group.replace("lub m??skozwierz??cy", "");
            }
            if (group.contains("rodzaj m??skoosobowy") || group.contains("lub m??skoosobowy")) {
                dpos.addAnnotation(LexinfoOnt.gender, LexinfoOnt.masculine);
                dpos.addAnnotation(LexinfoOnt.animacy, LexinfoOnt.animate);
                dpos.addAnnotation(LexinfoOnt.referentType, LexinfoOnt.personal);
                group = group.replace("rodzaj m??skoosobowy", "");
                group = group.replace("lub m??skoosobowy", "");
            }
            if (group.contains("rodzaj niem??skoosobowy")) {
                // What is this ? non-masculine ?

                group = group.replace("rodzaj niem??skoosobowy", "");
            }
            if (group.contains("nie??ywotny")) {
                dpos.addAnnotation(LexinfoOnt.animacy, LexinfoOnt.inanimate);
                group = group.replace("nie??ywotny", "");
            }
            if (group.contains("lub m??skorzeczowy")) {
                dpos.addAnnotation(LexinfoOnt.animacy, LexinfoOnt.inanimate);
                group = group.replace("lub m??skorzeczowy", "");
            }
            if (group.contains("liczebnik")) {
                dpos.addAnnotation(LexinfoOnt.partOfSpeech, LexinfoOnt.numeral);
                group = group.replace("liczebnik", "");
            }

            if (group.contains("nieprzechodnia") || group.contains("nieprzechodni")) {
                dpos.addAnnotation(SynSemOnt.synBehavior, LexinfoOnt.IntransitiveFrame);
                group = group.replace("nieprzechodnia", "");
                group = group.replace("nieprzechodni", "");
            }
            if (group.contains("przechodnia") || group.contains("przechodni")) {
                dpos.addAnnotation(SynSemOnt.synBehavior, LexinfoOnt.TransitiveFrame);
                group = group.replace("przechodnia", "");
                group = group.replace("przechodni", "");
            }

            if (group.contains("niedokonany") || group.contains("niedokonana")) {
                dpos.addAnnotation(LexinfoOnt.aspect, LexinfoOnt.imperfective);
                group = group.replace("niedokonany", "");
                group = group.replace("niedokonana", "");
            }
            if (group.contains("dokonany") || group.contains("dokonana")) {
                dpos.addAnnotation(LexinfoOnt.aspect, LexinfoOnt.perfective);
                group = group.replace("dokonany", "");
                group = group.replace("dokonana", "");
            }
            if (group.contains("zwrotny")) {
                dpos.addAnnotation(SynSemOnt.synBehavior, LexinfoOnt.ReflexiveFrame);
                group = group.replace("zwrotny", "");
            }
            if (group.contains("liczba mnoga")) {
                dpos.addAnnotation(LexinfoOnt.number, LexinfoOnt.plural);
                group = group.replace("liczba mnoga", "");
            }
            if (group.contains("zbiorowy")) {
                dpos.addAnnotation(LexinfoOnt.number, LexinfoOnt.collective);
                group = group.replace("zbiorowy", "");
            }

            if (group.contains("w funkcji rzeczownika")) {
                dpos.addAnnotation(LexinfoOnt.partOfSpeech, LexinfoOnt.noun);
                group = group.replace("w funkcji rzeczownika", "");
            }

            if (group.contains("w u??ycu przys????wkowym")) {
                dpos.addAnnotation(LexinfoOnt.partOfSpeech, LexinfoOnt.adverb);
                group = group.replace("w u??ycu przys????wkowym", "");
            }
            if (group.contains("w u??yciu przymiotnikowym")) {
                dpos.addAnnotation(LexinfoOnt.partOfSpeech, LexinfoOnt.adjective);
                group = group.replace("w u??yciu przymiotnikowym", "");
            }
            if (group.contains("w u??yciu przymiotnikowym")) {
                dpos.addAnnotation(LexinfoOnt.partOfSpeech, LexinfoOnt.adjective);
                group = group.replace("w u??yciu przymiotnikowym", "");
            }
            if (group.contains("czynny")) {
                dpos.addAnnotation(LexinfoOnt.voice, LexinfoOnt.activeVoice);
                group = group.replace("czynny", "");
            }
            if (group.contains("bierny")) {
                dpos.addAnnotation(LexinfoOnt.voice, LexinfoOnt.passiveVoice);
                group = group.replace("bierny", "");
            }

            if (log.isDebugEnabled()) {
                group = group.replace("nazwa w??asna", "");
                group = group.replace("fraza rzeczownikowa", "");
                group = group.replace("fraza przymiotnikowa", "");
                group = group.replace("fraza czasownikowa", "");
                group = group.replace("fraza przys????wekowa", "");
                group = group.replace("fraza przys????wkowa", "");
                group = group.replace("fraza wykrzyknikowa", "");
                group = group.replace("fraza wykrzyknkowa", "");

                group = group.replace("imies????w przymiotnikowy", "");

                group = group.replace("liczebnik", "");
                group = group.replace("porz??dkowy", "");
                group = group.replace("mno??ny", "");
                group = group.replace("g????wny", "");
                group = group.replace("zbiorowy", "");

                group = group.replace("rzeczownik", "");
                group = group.replace("przymiotnik dzier??awczy", "");
                group = group.replace("przymiotnik", "");
                group = group.replace("czasownik modalny", "");
                group = group.replace("czasownik u??omny", "");
                group = group.replace("czasownik", "");
                group = group.replace("przys????wek", "");
                group = group.replace("zwi??zek frazeologiczny", "");
                group = group.replace("{{przys??owie polskie}}", "");
                group = group.replace("skr??towiec", "");
                group = group.replace("symbol", "");
                group = group.replace("zwrot", "");
                group = group.replace("temat s??owotw??rczy", "");
                group = group.replace("przyrostek", "");
                group = group.replace("przedrostek", "");
                group = group.replace("partyku??a", "");
                group = group.replace("przyimek", "");
                group = group.replace("sp??jnik", "");

                group = group.replaceAll("[\\.,\\?i/]", "");
                group = group.trim();

                // TODO: check if there are remaining elements in pos.
                if (group.length() > 0) log.debug("Did not parse {} in MorphoSyntactic info \"{}\"", group, orig);
            }
        } else if (group.contains("forma")) {
            return null;
        } else {
            return new DecodedPOS(group, null, OntolexOnt.LexicalEntry);
        }

        return dpos;
    }


    private DecodedPOS computeDecodedPOS(String group) {
        if (group.startsWith("rzeczownik")) {
            if (group.contains("nazwa w??asna"))
                //TODO: remove dependency to DBnaryModel
                return new DecodedPOS("rzeczownik_nazwa_w??asna", LexinfoOnt.properNoun, OntolexOnt.Word);
            else
                return new DecodedPOS("rzeczownik", LexinfoOnt.noun, OntolexOnt.Word);
        } else if (group.startsWith("przymiotnik dzier??awczy")) {
            DecodedPOS res = new DecodedPOS("przymiotnik_dzier??awczy", LexinfoOnt.possessiveAdjective, OntolexOnt.Word);
            res.addAnnotation(LexinfoOnt.partOfSpeech, LexinfoOnt.adjective);
            return res;
        } else if (group.startsWith("przymiotnik")) {
            return new DecodedPOS("przymiotnik", LexinfoOnt.adjective, OntolexOnt.Word);
        } else if (group.startsWith("czasownik modalny")) {
            DecodedPOS res = new DecodedPOS("czasownik_modalny", LexinfoOnt.modal, OntolexOnt.Word);
            res.addAnnotation(LexinfoOnt.partOfSpeech, LexinfoOnt.verb);
            return res;
        } else if (group.startsWith("czasownik u??omny")) {
            DecodedPOS res = new DecodedPOS("czasownik_modalny", LexinfoOnt.modal, OntolexOnt.Word);
            res.addAnnotation(LexinfoOnt.partOfSpeech, LexinfoOnt.deficientVerb);
            return res;
        } else if (group.startsWith("czasownik")) {
            return new DecodedPOS("czasownik", LexinfoOnt.verb, OntolexOnt.Word);
        } else if (group.startsWith("przys????wek")) {
            return new DecodedPOS("przys????wek", LexinfoOnt.adverb, OntolexOnt.Word);
        } else if (group.startsWith("fraza")) {
            if (group.contains("rzeczownikowa")) {
                if (group.contains("nazwa w??asna"))
                    return new DecodedPOS("rzeczownik_nazwa_w??asna", LexinfoOnt.properNoun, OntolexOnt.MultiWordExpression);
                else
                    return new DecodedPOS("rzeczownik", LexinfoOnt.noun, OntolexOnt.MultiWordExpression);
            } else if (group.contains("przymiotnikowa")) {
                return new DecodedPOS("przymiotnik", LexinfoOnt.adjective, OntolexOnt.MultiWordExpression);
            } else if (group.contains("czasownikowa")) {
                return new DecodedPOS("czasownik", LexinfoOnt.verb, OntolexOnt.MultiWordExpression);
            } else if (group.contains("przys????wekowa") || group.contains("przys????wkowa")) {
                return new DecodedPOS("przys????wek", LexinfoOnt.adverb, OntolexOnt.MultiWordExpression);
            } else if (group.contains("wykrzyknikowa") || group.contains("wykrzyknkowa")) {
                return new DecodedPOS("wykrzyknik", LexinfoOnt.interjection, OntolexOnt.MultiWordExpression);
            }
        } else if (group.startsWith("zwi??zek frazeologiczny")) {
            return new DecodedPOS("zwi??zek frazeologiczny", LexinfoOnt.idiom, OntolexOnt.MultiWordExpression);
        } else if (group.startsWith("{{przys??owie polskie")) {
            return new DecodedPOS("przys??owie", LexinfoOnt.proverb, OntolexOnt.MultiWordExpression);
        } else if (group.startsWith("skr??towiec")) {
            return new DecodedPOS("skr??towiec", LexinfoOnt.acronym, OntolexOnt.Word);
        } else if (group.startsWith("skr??t")) {
            return new DecodedPOS("skr??t", LexinfoOnt.abbreviation, OntolexOnt.Word);
        } else if (group.startsWith("zaimek")) {
            if (group.contains("pytajny")) {
                return new DecodedPOS("zaimek_pytajny", LexinfoOnt.interrogativePronoun, LexinfoOnt.Pronoun);
            } else if (group.contains("nieokre??lony")) {
                return new DecodedPOS("zaimek_nieokre??lony", LexinfoOnt.indefinitePronoun, LexinfoOnt.Pronoun);
            } else if (group.contains("osobowy")) {
                return new DecodedPOS("zaimek_osobowy", LexinfoOnt.personalPronoun, LexinfoOnt.Pronoun);
            } else {
                // TODO add other type of pronouns
                return new DecodedPOS("zaimek", LexinfoOnt.pronoun, LexinfoOnt.Pronoun);
            }
        } else if (group.startsWith("wykrzyknik")) {
            return new DecodedPOS("wykrzyknik", LexinfoOnt.interjection, LexinfoOnt.Interjection);
        } else if (group.startsWith("partyku??a")) {
            return new DecodedPOS("partyku??a", LexinfoOnt.particle, LexinfoOnt.Particle);
        } else if (group.startsWith("sp??jnik")) {
            return new DecodedPOS("sp??jnik", LexinfoOnt.conjunction, LexinfoOnt.Conjunction);
        } else if (group.startsWith("przyimek")) {
            return new DecodedPOS("przyimek", LexinfoOnt.preposition, LexinfoOnt.Preposition);
        } else if (group.startsWith("liczebnik")) {
            if (group.contains("porz??dkowy")) {
                return new DecodedPOS("liczebnik_porz??dkowy", LexinfoOnt.ordinalAdjective, OntolexOnt.Word);
            } else if (group.contains("mno??ny")) {
                return new DecodedPOS("liczebnik_mno??ny", LexinfoOnt.multiplicativeNumeral, LexinfoOnt.Numeral);
            } else if (group.contains("g????wny")) {
                return new DecodedPOS("liczebnik_g????wny", LexinfoOnt.cardinalNumeral, LexinfoOnt.Numeral);
            } else if (group.contains("zbiorowy")) {
                return new DecodedPOS("liczebnik_zbiorowy", LexinfoOnt.collective, LexinfoOnt.Numeral);
            } else if (group.contains("nieokre??lony")) {
                return new DecodedPOS("liczebnik_nieokre??lony", LexinfoOnt.indefiniteCardinalNumeral, LexinfoOnt.Numeral);
            } else if (group.contains("u??amkowy")) {
                return new DecodedPOS("liczebnik_u??amkowy", LexinfoOnt.numeralFraction, LexinfoOnt.Numeral);
            } else {
                // TODO add other type of pronouns
                return new DecodedPOS("liczebnik", LexinfoOnt.numeral, LexinfoOnt.Numeral);
            }
        } else if (group.startsWith("symbol")) {
            return new DecodedPOS("symbol", LexinfoOnt.symbol, LexinfoOnt.Symbol);
        } else if (group.startsWith("zwrot")) {
            return new DecodedPOS("zwrot", null, OntolexOnt.LexicalEntry);
        } else if (group.startsWith("temat s??owotw??rczy")) {
            return new DecodedPOS("temat s??owotw??rczy", null, OntolexOnt.Affix);
        } else if (group.startsWith("przyrostek")) {
            return new DecodedPOS("przyrostek", LexinfoOnt.suffix, LexinfoOnt.Suffix);
        } else if (group.startsWith("przedrostek")) {
            return new DecodedPOS("przedrostek", LexinfoOnt.prefix, LexinfoOnt.Prefix);
        } else if (group.startsWith("imies????w przymiotnikowy przesz??y")) {
            return new DecodedPOS("imies????w przymiotnikowy przesz??y", LexinfoOnt.pastParticipleAdjective, LexinfoOnt.Adjective);
        } else if (group.startsWith("imies????w przymiotnikowy")) {
            return new DecodedPOS("imies????w przymiotnikowy", LexinfoOnt.participleAdjective, LexinfoOnt.Adjective);
        }

        return null;
    }

    @Override
    public void addPartOfSpeech(String pos) {
        // TODO: normalize POS and register a new lexical entry using abbreviated pos id.
        // TODO: extract simplified POS then register all category informations
        // DONE: register the normalized POS.
        DecodedPOS dpos = decodePOS(pos);

        if (dpos != null)
            super.addPartOfSpeech(dpos.simplePOSName, dpos.lexinfoPOS, dpos.entryType);
        else {
            this.voidPartOfSpeech();
            log.debug("Could not register a POS for {}", pos);
        }
    }

    public void voidPartOfSpeech() {
        // DONE: create a LexicalEntry for this part of speech only and attach info to it.
        currentWiktionaryPos = null;
        currentLexinfoPos = null;

        currentEncodedPageName = null;
        currentLexEntry = null;

        currentSense = null;
    }

    public boolean posIsValid() {
        return currentWiktionaryPos != null;
    }

    // TODO : All lex entries precede the translation.
    // There is a need to attach the correct translations to the correct lex entry (and lex sense).
    public void registerTranslation(String lang, String currentGloss, String usage, String word) {

        if (null == currentGloss) {
            if (currentLexEntries.size() == 1) {
                // Only one entry, link the translation to it
                Resource entry = currentLexEntries.iterator().next();
                registerTranslationToEntity(entry, lang, null, usage, word);
            } else {
                // Forget this translation.
                log.debug("No gloss for a translation in a multi entry page: {}, {} : {} / {}", currentLexEntry(), lang, word, usage);
            }
        } else {
            // parse the gloss to get the sense number(s)
            ArrayList<String> numlist = getSenseNumbers(currentGloss);
            StructuredGloss sg = new StructuredGloss(numlist.toString(), null);
            Resource currentGlossResource = createGlossResource(sg);
            ArrayList<Resource[]> senseAndEntries = new ArrayList<Resource[]>();
            for (String n : numlist) {
                Resource[] se = currentWordsenses.get(n);
                if (se == null) {
                    log.debug("Could not fetch sense resource for {} in {}", n, currentLexEntry());
                } else {
                    senseAndEntries.add(se);
                }
            }
            Map<Resource, ArrayList<Resource>> sensesByEntry = new HashMap<Resource, ArrayList<Resource>>();
            for (Resource[] se : senseAndEntries) {
                Resource sense = se[0];
                Resource entry = se[1];

                ArrayList<Resource> senses = sensesByEntry.get(entry);
                if (senses == null) {
                    senses = new ArrayList<Resource>();
                }
                senses.add(sense);
                sensesByEntry.put(entry, senses);

            }
            for (Entry<Resource, ArrayList<Resource>> es : sensesByEntry.entrySet()) {
                // register definition to the currentLexEntry
                Resource trans = registerTranslationToEntity(es.getKey(), lang, currentGlossResource, usage, word);

                // add a reference to the correct word sense(s)
                if (null != trans) {
                    for (Resource s : es.getValue()) {
                        aBox.add(aBox.createStatement(trans, DBnaryOnt.isTranslationOf, s));
                    }
                }
            }
        }
    }

    // TODO : move into GlossFilter
    static Pattern range1 = Pattern.compile("(\\d+)\\.(\\d+)[\\-,??????]\\s*(\\d+)");
    static Pattern range2 = Pattern.compile("(\\d+)[\\-??????](\\d+)");
//	static Pattern range3 = Pattern.compile("(\\d+)\\.(\\d+)[\\-??????](\\d+)\\.(\\d+)");

    public ArrayList<String> getSenseNumbers(String nums) {
        ArrayList<String> ns = new ArrayList<String>();
        nums = nums.trim();
        Matcher mRange1 = range1.matcher(nums);
        Matcher mRange2 = range2.matcher(nums);
//		Matcher mRange3 = range3.matcher(nums);

        if (nums.matches("\\d+")) {
            // When there is only one number, we attach to all wordsense whose prefix is the given number
            for (String n : currentWordsenses.keySet()) {
                if (n.startsWith(nums + ".")) {
                    ns.add(n);
                }
            }
        } else if (nums.matches("\\d+\\.")) {
            // When there is only one number, we attach to all wordsense whose prefix is the given number
            for (String n : currentWordsenses.keySet()) {
                if (n.startsWith(nums)) {
                    ns.add(n);
                }
            }
        } else if (mRange1.matches()) {
            String n1 = mRange1.group(1);
            String n2 = mRange1.group(2);
            String n3 = mRange1.group(3);

            try {
                int i2 = Integer.parseInt(n2);
                int i3 = Integer.parseInt(n3);

                if (i2 < i3) {
                    for (int i = i2; i <= i3; i++) {
                        ns.add(n1 + "." + i);
                    }
                } else {
                    log.debug("Invalid range: {} in {}", nums, currentLexEntry());
                    ns.add(nums);
                }
            } catch (NumberFormatException e) {
                log.debug(e.getLocalizedMessage());
                ns.add(nums);
            }
        } else if (mRange2.matches()) {
            String n1 = mRange2.group(1);
            String n2 = mRange2.group(2);

            try {
                int i1 = Integer.parseInt(n1);
                int i2 = Integer.parseInt(n2);

                if (i1 < i2) {
                    for (int i = i1; i <= i2; i++) {
                        // When there is only one number, we attach to all wordsense whose prefix is the given number
                        for (String n : currentWordsenses.keySet()) {
                            if (n.startsWith(nums + ".")) {
                                ns.add(n);
                            }
                        }
                    }
                } else {
                    log.debug("Invalide range: {} in {}", nums, currentLexEntry());
                    ns.add(nums);
                }
            } catch (NumberFormatException e) {
                log.debug(e.getLocalizedMessage());
                ns.add(nums);
            }
//		} else if (mRange3.matches()) {
//			String n1 = mRange3.group(1);
//			String n11 = mRange3.group(2);
//			String n2 = mRange3.group(3);
//			String n22 = mRange3.group(4);
//
//			try {
//				int i1 = Integer.parseInt(n1);
//				int i11 = Integer.parseInt(n11);
//				int i2 = Integer.parseInt(n2);
//				int i22 = Integer.parseInt(n22);
//				
//				if (i1 < i2) {
//					for (int i = i1; i <= i2; i++) {
//						// When there is only one number, we attach to all wordsense whose prefix is the given number
//						for (String n : currentWordsenses.keySet()) {
//							if (n.startsWith(nums + ".")) {
//								ns.add(n);
//							}
//						}
//					}
//				} else {
//					log.debug("Invalide range: {} in {}", nums, currentLexEntry());
//					ns.add(nums);
//				}
//			} catch (NumberFormatException e) {
//				log.debug(e.getLocalizedMessage());
//				ns.add(nums);
//			}
        } else if (nums.contains(",")) {
            String[] list = nums.split(",");
            for (String n : list) {
                if (n != null && !(n = n.trim()).equals("")) {
                    ns.addAll(getSenseNumbers(n));
                }
            }
        } else {
            ns.add(nums);
        }
        return ns;
    }

}
