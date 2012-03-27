/**
 * 
 */
package org.getalp.blexisma.wiktionary;

import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.getalp.blexisma.api.ISO639_3;

/**
 * @author serasset
 *
 */
public class FrenchWiktionaryExtractor extends WiktionaryExtractor {

    protected final static String languageSectionPatternString = "==\\s*\\{\\{=([^=]*)=\\}\\}\\s*==";
    
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
    protected final int DEFBLOCK = 2;
    /**
	 * @uml.property  name="oRTHOALTBLOCK"
	 */
    private final int ORTHOALTBLOCK = 3;
    /**
	 * @uml.property  name="nYMBLOCK"
	 */
    private final int NYMBLOCK = 4;
	/**
	 * @uml.property  name="iGNOREPOS"
	 */
	private final int IGNOREPOS = 5;

    private static HashSet<String> posMarkers;
    private static HashSet<String> ignorablePosMarkers;
    private static HashSet<String> sectionMarkers;
    private static HashSet<String> nymMarkers;
    
    private final static HashMap<String, String> nymMarkerToNymName;
    
    private static HashSet<String> unsupportedMarkers = new HashSet<String>();
    
    // private static Set<String> affixesToDiscardFromLinks = null;
    
    static {
    	
        posMarkers = new HashSet<String>(130);
        ignorablePosMarkers = new HashSet<String>(130);

        posMarkers.add("-déf-");
        posMarkers.add("-déf-/2");
        posMarkers.add("-déf2-");
        posMarkers.add("--");
        posMarkers.add("-adj-");
        posMarkers.add("-adj-/2");
        ignorablePosMarkers.add("-flex-adj-indéf-");
        posMarkers.add("-adj-dém-");
        posMarkers.add("-adj-excl-");
        posMarkers.add("-adj-indéf-");
        posMarkers.add("-adj-int-");
        posMarkers.add("-adj-num-");
        posMarkers.add("-adj-pos-");
        posMarkers.add("-adv-");
        posMarkers.add("-adv-int-");
        posMarkers.add("-adv-pron-");
        posMarkers.add("-adv-rel-");
        posMarkers.add("-aff-");
        posMarkers.add("-art-");
        ignorablePosMarkers.add("-flex-art-déf-");
        ignorablePosMarkers.add("-flex-art-indéf-");
        ignorablePosMarkers.add("-flex-art-part-");
        posMarkers.add("-art-déf-");
        posMarkers.add("-art-indéf-");
        posMarkers.add("-art-part-");
        posMarkers.add("-aux-");
        posMarkers.add("-circonf-");
        posMarkers.add("-class-");
        posMarkers.add("-cpt-");
        posMarkers.add("-conj-");
        posMarkers.add("-conj-coord-");
        posMarkers.add("-cont-");
        posMarkers.add("-copule-");
        posMarkers.add("-corrélatif-");
        posMarkers.add("-erreur-");
        posMarkers.add("-faux-prov-");
        ignorablePosMarkers.add("-flex-adj-");
        ignorablePosMarkers.add("-flex-adj-num-");
        ignorablePosMarkers.add("-flex-adj-pos-");
        ignorablePosMarkers.add("-flex-adv-");
        ignorablePosMarkers.add("-flex-art-");
        ignorablePosMarkers.add("-flex-aux-");
        ignorablePosMarkers.add("-flex-conj-");
        ignorablePosMarkers.add("-flex-interj-");
        ignorablePosMarkers.add("-flex-lettre-");
        ignorablePosMarkers.add("-flex-loc-adj-");
        ignorablePosMarkers.add("-flex-loc-conj-");
        ignorablePosMarkers.add("-flex-loc-nom-");
        ignorablePosMarkers.add("-flex-loc-verb-");
        ignorablePosMarkers.add("-flex-nom-");
        ignorablePosMarkers.add("-flex-nom-fam-");
        ignorablePosMarkers.add("-flex-nom-pr-");
        ignorablePosMarkers.add("-flex-mots-diff-");
        ignorablePosMarkers.add("-flex-prénom-");
        ignorablePosMarkers.add("-flex-prép-");
        ignorablePosMarkers.add("-flex-pronom-");
        ignorablePosMarkers.add("-flex-pronom-indéf-");
        ignorablePosMarkers.add("-flex-pronom-int-");
        ignorablePosMarkers.add("-flex-pronom-pers-");
        ignorablePosMarkers.add("-flex-pronom-rel-");
        ignorablePosMarkers.add("-flex-verb-");
        ignorablePosMarkers.add("-inf-");
        posMarkers.add("-interf-");
        posMarkers.add("-interj-");
        posMarkers.add("-lettre-");
        posMarkers.add("-loc-");
        posMarkers.add("-loc-adj-");
        posMarkers.add("-loc-adv-");
        posMarkers.add("-loc-conj-");
        posMarkers.add("-loc-dét-");
        posMarkers.add("-loc-interj-");
        posMarkers.add("-loc-nom-");
        posMarkers.add("-loc-phr-");
        posMarkers.add("-loc-post-");
        posMarkers.add("-loc-prép-");
        posMarkers.add("-loc-pronom-");
        posMarkers.add("-loc-verb-");
        posMarkers.add("-nom-");
        posMarkers.add("-nom-fam-");
        posMarkers.add("-nom-ni-");
        posMarkers.add("-nom-nu-");
        posMarkers.add("-nom-nn-");
        posMarkers.add("-nom-npl-");
        posMarkers.add("-nom-pr-");
        posMarkers.add("-nom-sciences-");
        posMarkers.add("-numér-");
        posMarkers.add("-onoma-");
        posMarkers.add("-part-");
        posMarkers.add("-post-");
        posMarkers.add("-préf-");
        posMarkers.add("-prénom-");
        posMarkers.add("-prép-");
        posMarkers.add("-pronom-");
        posMarkers.add("-pronom-adj-");
        posMarkers.add("-pronom-dém-");
        posMarkers.add("-pronom-indéf-");
        posMarkers.add("-pronom-int-");
        posMarkers.add("-pronom-pers-");
        posMarkers.add("-pronom-pos-");
        posMarkers.add("-pronom-rel-");
        posMarkers.add("-prov-");
        posMarkers.add("-racine-");
        posMarkers.add("-radical-");
        posMarkers.add("-rimes-");
        posMarkers.add("-signe-");
        posMarkers.add("-sin-");
        posMarkers.add("-subst-pron-pers-");
        ignorablePosMarkers.add("-suf-");
        ignorablePosMarkers.add("-flex-suf-");
        ignorablePosMarkers.add("-symb-");
        posMarkers.add("type");
        posMarkers.add("-var-typo-");
        posMarkers.add("-verb-");
        posMarkers.add("-verb-pr-");
        
        nymMarkers = new HashSet<String>(20);
        nymMarkers.add("-méro-"); // ??
        nymMarkers.add("-hyper-");
        nymMarkers.add("-hypo-");
        nymMarkers.add("-holo-");
        nymMarkers.add("-méton-");
        nymMarkers.add("-syn-");
        nymMarkers.add("-q-syn-");
        nymMarkers.add("-ant-");
        
        nymMarkerToNymName = new HashMap<String, String>(20);
        nymMarkerToNymName.put("-méro-", "mero");
        nymMarkerToNymName.put("-hyper-", "hyper");
        nymMarkerToNymName.put("-hypo-", "hypo");
        nymMarkerToNymName.put("-holo-", "holo");
        nymMarkerToNymName.put("-méton-", "meto");
        nymMarkerToNymName.put("-syn-", "syn");
        nymMarkerToNymName.put("-q-syn-", "qsyn");
        nymMarkerToNymName.put("-ant-", "ant");
        
        sectionMarkers = new HashSet<String>(200);
        sectionMarkers.addAll(posMarkers);
        sectionMarkers.addAll(nymMarkers);
        sectionMarkers.add("-étym-");
        sectionMarkers.add("-voc-");
        sectionMarkers.add("-trad-");
        sectionMarkers.add("-note-");
        sectionMarkers.add("-réf-");
        sectionMarkers.add("clé de tri");
        sectionMarkers.add("-anagr-");
        sectionMarkers.add("-drv-");
        sectionMarkers.add("-voir-");
        sectionMarkers.add("-pron-");
        sectionMarkers.add("-gent-");
        sectionMarkers.add("-apr-");
        sectionMarkers.add("-paro-");
        sectionMarkers.add("-homo-");
        sectionMarkers.add("-exp-");
        sectionMarkers.add("-compos-");
        // DONE: prendre en compte la variante orthographique (différences avec -ortho-alt- ?)
        sectionMarkers.add("-var-ortho-");
        
       // TODO trouver tous les modèles de section...
        
        // affixesToDiscardFromLinks = new HashSet<String>();
        // affixesToDiscardFromLinks.add("s");
    }
    
