package org.getalp.dbnary.experiment;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.RDF;
import com.wcohen.ss.ScaledLevenstein;

import org.apache.commons.cli.*;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.getalp.blexisma.api.ISO639_3;
import org.getalp.blexisma.api.ISO639_3.Lang;
import org.getalp.dbnary.DbnaryModel;
import org.getalp.dbnary.experiment.disambiguation.*;
import org.getalp.dbnary.experiment.disambiguation.translations.TranslationDisambiguator;
import org.getalp.dbnary.experiment.evaluation.EvaluationStats;
import org.getalp.dbnary.experiment.preprocessing.AbstractGlossFilter;
import org.getalp.dbnary.experiment.preprocessing.StatsModule;
import org.getalp.dbnary.experiment.preprocessing.StructuredGloss;
import org.getalp.dbnary.experiment.similarity.SimilarityMeasure;
import org.getalp.dbnary.experiment.similarity.string.TverskiIndex;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;


public final class DisambiguateTranslationSources {

	private static final String LANGUAGES_OPTION = "l";
	private static final String DEFAULT_LANGUAGES = "fra,eng,deu,rus";
	private static final String RDF_FORMAT_OPTION = "f";
	private static final String DEFAULT_RDF_FORMAT = "turtle";
	private static final String STATS_FILE_OPTION = "s";
	private static final String OUTPUT_FILE_SUFFIX_OPTION = "o";
	private static final String DEFAULT_OUTPUT_FILE_SUFFIX = "_disambiguated_translations.ttl";
	private static final String CONFIDENCE_FILE_OPTION = "c";
	private static final String COMPRESS_OPTION = "z";

	private static Options options = null; // Command line op

	static {
		options = new Options();
		options.addOption("h", false, "Prints usage and exits. ");
		options.addOption(LANGUAGES_OPTION, true,
				"Language (fra, eng, deu, por). " + DEFAULT_LANGUAGES + " by default.");
		options.addOption(RDF_FORMAT_OPTION, true, "RDF file format (xmlrdf, turtle, n3, etc.). " + DEFAULT_RDF_FORMAT + " by default.");
		options.addOption(STATS_FILE_OPTION, true, "if present generate a csv file of the specified name containing statistics about available glosses in translations.");
		options.addOption(CONFIDENCE_FILE_OPTION, true, "if present generate a csv file of the specified name containing confidence score of the similarity disambiguation.");
		options.addOption(OUTPUT_FILE_SUFFIX_OPTION, true, "if present, use the specified value as the filename suffix for the output "
				+ "RDF model containing the computed disambiguated relations for each language." + DEFAULT_OUTPUT_FILE_SUFFIX + " by default.");
		options.addOption(COMPRESS_OPTION, false, "if present, compress the ouput with BZip2.");
	}

//	private static Model model;
//	private static Model outputModel;
	private CommandLine cmd = null; // Command Line arguments

	private Disambiguator disambiguator;
	// private Locale language;
	private String[] languages;
	// private String NS;
	private PrintStream statsOutput = null;
	private StatsModule stats = null;
	private String rdfFormat;
	private SimilarityMeasure similarityMeasure;
	private PrintStream confidenceOutput;
	private EvaluationStats evaluator = null;
	private String outputFileSuffix;
	private boolean doCompress;
	private HashMap<String,Model> modelMap;


	private DisambiguateTranslationSources() {

		disambiguator = new TranslationDisambiguator();
		double w1 = 0.1;
		double w2 = 1d - w1;
		String mstr = String.format("_%f_%f", w1, w2);

        similarityMeasure = new TverskiIndex(w1, w2, true, false, new ScaledLevenstein());
        disambiguator.registerSimilarity("FTiLs" + mstr, similarityMeasure);
	}

	public static void main(String[] args) throws IOException {

		DisambiguateTranslationSources lld = new DisambiguateTranslationSources();
		lld.loadArgs(args);

		lld.doit();
		
	}

