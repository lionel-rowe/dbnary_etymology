package org.getalp.blexisma.wiktionary.cli;

import org.getalp.blexisma.semnet.SimpleSemanticNetwork;
import org.getalp.blexisma.wiktionary.EnglishWiktionaryExtractor;
import org.getalp.blexisma.wiktionary.FrenchWiktionaryExtractor;
import org.getalp.blexisma.wiktionary.GermanWiktionaryExtractor;
import org.getalp.blexisma.wiktionary.WiktionaryExtractor;
import org.getalp.blexisma.wiktionary.WiktionaryIndex;
import org.getalp.blexisma.wiktionary.WiktionaryIndexerException;

public class GetExtractedSemnet {

    public static void main(String[] args) throws WiktionaryIndexerException {
        if (args.length < 2) {
            printUsage();
            System.exit(1);
        }
        
        WiktionaryIndex wi = new WiktionaryIndex(args[1]);
        
        WiktionaryExtractor we = null;
        if (args[0].equals("fr")) {
            we = new FrenchWiktionaryExtractor();
        } else if (args[0].equals("en")) {
            we = new EnglishWiktionaryExtractor();
        } else if (args[0].equals("de")) {
            we = new GermanWiktionaryExtractor();
        } else {
            printUsage();
            System.exit(1);
        }
        

        SimpleSemanticNetwork<String, String> s = new SimpleSemanticNetwork<String, String>(5000, 5000);
        s.clear();
        for(int i = 2; i < args.length; i++) {
        	String pageContent = wi.getTextOfPage(args[i]);
            we.extractData(args[i], pageContent, s);
            s.dumpToWriter(System.out);
            s.clear();
        }
    }

    
    public static void printUsage() {
        System.err.println("Usage: ");
        System.err.println("  java org.getalp.blexisma.wiktionary.cli.GetExtractedSemnet languagecode wiktionaryDumpFile entryname ...");
        System.err.println("    where languagecode is either \"fr\" or \"en\" or \"de\".");
        System.err.println("Displays the extracted semnet of the wiktionary page named \"entryname\".");
    }

}
