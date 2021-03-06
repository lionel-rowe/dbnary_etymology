package org.getalp.dbnary.eng;

import org.getalp.dbnary.Pair;
import org.getalp.dbnary.wiki.WikiTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.String;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author pantaleo
 */
public class Etymology {
    static Logger log = LoggerFactory.getLogger(Etymology.class);

    private static final HashMap<String, List<String>> mappings;

    static {
	//tmp is used to define mappings
	//mappings is a map from a symbolic string like "FROM" to a list of string that will be match to it e.g. 
        HashMap<String, List<String>> tmp = new HashMap<String, List<String>>();
        tmp.put("FROM", Arrays.asList("[Ff]rom", "[Bb]ack-formation (?:from)?", "[Aa]bbreviat(?:ion|ed)? (?:of|from)?", "[Cc]oined from", "[Bb]orrow(?:ing|ed)? (?:of|from)?", "[Cc]ontracted from", "[Aa]dopted from", "[Cc]alque(?: of)?", "[Ii]terative of", "[Ss]hort(?:ening|en|ened)? (?:form )?(?:of|from)?", "[Tt]hrough", "[Bb]lend of", "[Pp]articiple of", "[Aa]lteration of", "[Vv]ia", "[Dd]iminutive (?:form )?of", "[Ee]nlargment (?:form )?of", "[Uu]ltimately of", "[Vv]ariant of", "[Pp]lural of", "[Ff]orm of", "[Aa]phetic variation of", "\\<", "[Aa] \\[\\[calque\\]\\] of", "[Ff]ormed as"));
        tmp.put("TEMPLATE", Arrays.asList("\\{\\{"));
        tmp.put("LINK", Arrays.asList("\\[\\["));//removed (?:'') as this causes an error in WiktinaryExtractor and function containedIn
        tmp.put("ABOVE", Arrays.asList("[Ss]ee above"));//this should precede cognateWith which matches against "[Ss]ee" ; this should become STOP
        tmp.put("COGNATE_WITH", Arrays.asList("[Rr]elated(?: also)? to", "[Cc]ognate(?:s)? (?:include |with |to |including )?", "[Cc]ompare (?:also )?", "[Ww]hence (?:also )?", "(?:[Bb]elongs to the )?[Ss]ame family as ", "[Mm]ore at ", "[Aa]kin to ", "[Ss]ee(?:n)? (?:also )?"));//this should follow abovePatternString which matches against "[Ss]ee above" ; this should become STOP
        tmp.put("COMPOUND_OF", Arrays.asList("[Cc]ompound(?:ed)? (?:of|from) ", "[Mm]erg(?:ing |er )(?:of |with )?(?: earlier )?", "[Uu]niverbation of ", "[Ff]usion of ", "[Cc]orruption of "));
        tmp.put("UNCERTAIN", Arrays.asList("[Oo]rigin uncertain")); //this should become STOP
        tmp.put("COMMA", Arrays.asList(","));
        tmp.put("YEAR", Arrays.asList("(?:[Aa].\\s*?[Cc].?|[Bb].?\\s*[Cc].?)?\\s*\\d++\\s*(?:[Aa].?\\s*[Cc].?|[Bb].?\\s*[Cc].?|th century|\\{\\{C\\.E\\.\\}\\})?"));
        tmp.put("AND", Arrays.asList("\\s+and\\s+", "with suffix "));
        tmp.put("PLUS", Arrays.asList("\\+", " plus "));
        tmp.put("DOT", Arrays.asList("\\.", ";"));
        tmp.put("OR", Arrays.asList("[^a-zA-Z0-9]or[^a-zA-Z0-9]", "either", "whether")); //this should become STOP
        tmp.put("WITH", Arrays.asList("[^a-zA-Z0-9]with[^a-zA-Z0-9]"));
	tmp.put("STOP", Arrays.asList("Via ", " via ", " through ", "[Ss]uperseded", "[Ss]uperseding", "[Dd]isplaced(?: native)?", "[Dd]isplacing", "[Rr]eplaced", "[Mm]ode(?:l)?led on", "[Rr]eplacing", "[Cc]oined by", "equivalent to\\s*\\{\\{[^\\}]+\\}\\}"));//this includes three types of patterns: via, superseded and equivalent to
        tmp.put("COLON", Arrays.asList(":"));
	tmp.put("SLASH", Arrays.asList("/"));
    
        mappings = new HashMap(tmp);
    }

