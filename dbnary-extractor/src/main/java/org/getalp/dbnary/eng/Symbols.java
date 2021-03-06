package org.getalp.dbnary.eng;

import org.getalp.dbnary.wiki.WikiTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author pantaleo
 */

/**
 * Symbols is an object that contains data about a Wiktionary template (e.g.: {{m|en|door}}) or 
 * a Wiktionary link (e.g.: [[door]]).
 * It has properties
 * .values an ArrayList&lt;String&gt;, e.g., {"LANGUAGE", "LEMMA"}
 * .args a Map&lt;String, String&gt;, e.g., {("1", "m"), ("lang", "eng"), ("word1", "door")}
 * .string, e.g., "m|en|door" (needed for debugging purposes only)
 */
public class Symbols {
    public ArrayList<String> values;
    public Map<String, String> args;
    public String string;//needed for debugging purposes only
    static Logger log = LoggerFactory.getLogger(Symbols.class);

    public Symbols(String group, String lang, String s) {
        if (s.equals("TEMPLATE")) {
            parseTemplate(group, lang);
        } else if (s.equals("LINK")) {
            parseLink(group, lang);
        } else {
            parseOther(group, s);
        }
	args = parseDiacritics(args);
    }

    public void parseOther(String group, String value) {
        string = group;
        args = null;
        values = new ArrayList<String>();
        values.add(value);
    }

    public Map<String, String> parseDiacritics(Map<String, String> a){
        if (a != null){
	    for (String key : a.keySet()){
		if (key.startsWith("lang")){
		    //parse Latin
		    String lang = a.get(key);
		    if (lang.equals("la") || lang.equals("lat")){
			String k = key.replace("lang", "word");
			if (k.equals("word")){
			    for (int h = 1; h < 12; h ++) {
				k = "word" + Integer.toString(h);
				String word = a.get(k);
				if (word != null){
				    String[] macrons = {"ā", "ē", "ī", "ō", "ū", "ȳ"};
				    String[] breves = {"ă", "ĕ", "ĭ", "ŏ", "ŭ", "y̆"};
				    String[] vocals = {"a", "e", "i", "o", "u", "y"};
				    for (int i = 0; i < 6; i ++){
					word = word.replace(macrons[i], vocals[i]);
					word = word.replace(breves[i], vocals[i]);
				    }
				    a.put(k, word);
				}
			    }
			}
			return a;	
		    }
		}
	    }
	}
	return a;
    }
    
    //create a new Symbol that is a compound of all Symbols in input a
    public Symbols(String type, String lang, ArrayList<Symbols> a) {
	if (type.equals("comp")){
	    int counter = 1;
	    StringBuilder compound = new StringBuilder();
	    compound.append("_etycomp|" + lang);
	    
	    for (int i = 0; i < a.size(); i++) {
		for (String key : a.get(i).args.keySet()) {
		    if (key.equals("lang")) {
			for (String key2 : a.get(i).args.keySet()) {
			    if (key2.startsWith("word")) {
				compound.append("|lang" + counter + "=" + a.get(i).args.get("lang"));
				compound.append("|word" + counter + "=" + a.get(i).args.get(key2));
				counter++;
			    }
			}
		    } else if (key.startsWith("lang")) {
			String word = a.get(i).args.get("word" + key.substring(4, key.length()));
			if (word == null || word.equals("")) {
			    log.debug("Error: invalid compound {}", a.get(i).string);
			}
			compound.append("|lang" + counter + "=" + a.get(i).args.get(key));
			compound.append("|word" + counter + "=" + word);
			counter++;
		    }
		}
	    }
	    string = compound.toString();
	    values = new ArrayList<String>();
	    values.add("LEMMA");
	    args = WikiTool.parseArgs(string);
	} else if (type.equals("mult")){
	    int counter = 1;
	    StringBuilder mult = new StringBuilder();
	    mult.append("mult|lang="+lang);
	    for (int i = 0; i < a.size(); i++) {
		Symbols sss = new Symbols("TEMPLATE", lang, a.get(i).string);
		if (a.get(i).args.get("lang") != null && a.get(i).args.get("word1") != null){
		    mult.append("|lang" + counter + "=" + a.get(i).args.get("lang"));
		    mult.append("|word" + counter + "=" + a.get(i).args.get("word1"));
		    counter++;
		}
	    }
	    string = mult.toString();
	    values = new ArrayList<String>();
	    values.add("LEMMA");
	    values.add("STOP");
	    args = WikiTool.parseArgs(string);
	}
    }

