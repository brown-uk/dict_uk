package org.dict_uk.common;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Comparator that also provides key-based sorting.
 * Follows sorting like in uk_UA.UTF-8 in glibc but is not a generic algorithm
 * and only has been tested within dict_uk 
 *
 */
public class UkDictComparator implements Comparator<String> {
	private static final char[] IGNORE_CHARS = new char[]{'\'', 'ʼ', '’', '-'};
//	private Pattern IGNORE_CHARS_PATTERN = Pattern.compile("["+IGNORE_CHARS+"]");
	private static final String UK_ALPHABET = 
//		    "АаБбВвГгҐґДдЕеЄєЖжЗзИиІіЇїЙйКкЛлМмНнОоПпРрСсТтУуФфХхЦцЧчШшЩщЬьЮюЯя";
			"абвгґдеєжзиіїйклмнопрстуфхцчшщьюя";
	
	private static final Map<Character, Character> UK_ALPHABET_MAP = new HashMap<>();
	
	static {
		for(int i=0; i<UK_ALPHABET.length(); i++) {
			UK_ALPHABET_MAP.put(UK_ALPHABET.charAt(i), (char)((int)'А' + i));
		}
	}

	@Override
	public int compare(String str0, String str1) {
		return getSortKey(str0).compareTo(getSortKey(str1));
	}
	
	/**
	 * Used by key-based sorting (like in python's sorted() with key= parameter)
	 * @param str
	 * @return
	 */
	public static String getSortKey(String str) {
		StringBuilder ignoreChars = new StringBuilder(6);
		StringBuilder tailCaps = new StringBuilder(str.length() + 1);
		StringBuilder normChars = new StringBuilder(str.length() + 1 + tailCaps.capacity() + ignoreChars.capacity());
		
		for(int i=0; i<str.length(); i++) {
			char ch = str.charAt(i);
			
			if( isIgnoredChar(ch) ) {
				if( ch == 0x02BC ) {
					ch = 0x2020;
				}
				ignoreChars.append(ch);
				continue;
			}
			
			
			char lowerCh = Character.toLowerCase(ch);
			
			Character sortValue = UK_ALPHABET_MAP.get(lowerCh);
			if (sortValue != null) {
				normChars.append(sortValue);
				tailCaps.append(ch);
			}
			else {
				tailCaps.append(ch);
			}
			
		}
		
		normChars.append(" ");
		tailCaps.append(" ");
		ignoreChars.append(" ");
		
		normChars.append(tailCaps);
		normChars.append(ignoreChars);
		
		return normChars.toString();
	}

	private static boolean isIgnoredChar(char ch) {
		for (char c : IGNORE_CHARS) {
			if( c == ch )
				return true;
		}
		return false;
	}
	
}