    public static List<String> bulletSymbolsList = Arrays.asList("COMMA", "TEMPLATE", "LINK", "COLON");
    public static List<String> definitionSymbolsList = Arrays.asList("FROM", "TEMPLATE", "LINK", "ABOVE", "COGNATE_WITH", "COMPOUND_OF", "UNCERTAIN", "COMMA", "YEAR", "AND", "PLUS", "DOT", "OR", "WITH", "STOP", "SLASH");

    //transform the list of symbols above into a regex (COMMA)|(TEMPLATE)|(LINK)|(COLON) where each symbols is replaces by the corresponding mappings (joined by "|")
    public static Pattern bulletSymbolsListPattern = Pattern.compile("(" + bulletSymbolsList.stream().map(e -> mappings.get(e).stream().collect(Collectors.joining("|"))).collect(Collectors.joining(")|(")) + ")");
    public static Pattern definitionSymbolsListPattern = Pattern.compile("(" + definitionSymbolsList.stream().map(e -> mappings.get(e).stream().collect(Collectors.joining("|"))).collect(Collectors.joining(")|(")) + ")");
    
    public static Pattern definitionSymbolsPattern = Pattern.compile("((FROM )?(LANGUAGE LEMMA |LEMMA |LANGUAGE )(COMMA |SLASH |DOT |OR |STOP ))+");
    public static Pattern compoundSymbolsPattern = Pattern.compile("((COMPOUND_OF |FROM )(LANGUAGE )?(LEMMA (COMMA LEMMA )*)(?:(PLUS |(COMMA )?AND |WITH )(LANGUAGE )?(LEMMA (COMMA LEMMA )*))+)|((LANGUAGE )?(LEMMA (COMMA LEMMA )*)(?:(PLUS )(LANGUAGE )?(LEMMA (COMMA LEMMA )*))+)");
    //TODO: add ARROW and allow for situations like Italian: LEMMA LEMMA COMMA LEMMA
    public static Pattern bulletSymbolsPattern = Pattern.compile("((((LEMMA )(COMMA )?)+)|(LANGUAGE ))(COLON ((LEMMA)( COMMA )?)+)?");
    public static Pattern tableDerivedLemmasPattern = Pattern.compile("(LEMMA)(?: COMMA (LEMMA))*");
    public static Pattern multipleBorrowingSymbolsPattern = Pattern.compile("(FROM )?(LANGUAGE LEMMA |LEMMA )((COMMA (LANGUAGE LEMMA |LEMMA ))+)?(AND (LANGUAGE LEMMA |LEMMA ))?(DOT |COMMA )");
    public static Pattern stopSymbolsPattern = Pattern.compile("(DOT |STOP |OR)");

    public String lang;
    public String string;
    public ArrayList<Symbols> symbols;

    public Etymology(String s, String l) {
        string = s;
        lang = l;
        symbols = new ArrayList<Symbols>();
    }

    public void fromTableToSymbols() {
        string = WikiTool.removeReferencesIn(string);
        string = WikiTool.removeTextWithinParenthesesIn(string);
        string = string.trim();

        if (string == null || string.equals("")) {
            return;
        }

        toSymbols(bulletSymbolsList, bulletSymbolsListPattern);
	
        ArrayList<Symbols> lemmas = new ArrayList<>();
        Matcher m = tableDerivedLemmasPattern.matcher(toString(symbols));
        while (m.find()) {
            for (Symbols p : symbols) {
                if (p.values.get(0).equals("LEMMA")) {
                    lemmas.add(p);
                }
            }
            break;
        }
        symbols = lemmas;
    }

