package org.getalp.dbnary.cli;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.getalp.blexisma.api.ISO639_3;
import org.getalp.dbnary.WiktionaryIndexerException;

public class UpdateAndExtractDumps {

	private static Options options = null; // Command line options

	private static final String SERVER_URL_OPTION = "s";
	private static final String DEFAULT_SERVER_URL = "ftp://ftp.fi.muni.cz/pub/wikimedia/";

	private static final String FORCE_OPTION = "f";
	private static final boolean DEFAULT_FORCE = false;

	private static final String OUTPUT_DIR_OPTION = "o";
	private static final String DEFAULT_OUTPUT_DIR = "dumps";



	private CommandLine cmd = null; // Command Line arguments

	private String outputDir = DEFAULT_OUTPUT_DIR;
	private boolean force = DEFAULT_FORCE;
	private String server = DEFAULT_SERVER_URL;

	String[] remainingArgs;

	static{
		options = new Options();
		options.addOption("h", false, "Prints usage and exits. ");	
		options.addOption(SERVER_URL_OPTION, true, "give the URL pointing to a wikimedia mirror. ");	
		options.addOption(FORCE_OPTION, false, 
				"force the updating even if a file with the same name already exists in the output directory. " + DEFAULT_FORCE + " by default.");
		options.addOption(OUTPUT_DIR_OPTION, true, "Output file. " + DEFAULT_OUTPUT_DIR + " by default ");	
	}

	/**
	 * @param args
	 * @throws IOException 
	 * @throws WiktionaryIndexerException 
	 */
	public static void main(String[] args) throws WiktionaryIndexerException, IOException {
		UpdateAndExtractDumps cliProg = new UpdateAndExtractDumps();
		cliProg.loadArgs(args);
		cliProg.updateAndExtract();
	}


	private String dumpFileName(String lang, String date) {

		return lang + "wiktionary-"+date+"-pages-meta-history.xml.bz2";
	}

	/**
	 * Validate and set command line arguments.
	 * Exit after printing usage if anything is astray
	 * @param args String[] args as featured in public static void main()
	 */
	private void loadArgs(String[] args){
		CommandLineParser parser = new PosixParser();
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.err.println("Error parsing arguments: " + e.getLocalizedMessage());
			printUsage();
			System.exit(1);
		}

		// Check for args
		if (cmd.hasOption("h")){
			printUsage();
			System.exit(0);
		}

		if (cmd.hasOption(SERVER_URL_OPTION)) {
			server = cmd.getOptionValue(SERVER_URL_OPTION);
		}

		force = cmd.hasOption(FORCE_OPTION);

		if (cmd.hasOption(OUTPUT_DIR_OPTION)) {
			outputDir = cmd.getOptionValue(OUTPUT_DIR_OPTION);
		}

		remainingArgs = cmd.getArgs();
		if (remainingArgs.length == 0) {
			printUsage();
			System.exit(1);
		}

	}

	public void updateAndExtract() throws WiktionaryIndexerException, IOException {
		String [] dirs = updateDumpFiles(remainingArgs);
		uncompressDumpFiles(remainingArgs, dirs);
	}

	private String updateDumpFile(String lang) {
		FTPClient client = new FTPClient();

		try {
			System.err.println("Updating " + lang);
			URL url = new URL(server);

			if (url.getPort() != -1) {
				client.connect(url.getHost(), url.getPort());
			} else {
				client.connect(url.getHost());
			}
			client.login( "anonymous", "" );
			System.err.println("Logged in...");
			client.enterLocalPassiveMode();
			client.changeWorkingDirectory(url.getPath());

			client.changeWorkingDirectory(lang+"wiktionary");

			SortedSet<String> dirs = new TreeSet<String>();
			System.err.println("Retrieving directory list.");
			FTPFile[] ftpFiles = client.listFiles();
			System.err.println("Retrieved: " + ftpFiles);
			for (FTPFile ftpFile : ftpFiles) {
				if (ftpFile.getType() == FTPFile.DIRECTORY_TYPE && ! ftpFile.getName().startsWith(".")) {
					dirs.add(ftpFile.getName());
				}
			}
			String lastDir = dirs.last();
			System.err.println("Last version of dump is " + lastDir);

			client.changeWorkingDirectory(lastDir);

			try {
				String dumpdir = outputDir + "/" + lastDir;
				String filename = dumpdir + "/" + dumpFileName(lang,lastDir);
				File file = new File(filename);
				if (file.exists() && !force) {
					System.err.println("Dump file " + filename + " already retreived.");
					return lastDir;
				}
				File dumpFile = new File(dumpdir);
				dumpFile.mkdirs();
				FileOutputStream dfile = new FileOutputStream(file);
				System.err.println("Retreiving " + filename);
				long s = System.currentTimeMillis();
				client.retrieveFile(dumpFileName(lang,lastDir),dfile);
				System.err.println("Retreived " + filename + "[" + (System.currentTimeMillis() - s) + " ms]");


			} catch(IOException e){
				System.out.println(e);
			}
			client.logout();
			return lastDir;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			try {
				client.disconnect();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


	private void uncompressDumpFiles(String[] langs, String[] dirs) {
		for (int i = 0; i < langs.length; i++) {
			uncompressDumpFile(langs[i], dirs[i]);
		}
	}

	private void uncompressDumpFile(String prefix, String dir) {
		Reader r = null;
		Writer w = null;
		try {
			String compressedDumpFile = outputDir + "/" + dir + "/" + dumpFileName(prefix, dir);
			String uncompressedDumpFile = outputDir + "/" + dir + "/" + prefix + "wkt.xml";
			
			System.err.println("uncompressing file :" + compressedDumpFile + " to " + uncompressedDumpFile);

			BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(new FileInputStream(compressedDumpFile));
			r = new BufferedReader(new InputStreamReader(bzIn, "UTF-8"));

			FileOutputStream out = new FileOutputStream(uncompressedDumpFile);
			w = new BufferedWriter(new OutputStreamWriter(out, "UTF-16"));

			final char[] buffer = new char[4096];
			int len;
			while ((len = r.read(buffer)) != -1) 
				w.write(buffer, 0, len); 
			r.close(); 
			w.close(); 
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (null != r) {
				try {
					r.close();
				} catch (IOException e) {
					// nop
				}
			}
			if (null != w) {
				try {
					w.close();
				} catch (IOException e) {
					// nop
				}
			}
		}
	}

	private String[] updateDumpFiles(String[] langs) {
		String[] res = new String[langs.length];
		int i = 0;
		for (String prefix : remainingArgs) {
			res[i] = updateDumpFile(prefix);
			i++;
		}
		return res;
	}

	public static void printUsage() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java -cp /path/to/wiktionary.jar org.getalp.dbnary.cli.ExtractWiktionary [OPTIONS] languageCode...", 
				"With OPTIONS in:", options, 
				"languageCode is the wiktionary code for a language (usualy a 2 letter code).", false);
	}

}