    public void parseTemplate(String group, String lang) {
        string = group;
        values = new ArrayList<String>();
        args = WikiTool.parseArgs(string);
	if (args.get("1").equals("m") || args.get("1").equals("mention") || args.get("1").equals("l") || args.get("1").equals("link") || args.get("1").equals("_m") || args.get("1").equals("desc") || args.get("1").equals("descendant") || args.get("1").equals("desctree")) {
	    //====Descendants====
	    //* {{desctree|fro|estoner}}
	    //* {{desc|it|stonare}}
	    //The parameter "1" is required.
	    args.put("lang", args.get("2"));
	    
	    if (args.get("3") != null && !args.get("3").equals("")) {
		values.add("LEMMA");
		args.put("word1", cleanUp(args.get("3")));
		args.remove("3");
		//I'm commenting what follows as it is wrong for {{m|oty|tr=vēr|4=root}}
		//} else if (args.get("4") != null && !args.get("4").equals("")) {
		//values.add("LEMMA");
		//args.put("word1", cleanUp(args.get("4")));
		//args.remove("4");
		//if (args.get("5") != null && !args.get("5").equals("")) {
		//    args.put("gloss1", args.get("5").replaceAll("\\[", "").replaceAll("\\]", ""));
		//    args.remove("5");
		//}
	    } else if (args.get("4") != null && !args.get("4").equals("")) {
		values.add("LEMMA");
		args.put("word1", cleanUp(args.get("4")));
		args.remove("4");
	    } else {
		args.clear();
		values = null;
	    }
	} else if (args.get("1").equals("inh") || args.get("1").equals("inherited") || args.get("1").equals("der") || args.get("1").equals("derived")) {//TO DO: maybe add values.ad("FROM") by default;
	    //e.g.:
	    //from a {{inh|ro|VL.|-}} root
	    //{{der|la|ett||tr=HERCLE}}
	    int offset = 0;
	    if (args.get("lang") == null) {
                offset = 1;
		args.put("lang", args.get("3"));
		args.remove("3");
	    }

	    String key = Integer.toString(3 + offset);//first case
	    if (args.get(key) != null && !args.get(key).equals("")) {
                if (args.get(key).equals("-")) {
                    values.add("LANGUAGE");
		} else {
                    values.add("LEMMA");
		    args.put("word1", cleanUp(args.get(key)));
		    args.remove(key);
		}
	    } else {
                key = "alt";//second case
		if (args.get(key) != null) {
                    if (!args.get(key).equals("")) {
                        values.add("LEMMA");
			args.put("word1", cleanUp(args.get(key)));
			args.remove(key);
		    }
		} else {
                    //I'm commenting this case as it is wrong for template  from {{der|gl|oty|tr=iṅci|5=ginger}}
                    //key = Integer.toString(4 + offset); //third case
                    //if (args.get(key) != null && !args.get(key).equals("")) {
                    //    values.add("LEMMA");
                    //    args.put("word1", cleanUp(args.get(key)));
                    //    args.remove(key);
                    //} else {
		    values.add("LANGUAGE");
		    //}
		}
	    }
	} else if (args.get("1").equals("etyl") || args.get("1").equals("_etyl")) {
	    values.add("LANGUAGE");
	    if (args.get("2") != null) {
		args.put("lang", args.get("2"));
		args.remove("2");
	    }
	} else if (args.get("1").equals("cog") || args.get("1").equals("cognate")) {//e.g.:       {{cog|fr|orgue}}
	    values.add("STOP");
	} else if (args.get("1").equals("etymtwin")) {//e.g.:    {{etymtwin|lang=en}} {{m|en|foo}}
	    values.add("COGNATE_WITH");
	} else if (args.get("1").equals("bor") || args.get("1").equals("borrowing") || args.get("1").equals("loan")) {//borrowing
	    if (args.get("notext") != null && !args.get("notext").equals("1")) {
		values.add("FROM");
	    }
	    int offset = 0;
	    if (args.get("lang") == null) {
		offset = 1;
		args.put("lang", args.get("3"));
		args.remove("3");
	    }
	    String word = args.get(Integer.toString(3 + offset));
	    if (word != null) {
		if (!word.equals("")) {
		    if (!word.equals("-")) {//if lemma is not specified Wiktionary prints borrowing from Language.
			values.add("LEMMA");
			args.put("word1", cleanUp(word));
			args.remove(Integer.toString(3 + offset));
		    } else {
			values.add("LANGUAGE");
			args.remove(Integer.toString(3 + offset));
		    }
		} else {
		    if (args.get(Integer.toString(4 + offset)) != null){
			word = args.get(Integer.toString(4 + offset));
			values.add("LEMMA");
			args.put("word1", cleanUp(word));
			args.remove(Integer.toString(4 + offset));
		    }
		}
	    }
        } else if (args.get("1").equals("etymtree")) {
            if (args.get("3") != null && args.get("4") != null) {
                args.put("lang", args.get("3"));
                args.remove("3");
                args.put("word1", cleanUp(args.get("4")));
                args.remove("4");
            } else if (args.get("2") != null) {
                args.put("lang", args.get("2"));
                args.remove("2");
                args.put("word1", "");
            }
            values.add("ETYMTREE");
            args.put("page", "Template:etymtree/" + args.get("lang") + "/" + args.get("word1"));
	} else if (args.get("1").equals("lbor") || args.get("1").equals("learned borrowing")) {
            //The parameter "1" is required. The parameter "2" is required.
            String language = args.get("lang");
            int offset = 0;
            if (language == null) {
                args.put("lang", args.get("3"));
                args.remove("3");
                offset = 1;
            }
            values.add("FROM");
            String word = args.get(Integer.toString(4 + offset));
            if (word != null && !word.equals("-")) {
                args.put("word1", cleanUp(word));
                values.add("LEMMA");
            }
        } else if (args.get("1").equals("cal") || args.get("1").equals("calque")) {
	    //TODO: would like to extract more info from:
	    //From {{calque|el|φως|t1=light|βολίδα|t2=missile, projectile|etyl lang=de|etyl term=Leuchtkugel}} -> From φως ‎(fos, “light”) +‎ βολίδα ‎(volída, “missile, projectile”), calque of German Leuchtkugel.
	    //{{calque|he|en|Halloween|nocap=1}}
	    //{{calque|bo|cmn|-}} {{zh-l|鐵路}}.
	    if (args.get("etyl lang") != null){
		args.put("lang", args.get("etyl lang"));
		args.remove("etyl lang");
		args.put("word1", cleanUp(args.get("etyl term")));
		args.remove("etyl term");
		values.add("FROM");
		values.add("LEMMA");
		if (args.get("etyl t") != null){
		    args.put("gloss1", args.get("etyl t").replaceAll("\\[", "").replaceAll("\\]", ""));
		    args.remove("etyl t");
		}
	    } else if (args.get("3") != null){
		args.put("lang", args.get("3"));
		args.remove("3");
		values.add("FROM");
		values.add("LANGUAGE");
		if (args.get("4") != null && !args.get("4").equals("-") && !args.get("4").equals("&nbsp;")){
		    args.put("word1", cleanUp(args.get("4")));
		    args.remove("4");
		    values.add("LEMMA");
		}
	    }
	} else if (args.get("1").equals("abbreviation of")) {
            values.add("FROM");
            values.add("LEMMA");
            args.put("word1", cleanUp(args.get("2")));
            args.remove("2");
            if (args.get("4") != null) {
                args.put("gloss1", args.get("4").replaceAll("\\[", "").replaceAll("\\]", ""));
                args.remove("4");
            }
        } else if (args.get("1").equals("named-after")) {
	    if (args.get("2") != null) {
		args.put("word1", cleanUp(args.get("2")));
		args.remove("2");
		values.add("FROM");
		values.add("LEMMA");
		values.add("STOP");
	    }
	} else if (args.get("1").equals("back-form")) { //1=term, (2=display form)
            if (args.get("lang") == null) {//how to deal with {{back-form|ilu|lang=io|gloss=he, him|nodot=yes}}, {{l|io|elu|gloss=she, her}} and {{l|io|olu|gloss=it}}.
                if (args.get("2") != null) {
                    args.put("lang", args.get("2"));
                    args.remove("2");
                    values.add("FROM");
                } else {
                    args.put("lang", "en");
                    log.debug("Warning: no language specified for lemma {} in back-formation template, using English", string);
                }
                if (args.get("3") != null) {
                    args.put("word1", cleanUp(args.get("3")));
                    args.remove("3");
                    values.add("LEMMA");
                }
            } else {
                if (args.get("2") != null) {
                    args.put("word1", cleanUp(args.get("2")));
                    args.remove("2");
                    values.add("FROM");
                    values.add("LEMMA");
                }
            }
        } else if (args.get("1").equals("lang")) {
            if (args.get("2") != null) {
                args.put("lang", args.get("2"));
            }
            if (args.get("3") != null) {
                args.put("word1", cleanUp(args.get("3")));
                values.add("LEMMA");
            } else {
                args.put("1", "_etyl");
                values.add("LANGUAGE");
            }
        } else if (args.get("1").equals("blend") || args.get("1").equals("com") || args.get("1").equals("compound")) {
            //examples:
            //{{blend|digital|literati|lang=en}},
            //{{blend|he|תַּשְׁבֵּץ|tr1=tashbéts|t1=crossword puzzle|חֵץ|t2=arrow|tr2=chets}}
            //{{compound|crow|bar|lang=en}}
            //{{compound||alt1=solis-|luu|gloss2=bone|lang=fi}}
	    //compound: you must provide at least one part of the compound
	    //blend: if only given one word returns word1 + ?Term
            int offset = 0;
            String language = args.get("lang");
            if (language == null) {
                offset = 1;
                language = args.get("2");
                if (language != null && language != "") {
                    args.remove("2");
                } else {
                    language = lang;
                }
            }
            args.put("lang", language);
            for (int kk = 2 + offset; kk < 12; kk++) {
                String key = Integer.toString(kk);
                if (args.get(key) != null && !args.get(key).equals("")) {
                    args.put("word" + Integer.toString(kk - 1 - offset), cleanUp(args.get(key)));
                    args.remove(key);
                } else {
                    key = "alt" + Integer.toString(kk - 1 - offset);
                    if (args.get(key) != null) {
                        args.put("word" + Integer.toString(kk - 1 - offset), cleanUp(args.get(key)));
                        args.remove(key);
                    } else {
                        break;
                    }
                }
            }
            //args.put("1", "_compound");??
            values.add("FROM");
            values.add("LEMMA");
	    values.add("STOP");
        } else if (args.get("1").equals("etycomp")) {
	    //etycomp template can be given only word1
            //e.g.:
            //{{etycomp|lang1=de|inf1=|case1=|word1=dumm|trans1=dumb|lang2=|inf2=|case2=|word2=Kopf|trans2=head}}
            //also from the documentation: All parameters except word1= can be omitted.
            for (int kk = 1; kk < 12; kk++) {
                if (args.get("word" + Integer.toString(kk)) != null && !args.get("word" + Integer.toString(kk)).equals("")) {
                    args.put("word" + Integer.toString(kk), cleanUp(args.get("word" + Integer.toString(kk))));
                }
                if (args.get("lang" + Integer.toString(kk)) == null) {
                    args.put("lang" + Integer.toString(kk), lang);
                }
            }
            args.put("1", "_etycomp");
            values.add("FROM");
            values.add("LEMMA");
	    values.add("STOP");
        } else if (args.get("1").equals("_etycomp")) {
            values.add("FROM");
            values.add("LEMMA");
	    values.add("STOP");
	} else if (args.get("1").equals("mult")) {
	    values.add("FROM");
	    values.add("LEMMA");
	    values.add("STOP");
        } else if (args.get("1").equals("infix")) {//no shortening
            //You must provide a base term and an infix.
            //e.g.:
            //From {{etyl|ceb|-}} {{infix|bata|in|lang=ceb}}
            //{{infix|bitch|iz||lang=en}}
            int offset = 0;
            if (args.get("lang") == null) {
                offset = 1;
                args.put("lang", args.get("2"));//if args.get("2") == "" lua gives error
                args.remove("2");
            }
            args.put("word1", cleanUp(args.get(Integer.toString(2 + offset))));//base
            args.remove(Integer.toString(2 + offset));
            args.put("word2", addFinalDash(addInitialDash(cleanUp(args.get(Integer.toString(3 + offset))))));
            args.remove(Integer.toString(3 + offset));
            values.add("LEMMA");
	    values.add("STOP");
        } else if (args.get("1").equals("af") || args.get("1").equals("affix")) {//affix
            //You must provide at least one part and an affix.
            //e.g.:
            //From {{affix|en|non-|sense}}
            //From {{affix|en|multicultural|-ism}}.
            //From {{affix|en|a-|gloss1=not}} + {{der|en|la|calyx}} + {{affix|en|-ine}}.
            //{{affix|en|over-|weight}}
	    //affix|he|לילה|alt1=לֵיל|tr1=leil|t1=night or evening of|כָּל|tr2=kol|t2=all of|ה־|tr3=ha-|t3=the|קָדוֹשׁ|alt4=קְּדוֹשִׁים|tr4=k'doshim|t4=saints
	    args.put("lang", args.get("2"));
	    args.put("word1", cleanUp(args.get("3")));
	    args.remove("3");
	    values.add("LEMMA");
            if (args.get("4") != null && !args.get("4").equals("")) {
                args.put("word2", cleanUp(args.get("4")));
		args.remove("4");
	    } 
	    if (args.get("5") != null && !args.get("5").equals("")) {
		args.put("word3", cleanUp(args.get("5")));
		args.remove("5");
		values.add("PLUS");
		values.add("LEMMA");
		if (args.get("6") != null && !args.get("6").equals("")) {
		    args.put("word4", cleanUp(args.get("6")));
		    args.remove("6");
		    values.add("PLUS");
		    values.add("LEMMA");
		}
		values.add("STOP");
            } else {
		values.add("PLUS");
	    }
	} else if (args.get("1").equals("pre") || args.get("1").equals("prefix")) {//prefix
	    //{{prefix|poly|lang=en}}  -> poly- +
	    //{{prefix|poly|theism|lang=en}}  -> poly- + theism
            //"You must provide at least one prefix."
            int offset = 0;
            if (args.get("lang") == null) {
                offset = 1;
                if (!args.get("2").equals("")) {
                    args.put("lang", args.get("2"));
		    args.remove("2");  
                } else {
                    args.put("lang", lang);
                }
            }
	    String prefix = addFinalDash(cleanUp(args.get(Integer.toString(2 + offset))));	    
            args.put("word1", prefix);//prefix
            args.remove(Integer.toString(2 + offset));
	    values.add("LEMMA");
	    
            for (int kk = 3 + offset; kk < 12; kk++) {
                if (args.get(Integer.toString(kk)) != null) {
                    args.put("word" + Integer.toString(kk - 1 - offset), args.get(Integer.toString(kk)));
                } else {
		    if (kk - 1 - offset == 2){//word2 is not specified -> add PLUS
			values.add("PLUS");
		    }
                    break;
                }
            }
	} else if (args.get("1").equals("con") || args.get("1").equals("confix")) {//confix
            //You must specify a prefix part, an optional base term and a suffix part.
            //e.g.:
            //{{confix|atmo|sphere|lang=en}}
            int offset = 0;
            if (args.get("lang") == null) {
                offset = 1;
                if (!args.get("2").equals("")) {
                    args.put("lang", args.get("2"));
		    args.remove("2");
                } else {
                    args.put("lang", lang);
                }
            }
            if (args.get(Integer.toString(2 + offset)) != null && !args.get(Integer.toString(2 + offset)).equals("")) {
                args.put("word1", addFinalDash(cleanUp(args.get(Integer.toString(2 + offset)))));//prefix-
		args.remove(Integer.toString(2 + offset));
            }
            if (args.get(Integer.toString(4 + offset)) != null && !args.get(Integer.toString(4 + offset)).equals("")) {
                args.put("word3", addInitialDash(cleanUp(args.get(Integer.toString(4 + offset)))));//-suffix
                args.remove(Integer.toString(4 + offset));
                if (!args.get(Integer.toString(3 + offset)).equals("")) {
                    args.put("word2", cleanUp(args.get(Integer.toString(3 + offset))));//base
                    args.remove(Integer.toString(3 + offset));
                }
            } else if (args.get(Integer.toString(3 + offset)) != null && !args.get(Integer.toString(3 + offset)).equals("")) {
                args.put("word2", addInitialDash(cleanUp(args.get(Integer.toString(3 + offset)))));//suffix
                args.remove(Integer.toString(3 + offset));
            }
            values.add("LEMMA");
	    values.add("STOP");
	} else if (args.get("1").equals("suf") || args.get("1").equals("suffix")) {//suffix
	    //suf: You must provide at least one suffix.
            //examples:
            //{{bor|eo|en|boycott}} {{suffix||i|lang=eo}} -> Borrowing from English boycott + -i.
	    //from {{etyl|grc|mul}} {{m|grc|χλωρός||pale green}} {{suffix||ophyta|lang=mul}}
            //{{suffix|Graham|ite|lang=en}}
	    //{{suffix|tessere|tura|lang=it}}.
            int offset = 0;
            if (args.get("lang") == null) {
                offset = 1;
                if (!args.get("2").equals("")) {
                    args.put("lang", args.get("2"));
		    args.remove("2");   
                } else {
                    args.put("lang", lang);
                }
            }
	    
            boolean base = false;
            String key = Integer.toString(2 + offset);
            if (args.get(key) != null && !args.get(key).equals("")) {
                args.put("word1", cleanUp(args.get(key)));//base
                args.remove(key);
                base = true;
            } else {
                if (args.get("alt1") != null && !args.get("alt1").equals("")) {
                    args.put("word1", cleanUp(args.get("alt1")));//base
                    args.remove("alt1");
                    base = true;
                }
            }
            for (int kk = 3 + offset; kk < 12; kk++) {
                key = Integer.toString(kk);
                if (args.get(key) == null) {
                    key = "alt" + Integer.toString(kk - 1 - offset);
                    if (args.get(key) == null) {
                        break;
                    }
                }
                if (!args.get(key).equals("")) {
		    String word = addInitialDash(cleanUp(args.get(key)));
                    args.put("word" + Integer.toString(kk - 1 - offset), word);//suffixes
                    args.remove(key);
                }
            }
	    if (!base){
		values.add("PLUS");
	    }
	    values.add("LEMMA");
	    values.add("STOP");
	} else if (args.get("1").equals("circumfix")) {//no shortening for circumfix
            //You must specify a prefix part, a base term and a suffix part.
            //e.g.:
            //From {{circumfix|nl|be|wonder|en}}.     ->   From be- + wonder + -en.
	    //{{circumfix|jv|N|ugem|i}}
            int offset = 0;
            if (args.get("lang") == null) {
                offset = 1;
                if (!args.get("2").equals("")) {
		    args.put("lang", args.get("2"));
		    args.remove("2");
                } 
            } 
            if (args.get(Integer.toString(2 + offset)) != null && !args.get(Integer.toString(2 + offset)).equals("")) {
                args.put("word1", addFinalDash(cleanUp(args.get(Integer.toString(2 + offset)))));
		args.remove(Integer.toString(2 + offset));
            }
            if (args.get(Integer.toString(3 + offset)) != null && !args.get(Integer.toString(3 + offset)).equals("")) {
                args.put("word2", cleanUp(args.get(Integer.toString(3 + offset))));//base
                args.remove(Integer.toString(3 + offset));
            }
            if (args.get(Integer.toString(4 + offset)) != null && !args.get(Integer.toString(4 + offset)).equals("")) {
                args.put("word3", addInitialDash(cleanUp(args.get(Integer.toString(4 + offset)))));//suffix
                args.remove(Integer.toString(4 + offset));
            }
            values.add("LEMMA");
	    values.add("STOP");
	} else if (args.get("1").equals("clipping")) {//no shortening for clipping
            //e.g.:
            //{{clipping|penetration test|lang=en}}  --> Clipping of penetration test
            //{{clipping|lang=en|unicycle}}
            //{{clipping|unicycle}}
            if (args.get("lang") == null) {
                args.put("lang", lang);
            }
            for (int kk = 2; kk < 12; kk++) {
                if (args.get(Integer.toString(kk)) != null) {
                    if (!args.get(Integer.toString(kk)).equals("")) {
                        args.put("word" + Integer.toString(kk), cleanUp(args.get(Integer.toString(kk))));
                        args.remove(Integer.toString(kk));
                    }
                } else {
                    break;
                }
            }
            values.add("FROM");
            values.add("LEMMA");
	} else if (args.get("1").equals("hu-prefix")) {
            if (args.get("2") != null && !args.get("2").equals("")) {
                args.put("word1", addFinalDash(cleanUp(args.get("2"))));//prefix-
                args.remove("2");
            }
            if (args.get("3") != null && !args.get("3").equals("")) {
                args.put("word2", addInitialDash(cleanUp(args.get("3"))));//base
                args.remove("3");
            } else {
                values.add("PLUS");
            }
            args.put("lang", "hu");
            values.add("LEMMA");
	} else if (args.get("1").equals("hu-suffix")) {
	    //e.g.: {{hu-suffix|barát|t1=friend|ság|pos=n}}  -> barát (“friend”) + -ság
	    //e.g.: {{hu-suffix|bara||pos=n}} -> base + -suffix
	    //e.g.: {{hu-suffix||ság|pos=n}} -> + -ság
	    
            if (args.get("2") != null && !args.get("2").equals("")) {
                args.put("word1", cleanUp(args.get("2")));//base
                args.remove("2");
            } else {
                values.add("PLUS");
            }
            if (args.get("3") != null && !args.get("3").equals("")) {
                args.put("word2", addFinalDash(addInitialDash(cleanUp(args.get("3")))));//-suffix
                args.remove("3");
            }
            args.put("lang", "hu");
            values.add("LEMMA");
	    values.add("STOP");
	} else if (args.get("1").equals("term")) {//e.g.: {{term|de-|di-|away}}
	    if (args.get("lang") == null) {
		args.put("lang", lang);
            }
            if (args.get("2").equals("")) {
                args.remove("2");
                args.put("word1", cleanUp(args.get("3")));
                args.remove("3");
            } else {
                args.put("word1", cleanUp(args.get("2")));
		args.remove("2");
	    }
	    values.add("LEMMA");
	} else if (args.get("1").equals("etystub") || args.get("1").equals("rfe") || args.get("1").equals("unk.")) {
            values.add("STOP");
	} else if (args.get("1").equals("-er")) {
            args.put("word1", cleanUp(args.get("2")));
            args.remove("2");
            args.put("word2", cleanUp(args.get("1")));
            args.put("1", "_compound");
            args.put("lang", "en");
            values.add("LEMMA");
	    values.add("STOP");
	} else if (args.get("1").equals("-or")) {
            args.put("word1", cleanUp(args.get("2")));
            args.remove("2");
            args.put("word2", cleanUp(args.get("1")));
            args.put("1", "_compound");
            args.put("lang", "en");
            values.add("LEMMA");
	    values.add("STOP");
	} else if (args.get("1").equals("fi-form of")) {
	    values.add("FROM");
	    values.add("LEMMA");
	    args.put("word1", cleanUp(args.get("2")));
	    args.put("lang", "fi");
	    args.remove("2");
	} else if (args.get("1").equals("he-m") || args.get("1").equals("m/he") ) {//{{m/he|אוכל|dwv=אֹכֶל||food}}
	    args.put("lang", "he");
	    if (args.get("2") != null && !args.get("2").equals("")) {
		values.add("LEMMA");
		args.put("word1", cleanUp(args.get("2")));
		args.remove("2");
	    } else {
		args.clear();
		values = null;
	    }
	} else if (args.get("1").equals("got-nom form of")){
	    values.add("LEMMA");
	    args.put("word1", cleanUp(args.get("2")));
	    args.remove("2");
	    args.put("lang", "got");
	} else if (args.get("1").equals("ar-root")){
	    args.put("lang", "ar");
	    StringBuilder word = new StringBuilder();
	    for (int kk = 2; kk < 12; kk++) {
		if (args.get(kk) != null && !args.get(kk).equals("")) {
		    if (kk > 2){
			word.append(" ");
		    }
		    word.append(args.get(kk));
		}
	    }
	    if (word != null && !word.equals("")) {
		args.put("word1", word.toString());
		values.add("FROM");
		values.add("LEMMA");
	    } else {
		args.clear();
		values = null;
	    }
	} else if (args.get("1").equals("sense")) {
	    log.debug("Found sense template {}", string);
	    values.add("SENSE");
	} else if (args.get("1").equals("eye dalect")) {
	    if (args.get("2") == null) {
		values.add("FROM");
	    } else {
		args.put("word1", cleanUp(args.get("2")));
		args.remove("2");
		values.add("LEMMA");
	    }
	} else if (args.get("1").equals("Han compound")) {
	    for (int kk = 2; kk < 12; kk++) {
		if (args.get(Integer.toString(kk)) != null) {
		    if (!args.get(Integer.toString(kk)).equals("")) {
			args.put("word" + Integer.toString(kk - 2), cleanUp(args.get(Integer.toString(kk))));
			args.remove(Integer.toString(kk));
		    }
		} else {
		    break;
		}
	    }
	    args.put("lang", "hni");
	    values.add("FROM");
	    values.add("LEMMA");
	    values.add("STOP");
	    //} else if (args.get("1").startsWith("Han ety"){//Han etyl, Han etym
	    //ignore                            
	} else if (args.get("1").equals("Han simp")) {
	    if (args.get("2") != null) {
		args.put("lang", "hni");
		args.put("word1", cleanUp(args.get("2")));
		args.remove("2");
		values.add("FROM");
		values.add("LEMMA");
	    } else {
		args.clear();
		values = null;
		log.debug("Skipping Han simp template {}", string);
	    }
	} else if (args.get("1").equals("rel-top")) {
	    if (args.get("2").equals("cognates") || args.get("2").equals("detailed etymology") || args.get("2").equals("Etymology Theories")) {
		values.add("STOP");
	    } else {
		log.debug("Ignoring template {} in either Etymology or Derived terms or Descendants section", string);
		args.clear();
		string = null;
		values = null;
	    }
	} else if (args.get("1").equals("ja-r") || args.get("1").equals("ja-l")) {// {{ja-r|宮古島|^みやこじま|[[w:Miyako Island|Miyako Island]]; [[w:Miyakojima, Okinawa|Miyakojima, Okinawa]]}}
	    args.put("lang", "ja");//Japanese
	    args.put("word1", cleanUp(args.get("2")));
	    values.add("LEMMA");
	} else if (args.get("1").equals("SI link")) {
	    args.put("lang", lang);
	    args.put("word1", cleanUp(args.get("2")));
	    values.add("LEMMA");
	} else if (args.get("1").equals("jbo-etym")) {
	    //make a copy of args.keySet()
	    ArrayList<String> tt = new ArrayList<>();
	    for (String kk : args.keySet()) {
		tt.add(kk);
	    }
	    int counter = 1;
	    for (String k : tt) {
		if (args.get(k) != null) {
		    if (k.endsWith("_t")) {
			k = k.substring(0, k.length() - 2);
			args.put("lang" + Integer.toString(counter), k);
			args.put("word" + Integer.toString(counter), cleanUp(args.get(k + "_t")));
			args.remove(k + "_t");
			String tr = args.get(k + "_tr");
			if (tr != null) {
			    args.put("tr" + counter, tr);
			    args.remove(k + "_tr");
			}
			args.remove(k);
			counter++;
		    }
		}
	    }
	    args.put("lang", "jbo");
	    values.add("FROM");
	    values.add("LEMMA");
	} else if (args.get("1").equals("ko-etym-native")){
	    if (args.get("3") != null){
		args.put("lang", "ko");
		args.put("word1", cleanUp(args.get("3")));
		args.remove("3");
		values.add("FROM");
		values.add("LEMMA");
	    } else {
		args.clear();
		values = null;
	    }
	} else if (args.get("1").equals("zh-psm")) {
	    args.put("word1", cleanUp(args.get("2")));
	    values.add("LEMMA");
	    args.remove("2");
	} else if (args.get("1").startsWith("vi-l") || args.get("1").equals("zh-l") || args.get("1").equals("zh-m") || args.get("1").equals("ko-l") || args.get("1").equals("och-l") || args.get("1").equals("th-l") || args.get("1").equals("ltc-l")) {
	    if (args.get("1").startsWith("vi-l")){
		args.put("lang", "vi");
	    } else if (args.get("1").equals("zh-l") || args.get("1").equals("zh-m") || args.get("1").equals("ltc-l")){ //{{ltc-l|覺|to awake, get insight}} 
		args.put("lang", "zh");
	    } else if (args.get("1").equals("ko-l")){// {{ko-l|대문||[[gate]]|大門}}
		args.put("lang", "ko");
	    } else if (args.get("1").equals("och-l")){// och-l|圈|circle
		args.put("lang", "och");
	    } else if (args.get("1").equals("th-l")){
		args.put("lang", "th");
	    }
	    if (args.get("2") != null) {
		args.put("word1", cleanUp(args.get("2")));
		values.add("LEMMA");
		args.remove("2");
		if (args.get("3") != null) {
		    args.put("gloss1", args.get("3").replaceAll("\\[", "").replaceAll("\\]", ""));
		    args.remove("3");
		}
		if (args.get("4") != null) {
		    args.put("synonim", args.get("4"));
		    args.remove("4");
		}
	    } else {
		args.clear();
		values = null;
	    }
	} else if (args.get("1").equals("vi-etym-sino") || args.get("1").equals("ko-etym-Sino") || args.get("1").equals("ko-etym-sino")) {//this is imprecise
	    //ko-etym-sino|生物|[[organism]]|化學|[[chemistry]]
	    int nWords = 0;
	    args.put("lang", "zh");//TODO :check this
	    if (args.get("2") != null) {
		args.put("word1", cleanUp(args.get("2")));
		args.remove("2");
		nWords ++;
	    } else {
		values = null;
		args.clear();
	    }
	    if (args.get("3") != null) {
		args.put("gloss1", args.get("3").replaceAll("\\[", "").replaceAll("\\]", ""));
		args.remove("3");
	    }
	    if (args.get("4") != null) {
		args.put("word2", cleanUp(args.get("4")));
		args.remove("4");
		nWords ++;
	    }
	    if (args.get("5") != null) {
		args.put("gloss2", args.get("5").replaceAll("\\[", "").replaceAll("\\]", ""));
		args.remove("5");
	    }
	    if (args.get("6") != null) {
		args.put("word3", cleanUp(args.get("6")));
		args.remove("6");
		nWords ++;
	    }
	    if (args.get("7") != null) {
		args.put("gloss3", args.get("7"));
		args.remove("7");
	    }
	    if (nWords > 0){
		values.add("LEMMA");
	    }
	    if (nWords > 1){
		values.add("STOP");
	    }
	    if (nWords == 0){
		args.clear();
		values = null;
	    }
	} else {
            log.debug("Ignoring template {} in either Etymology or Derived terms or Descendants section", string);
            args.clear();
            string = null;
            values = null;
	}
	this.normalizeLang();
    }
	    
