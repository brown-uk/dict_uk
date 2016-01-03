package org.dict_uk.common

import java.util.List;
import groovy.transform.CompileStatic
import groovy.transform.Immutable


@CompileStatic
@Immutable
class DicEntry {
	String word
	String lemma
	List<String> tags
	String tagStr
	String comment

	public static DicEntry fromLine(String line) {
	  def comment_
	  if( line.contains("#") ) {
		  def parts = line.split(/\s*#\s*/)
		  line = parts[0]
		  comment_ = parts[1]
	  }
	  else {
		  comment_ = ""
	  }
		
	  def parts = line.split()

	  return new DicEntry(parts[0], parts[1], Arrays.asList(parts[2].split(":")), parts[2], comment_)
	}
}