    public void fromDefinitionToSymbols() {
        if (string == null || string.equals("")) {
            return;
        }

        //REMOVE TEXT WITHIN HTML REFERENCE TAG
        string = WikiTool.removeReferencesIn(string);
        //REMOVE TEXT WITHIN TABLES
        string = WikiTool.removeTablesIn(string);
        //REMOVE TEXT WITHIN PARENTHESES UNLESS PARENTHESES FALL INSIDE A WIKI LINK OR A WIKI TEMPLATE
        string = WikiTool.removeTextWithinParenthesesIn(string);
        string = string.trim();

        if (string == null || string.equals("")) {
            return;
        } else {
            if (!string.endsWith(".")) {//add final dot if etymology string doesn't end with a dot 
                string += ".";
            }
        }
	toSymbols(definitionSymbolsList, definitionSymbolsListPattern);
	
        parseEtyl();

	if (lang.equals("io") || lang.equals("ido") || lang.equals("epo")) {//lemmas in ido or esperanto
	    //parsing languages where etymology is written differently: "From wordA, wordB, wordC, wordD", i.e., the word was constructed starting from wrods A,B,C and D 
	    parseMultipleBorrowing();
	} else {
	    parseCompound();
	}
	
        //find where list of cognates or OR statements start
        //e.g., if toString(symbols) == "FROM LEMMA COMMA FROM LEMMA COMMA COGNATE_WITH LEMMA COMMA" registers 6, the index of "COGNATE_WITH" or
        //e.g., if toString(symbols) == "FROM LEMMA OR LEMMA" it registers 2, the index of "OR",
        //remove any element of the input ArrayList<Symbols> after that index.
        for (int j = 0; j < symbols.size(); j++) {
            if (symbols.get(j).values.size() > 0) {
                if (symbols.get(j).values.get(0).equals("COGNATE_WITH") || symbols.get(j).values.get(0).equals("OR")) {
                    symbols.subList(j, symbols.size()).clear();
                    break;
                }
            }
        }
	
        ArrayList<Pair> m = findMatch(symbols, definitionSymbolsPattern);
        if (m.size() == 0) {
            return;//there is no match to the definitionSymbolsPattern
        }

        //remove any Symbols that follows the first match
	symbols.subList(m.get(0).end + 1, symbols.size()).clear();
        //remove any Symbols that preceeds the first match to the definitionSymbolsPattern
        symbols.subList(0, m.get(0).start).clear();
    }

    //TODO: handle * [[crisismanager]] {{g|m}}    
    public void fromBulletToSymbols() {
	string = WikiTool.removeReferencesIn(string);
	string = WikiTool.removeTextWithinParenthesesIn(string);
	string = string.trim();
	
        //REPLACE LANGUAGE STRING WITH LANGUAGE _ETYL TEMPLATE
        parseLanguage();
	//* &rarr; Italian: {{l|it|baruffare}}
	//Sardinian: [[pobulu]], [[poburu]], [[populu]] -> {{_etyl|en|sc}}: [[pobulu]], [[poburu]], [[populu]]  
	//{{_etyl|eng|sc}}: [[pobulu]], [[poburu]], [[populu]]-> LANGUAGE COLON LEMMA COMMA LEMMA COMMA LEMMA
	//case "{{ja-r|??????|??????}}: [[military]] [[power]]"
        toSymbols(bulletSymbolsList, bulletSymbolsListPattern);

        parseEtyl();//[[pobulu]] -> {{m|lang=sc|word1=pobulu}}

        //REPLACE SENSE TEMPLATE
        //case "{{sense|kill}} {{l|en|top oneself}}" -> {{l|en|top oneself|sense=kill}}
        parseSense();

        ArrayList<Symbols> lemmas = new ArrayList<>();
        Matcher m = bulletSymbolsPattern.matcher(toString(symbols));
        while (m.find()) {
            //case LANGUAGE COLON LEMMA COMMA LEMMA, e.g.:
            //case "Sardinian: [[pobulu]], [[poburu]], [[populu]]"
            //and case "[[Asturian]]: {{l|ast|??guila}}"
            if (m.group(6) != null && m.group(7) != null) {
		//System.out.format("case 1");
		//System.out.format(toString(symbols));
		String language = null;
		for (Symbols b : symbols) {
		    if (b.values.get(0).equals("LANGUAGE")) {
			language = b.args.get("lang");
		    }
		    if (language != null && b.values.get(0).equals("LEMMA")) {
			b.args.put("lang", language);
			lemmas.add(b);
		    }      
		}
	    } else if (m.group(2) != null  && m.group(7) != null) { //case "{{ja-r|??????|??????}}: [[military]] [[power]]" 
		for (Symbols b : symbols) {
		    if (b.values.get(0).equals("COLON")) {
			break;
		    } else if (b.values.get(0).equals("LEMMA")) {
			lemmas.add(b);
		    }
		}
	    } else if (m.group(2) != null  && m.group(7) == null && m.group(6) == null) {
		//System.out.format("case 3");
		//System.out.format(toString(symbols));
		for (Symbols b : symbols) {
		    if (b.values.get(0).equals("LEMMA")) {
			lemmas.add(b);
			break;
		    }
		}
            } 
	}
        symbols = lemmas;
    }

