package org.getalp.dbnary.jpn;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.getalp.blexisma.api.ISO639_3;
import org.getalp.dbnary.AbstractWiktionaryExtractor;
import org.getalp.dbnary.WiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.wiki.WikiPatterns;
import org.getalp.dbnary.wiki.WikiTool;

public class JapaneseTranslationsExtractorWikiModel {
	
	// static Set<String> ignoredTemplates = new TreeSet<String>();
	// static {
	// 	ignoredTemplates.add("Wikipedia");
	// 	ignoredTemplates.add("Incorrect");
	// }
	
	private WiktionaryDataHandler delegate;
	
	
	public JapaneseTranslationsExtractorWikiModel(WiktionaryDataHandler we) {
		this(we, (WiktionaryIndex) null);
	}
	
	public JapaneseTranslationsExtractorWikiModel(WiktionaryDataHandler we, WiktionaryIndex wi) {
		this.delegate = we;
	}

	
	public void parseTranslations(String translations) {
		// Render the definition to plain text, while ignoring the example template
		// this.delegate.registerTranslation("xxx", null, null, translations);
		extractTranslations(translations);
	}

    protected final static String carPatternString;
	protected final static String macroOrLinkOrcarPatternString;
	
   
    static {
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
		.append("^;([^:\\n\\r]*)") // Term definition
		.append(")|(?:")
		.append(carPatternString)
		.append(")")
		.toString();
    }

    protected final static Pattern macroOrLinkOrcarPattern;
	protected final static Pattern carPattern;
	static {
		carPattern = Pattern.compile(carPatternString);
		macroOrLinkOrcarPattern = Pattern.compile(macroOrLinkOrcarPatternString, Pattern.DOTALL + Pattern.MULTILINE);
	}

	protected final int INIT = 1;
	protected final int LANGUE = 2;
	protected final int TRAD = 3;  