    public FrenchWiktionaryExtractor(WiktionaryDataHandler wdh) {
        super(wdh);
    }

    protected final static Pattern languageSectionPattern;

    static {
        languageSectionPattern = Pattern.compile(languageSectionPatternString);
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
	 * @uml.property  name="nymBlockStart"
	 */
    private int nymBlockStart = -1;

    /**
	 * @uml.property  name="currentNym"
	 * @uml.associationEnd  qualifier="key:java.lang.String java.lang.String"
	 */
    private String currentNym = null;

    /* (non-Javadoc)
     * @see org.getalp.blexisma.wiktionary.WiktionaryExtractor#extractData(java.lang.String, org.getalp.blexisma.semnet.SemanticNetwork)
     */
    @Override
    public void extractData() {
        // System.out.println(pageContent);
        Matcher languageFilter = languageSectionPattern.matcher(pageContent);
        while (languageFilter.find() && ! languageFilter.group(1).equals("fr")) {
            ;
        }
        // Either the filter is at end of sequence or on French language header.
        if (languageFilter.hitEnd()) {
            // There is no french data in this page.
            return ;
        }
        int frenchSectionStartOffset = languageFilter.end();
        // Advance till end of sequence or new language section
        languageFilter.find();
        int frenchSectionEndOffset = languageFilter.hitEnd() ? pageContent.length() : languageFilter.start();
        
        extractFrenchData(frenchSectionStartOffset, frenchSectionEndOffset);
     }

    
    void gotoNoData(Matcher m) {
        state = NODATA;
    }

    
    void gotoTradBlock(Matcher m) {
        state = TRADBLOCK;
    }

    // TODO: put up in root class extractor.
    void gotoDefBlock(Matcher m) {
        state = DEFBLOCK;
        definitionBlockStart = m.end();
        wdh.addPartOfSpeech(m.group(1));
    }
    
    void gotoOrthoAltBlock(Matcher m) {
        state = ORTHOALTBLOCK;    
        orthBlockStart = m.end();
    }

    void leaveOrthoAltBlock(Matcher m) {
        extractOrthoAlt(orthBlockStart, computeRegionEnd(orthBlockStart, m));
        orthBlockStart = -1;
    }

    
    void leaveDefBlock(Matcher m) {
        extractDefinitions(definitionBlockStart, computeRegionEnd(definitionBlockStart, m));
        definitionBlockStart = -1;
    }
    
    void gotoSynBlock(Matcher m) {
        state = NYMBLOCK;
        currentNym = nymMarkerToNymName.get(m.group(1));
        nymBlockStart = m.end();      
     }

    void gotoIgnorePos(Matcher m) {
        state = IGNOREPOS;
     }

    void leaveSynBlock(Matcher m) {
        extractNyms(currentNym, nymBlockStart, computeRegionEnd(nymBlockStart, m));
        currentNym = null;
        nymBlockStart = -1;         
     }
    
    protected void extractFrenchData(int startOffset, int endOffset) {        
        Matcher m = macroPattern.matcher(pageContent);
        m.region(startOffset, endOffset);
        wdh.initializeEntryExtraction(wiktionaryPageName);
        gotoNoData(m);
        // WONTDO: (priority: low) should I use a macroOrLink pattern to detect translations that are not macro based ?
        // DONE: (priority: top) link the definition node with the current Part of Speech
        // DONE: (priority: top) type all nodes by prefixing it by language, or #pos or #def.
        // DONE: handle alternative spelling
        // DONE: extract synonyms
        // DONE: extract antonyms
        // DONE: add an IGNOREPOS state to ignore the entire part of speech
        String currentGlose = null;
        while (m.find()) {
            if (! sectionMarkers.contains(m.group(1))) unsupportedMarkers.add(m.group(1));
            switch (state) {
            case NODATA:
                
                // Iterate until we find a new section
                if (m.group(1).equals("-trad-")) {
                    gotoTradBlock(m);
                } else if (posMarkers.contains(m.group(1))) {
                    gotoDefBlock(m);
                } else if (ignorablePosMarkers.contains(m.group(1))) {
                    gotoIgnorePos(m);
                } else if (m.group(1).equals("-ortho-alt-") || m.group(1).equals("-var-ortho-")) {
                    gotoOrthoAltBlock(m);
                } else if (nymMarkers.contains(m.group(1))) {
                    gotoSynBlock(m);
                } else if (sectionMarkers.contains(m.group(1))) {
                    // nop
                }

                break;
            case DEFBLOCK:
                // Iterate until we find a new section
                if (m.group(1).equals("-trad-")) {
                    leaveDefBlock(m);
                    gotoTradBlock(m);
                } else if (posMarkers.contains(m.group(1))) {
                    leaveDefBlock(m);
                    gotoDefBlock(m);
                } else if (ignorablePosMarkers.contains(m.group(1))) {
                    leaveDefBlock(m);
                    gotoIgnorePos(m);
                } else if (m.group(1).equals("-ortho-alt-") || m.group(1).equals("-var-ortho-")) {
                    leaveDefBlock(m);
                    gotoOrthoAltBlock(m);
                } else if (nymMarkers.contains(m.group(1))) {
                    leaveDefBlock(m);
                    gotoSynBlock(m);
                } else if (sectionMarkers.contains(m.group(1))) {
                    leaveDefBlock(m);
                    gotoNoData(m);
                }
                break;
            case TRADBLOCK:
                String g1 = m.group(1);
                if (g1.equals("trad+") || g1.equals("trad-") || g1.equals("trad")) {
                    // DONE: Sometimes translation links have a remaining info after the word, keep it.
                    String g2 = m.group(2);
                    int i1, i2;
                    String lang, word;
                    if (g2 != null && (i1 = g2.indexOf('|')) != -1) {
                        lang = g2.substring(0, i1);
                     // normalize language code
                        String normLangCode;
                        if ((normLangCode = ISO639_3.sharedInstance.getIdCode(lang)) != null) {
                            lang = normLangCode;
                        } 
                        String usage = null;
                        if ((i2 = g2.indexOf('|', i1+1)) == -1) {
                            word = g2.substring(i1+1);
                        } else {
                            word = g2.substring(i1+1, i2);
                            usage = g2.substring(i2+1);
                        }
                        wdh.registerTranslation(lang, currentGlose, usage, word);
                    }
                } else if (g1.equals("boîte début") || g1.equals("(")) {
                    // Get the glose that should help disambiguate the source acception
                    String g2 = m.group(2);
                    // Ignore glose if it is a macro
                    if (g2 != null && ! g2.startsWith("{{")) {
                        currentGlose = g2;
                    }
                } else if (g1.equals("-")) {
                    // just ignore it
                } else if (g1.equals(")")) {
                    // Forget the current glose
                    currentGlose = null;
                } else if (g1.equals("T")) {
                    // this a a language identifier, 
                    
                } else if (m.group(1).equals("-trad-")) {
                    gotoTradBlock(m);
                } else if (posMarkers.contains(m.group(1))) {
                    gotoDefBlock(m);
                } else if (ignorablePosMarkers.contains(m.group(1))) {
                    gotoIgnorePos(m);
                } else if (m.group(1).equals("-ortho-alt-") || m.group(1).equals("-var-ortho-")) {
                    gotoOrthoAltBlock(m);
                } else if (nymMarkers.contains(m.group(1))) {
                    gotoSynBlock(m);
                } else if (sectionMarkers.contains(m.group(1))) {
                    gotoNoData(m);
                }
                break;
            case ORTHOALTBLOCK:
                if (m.group(1).equals("-trad-")) {
                    leaveOrthoAltBlock(m);
                    gotoTradBlock(m);
                } else if (posMarkers.contains(m.group(1))) {
                    leaveOrthoAltBlock(m);
                    gotoDefBlock(m);
                } else if (ignorablePosMarkers.contains(m.group(1))) {
                    leaveOrthoAltBlock(m);
                    gotoIgnorePos(m);
                } else if (nymMarkers.contains(m.group(1))) {
                    leaveOrthoAltBlock(m);
                    gotoSynBlock(m);
                } else if (sectionMarkers.contains(m.group(1))) {
                    leaveOrthoAltBlock(m);
                    gotoNoData(m);
                }
                break;
            case NYMBLOCK:
                if (m.group(1).equals("-trad-")) {
                    leaveSynBlock(m);
                    gotoTradBlock(m);
                } else if (posMarkers.contains(m.group(1))) {
                    leaveSynBlock(m);
                    gotoDefBlock(m);
                } else if (ignorablePosMarkers.contains(m.group(1))) {
                    leaveSynBlock(m);
                    gotoIgnorePos(m);
                } else if (nymMarkers.contains(m.group(1))) {
                    leaveSynBlock(m);
                    gotoSynBlock(m);
                } else if (sectionMarkers.contains(m.group(1))) {
                    leaveSynBlock(m);
                    gotoNoData(m);
                }
                break;
            case IGNOREPOS:
            	if (m.group(1).equals("-trad-")) {
                    // nop
                } else if (posMarkers.contains(m.group(1))) {
                	gotoDefBlock(m);
                } else if (ignorablePosMarkers.contains(m.group(1))) {
                    // nop
                } else if (nymMarkers.contains(m.group(1))) {
                    //nop
                } else if (sectionMarkers.contains(m.group(1))) {
                	//nop
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
            break;
        case ORTHOALTBLOCK:
            leaveOrthoAltBlock(m);
            break;
        case NYMBLOCK:
            leaveSynBlock(m);
           break;
        case IGNOREPOS:
        	break;
        default:
            assert false : "Unexpected state while extracting translations from dictionary.";
        } 
        
        wdh.finalizeEntryExtraction();
    }


}