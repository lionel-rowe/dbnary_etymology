package org.getalp.dbnary.rus;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.vocabulary.DCTerms;
import info.bliki.wiki.filter.ParsedPageName;
import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.model.WikiModelContentException;
import info.bliki.wiki.namespaces.INamespace;
import org.getalp.dbnary.DbnaryWikiModel;
import org.getalp.dbnary.IWiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.wiki.ExpandAllWikiModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class RussianDefinitionExtractorWikiModel extends DbnaryWikiModel {

    // static Set<String> ignoredTemplates = new TreeSet<String>();
    // static {
    // 	ignoredTemplates.add("Wikipedia");
    // 	ignoredTemplates.add("Incorrect");
    // }

    protected class Example {
        String value;
        Map<Property, String> context = new HashMap<>();

        protected Example(String ex) {
            value = ex;
        }

        protected void put(Property p, String v) {
            context.put(p, v);
        }
    }

    private ExpandAllWikiModel expander;

    private IWiktionaryDataHandler delegate;
    private Set<Example> currentExamples = new HashSet<>();
    private Logger log = LoggerFactory.getLogger(RussianDefinitionExtractorWikiModel.class);

    public RussianDefinitionExtractorWikiModel(IWiktionaryDataHandler we, Locale locale, String imageBaseURL, String linkBaseURL) {
        this(we, (WiktionaryIndex) null, locale, imageBaseURL, linkBaseURL);
    }

    public RussianDefinitionExtractorWikiModel(IWiktionaryDataHandler we, WiktionaryIndex wi, Locale locale, String imageBaseURL, String linkBaseURL) {
        super(wi, locale, imageBaseURL, linkBaseURL);
        expander = new ExpandAllWikiModel(wi, locale, imageBaseURL, linkBaseURL);
        this.delegate = we;
    }

    @Override
    public void setPageName(String pageTitle) {
        super.setPageName(pageTitle);
        expander.setPageName(pageTitle);
    }

    public void parseDefinition(String definition, int defLevel) {
        // Render the definition to plain text, while ignoring the example template
        currentExamples.clear();
        String def = null;
        try {
            def = render(new PlainTextConverter(), definition).trim();
        } catch (IOException e) {
            e.printStackTrace();
        }
        delegate.registerNewDefinition(def, defLevel);
        if (!currentExamples.isEmpty()) {
            for (Example example : currentExamples) {
                delegate.registerExample(example.value, example.context);
            }
        }
    }

    @Override
    public void substituteTemplateCall(String templateName,
                                       Map<String, String> parameterMap, Appendable writer)
            throws IOException {
        if ("????????????".equals(templateName)) {
            // This is an example of usage of the definition.
            // DONE: add this example in the extracted data.
            if (parameterMap.containsKey("??????????")) {
                // Call with named parameters
                // {{????????????|??????????=|??????????????=|??????????=|??????????=|??????????????????????????=|??????????????=|??????????=|????????=|????????????????=}}
                String ex = expander.expandAll(parameterMap.get("??????????"), null);
                if (null != ex && ex.length() != 0) {
                    Example example = new Example(ex);
                    parameterMap.remove("??????????");
                    example.put(DCTerms.bibliographicCitation, formatMap(parameterMap));
                    currentExamples.add(example);
                }
            } else if (parameterMap.containsKey("1")) {
                // Call with positional parameters
                // {{????????????|??????????|??????????|??????????|????????|}}
                String ex = expander.expandAll(parameterMap.get("1"), null);
                if (null != ex && ex.length() != 0) {
                    Example example = new Example(ex);
                    parameterMap.remove("1");
                    example.put(DCTerms.bibliographicCitation, formatMap(parameterMap));
                    currentExamples.add(example);
                }
            }
        } else {
            // Do not ignore the other template calls.
            // log.debug("Called macro: {} when expanding definition block in {}.", templateName, this.getPageName());
            super.substituteTemplateCall(templateName, parameterMap, writer);
        }
    }

    private String formatMap(Map<String, String> parameterMap) {
        StringBuffer b = new StringBuffer();
        for (Map.Entry<String, String> entry : parameterMap.entrySet()) {
            b.append(entry.getKey()).append("=").append(entry.getValue()).append("|");
        }
        b.setLength(b.length() - 1);
        return b.toString();
    }

    @Override
    public String getRawWikiContent(ParsedPageName parsedPagename, Map<String, String> map)
            throws WikiModelContentException {
        ParsedPageName fixedPageName = parsedPagename;
        if (parsedPagename.namespace.isType(INamespace.NamespaceCode.MODULE_NAMESPACE_KEY) &&
                parsedPagename.pagename.startsWith("Module:")) {
            fixedPageName = new ParsedPageName(parsedPagename.namespace, parsedPagename.pagename.substring(7), parsedPagename.valid);
        }
        return super.getRawWikiContent(fixedPageName, map);
    }
}
