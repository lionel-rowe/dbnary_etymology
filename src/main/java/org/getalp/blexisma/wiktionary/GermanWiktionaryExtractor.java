package org.getalp.blexisma.wiktionary;

import java.io.PrintStream;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.getalp.blexisma.api.ISO639_3;
import org.getalp.blexisma.semnet.SimpleSemanticNetwork;

public class GermanWiktionaryExtractor extends WiktionaryExtractor {

    protected final static String languageSectionPatternString = "={2}\\s*([^\\(]*)\\(\\{\\{Sprache\\|([^\\}]*)\\}\\}\\s*\\)\\s*={2}";
    protected final static String partOfSpeechPatternString = "={3}[^\\{]*\\{\\{Wortart\\|([^\\}\\|]*)(?:\\|([^\\}]*))?\\}\\}.*={3}";
    protected final static String subSection4PatternString = "={4}\\s*(.*)\\s*={4}";
    protected final static String germanDefinitionPatternString = "^:{1,3}\\[[^\\]]*]\\s*(.*)$";
    /**
	 * @uml.property  name="nODATA"
	 */
    private final int NODATA = 0;
    /**
	 * @uml.property  name="tRADBLOCK"
	 */
    private final int TRADBLOCK = 1;
    /**
	 * @uml.property  name="dEFBLOCK"
	 */
    private final int DEFBLOCK = 2;
    /**
	 * @uml.property  name="oRTHOALTBLOCK"
	 */
    private final int ORTHOALTBLOCK = 3;
    /**
	 * @uml.property  name="nYMBLOCK"
	 */
    private final int NYMBLOCK = 4;

    public GermanWiktionaryExtractor(WiktionaryDataHandler wdh) {
        super(wdh);
    }

    // protected final static Pattern languageSectionPattern;
    protected final static String macroOrPOSPatternString;
    protected final static Pattern languageSectionPattern;
    protected final static Pattern germanDefinitionPattern;
    protected final static String multilineMacroPatternString;
    protected final static Pattern macroOrPOSPattern; // Combine macro pattern
                                                      // and pos pattern.
    protected final static HashSet<String> posMarkers;
    protected final static HashSet<String> ignorableSectionMarkers;
    protected final static HashSet<String> nymMarkers;
    protected final static HashMap<String, String> nymMarkerToNymName;