	private void output(String lang, Model m) {
		String outputModelFileName = lang + outputFileSuffix;
		OutputStream outputModelStream;

		try {
			if (doCompress) {
				outputModelFileName = outputModelFileName + ".bz2";
				outputModelStream = new BZip2CompressorOutputStream(new FileOutputStream(outputModelFileName));
			} else {
				outputModelStream = new FileOutputStream(outputModelFileName);
			}

			m.write(outputModelStream, this.rdfFormat);
			
		} catch (FileNotFoundException e) {
			System.err.println("Could not create output stream: " + e.getLocalizedMessage());
			e.printStackTrace(System.err);
			return;
		} catch (IOException e) {
			System.err.println("IOException while creating output stream: " + e.getLocalizedMessage());
			e.printStackTrace();
			return;
		}
	}

	private void doit() throws FileNotFoundException {
		System.err.println("Pre-processing translations.");
		
		for(String lang: languages) {
			AbstractGlossFilter filter = createGlossFilter(lang);
			this.preprocessTranslations(filter, lang);
		}

		if (null!= statsOutput) {
			System.err.println("Writing Stats");
			stats.displayStats(statsOutput);
			statsOutput.close();
		}
		
		for(String lang: languages) {
			System.err.println("Disambiguating " + lang);
			Model m = ModelFactory.createDefaultModel();
			m.setNsPrefixes(modelMap.get(lang).getNsPrefixMap());

			this.processTranslations(m, lang);
			System.err.println("Outputting disambiguation links for " + lang);
			this.output(lang, m);
		}

        if(null != confidenceOutput){
            System.err.println("Writing confidence stats");
            evaluator.printConfidenceStats(confidenceOutput);
            confidenceOutput.close();
        }
	}

	public static void printUsage() {
		HelpFormatter formatter = new HelpFormatter();
		String help =
				"urlOrFile must point on an RDF model file extracted from wiktionary by DBnary.";
		formatter.printHelp("java -cp /path/to/wiktionary.jar org.getalp.dbnary.experiment.DisambiguateTranslationSources [OPTIONS] urlOrFile ...",
				"With OPTIONS in:", options,
				help, false);
	}

	private void loadArgs(String[] args) {
		CommandLineParser parser = new PosixParser();
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.err.println("Error parsing arguments: " + e.getLocalizedMessage());
			printUsage();
			System.exit(1);
		}
		String[] remainingArgs = cmd.getArgs();

		if (remainingArgs.length == 0) {
			System.err.println("Missing model files or URL.");
			printUsage();
			System.exit(1);
		}
		
		if (cmd.hasOption("h")) {
			printUsage();
			System.exit(0);
		}

		doCompress = cmd.hasOption(COMPRESS_OPTION);
		
		rdfFormat = cmd.getOptionValue(RDF_FORMAT_OPTION, DEFAULT_RDF_FORMAT);
		rdfFormat = rdfFormat.toUpperCase();

		languages = cmd.getOptionValue(LANGUAGES_OPTION, DEFAULT_LANGUAGES).split(",");
		for (int i = 0; i < languages.length; i++) {
			Lang l = ISO639_3.sharedInstance.getLang(languages[i]);
			languages[i] = l.getId();
		}
		
		if (cmd.hasOption(STATS_FILE_OPTION)) {
			String statsFile = cmd.getOptionValue(STATS_FILE_OPTION);
			try {
				statsOutput = new PrintStream(statsFile, "UTF-8");
			} catch (FileNotFoundException e) {
				System.err.println("Cannot output statistics to file " + statsFile);
				System.exit(1);
			} catch (UnsupportedEncodingException e) {
				// Should never happen
				e.printStackTrace();
				System.exit(1);
			}
			stats = new StatsModule();
		}
		