    public void toSymbols(List<String> l, Pattern p) {
        if (string == null || string.trim().isEmpty()) {
            return;
        }

        ArrayList<Pair> templatesLocations = WikiTool.locateEnclosedString(string, "{{", "}}");
        ArrayList<Pair> linksLocations = WikiTool.locateEnclosedString(string, "[[", "]]");

        //match against regex pattern of symbols p
        Matcher m = p.matcher(string);
        while (m.find()) {
            for (int i = 0; i < m.groupCount(); i++) {
                if (m.group(i + 1) != null) {
                    //check if match is contained in template or link
                    boolean check = false;
                    //check if match is contained in a template (or is a template)
                    Pair match = new Pair(m.start(), m.end());
                    for (Pair template : templatesLocations) {
                        if (match.containedIn(template)) {//match is contained in a template
                            check = true;
                            if (l.get(i).equals("TEMPLATE")) {//match is a template
                                Symbols b = new Symbols(string.substring(template.start + 2, template.end - 2), lang, l.get(i));
                                if (b.values != null && b.args != null) {
				    for (String values : b.values) {
					if (values.equals("STOP")) {
					    symbols.add(b);
					    return;
					}
				    }
                                    symbols.add(b);
                                }
                                break;
                            }//else ignore match
                        }
                    }

                    //change match by adding "+ 2 (- 1 as above)" to its start to check both:
                    //*   if match "''[[" is contained in link "[[...]]"
                    //*   if match "[[" is contained in link "[[...]]"
                    //check if match is contained in a link (or is a link)
                    if (check == false) {//if match is not contained in a template
                        for (Pair link : linksLocations) {
                            if (match.containedIn(link)) {
                                check = true;
                                if (l.get(i).equals("LINK")) {//match is a link
                                    Symbols b = new Symbols(string.substring(link.start + 2, link.end - 2), lang, l.get(i));
                                    if (b.values != null && b.args != null) {
                                        symbols.add(b);
                                    }
                                    break;
                                }//else ignore match
                            }
                        }
                    }
                    if (check == false) {//if match is neither contained in a template nor in a link
                        Symbols b = new Symbols(m.group(i + 1), lang, l.get(i));
                        if (b.values != null) {
			    for (String values : b.values) {
				if (values.equals("STOP")) {
				    symbols.add(b);
				    return;
				}
			    }
                            symbols.add(b);
                        }
                    }
                }
            }
        }
    }

    //when a LEMMA is preceded by a LANGUAGE etyl template
    //set language of LEMMA to language of etyl template
    private void parseEtyl() {
        String etylLang = null;
        int etylIndex = -1;
        for (int i = 0; i < symbols.size(); i++) {
            if (symbols.get(i).values != null && symbols.get(i).args != null) {
                if (symbols.get(i).args.get("1").equals("etyl") || symbols.get(i).args.get("1").equals("_etyl")) {
                    etylLang = symbols.get(i).args.get("lang");
                    etylIndex = i;
                }
                if (etylIndex != -1 && i == etylIndex + 1) {
                    symbols.get(i).args.put("lang", etylLang);
                }
            }
        }
    }