    static {
        // languageSectionPattern =
        // Pattern.compile(languageSectionPatternString);

        languageSectionPattern = Pattern.compile(languageSectionPatternString);
        multilineMacroPatternString = 
            new StringBuilder().append("\\{\\{")
            .append("([^\\}\\|]*)(?:\\|([^\\}]*))?")
            .append("\\}\\}")
            .toString();
        
        macroOrPOSPatternString = new StringBuilder()
        	.append("(?:").append(macroPatternString)
        	.append(")|(?:").append(partOfSpeechPatternString)
        	.append(")|(?:").append(subSection4PatternString).append(")")
            .append("|(?:").append(multilineMacroPatternString).append(")")
            .toString();

        macroOrPOSPattern = Pattern.compile(macroOrPOSPatternString);
        germanDefinitionPattern = Pattern.compile(germanDefinitionPatternString, Pattern.MULTILINE);

        posMarkers = new HashSet<String>(20);
        posMarkers.add("Substantiv"); // Should I get the
                                      // Toponym/Vorname/Nachname additional
                                      // info ?
        posMarkers.add("Adjektiv");
        posMarkers.add("Absolutadjektiv");
        posMarkers.add("Partizip");
        posMarkers.add("Adverb");
        posMarkers.add("Wortverbindung");
        posMarkers.add("Verb");

        ignorableSectionMarkers = new HashSet<String>(20);
        ignorableSectionMarkers.add("Silbentrennung");
        ignorableSectionMarkers.add("Aussprache");
        ignorableSectionMarkers.add("Herkunft");
        ignorableSectionMarkers.add("Gegenworte");
        ignorableSectionMarkers.add("Beispiele");
        ignorableSectionMarkers.add("Redewendungen");
        ignorableSectionMarkers.add("Abgeleitete Begriffe");
        ignorableSectionMarkers.add("Charakteristische Wortkombinationen");
        ignorableSectionMarkers.add("Dialektausdrücke (Deutsch)");
        ignorableSectionMarkers.add("Referenzen");
        ignorableSectionMarkers.add("Ähnlichkeiten");
        ignorableSectionMarkers.add("Anmerkung");
        ignorableSectionMarkers.add("Alte Rechtschreibung"); // TODO: Integrate
                                                             // these in
                                                             // alternative
                                                             // spelling ?
        ignorableSectionMarkers.add("Nebenformen");
        ignorableSectionMarkers.add("Vokalisierung");
        ignorableSectionMarkers.add("Grammatische Merkmale");
        ignorableSectionMarkers.add("Abkürzungen"); // TODO: Integrate these in
                                                    // alternative spelling ?
        ignorableSectionMarkers.add("Sinnverwandte Wörter"); // TODO: related
                                                             // words (should I
                                                             // keep it ?)
        ignorableSectionMarkers.add("Weibliche Wortformen");
        ignorableSectionMarkers.add("Männliche Wortformen");
        ignorableSectionMarkers.add("Verkleinerungsformen"); // TODO:
                                                             // Diminutif...
                                                             // qu'en faire ?
        ignorableSectionMarkers.add("Vergrößerungsformen");
        ignorableSectionMarkers.add("Kurzformen");
        ignorableSectionMarkers.add("Koseformen");
        ignorableSectionMarkers.add("Kurz- und Koseformen");
        ignorableSectionMarkers.add("Namensvarianten");
        ignorableSectionMarkers.add("Weibliche Namensvarianten");
        ignorableSectionMarkers.add("Männliche Namensvarianten");
        ignorableSectionMarkers.add("Bekannte Namensträger");
        ignorableSectionMarkers.add("Sprichwörter");
        ignorableSectionMarkers.add("Charakteristische Wortkombinationen");
        ignorableSectionMarkers.add("Abgeleitete Begriffe");

        nymMarkers = new HashSet<String>(20);
        nymMarkers.add("Synonyme");
        nymMarkers.add("Gegenwörter");
        nymMarkers.add("Gegenworte");
        nymMarkers.add("Oberbegriffe");
        nymMarkers.add("Unterbegriffe");
        nymMarkers.add("Meronyms"); // TODO: Any meronym/metonym info in German
                                    // ?

        nymMarkerToNymName = new HashMap<String, String>(20);
        nymMarkerToNymName.put("Synonyme", "syn");
        nymMarkerToNymName.put("Gegenwörter", "ant");
        nymMarkerToNymName.put("Gegenworte", "ant");
        nymMarkerToNymName.put("Unterbegriffe", "hypo");
        nymMarkerToNymName.put("Oberbegriffe", "hyper");
        nymMarkerToNymName.put("Meronyms", "mero");

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.getalp.blexisma.wiktionary.WiktionaryExtractor#extractData(java.lang
     * .String, org.getalp.blexisma.semnet.SemanticNetwork)
     */
    @Override
    public void extractData() {

        // System.out.println(pageContent);
        Matcher languageFilter = languageSectionPattern.matcher(pageContent);
        while (languageFilter.find() && !languageFilter.group(2).equals("Deutsch")) {
            ;
        }
        // Either the filter is at end of sequence or on German language header.
        if (languageFilter.hitEnd()) {
            // There is no German data in this page.
            return;
        }
        int germanSectionStartOffset = languageFilter.end();
        // Advance till end of sequence or new language section
        // WHY filter on section level ?: while (languageFilter.find() && (languageFilter.start(1) - languageFilter.start()) != 2) {
        languageFilter.find();
        // languageFilter.find();
        int germanSectionEndOffset = languageFilter.hitEnd() ? pageContent.length() : languageFilter.start();

        extractGermanData(germanSectionStartOffset, germanSectionEndOffset);
    }