    private void extractTranslations(String translations) {
	Matcher macroOrLinkOrcarMatcher = macroOrLinkOrcarPattern.matcher(translations);
		int ETAT = INIT;

		String currentGlose = null;
		String lang=null, word= ""; 
		String usage = "";       
		String langname = "";

        while (macroOrLinkOrcarMatcher.find()) {

			String macro = macroOrLinkOrcarMatcher.group(1);
			String link = macroOrLinkOrcarMatcher.group(3);
			String star = macroOrLinkOrcarMatcher.group(5);
			String term = macroOrLinkOrcarMatcher.group(6);
			String car = macroOrLinkOrcarMatcher.group(7);

			switch (ETAT) {

			case INIT:
				if (macro!=null) {
					if (macro.equalsIgnoreCase("trans-top") || macro.equalsIgnoreCase("top"))  {
						if (macroOrLinkOrcarMatcher.group(2) != null) {
							currentGlose = macroOrLinkOrcarMatcher.group(2);
						} else {
							currentGlose = null;
						}

					} else if (macro.equalsIgnoreCase("trans-bottom") || macro.equalsIgnoreCase("bottom")) {
						currentGlose = null;
					} else if (macro.equalsIgnoreCase("trans-mid") || macro.equalsIgnoreCase("mid")) {
						//ignore
					} else {
						System.err.println("Got " + macro + " macro while in INIT state. for page: ");// + this.delegate.currentLexEntry());
					}
				} else if(link!=null) {
					System.err.println("Unexpected link " + link + " while in INIT state. for page: ");//+ this.delegate.currentLexEntry());
				} else if (star != null) {
					ETAT = LANGUE;
				} else if (term != null) {
					currentGlose = term;
				} else if (car != null) {
					if (car.equals(":")) {
						//System.err.println("Skipping ':' while in INIT state.");
					} else if (car.equals("\n") || car.equals("\r")) {

					} else if (car.equals(",")) {
						//System.err.println("Skipping ',' while in INIT state.");
					} else {
						//System.err.println("Skipping " + g5 + " while in INIT state.");
					}
				}

				break;

			case LANGUE:

				if (macro!=null) {
					if (macro.equalsIgnoreCase("trans-top") || macro.equalsIgnoreCase("top"))  {
						if (macroOrLinkOrcarMatcher.group(2) != null) {
							currentGlose = macroOrLinkOrcarMatcher.group(2);
						} else {
							currentGlose = null;
						}
						langname = ""; word = ""; usage = "";
						ETAT = INIT;
					} else if (macro.equalsIgnoreCase("trans-bottom") || macro.equalsIgnoreCase("bottom")) {
						currentGlose = null;
						langname = ""; word = ""; usage = "";
						ETAT = INIT;
					} else if (macro.equalsIgnoreCase("trans-mid") || macro.equalsIgnoreCase("mid")) {
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
					// TODO: extract [[{{eng}}]] kind of links
					// TODO: some links come from *# bullet list used in a language.
					langname = extractLanguage(link);
					String l = ISO639_3.sharedInstance.getIdCode(langname);
					if (l != null) {
						langname = l;
					} else 
						System.err.println("Unexpected link: " + link + " while in LANGUE state.");
				} else if (star != null) {
					//System.err.println("Skipping '*' while in LANGUE state.");
				} else if (term != null) {
					currentGlose = term;
					langname = ""; word = ""; usage = "";
					ETAT = INIT;
				} else if (car != null) {
					if (car.equals(":")) {
						lang = langname.trim();
						lang = AbstractWiktionaryExtractor.supParenthese(lang);
						lang = JapaneseLangtoCode.triletterCode(lang);
						langname = "";
						ETAT = TRAD;
					} else if (car.equals("\n") || car.equals("\r")) {
						//System.err.println("Skipping newline while in LANGUE state.");
					} else if (car.equals(",")) {
						//System.err.println("Skipping ',' while in LANGUE state.");
					} else {
						langname = langname + car;
					}
				} 

				break ;
			case TRAD:
				if (macro!=null) {
					if (macro.equalsIgnoreCase("trans-top") || macro.equalsIgnoreCase("top"))  {
						if (macroOrLinkOrcarMatcher.group(2) != null) {
							currentGlose = macroOrLinkOrcarMatcher.group(2);
						} else {
							currentGlose = null;
						}
						//if (word != null && word.length() != 0) {
							//lang=supParenthese(lang);
							//wdh.registerTranslation(lang, currentGlose, usage, word);
						//}
						langname = ""; word = ""; usage = ""; lang=null;
						ETAT = INIT;
					} else if (macro.equalsIgnoreCase("trans-bottom") || macro.equalsIgnoreCase("bottom")) {
						if (word != null && word.length() != 0) {
							if(lang!=null) {
								this.delegate.registerTranslation(lang, currentGlose, usage, word);
							}
						}
						currentGlose = null;
						langname = ""; word = ""; usage = ""; lang=null;
						ETAT = INIT;
					} else if (macro.equalsIgnoreCase("trans-mid") || macro.equalsIgnoreCase("mid")) {
						if (word != null && word.length() != 0) {
							if(lang!=null){
								this.delegate.registerTranslation(lang, currentGlose, usage, word);
							}
						}
						langname = ""; word = ""; usage = ""; lang = null;
						ETAT = INIT;
					} else if (macro.equals("朝鮮語訳")) {
						// Get the korean translation and romanization
						Map<String,String> argmap = WikiTool.parseArgs(macroOrLinkOrcarMatcher.group(2));
						word = argmap.get("word");
						argmap.remove("word");
						usage = argmap.toString();
						if(lang!=null) { 
							this.delegate.registerTranslation(lang, currentGlose, usage, word);
						}
						word = ""; usage = "";
					} else if (macro.equals("ZHfont")) { 
						// Switch for the Chinese fonts.
						Map<String,String> argmap = WikiTool.parseArgs(macroOrLinkOrcarMatcher.group(2));
						// Check if previous word has not been registered. TODO: Check when this arises.
						if (word != null && word.length() != 0) {
							System.err.println("Word is not null when handling ZHfont macro in " + this.delegate.currentLexEntry());
						}
						word = argmap.get("1");
						// TODO: split [[ ]] / [[ ]] translations where the second seems to be a japanese usage note (equivalent in japanese chars ?)
						argmap.remove("1"); if (! argmap.isEmpty()) usage = argmap.toString();
					} else if (macro.equals("trans_link")) { 
						Map<String,String> argmap = WikiTool.parseArgs(macroOrLinkOrcarMatcher.group(2));
						if (null != word && word.length() != 0) System.err.println("Word is not null when handling trans_link macro in " + this.delegate.currentLexEntry());
						word = argmap.get("2");
					} else if (macro.equals("t+") || macro.equals("t-") || macro.equals("t") || macro.equals("tø")) { 
						Map<String,String> argmap = WikiTool.parseArgs(macroOrLinkOrcarMatcher.group(2));
						if (null != word && word.length() != 0) System.err.println("Word is not null when handling t+- macro in " + this.delegate.currentLexEntry());
						String l = null;
						if (null != argmap.get("1")
							&& (null != lang)
							&& ! lang.equals(ISO639_3.sharedInstance.getIdCode(argmap.get("1"))))
							System.err.println("Language in t+ macro does not map language in list in ");// + this.delegate.currentLexEntry());
						word = argmap.get("2");
						argmap.remove("1"); argmap.remove("2");
						if (! argmap.isEmpty()) usage = argmap.toString();
					} else {
						System.err.println("Got " + macro + " macro in usage. for page: " );//+ this.delegate.currentLexEntry());
						usage = usage + "{{" + macro + "}}";
					}
				} else if (link!=null) {
					if (! isAnExternalLink(link)) {
						word =word + " " + ((macroOrLinkOrcarMatcher.group(4) == null) ? link : macroOrLinkOrcarMatcher.group(4));
					}
				} else if (star != null) {
					//System.err.println("Skipping '*' while in LANGUE state.");
				} else if (term != null) {
					currentGlose = term;
					langname = ""; word = ""; usage = ""; lang = null;
					ETAT = INIT;
				} else if (car != null) {
					if (car.equals("\n") || car.equals("\r")) {
						usage = usage.trim();
						// System.err.println("Registering: " + word + ";" + lang + " (" + usage + ") " + currentGlose);
						if (word != null && word.length() != 0) {
							if(lang!=null){
								this.delegate.registerTranslation(lang, currentGlose, usage, word);
							}
						}
						lang = null; 
						usage = "";
						word="";
						ETAT = INIT;
					} else if (car.equals(",") || car.equals("、")) {
						usage = usage.trim();
						// System.err.println("Registering: " + word + ";" + lang + " (" + usage + ") " + currentGlose);
						if (word != null && word.length() != 0) {
							if (lang!=null){
								this.delegate.registerTranslation(lang, currentGlose, usage, word);
							}
						}
						usage = "";
						word = "";
					} else {
						usage = usage + car;
					}
				}
				break;
			default: 
				System.err.println("Unexpected state number:" + ETAT);
				break; 
			}
        	

        }
    }
	// NOTE: trans-top is sometimes used.
	// Sometimes something is given after the {{trans}} macro to represent the entry of the translation, however, it seems to be redundant with the position.

    
	private String extractLanguage(String link) {
		Matcher m = WikiPatterns.macroPattern.matcher(link);
		if (m.matches())
			return m.group(1);
		else
			return "";
	}

	private boolean isAnExternalLink(String link) {
		// TODO Auto-generated method stub
		return link.startsWith(":");
	}
}