    private void parseLanguage() {
	ArrayList<String> subs = WikiTool.splitUnlessInTemplateOrLink(string, ':');
			
        if (subs.size() == 2) {
	    String bulletLang = null;
            ArrayList<Pair> linksLocations = WikiTool.locateEnclosedString(subs.get(0), "[[", "]]");
            if (linksLocations.size() == 0) {//PARSE case "Sardinian: [[pobulu]], [[poburu]], [[populu]]"
		//also PARSE case "??? Georgian: {{l|ka|????????????|gloss=coffee}}"
		//parse case ": {{l|el|??????????????????|gloss=laconic, laconian}}" (i.e. the ":" only signals an indentation)
		bulletLang = subs.get(0).replace("???", "").replace("&rarr;", "").trim();
		if (bulletLang.equals("")){
		    log.debug("Ignoring bullet {}", string);
		    string = "";
		    return;
		} 
            } else if (linksLocations.size() == 1) {//PARSE case "[[Asturian]]: {{l|ast|??guila}}"
	        bulletLang = subs.get(0).substring(2, subs.get(0).length() - 2).trim();
            }
	    if (!bulletLang.startsWith("{{") && !bulletLang.startsWith("adjective") && !bulletLang.startsWith("noun") && !bulletLang.startsWith("verb") && !bulletLang.startsWith("prefix") && !bulletLang.startsWith("phrase") && !bulletLang.startsWith("idiom") && !bulletLang.startsWith("antonym") && !bulletLang.startsWith("adverb") && !bulletLang.startsWith("proverb") && !bulletLang.startsWith("given") && !bulletLang.startsWith("interjection")&& !bulletLang.startsWith("postposition") && !bulletLang.startsWith("surname") && !bulletLang.startsWith("index") && !bulletLang.startsWith("condition") && !bulletLang.startsWith("saying") && !bulletLang.startsWith("suffix") && !bulletLang.startsWith("pronoun") && !bulletLang.startsWith("substantive") && !bulletLang.startsWith("determiner")){
		bulletLang = EnglishLangToCode.threeLettersCode(bulletLang);
		if (bulletLang != null) {
		    string = "{{_etyl|" + bulletLang + "|" + lang + "}} : " + subs.get(1).trim();
		}
	    }
        } else if (subs.size() > 2) {
            log.debug("In function: parseLanguage(): ignoring bullet {}", string);
            string = "";
        }
    }

    public void parseSense() {
        int aSize = symbols.size();

        if (aSize > 1) {
            for (int i = 0; i < aSize; i++) {
                if (symbols.get(i).values.get(0).equals("SENSE")) {
                    String sense = symbols.get(i).args.get("2");
                    if (i + 1 < aSize && symbols.get(i + 1).values.get(0).equals("LEMMA")) {
                        Symbols tmp = symbols.get(i + 1);
                        tmp.args.put("sense", sense);
                        symbols.set(i, tmp);
                        symbols.remove(i + 1);
                        aSize--;
                    } else if (i - 1 >= 0 && symbols.get(i - 1).values.get(0).equals("LEMMA")) {
                        Symbols tmp = symbols.get(i - 1);
                        tmp.args.put("sense", sense);
                        symbols.set(i, tmp);
                        symbols.remove(i - 1);
                        aSize--;
                        i--;
                    }
                }
            }
        }
        return;
    }

    /**
     * @return a String (e.g., "FROM LEMMA OR LEMMA") concatenating the property "symbols" of each element of ArrayList<Symbols>
     */
    public static String toString(ArrayList<Symbols> a) {
        StringBuilder s = new StringBuilder();
        for (Symbols b : a) {
            for (String values : b.values) {
                s.append(values);
                s.append(" ");
            }
        }
        return s.toString();
    }

    /**
     * @param a an ArrayList of Symbols
     * @param p a pattern  
     * @return a Pair with elements start and end; a.get(start) is where the match starts and a.get(end) is where the match ends
     */
    public ArrayList<Pair> findMatch(ArrayList<Symbols> a, Pattern p) {
	//looking for a match of toString(a) to pattern p
	ArrayList<Pair> toreturn = new ArrayList<Pair>();
        if (a == null || a.size() == 0) {
            return toreturn;
        }

	//if i is in arrayListIndex you can do a.get(i) (so i is the index of the element in the array a)
	//if c is in coordinate you can do toSymbol(a)[c] (so c is the position of the character in the symbol string)
        ArrayList<Integer> index = new ArrayList<Integer>();
        ArrayList<Integer> coordinate = new ArrayList<Integer>();
        int c = 0;
        coordinate.add(c);
        for (int i = 0; i < a.size(); i++) {
            Symbols b = a.get(i);
            for (int j = 0; j < b.values.size(); j++) {
                index.add(i);
		//the symbol corresponding to b.values.get(j) is b.string
                c += b.values.get(j).length() + 1;
                coordinate.add(c);
            }
        }
			  
        Matcher m = p.matcher(toString(a));
        
        while (m.find()) {
            int start = -1, end = -1;
            for (int i = 0; i < coordinate.size() - 1; i++) {
                if (coordinate.get(i) == m.start()) {
                    start = index.get(i);
                } else if (coordinate.get(i + 1) == m.end()) {
                    end = index.get(i);
                }
            }
            if (start < 0) {
                log.debug("Error: start of match is not available\n");
            } else if (end < 0) {
		log.debug("Error: end of match is not available\n");
	    } else {
		//stop at first stop symbol i.e. STOP | DOT | OR
		Matcher mStop = stopSymbolsPattern.matcher(toString(a).substring(m.start(), m.end()));
		if (mStop.find()){
		    //found a stop in string toString(a).substring(m.start(), m.end())
		    //the match starts at m.start() + mStop.start()
		    //the match ends at m.start() + mStop.end()
		    //the matching string is toString(a).substring(m.start() + mStop.start(), m.start() + mStop.end())
		    //transform from coordinate to index
		    for (int i = 0; i < coordinate.size() - 1; i++) {
			if (coordinate.get(i + 1) == m.start() + mStop.end()) {
			    end = index.get(i);
			}
		    }
		}
		toreturn.add(new Pair(start, end));
	    }
        }
        return toreturn;
    }