    /**
	 * @uml.property  name="state"
	 */
    int state = NODATA;
    /**
	 * @uml.property  name="definitionBlockStart"
	 */
    int definitionBlockStart = -1;
    /**
	 * @uml.property  name="orthBlockStart"
	 */
    int orthBlockStart = -1;
    /**
	 * @uml.property  name="translationBlockStart"
	 */
    int translationBlockStart = -1;
    /**
	 * @uml.property  name="nymBlockStart"
	 */
    private int nymBlockStart = -1;
    /**
	 * @uml.property  name="currentNym"
	 * @uml.associationEnd  qualifier="key:java.lang.String java.lang.String"
	 */
    private String currentNym = null;

    void gotoNoData(Matcher m) {
        state = NODATA;
    }

    void gotoTradBlock(Matcher m) {
        translationBlockStart = m.end();
        state = TRADBLOCK;
    }

    void registerNewPartOfSpeech(Matcher m) {
    	// if (m.group(4) != null && ! m.group(4).equals("Deutsch"))
    	//	System.err.println("lang = " + m.group(4) + " where pos = " + m.group(3) + " in page: " + wiktionaryPageName);
    	// TODO: language in group 4 is the language of origin of the entry. Maybe we should keep it.
    	// TODO: filter out ignorable part of speech;
        wdh.addPartOfSpeech(m.group(3));
    }

    void gotoDefBlock(Matcher m) {
        state = DEFBLOCK;
        definitionBlockStart = m.end();
    }

    void gotoOrthoAltBlock(Matcher m) {
        state = ORTHOALTBLOCK;
        orthBlockStart = m.end();
    }

    void leaveDefBlock(Matcher m) {
        extractDefinitions(definitionBlockStart, computeRegionEnd(definitionBlockStart, m));
        definitionBlockStart = -1;
    }

    void leaveTradBlock(Matcher m) {
        extractTranslations(translationBlockStart, computeRegionEnd(translationBlockStart, m));
        translationBlockStart = -1;
    }

    void leaveOrthoAltBlock(Matcher m) {
        extractOrthoAlt(orthBlockStart, computeRegionEnd(orthBlockStart, m));
        orthBlockStart = -1;
    }

    private void gotoNymBlock(Matcher m) {
        state = NYMBLOCK;
        currentNym = nymMarkerToNymName.get(m.group(1));
        nymBlockStart = m.end();
    }

    private void leaveNymBlock(Matcher m) {
        extractNyms(currentNym, nymBlockStart, computeRegionEnd(nymBlockStart, m));
        currentNym = null;
        nymBlockStart = -1;
    }

