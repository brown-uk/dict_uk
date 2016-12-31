package org.dict_uk.common

import java.util.ArrayList
import java.util.List
import java.util.regex.Matcher

import org.dict_uk.expand.Affix

class AffixUtil {

	public static List<DicEntry> expand_alts(List<DicEntry> lines, String splitter) {
			ArrayList<DicEntry> out = new ArrayList<>();
	
			for( DicEntry line: lines ) {
	
				String tagStr = line.getTagStr();
				
				if( ! tagStr.contains(splitter) ) {
					out.add( line );
					continue;
				}
	
				Matcher matcher;
				if( splitter.equals("/") ) {
					matcher = Affix.re_alts_slash.matcher(tagStr);
				}
	//			else if( splitter.equals("|") ) {
	//				if( tagStr.contains("tag=") ) {
	//					out.add( line );
	//					continue;
	//				}
	//				matcher = re_alts_vert.matcher(tagStr);
	//			}
				else {
					matcher = Affix.re_alts_dbl_slash.matcher(tagStr);
				}
				
				if( ! matcher.matches() )
					throw new IllegalArgumentException("Not found splitter regex " + splitter + " for " + line + " ==~ " + matcher.toString());
				
				String[] split1 = matcher.group(2).split(splitter);
				
				String base = matcher.group(1);
				String end = "";
	
				if( matcher.groupCount() > 2 && matcher.group(3) != null ) {
					end = matcher.group(3);
				}
	
				for(String split_: split1) {
					out.add(new DicEntry(line.getWord(), line.getLemma(), base + split_ + end ));
				}
			}
	
			return out;
		}

}
