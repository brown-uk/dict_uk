package org.dict_uk.expand;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Suffix {
	private final String to;
	private final String fromm;
	private final String tags;
//	private final int sub_from_len;
	private final Pattern sub_from_sfx;

	Suffix(String from_, String to_, String tags_) {
		fromm = convert0(from_);
		to = convert0(to_).replace('\\',  '$');
		tags = tags_;
//		sub_from_len = fromm != "" ? -fromm.length() : 100;
		sub_from_sfx = Pattern.compile(fromm + "$");
	}

	public String apply(String word) {
//		System.err.println("applying: " + sub_from_sfx + " " + to + " " + word);
		
		Matcher matcher = sub_from_sfx.matcher(word);
//		if( ! matcher.matches() )
//			throw new IllegalArgumentException("Failed to find ending {} -> {} to {}".format(fromm, to, word));
					
		String replaced = matcher.replaceFirst(to);
		
		if( replaced.equals(word) && ! fromm.equals(to) )	// in some special cases replacement is the same as suffix
			throw new IllegalArgumentException(
					String.format("Affix wasn't applied %s -> %s to %s", fromm, to, word));
		
		return replaced;
	}

	@Override
	public String toString() {
		return "Suffix [to=" + to + ", fromm=" + fromm + ", tags=" + tags + "]";
	}

	private static String convert0(String part) {
		return part.equals("0") ? "" : part;
	}

}