    // TODO: Prise en compte des diminutifs (Verkleinerungsformen)
    // TODO: Prise en compte des "concepts dérivés" ? (Abgeleitete Begriffe)
    private void extractGermanData(int startOffset, int endOffset) {
        Matcher m = macroOrPOSPattern.matcher(pageContent);
        m.region(startOffset, endOffset);
        wdh.initializeEntryExtraction(wiktionaryPageName);
        gotoNoData(m);
        while (m.find()) {
            switch (state) {
            case NODATA:
                if (m.group(1) != null) {
                    // It's a macro
                    if (m.group(1).equals("Bedeutungen")) {
                        // Definitions
                        gotoDefBlock(m);
                    } else if (m.group(1).equals("Alternative Schreibweisen")) {
                        // Alternative spelling
                        gotoOrthoAltBlock(m);
                    } else if (nymMarkers.contains(m.group(1))) {
                        // Nyms
                        gotoNymBlock(m);
                    } else if (ignorableSectionMarkers.contains(m.group(1))) {
                        gotoNoData(m);
                    }
                } else if (m.group(3) != null) {
                    // partOfSpeech
                    registerNewPartOfSpeech(m);
                } else if (m.group(5) != null) {
                    // translations
                    if (m.group(5).trim().equals("Übersetzungen")) {
                        gotoTradBlock(m);
                    }
                } else {
                	// Multiline macro
                	// System.out.println(m.group());
                }

                break;
            case DEFBLOCK:
                if (m.group(1) != null) {
                    // It's a macro
                    if (m.group(1).equals("Bedeutungen")) {
                        // Definitions
                        leaveDefBlock(m);
                        gotoDefBlock(m);
                    } else if (m.group(1).equals("Alternative Schreibweisen")) {
                        // Alternative spelling
                        leaveDefBlock(m);
                        gotoOrthoAltBlock(m);
                    } else if (nymMarkers.contains(m.group(1))) {
                        // Nyms
                        leaveDefBlock(m);
                        gotoNymBlock(m);
                    } else if (ignorableSectionMarkers.contains(m.group(1))) {
                        leaveDefBlock(m);
                        gotoNoData(m);
                    }
                } else if (m.group(3) != null) {
                    // partOfSpeech
                    leaveDefBlock(m);
                    registerNewPartOfSpeech(m);
                    gotoNoData(m);
                } else if (m.group(5) != null) {
                    // translations
                    if (m.group(5).trim().equals("Übersetzungen")) {
                        leaveDefBlock(m);
                        gotoTradBlock(m);
                    }
                } else {
                	// Multiline macro
                	// System.out.println(m.group());
                }

                break;
            case TRADBLOCK:
                if (m.group(1) != null) {
                    // It's a macro
                    if (m.group(1).equals("Bedeutungen")) {
                        // Definitions
                        leaveTradBlock(m);
                        gotoDefBlock(m);
                    } else if (m.group(1).equals("Alternative Schreibweisen")) {
                        // Alternative spelling
                        leaveTradBlock(m);
                        gotoOrthoAltBlock(m);
                    } else if (nymMarkers.contains(m.group(1))) {
                        // Nyms
                        leaveTradBlock(m);
                        gotoNymBlock(m);
                    } else if (ignorableSectionMarkers.contains(m.group(1))) {
                        leaveTradBlock(m);
                        gotoNoData(m);
                    }
                } else if (m.group(3) != null) {
                    // partOfSpeech
                    leaveTradBlock(m);
                    registerNewPartOfSpeech(m);
                    gotoNoData(m);
                } else if (m.group(5) != null) {
                    // translations
                    if (m.group(5).trim().equals("Übersetzungen")) {
                        leaveTradBlock(m);
                        gotoTradBlock(m);
                    }
                } else {
                	// Multiline macro
                	// System.out.println(m.group());
                }

                break;
            case ORTHOALTBLOCK:
                if (m.group(1) != null) {
                    // It's a macro
                    if (m.group(1).equals("Bedeutungen")) {
                        // Definitions
                        leaveOrthoAltBlock(m);
                        gotoDefBlock(m);
                    } else if (m.group(1).equals("Alternative Schreibweisen")) {
                        // Alternative spelling
                        leaveOrthoAltBlock(m);
                        gotoOrthoAltBlock(m);
                    } else if (nymMarkers.contains(m.group(1))) {
                        // Nyms
                        leaveOrthoAltBlock(m);
                        gotoNymBlock(m);
                    } else if (ignorableSectionMarkers.contains(m.group(1))) {
                        leaveOrthoAltBlock(m);
                        gotoNoData(m);
                    }
                } else if (m.group(3) != null) {
                    // partOfSpeech
                    leaveOrthoAltBlock(m);
                    registerNewPartOfSpeech(m);
                    gotoNoData(m);
                } else if (m.group(5) != null) {
                    // translations
                    if (m.group(5).trim().equals("Übersetzungen")) {
                        leaveOrthoAltBlock(m);
                        gotoTradBlock(m);
                    }
                } else {
                	// Multiline macro
                	// System.out.println(m.group());
                }

                break;
            case NYMBLOCK:
                // ICI
                if (m.group(1) != null) {
                    // It's a macro
                    if (m.group(1).equals("Bedeutungen")) {
                        // Definitions
                        leaveNymBlock(m);
                        gotoDefBlock(m);
                    } else if (m.group(1).equals("Alternative Schreibweisen")) {
                        // Alternative spelling
                        leaveNymBlock(m);
                        gotoOrthoAltBlock(m);
                    } else if (nymMarkers.contains(m.group(1))) {
                        // Nyms
                        leaveNymBlock(m);
                        gotoNymBlock(m);
                    } else if (ignorableSectionMarkers.contains(m.group(1))) {
                        leaveNymBlock(m);
                        gotoNoData(m);
                    }
                } else if (m.group(3) != null) {
                    // partOfSpeech
                    leaveNymBlock(m);
                    registerNewPartOfSpeech(m);
                    gotoNoData(m);
                } else if (m.group(5) != null) {
                    // translations
                    if (m.group(5).trim().equals("Übersetzungen")) {
                        leaveNymBlock(m);
                        gotoTradBlock(m);
                    }
                } else {
                	// Multiline macro
                	// System.out.println(m.group());
                }

            default:
                assert false : "Unexpected state while extracting translations from dictionary.";
            }
        }
        // Finalize the entry parsing
        switch (state) {
        case NODATA:
            break;
        case DEFBLOCK:
            leaveDefBlock(m);
            break;
        case TRADBLOCK:
            leaveTradBlock(m);
            break;
        case ORTHOALTBLOCK:
            leaveOrthoAltBlock(m);
            break;
        case NYMBLOCK:
            leaveNymBlock(m);
            break;
        default:
            assert false : "Unexpected state while extracting translations from dictionary.";
        }
        wdh.finalizeEntryExtraction();
    }

