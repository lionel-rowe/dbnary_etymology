package org.getalp.dbnary.experiment.disambiguation;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.sparql.pfunction.library.concat;
import com.hp.hpl.jena.vocabulary.RDF;
import com.wcohen.ss.ScaledLevenstein;
import org.getalp.dbnary.DbnaryModel;
import org.getalp.dbnary.experiment.similarity.string.TverskiIndex;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TransitiveTranslationClosireDisambiguationMethod implements
        DisambiguationMethod {

    int degree = 0;

    {
        wordDistribution = new HashMap<>();
    }

    private TverskiIndex tversky = new TverskiIndex(.1, .9, true, false, new ScaledLevenstein());
    private Map<String, Double> wordDistribution;
    private Map<String, Model> models;
    private String lang;

    public TransitiveTranslationClosireDisambiguationMethod(int degree, String lang, Map<String, Model> models) {
        this.degree = degree;
        this.models = models;
        this.lang = lang;
    }

    private String getTranslationLanguage(String uri) {
        String lang = "";
        Pattern lp = Pattern.compile(".*__tr_(...)_[0-9].*");
        Matcher lm = lp.matcher(uri);
        if (lm.find()) {
            lang = lm.group(1);
        }
        return lang;
    }

    StmtIterator getTranslationLexicalEntryStmtIterator(Resource translation, String currentLang) {
        String writtenForm = translation.getProperty(DbnaryModel.equivalentTargetProperty).getObject().toString();
        String uri = DbnaryModel.DBNARY_NS_PREFIX + "/" + currentLang + "/" + DbnaryModel.uriEncode(writtenForm).split("@")[0];
        Resource r = models.get(currentLang).getResource(uri);
        return models.get(currentLang).listStatements(r, DbnaryModel.refersTo, (RDFNode) null);
    }

    private List<String> computeTranslationClosure(Resource translation, String pos, int degree) {
        String topLevelLang = lang;
        String currentLang = getTranslationLanguage(translation.getURI());
        List<String> output = new ArrayList<>();
        if (degree != 0 && models.containsKey(currentLang)) {
            StmtIterator lexEntries = getTranslationLexicalEntryStmtIterator(translation, currentLang);
            while (lexEntries.hasNext()) {
                Statement lnext = lexEntries.next();
                Statement stmtPos = lnext.getObject().asResource().getProperty(DbnaryModel.posProperty);
                String foreignpos = null;
                if (stmtPos != null) {
                    foreignpos = stmtPos.getObject().toString();
                }

                if (pos == null || (pos != null && foreignpos != null && pos.equals(foreignpos))) {
                    RDFNode lexEntryNode = lnext.getObject();
//                    System.out.println("\t ->" + lexEntryNode);
                    //Find translations pointing back to top level lang
                    StmtIterator trans = models.get(currentLang).listStatements(lexEntryNode.asResource(), DbnaryModel.isTranslationOf, (RDFNode) null);
                    while (trans.hasNext()) {
                        Statement ctransstmt = trans.next();
                        Resource ctrans = ctransstmt.getSubject();
                        System.out.println(ctrans.getURI());
                        Statement l = ctrans.getProperty(DbnaryModel.targetLanguageCodeProperty);
                        if (l != null && l.getString().equals(topLevelLang)) { // Back to topLevel
                            StmtIterator backLex = getTranslationLexicalEntryStmtIterator(ctrans, l.getString());
                            while (backLex.hasNext()) {
                                Statement backLexnext = backLex.next();
                                Statement lexcfp = backLexnext.getObject().asResource().getProperty(DbnaryModel.canonicalFormProperty);
                                output.add(lexcfp.getObject().toString());
                                //Iterating senses
                                StmtIterator senses = backLexnext.getObject().asResource().listProperties(DbnaryModel.lemonSenseProperty);
                                while (senses.hasNext()) {
                                    Statement nextSense = senses.next();
                                    Resource wordsense = nextSense.getResource();
                                    Statement dRef = wordsense.getProperty(DbnaryModel.lemonDefinitionProperty);
                                    Statement dVal = dRef.getProperty(DbnaryModel.lemonValueProperty);
                                    String deftext = dVal.getObject().toString();
                                    output.add(deftext);
                                }
                            }
                        } else if(degree>0){ //Recurse away!
                            output.addAll(computeTranslationClosure(ctrans, pos, degree--));
                        }
                    }
                }
            }

        }
        return output;
    }

    @Override
    public Set<Resource> selectWordSenses(Resource lexicalEntry,
                                          Object context) throws InvalidContextException,
            InvalidEntryException {
        HashSet<Resource> res = new HashSet<Resource>();

        if (!lexicalEntry.hasProperty(RDF.type, DbnaryModel.lexEntryType) && !lexicalEntry.hasProperty(RDF.type, DbnaryModel.wordEntryType))
            throw new InvalidEntryException("Expecting a LEMON Lexical Entry.");
        if (context instanceof Resource) {
            Resource trans = (Resource) context;
            if (!trans.hasProperty(RDF.type, DbnaryModel.translationType))
                throw new InvalidContextException("Expecting a DBnary Translation Resource.");
            List<String> closure = computeTranslationClosure(trans, null, this.degree);
            StringBuilder concatAll = new StringBuilder();
            for(String item: closure){
                concatAll.append(item);
            }
            ArrayList<WeigthedSense> weightedList = new ArrayList<WeigthedSense>();

            StmtIterator senses = lexicalEntry.listProperties(DbnaryModel.lemonSenseProperty);
            while (senses.hasNext()) {
                Statement nextSense = senses.next();
                Resource wordsense = nextSense.getResource();
                Statement dRef = wordsense.getProperty(DbnaryModel.lemonDefinitionProperty);
                Statement dVal = dRef.getProperty(DbnaryModel.lemonValueProperty);
                String deftext = dVal.getObject().toString();
                double sim = tversky.compute(deftext, concatAll.toString());
                insert(weightedList, wordsense, sim);
            }

        } else {
            throw new InvalidContextException("Expecting a JENA Resource.");
        }

        return res;
    }

    private void insert(ArrayList<WeigthedSense> weightedList,
                        Resource wordsense, double sim) {
        weightedList.add(null);
        int i = weightedList.size() - 1;
        while (i != 0 && weightedList.get(i - 1).weight < sim) {
            weightedList.set(i, weightedList.get(i - 1));
            i--;
        }
        weightedList.set(i, new WeigthedSense(sim, wordsense));
    }

    private class WeigthedSense {
        protected double weight;
        protected Resource sense;

        public WeigthedSense(double weight, Resource sense) {
            super();
            this.weight = weight;
            this.sense = sense;
        }
    }

}