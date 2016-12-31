package org.dict_uk.common

import java.util.List;

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.Immutable


@CompileStatic
@Canonical
//@Immutable
class DicEntry {
	String word
	String lemma
//	List<String> tags
	String tagStr
	String comment

	public DicEntry(String word, String lemma, String tagStr) { //, String comment="") {
		this.word = word
		this.lemma = lemma
		this.tagStr = tagStr
//		this.tags = splitTags(tagStr)
//		this.comment = comment
	}
	
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
	  assert parts.size() == 3
	  	

	  return new DicEntry(parts[0], parts[1], parts[2]) //, comment_)
	}
	
	public String[] getTags() {
		return tagStr.split(/:/)
	}
	
	public boolean isValid() {
		return word.trim() && lemma.trim() //&& tagStr.trim()
	}
	
	public void setTagStr(String tagStr) {
		this.tagStr = tagStr;
	}

	private static List<String> splitTags(String tagStr) {
		return Arrays.asList(tagStr.split(":"))
	}
	
	public static List<DicEntry> fromLines(List<String> lines) {
		return lines.collect{ fromLine(it) }
	}

	public static List<DicEntry> fromLines(String[] lines) {
		return Arrays.asList(lines).collect{ fromLine(it) }
	}

	@Override
	public String toString() {
		return String.format("<%s %s %s>", word, lemma, tagStr);
	}
	
	public String toFlatString() {
		return String.format("%s %s %s", word, lemma, tagStr);
	}

}
