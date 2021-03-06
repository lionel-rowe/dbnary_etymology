package org.getalp.dbnary.ita;

import info.bliki.wiki.filter.PlainTextConverter;
import org.getalp.dbnary.DbnaryWikiModel;
import org.getalp.dbnary.IWiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.iso639.ISO639_3;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

public class ItalianDefinitionExtractorWikiModel extends DbnaryWikiModel {

    // static Set<String> ignoredTemplates = new TreeSet<String>();
    // static {
    // 	ignoredTemplates.add("Wikipedia");
    // 	ignoredTemplates.add("Incorrect");
    // }

    private IWiktionaryDataHandler delegate;


    public ItalianDefinitionExtractorWikiModel(IWiktionaryDataHandler we, Locale locale, String imageBaseURL, String linkBaseURL) {
        this(we, (WiktionaryIndex) null, locale, imageBaseURL, linkBaseURL);
    }

    public ItalianDefinitionExtractorWikiModel(IWiktionaryDataHandler we, WiktionaryIndex wi, Locale locale, String imageBaseURL, String linkBaseURL) {
        super(wi, locale, imageBaseURL, linkBaseURL);
        this.delegate = we;
    }

    public void parseDefinition(String definition, int defLevel) {
        // Render the definition to plain text, while ignoring the example template
        String def = null;
        try {
            def = render(new PlainTextConverter(), definition).trim();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (null != def && !def.equals(""))
            delegate.registerNewDefinition(def, defLevel);
    }

    @Override
    public void substituteTemplateCall(String templateName,
                                       Map<String, String> parameterMap, Appendable writer)
            throws IOException {
        // Currently just expand the definition to get the full text.
        if (templateName.equals("Nodef")) {
            // No definition given, simply ignore.

        } else {
            super.substituteTemplateCall(templateName, parameterMap, writer);
        }
    }

}