		if (cmd.hasOption(CONFIDENCE_FILE_OPTION)) {
			String confidenceFile = cmd.getOptionValue(CONFIDENCE_FILE_OPTION);
			try {
				confidenceOutput = new PrintStream(confidenceFile, "UTF-8");
			} catch (FileNotFoundException e) {
				System.err.println("Cannot output statistics to file " + confidenceFile);
				System.exit(1);
			} catch (UnsupportedEncodingException e) {
				// Should never happen
				e.printStackTrace();
				System.exit(1);
			}
			evaluator = new EvaluationStats();
		}
		
		outputFileSuffix = cmd.getOptionValue(OUTPUT_FILE_SUFFIX_OPTION, DEFAULT_OUTPUT_FILE_SUFFIX);
		
		modelMap = new HashMap<String,Model>();
		
		for (String arg: remainingArgs) {
			Model m = ModelFactory.createDefaultModel();
			String lang = guessLanguage(arg);
			modelMap.put(lang, m);
			try {
				if (arg.matches("[^:]{2,6}:.*")) {
					// It's an URL
					m.read(arg);
				} else {
					// It's a file
					if (arg.endsWith(".bz2")) {
						InputStreamReader modelReader = new InputStreamReader(new BZip2CompressorInputStream(new FileInputStream(arg)));
						m.read(modelReader, null, rdfFormat);
					} else {
						InputStreamReader modelReader = new InputStreamReader(new FileInputStream(arg));
						m.read(modelReader, null, rdfFormat);
					}
				}

			} catch (FileNotFoundException e) {
				System.err.println("Could not read " + remainingArgs[0]);
				System.exit(1);
			} catch (IOException e) {
				e.printStackTrace(System.err);
			}

		}
		
	}

	// TODO: adapt for several comma separated languages
	
	private String guessLanguage(String arg) {
		if (arg.matches("[^:]{2,6}:.*")) {
			// It's an URL
            try {
				String fname = new File (new URL(arg).getPath()).getName();
	            return ISO639_3.sharedInstance.getIdCode(fname.split("_")[0]);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			// It's a file
            String fname = new File(arg).getName();
            return ISO639_3.sharedInstance.getIdCode(fname.split("_")[0]);
		}
		return null;
	}


	private AbstractGlossFilter createGlossFilter(String lang) {
		AbstractGlossFilter f = null;
		String cname = AbstractGlossFilter.class.getCanonicalName();
		int dpos = cname.lastIndexOf('.');
		String pack = cname.substring(0, dpos);
		Class<?> wec = null;
		try {
			wec = Class.forName(pack + "." + lang + ".GlossFilter");
			f = (AbstractGlossFilter) wec.getConstructor().newInstance();
		} catch (ClassNotFoundException e) {
			System.err.println("No gloss filter found for " + lang+" reverting to default "+pack + ".DefaultGlossFilter");
			try {
				wec = Class.forName(pack + ".DefaultGlossFilter");
				f = (AbstractGlossFilter) wec.getConstructor().newInstance();
			} catch (ClassNotFoundException e1) {
				System.err.println("Default gloss filter not found");
			} catch (InvocationTargetException e1) {
				System.err.println("Default gloss filter failed to be instanciated");
			} catch (NoSuchMethodException e1) {
				System.err.println("Default gloss filter failed to be instanciated");
			} catch (InstantiationException e1) {
				System.err.println("Default gloss filter failed to be instanciated");
			} catch (IllegalAccessException e1) {
				System.err.println("Default gloss filter failed to be instanciated");
			}
		} catch (InstantiationException e) {
			System.err.println("Could not instanciate wiktionary extractor for " + lang);
		} catch (IllegalAccessException e) {
			System.err.println("Illegal access to wiktionary extractor for " + lang);
		} catch (IllegalArgumentException e) {
			System.err.println("Illegal argument passed to wiktionary extractor's constructor for " + lang);
			e.printStackTrace(System.err);
		} catch (SecurityException e) {
			System.err.println("Security exception while instanciating wiktionary extractor for " + lang);
			e.printStackTrace(System.err);
		} catch (InvocationTargetException e) {
			System.err.println("InvocationTargetException exception while instanciating wiktionary extractor for " + lang);
			e.printStackTrace(System.err);
		} catch (NoSuchMethodException e) {
			System.err.println("No appropriate constructor when instanciating wiktionary extractor for " + lang);
		}
		return f;
	}

	private void preprocessTranslations(AbstractGlossFilter filter, String lang) {
		// Iterate over all translations
		// TODO: adapt stats module for current language
		if (null != stats) stats.reset(lang);
		Model m = modelMap.get(lang);

		StmtIterator translations = m.listStatements((Resource) null, DbnaryModel.isTranslationOf, (RDFNode) null);
		while (translations.hasNext()) {
			Resource e = translations.next().getSubject();

			Statement g = e.getProperty(DbnaryModel.glossProperty);

			if (null == g) {
				if (null != stats) stats.registerTranslation(e.getURI(), null);
			} else {
				StructuredGloss sg = filter.extractGlossStructure(g.getString());
				if (null != stats) stats.registerTranslation(e.getURI(), sg);

				if (null == sg) {
                    // remove gloss from model
					g.remove();
				} else {
					if (null != sg.getSenseNumber()) {
						g.getModel().add(g.getModel().createLiteralStatement(g.getSubject(), DbnaryModel.senseNumberProperty, sg.getSenseNumber()));
					}
					if (null == sg.getGloss()) {
						// remove gloss from model
						g.remove();
					} else {
						g.changeObject(sg.getGloss());
					}
				}
			}
		}
	}

	private void processTranslations(Model outputModel, String lang) throws FileNotFoundException {
		
		if (null != evaluator) evaluator.reset(lang);
		SenseNumberBasedTranslationDisambiguationMethod snumDisamb = new SenseNumberBasedTranslationDisambiguationMethod();
		TverskyBasedTranslationDisambiguationMethod tverskyDisamb = new TverskyBasedTranslationDisambiguationMethod(.05);
        TransitiveTranslationClosureDisambiguationMethod transitDisamb = new TransitiveTranslationClosureDisambiguationMethod(1,lang,modelMap,0.05);
		
		Model inputModel = modelMap.get(lang);
		StmtIterator translations = inputModel.listStatements(null, DbnaryModel.isTranslationOf, (RDFNode) null);
        while (translations.hasNext()) {
			Statement next = translations.next();

			Resource trans = next.getSubject();
			
			Resource lexicalEntry = next.getResource();
			if (lexicalEntry.hasProperty(RDF.type, DbnaryModel.lexEntryType) || lexicalEntry.hasProperty(RDF.type, DbnaryModel.wordEntryType)) {
				try {
				Set<Resource> resSenseNum = snumDisamb.selectWordSenses(lexicalEntry, trans);
				Set<Resource> resSim = null;

				if (null != evaluator || resSenseNum.size() == 0) {
					// disambiguate by similarity
					
				   resSim = tverskyDisamb.selectWordSenses(lexicalEntry, trans);

                   if(resSim.isEmpty()){ //No gloss!
                        resSim = transitDisamb.selectWordSenses(lexicalEntry,trans);
                        //System.out.println("Transitive dismabiguation: "+resSim.size());
                   }
					// compute confidence if snumdisamb is not empty and confidence is required
					if (null != evaluator && resSenseNum.size() != 0) {
						evaluator.registerAnswer(resSenseNum, resSim);
					}
				}
				
				// Register results in output Model
				Resource translation = outputModel.createResource(trans.getURI());
				
				Set<Resource> res = (resSenseNum.isEmpty()) ? resSim : resSenseNum;
				
				if (res != null) {
					for (Resource ws : res) {
						outputModel.add(outputModel.createStatement(translation, DbnaryModel.isTranslationOf, outputModel.createResource(ws.getURI())));
					}
				}
				
				} catch (InvalidContextException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvalidEntryException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
        }
	}

}