    /**
     * This function is used to replace "FROM LANGUAGE LEMMA, LANGUAGE LEMMA"; "FROM LANGUAGE LEMMA AND LANGUAGE LEMMA"
     * with a single "LEMMA" Symbol of type mult|lang|lang1|word1|lang2|word2 etc
     */
    //TO DO: parse {{borrowing|io|fr|elle}}, {{etyl|it|io}}/{{etyl|es|io}} {{m|it|ella}}, from {{etyl|la|io}} {{m|la|illa}}, feminine of {{m|la|ille}} {{suffix|io||u|gloss2=denoting a person}}.
    public void parseMultipleBorrowing(){
	for (int k = 0; k < symbols.size(); k++){
	    Symbols b = symbols.get(k);	    
	}
	ArrayList<Pair> match = findMatch(symbols, multipleBorrowingSymbolsPattern);
       
	if (match == null || match.size() == 0) {
	    return;
	}
        Pair m = match.get(0);
	ArrayList<Symbols> a = new ArrayList<Symbols>();
	for (int k = m.start; k < m.end + 1; k++) {
	    Symbols b = symbols.get(k);
	    for (String values : b.values) {
		if (values.equals("LEMMA") && b.args != null) {
		    a.add(b);
		    break;
		}
	    }
	}
	Symbols b = new Symbols("mult", lang, a);
	if (b.string != null) {
	    Symbols f = new Symbols("from", lang, "FROM");
	    symbols.set(m.start, f);
	    symbols.set(m.start + 1, b);
	    symbols.subList(m.start + 2, m.end + 1).clear();
	 	} else {
	    symbols.subList(m.start, m.end + 1).clear();
	}
    }
    
    /**
     * This function is used to replace "COMPOUND_OF LEMMA AND LEMMA" and equivalents
     * with a single "LEMMA" Symbol of type compound|lang1|word1|lang2|word2
     */
    public void parseCompound() {
	//System.out.format(toString(symbols));
	//System.out.format(compoundSymbolsPattern.pattern());
        ArrayList<Pair> match = findMatch(symbols, compoundSymbolsPattern);
        if (match == null || match.size() == 0) {
            return;
        }
	//iterate over all matches to a compound pattern
	//starting from the last
        for (int i = match.size() - 1; i >= 0; i--) {
            Pair m = match.get(i);
            ArrayList<Symbols> a = new ArrayList<Symbols>();
            for (int k = m.start; k < m.end + 1; k++) {
                Symbols b = symbols.get(k);
                for (String values : b.values) {
                    if (values.equals("LEMMA") && b.args != null) {
                        a.add(b);
                        break;
                    }
                }
            }
            Symbols b = new Symbols("comp", lang, a);
            if (b.string != null) {
                Symbols f = new Symbols("from", lang, "FROM");
                symbols.set(m.start, f);
                symbols.set(m.start + 1, b);
                symbols.subList(m.start + 2, m.end + 1).clear();
            } else {
                symbols.subList(m.start, m.end + 1).clear();
                break;
            }
        }
    }
}