    private void extractTranslations(int startOffset, int endOffset) {
        Matcher macroMatcher = macroPattern.matcher(pageContent);
        macroMatcher.region(startOffset, endOffset);
        String currentGlose = null;

        while (macroMatcher.find()) {
            String g1 = macroMatcher.group(1);

            if (g1.equals("Ü") || g1.equals("Üxx")) {
                // DONE: Sometimes translation links have a remaining info after
                // the word, keep it.
                // TODO: German wiktionary provides a word sense number for
                // translation. Keep it.
                String g2 = macroMatcher.group(2);
                int i1, i2;
                String lang, word;
                if (g2 != null && (i1 = g2.indexOf('|')) != -1) {
                    lang = g2.substring(0, i1);
                    // normalize language code
                    String normLangCode;
                    if ((normLangCode = ISO639_3.sharedInstance.getIdCode(lang)) != null) {
                        lang = normLangCode;
                    }
                    String transcription = null;
                    if ((i2 = g2.indexOf('|', i1 + 1)) == -1) {
                        word = g2.substring(i1 + 1);
                    } else {
                        transcription = g2.substring(i1 + 1, i2);
                        word = g2.substring(i2 + 1);
                    }
                    // TODO: Should I keep the transcription ?
                    wdh.registerTranslation(lang, currentGlose, transcription, word);
                }
            } else if (g1.equals("Ü-links")) {
                // German wiktionary does not provide a glose to disambiguate.
                // Just ignore this marker.
            } else if (g1.equals("Ü-Abstand")) {
                // just ignore it
            } else if (g1.equals("Ü-rechts")) {
                // Forget the current glose
                currentGlose = null;
            }
        }
    }

    @Override
    protected void extractDefinitions(int startOffset, int endOffset) {
    	// TODO: The definition pattern is the only one that changes. Hence, we should normalize this processing and put the macro in language specific parameters.
    	Matcher definitionMatcher = germanDefinitionPattern.matcher(this.pageContent);
        definitionMatcher.region(startOffset, endOffset);
        while (definitionMatcher.find()) {
            String def = cleanUpMarkup(definitionMatcher.group(1));
            if (def != null && !def.equals("")) {
            	wdh.registerNewDefinition(definitionMatcher.group(1));
            }
        }
    }
    


}