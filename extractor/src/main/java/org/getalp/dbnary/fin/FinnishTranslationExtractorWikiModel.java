package org.getalp.dbnary.fin;

import info.bliki.wiki.filter.WikipediaParser;

import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.getalp.blexisma.api.ISO639_3;
import org.getalp.dbnary.AbstractWiktionaryExtractor;
import org.getalp.dbnary.DbnaryWikiModel;
import org.getalp.dbnary.WiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.jpn.JapaneseTranslationsExtractorWikiModel;
import org.getalp.dbnary.wiki.WikiPatterns;
import org.getalp.dbnary.wiki.WikiTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FinnishTranslationExtractorWikiModel extends DbnaryWikiModel {
	
	private WiktionaryDataHandler delegate;
	private Logger log = LoggerFactory.getLogger(JapaneseTranslationsExtractorWikiModel.class);

	public FinnishTranslationExtractorWikiModel(WiktionaryDataHandler we, Locale locale, String imageBaseURL, String linkBaseURL) {
		this(we, (WiktionaryIndex) null, locale, imageBaseURL, linkBaseURL);
	}
	
	public FinnishTranslationExtractorWikiModel(WiktionaryDataHandler we, WiktionaryIndex wi, Locale locale, String imageBaseURL, String linkBaseURL) {
		super(wi, locale, imageBaseURL, linkBaseURL);
		this.delegate = we;
	}

	
	// TODO: handle entries where translations refer to the translations of another term (form: Kts. [[other entry]]).
	public void parseTranslationBlock(String block) {
		// Heuristics: if the translation block uses kohta macro, we assume that ALL translation data is available in the macro.
		if (block.contains("{{kohta")) {
			parseTranslationBlockWithBliki(block);
		} else {
			extractTranslations(block);
		}
	}
	
	public void parseTranslationBlockWithBliki(String block) {
		initialize();
		if (block == null) {
			return;
		}
		WikipediaParser.parse(block, this, true, null);
		initialize();
	}

	private static final HashSet<String> transMacroWithNotes = new HashSet<String>();
	static {
		transMacroWithNotes.add("xlatio");
		transMacroWithNotes.add("trad-");

	}
	
	@Override
	public void substituteTemplateCall(String templateName,
			Map<String, String> parameterMap, Appendable writer)
			throws IOException {
		if ("kohta".equals(templateName)) {
			// kohta macro contains a set of translations with no usage note.
			// Either: (1) arg 1 is the sens number and arg2 is the gloss, arg3 are translations and arg 4 is final
			// Or: arg1 is translations and arg 2 is final
			int translationPositionalArg = findTranslations(parameterMap); 
			String xans = parameterMap.get(Integer.toString(translationPositionalArg));
			String gloss = computeGlossValue(parameterMap, translationPositionalArg);
			extractTranslations(xans, gloss);
			
		} else if ("käännökset/korjattava".equals(templateName) || "kään/korj".equals(templateName) || "korjattava/käännökset".equals(templateName)) { 
			// Missing translation message, just ignore it
		} else {
			 log.debug("Called template: {} while parsing translations of: {}", templateName, delegate.currentLexEntry());
			// Just ignore the other template calls (uncomment to expand the template calls).
			// super.substituteTemplateCall(templateName, parameterMap, writer);
		}
	}

	StringBuffer glossbuff = new StringBuffer();
	private String computeGlossValue(Map<String, String> parameterMap, int translationPositionalArg) {
		glossbuff.setLength(0);
		int i = 1;
		while (i != translationPositionalArg) {
			glossbuff.append(parameterMap.get(Integer.toString(i)).trim());
			glossbuff.append("|");
			i++;
		}
		if (glossbuff.length() > 0) glossbuff.setLength(glossbuff.length()-1);
		return glossbuff.toString();
	}

	private int findTranslations(Map<String, String> parameterMap) {
		// The number of args should no exceed 7 (arbitrary) 
		// Find last non null arg
		int p = 7;
		while (p != 1 && parameterMap.get(Integer.toString(p)) == null) {
			p--;
		}
		String v = parameterMap.get(Integer.toString(p));
		if (1 != p && ("".equals(v) || "loppu".equals(v))) {
			// The last parameter is the closing one... It should be mandatory
			p--;
		}
		return p;
	}	
	
	protected final static String carPatternString;
	protected final static String macroOrLinkOrcarPatternString;


	static {
		// DONE: Validate the fact that links and macro should be on one line or may be on several...
		// DONE: for this, evaluate the difference in extraction !
		
		// les caractères visible 
		carPatternString=
				new StringBuilder().append("(.)")
				.toString();

		// TODO: We should suppress multiline xml comments even if macros or line are to be on a single line.
		macroOrLinkOrcarPatternString = new StringBuilder()
		.append("(?:")
		.append(WikiPatterns.macroPatternString)
		.append(")|(?:")
		.append(WikiPatterns.linkPatternString)
		.append(")|(?:")
		.append("(:*\\*)")
		.append(")|(?:")
		.append("(\\*:)")
		.append(")|(?:")
		.append(carPatternString)
		.append(")").toString();



	}
	protected final static Pattern carPattern;
	protected final static Pattern macroOrLinkOrcarPattern;


	static {
		carPattern = Pattern.compile(carPatternString);
		macroOrLinkOrcarPattern = Pattern.compile(macroOrLinkOrcarPatternString, Pattern.MULTILINE|Pattern.DOTALL);
		
	}

	public void extractTranslations(String block) {
		extractTranslations(block, null);
	}
	
	public void extractTranslations(String block, String gloss) {
		Matcher macroOrLinkOrcarMatcher = macroOrLinkOrcarPattern.matcher(block);
		final int INIT = 1;
		final int LANGUE = 2;
		final int TRAD = 3;

	
		
		int ETAT = INIT;

		String currentGlose = gloss;
		String lang=null, word= ""; 
		String usage = "";       
		String langname = "";
		String previousLang = null;
		
		while (macroOrLinkOrcarMatcher.find()) {

			String macro = macroOrLinkOrcarMatcher.group(1);
			String link = macroOrLinkOrcarMatcher.group(3);
			String star = macroOrLinkOrcarMatcher.group(5);
			String starcont = macroOrLinkOrcarMatcher.group(6);
			String character = macroOrLinkOrcarMatcher.group(7);

			switch (ETAT) {

			case INIT:
				if (macro!=null) {
					if (macro.equalsIgnoreCase("ylä"))  {
						if (macroOrLinkOrcarMatcher.group(2) != null) {
							currentGlose = macroOrLinkOrcarMatcher.group(2);
						} else {
							currentGlose = null;
						}

					} else if (macro.equalsIgnoreCase("ala")) {
						currentGlose = null;
					} else if (macro.equalsIgnoreCase("keski")) {
						//ignore
					}
				} else if(link!=null) {
					//System.err.println("Unexpected link while in INIT state.");
				} else if (starcont != null) {
					log.debug("Unexpected point continuation while in INIT state.");
				} else if (star != null) {
					ETAT = LANGUE;
				} else if (character != null) {
					if (character.equals(":")) {
						//System.err.println("Skipping ':' while in INIT state.");
					} else if (character.equals("\n") || character.equals("\r")) {

					} else if (character.equals(",")) {
						//System.err.println("Skipping ',' while in INIT state.");
					} else {
						//System.err.println("Skipping " + g5 + " while in INIT state.");
					}
				}

				break;

			case LANGUE:

				if (macro!=null) {
					if (macro.equalsIgnoreCase("ylä"))  {
						if (macroOrLinkOrcarMatcher.group(2) != null) {
							currentGlose = macroOrLinkOrcarMatcher.group(2);
						} else {
							currentGlose = null;
						}
						langname = ""; word = ""; usage = "";
						ETAT = INIT;
					} else if (macro.equalsIgnoreCase("ala")) {
						currentGlose = null;
						langname = ""; word = ""; usage = "";
						ETAT = INIT;
					} else if (macro.equalsIgnoreCase("keski")) {
						langname = ""; word = ""; usage = "";
						ETAT = INIT;
					} else {
						langname = macro;
						String l = ISO639_3.sharedInstance.getIdCode(langname);
						if (l != null) {
							langname = l;
						}
					}
				} else if(link!=null) {
					//System.err.println("Unexpected link while in LANGUE state.");
				} else if (starcont != null) {
					lang = previousLang;
					ETAT = TRAD;
				} else if (star != null) {
					//System.err.println("Skipping '*' while in LANGUE state.");
				} else if (character != null) {
					if (character.equals(":")) {
						lang = langname.trim();
						lang=AbstractWiktionaryExtractor.supParenthese(lang);
						lang =SuomiLangToCode.triletterCode(lang);
						langname = "";
						ETAT = TRAD;
					} else if (character.equals("\n") || character.equals("\r")) {
						//System.err.println("Skipping newline while in LANGUE state.");
					} else if (character.equals(",")) {
						//System.err.println("Skipping ',' while in LANGUE state.");
					} else {
						langname = langname + character;
					}
				} 

				break ;
				// TODO: maybe extract words that are not linked (currently kept in usage, but dropped as translation word is null).
			case TRAD:
				if (macro!=null) {
					if (macro.equalsIgnoreCase("ylä"))  {
						if (macroOrLinkOrcarMatcher.group(2) != null) {
							currentGlose = macroOrLinkOrcarMatcher.group(2);
						} else {
							currentGlose = null;
						}
						//if (word != null && word.length() != 0) {
						//	if(lang!=null){
						//		delegate.registerTranslation(lang, currentGlose, usage, word);
						//	}
						//}
						langname = ""; word = ""; usage = ""; lang=null;
						ETAT = INIT;
					} else if (macro.equalsIgnoreCase("ala")) {
						if (word != null && word.length() != 0) {
							if(lang!=null){
								delegate.registerTranslation(lang, currentGlose, usage, word);
							}
						}
						currentGlose = null;
						langname = ""; word = ""; usage = ""; lang=null;
						ETAT = INIT;
					} else if (macro.equalsIgnoreCase("kski")) {
						if (word != null && word.length() != 0) {
							if(lang!=null){
								delegate.registerTranslation(lang, currentGlose, usage, word);
							}
						}
						langname = ""; word = ""; usage = ""; lang = null;
						ETAT = INIT;
					} else if (macro.equalsIgnoreCase("käännös") || macro.equalsIgnoreCase("l")) {
						Map<String,String> argmap = WikiTool.parseArgs(macroOrLinkOrcarMatcher.group(2));
						if (null != word && word.length() != 0) log.debug("Word is not null ({}) when handling käännös macro in {}", word, this.delegate.currentLexEntry());
						String l = argmap.get("1");
						if (null != l && (null != lang) && ! lang.equals(ISO639_3.sharedInstance.getIdCode(l))) {
							log.debug("Language in käännös macro does not map language in list in {}", this.delegate.currentLexEntry());
						}
						word = argmap.get("2");
						argmap.remove("1"); argmap.remove("2");
						if (! argmap.isEmpty()) usage = argmap.toString();
					} else {
						usage = usage + "{{" + macro + "}}";
					}
				} else if (link!=null) {
					word = word + " " + link;
				} else if (starcont != null) {
					// System.err.println("Skipping '*:' while in LANGUE state.");
				} else if (star != null) {
					//System.err.println("Skipping '*' while in LANGUE state.");
				} else if (character != null) {
					if (character.equals("\n") || character.equals("\r")) {
						usage = usage.trim();
						// System.err.println("Registering: " + word + ";" + lang + " (" + usage + ") " + currentGlose);
						if (word != null && word.length() != 0) {
							if(lang!=null){
								delegate.registerTranslation(lang, currentGlose, usage, word);
							}
						} else if (usage.length() != 0) {
							log.debug("Non empty usage ({}) while word is null in: {}", usage, delegate.currentLexEntry());
						}
						previousLang = lang;
						lang = null; 
						usage = "";
						word = "";
						ETAT = INIT;
					} else if (character.equals(",")) {
						usage = usage.trim();
						// System.err.println("Registering: " + word + ";" + lang + " (" + usage + ") " + currentGlose);
						if (word != null && word.length() != 0) {
							if(lang!=null){
								delegate.registerTranslation(lang, currentGlose, usage, word);
							}
						}
						usage = "";
						word = "";
					} else {
						usage = usage + character;
					}
				}
				break;
			default: 
				log.error("Unexpected state number: {}", ETAT);
				break; 
			}

		}
	}  

}