    public void parseLink(String group, String lang) {
	args = new HashMap<String, String>();
	string = group;
        values = new ArrayList<String>();
		
        ArrayList<String> splitArgs = WikiTool.splitUnlessInTemplateOrLink(string, '|');
        ArrayList<String> splitColumn = WikiTool.splitUnlessInTemplateOrLink(splitArgs.get(0),':');
	int nCol = splitColumn.size();
	
	String link = null,
	    language = null,
	    word = null;
	// TODO: some links points to pages with colon (e.g. [[w:Zelda II: The Adventure of Link|Zelda II]])
        // Currently, such link is taken with Zelda II as a language.
	int offset = 0;
	if (splitColumn.get(0).length() == 0){//e.g. [[:door]], [[:door#portuguese]], [[:door#verb]]
	    offset = 1;
	}
	
	if (nCol == 1 + offset){//e.g. [[door]], [[door#portuguese]], [[door#verb]], [[:door]], [[:door#portuguese]], [[:door#verb]]
            args.put("link", "wiktionary");
	    ArrayList<String> splitPound = WikiTool.splitUnlessInTemplateOrLink(splitColumn.get(0 + offset),'#');
	    word = splitPound.get(0).trim();
	    if (splitPound.size() == 2){//e.g.            [[door#portuguese]], [[door#verb]]
		language = EnglishLangToCode.threeLettersCode(splitPound.get(1).trim());
		if (language == null){
		    language = lang;
		} 
	    } else {
		language = lang;
	    }
	} else if (nCol == 2 + offset) {//e.g. [[en:door]], [[en:door#portuguese]], [[en:door#verb]], [[:en:door]], [[:en:door#portuguese]], [[:en:door#verb]]
	    ArrayList<String> splitPound = WikiTool.splitUnlessInTemplateOrLink(splitColumn.get(1 + offset),'#');
	    word = splitPound.get(0).trim();
	    link = splitColumn.get(0 + offset).trim().toLowerCase();
	    if (link.equals("wiktionary")){
		args.put("link", "wiktionary");
		language = lang;
	    } else if (link.equals("image") || link.equals("category") || link.equals("file") || link.equals("wikisource") || link.equals("s") || link.equals("appendix") || link.equals("citations") || link.equals("special") || link.equals("image") || link.equals("meta") || link.equals("m") || link.equals("wikipedia") || link.equals("w") || link.equals("d")){// || link.equals("folk etymology") || link.equals("habitational")){
		log.debug("Ignoring link {}", string);
		args = null;
		string = null;
		values = null;
		return;
	    } else {
	        language = EnglishLangToCode.threeLettersCode(splitColumn.get(0 + offset).trim());
		if (language == null){
		    log.debug("Ignoring link {}", string);
		    args = null;
		    string = null;
		    values = null;
		    return;
		} else {
		    args.put("link", "wiktionary");
		    log.debug("Parsing link {} as link to {} in {}", string, word, language);
		}
	    } 
	} else if (nCol == 3 + offset) {//e.g. [[q:en:door]], [[:q:en:door]] 
	    ArrayList<String> splitPound = WikiTool.splitUnlessInTemplateOrLink(splitColumn.get(2 + offset),'#');
	    word = splitPound.get(0).trim();
	    language = EnglishLangToCode.threeLettersCode(splitColumn.get(1 + offset).trim());
	    if (language == null){
		log.debug("Ignoring link {}", string);
		args = null;
		string = null;
		values = null;
		return;
	    } else {
		link = splitColumn.get(0 + offset).trim();
	    }
	} else {
	    log.debug("Ignoring link {}", string);
	    args = null;
	    string = null;
	    values = null;
	    return;
	}
		
	//parse word
	if (word.equals("")){ //[[#English|tule]]
	    word = splitArgs.get(1).trim();
	} else if (word.equalsIgnoreCase("ateji")) {
	    log.debug("Ignoring link {}", string);
	    args = null;
	    string = null;
	    values = null;
	    return;
	}
	
	args.put("word1", cleanUp(word));
	
	args.put("1", "l");
	args.put("lang", language);
	values.add("LEMMA");
	string = "l|" + args.get("lang") + "|" + args.get("word1");
        this.normalizeLang();
    }
    
