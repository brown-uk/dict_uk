package org.dict_uk.common

import java.util.ArrayList
import java.util.List
import java.util.regex.Matcher
import java.util.regex.Pattern

import org.dict_uk.expand.Affix

import groovy.transform.CompileStatic

class AffixUtil {
	private static final Pattern re_alts_slash = Pattern.compile("([^/]+:)([^:]+)(:[^/]+)?")
	private static final Pattern re_alts_dbl_slash = Pattern.compile('^(.+?:)((?:.:(?:nv|v_...)(?:/(?:nv|v_...))*)(?://.:(?:nv|v_...)(?:/(?:nv|v_...))*)+)(:[^/]+)?$')

	@CompileStatic
	public static List<DicEntry> expand_alts(List<DicEntry> entries, String splitter) {
		ArrayList<DicEntry> out = new ArrayList<>();

		for(DicEntry entry: entries) {

			String tagStr = entry.getTagStr();

			if( ! tagStr.contains(splitter) ) {
				out.add( entry );
				continue;
			}

			Matcher matcher;
			if( splitter.equals("/") ) {
				matcher = re_alts_slash.matcher(tagStr);
			}
			else {
				matcher = re_alts_dbl_slash.matcher(tagStr);
			}

			if( ! matcher.matches() )
				throw new IllegalArgumentException("Not found splitter regex " + splitter + " for " + entry + " ==~ " + matcher.toString());

			String[] split1 = matcher.group(2).split(splitter);

			String base = matcher.group(1);
			String end = "";

			if( matcher.groupCount() > 2 && matcher.group(3) != null ) {
				end = matcher.group(3);
			}

			for(String split_: split1) {
				out.add(new DicEntry(entry.getWord(), entry.getLemma(), base + split_ + end ));
			}
		}

		return out;
	}

}
