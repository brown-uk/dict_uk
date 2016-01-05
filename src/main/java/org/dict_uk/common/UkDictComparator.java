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
	private static final String IGNORE_CHARS = "'ʼ’-";
//	private Pattern IGNORE_CHARS_PATTERN = Pattern.compile("["+IGNORE_CHARS+"]");
	private static final String UK_ALPHABET = 
//		    "АаБбВвГгҐґДдЕеЄєЖжЗзИиІіЇїЙйКкЛлМмНнОоПпРрСсТтУуФфХхЦцЧчШшЩщЬьЮюЯя";
			"абвгґдеєжзиіїйклмнопрстуфхцчшщьюя";
	
	private static final Map<Character, Integer> UK_ALPHABET_MAP = new HashMap<>();
	
	static {
		for(int i=0; i<UK_ALPHABET.length(); i++) {
			UK_ALPHABET_MAP.put(UK_ALPHABET.charAt(i), i);
		}
	}

	@Override
	public int compare(String str0, String str1) {
		
//		String clean0 = IGNORE_CHARS_PATTERN.matcher(str0).replaceAll("");
//		String clean1 = IGNORE_CHARS_PATTERN.matcher(str1).replaceAll("");
//		
//		for(int i=0; i<Math.min(clean0.length(), clean1.length()); i++) {
//			int cmp = getOrd(clean0.charAt(i)) - getOrd(clean1.charAt(i));
//			if( cmp != 0 )
//				return cmp;
//		}
//
//		for(int i=0; i<Math.min(clean0.length(), clean1.length()); i++) {
//			int cmp = getCase(clean0.charAt(i)) - getCase(clean1.charAt(i));
//			if( cmp != 0 )
//				return -cmp;
//		}
//
//		return str0.length() - str1.length();
		return getSortKey(str0).compareTo(getSortKey(str1));
	}
	
//	private int getCase(char charAt) {
//		return Character.isUpperCase(charAt) ? 1 : 0;
//	}
//
//	private int getOrd(char ch) {
//		Character cLower = Character.toLowerCase(ch);
//        if( UK_ALPHABET_MAP.containsKey(cLower) )
//            return UK_ALPHABET_MAP.get(cLower);
//
//        return ch;
//	}
	
	/**
	 * Used by key-based sorting (like in python's sorted() with key= parameter)
	 * @param str
	 * @return
	 */
	public static String getSortKey(String str) {
		StringBuilder ignoreChars = new StringBuilder();
		StringBuilder tailCaps = new StringBuilder(str.length());
		StringBuilder normChars = new StringBuilder(str.length());
		
		for(int i=0; i<str.length(); i++) {
			char ch = str.charAt(i);
			
			if( IGNORE_CHARS.indexOf(ch) != -1 ) {
				if( ch == 0x02BC )
					ch = 0x2020;
				ignoreChars.append(ch);
				continue;
			}
			
			
			char lowerCh = Character.toLowerCase(ch);
			
			if (UK_ALPHABET_MAP.containsKey(lowerCh)) {
				char normChar = (char)('А' + UK_ALPHABET_MAP.get(lowerCh));
				normChars.append(normChar);
				tailCaps.append(ch);
//				normChars.append(ch);
			}
			else {
//				normChars.append(ch);
				tailCaps.append(ch);
			}
			
		}
		
		normChars.append(" ");
		tailCaps.append(" ");
		ignoreChars.append(" ");
		
		return normChars.toString() + tailCaps.toString() + ignoreChars.toString();
	}
	
}