    private void normalizeLang() {
        if (this.args != null) {
            for (String key : this.args.keySet()) {
                if (key.startsWith("lang")) {
                    String languageCode = EnglishLangToCode.threeLettersCode(this.args.get(key));
                    if (languageCode != null) {
                        this.args.put(key, languageCode);
                    }
                }//else leave it as is
            }
        }
    }
    
    /**
     * Given a String, this function replaces some symbols
     * e.g., cleanUp("[[door]],[[dur]]") returns "door,dur"
     * e.g., cleanUp("o'hare") returns "o__hare"
     * e.g., cleanUp("*duhr") returns "_duhr"
     *
     * @param word an input String
     * @return a String where some characters have been replaced
     */
    public String cleanUp(String word) {
        word = word.replaceAll("\\[", "").replaceAll("\\]", "").trim().replaceAll("'", "__").replaceAll("^w:", "").replaceAll("-\\*", "-_").replaceAll("\\*", "_");
        return word;
    }
    
    public String toString() {
        return "[" + (null==values ? "[]" : values.toString()) + "|" + string + "]";
    }
    
    public String addInitialDash(String word) {
	if (word.startsWith("*")){
	    if (!word.startsWith("*-")){
		word = word.replace("*","*-");
	    }
	} else if (!word.startsWith("-")){
	    word = "-" + word;
	}
	return word;
    }
    
    public String addFinalDash(String word) {
	if (!word.endsWith("-")){
	    word = word + "-";
	}
	return word;
    }
}
	
