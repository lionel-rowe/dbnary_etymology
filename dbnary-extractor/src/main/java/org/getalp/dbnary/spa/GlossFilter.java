package org.getalp.dbnary.spa;

import org.getalp.dbnary.AbstractGlossFilter;
import org.getalp.dbnary.StructuredGloss;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GlossFilter extends AbstractGlossFilter {

	public static final String senseNumRegExp = "\\d+(?:[abcdefghijklmn][iv]*)?";
		
	public static final String simpleNumListFilter = "^\\s*(" + senseNumRegExp +"(?:\\s*[\\,\\-–]\\s*" + senseNumRegExp +")*)\\s*$";
	public static final Pattern simpleNumListPattern = Pattern.compile(simpleNumListFilter);
	public static final Matcher simpleNumListMatcher = simpleNumListPattern.matcher("");
	
	public StructuredGloss extractGlossStructure(String rawGloss) {
		if (null == rawGloss) return null;

		simpleNumListMatcher.reset(rawGloss);
		if (simpleNumListMatcher.matches()) {
			return new StructuredGloss(simpleNumListMatcher.group(1), null);
		}
		return new StructuredGloss(null, rawGloss);
	}
}
