package org.dict_uk.spell;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Collator;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Pattern;

public class Spell {
    private static final Pattern UKR_LETTER = Pattern.compile("[а-яіїєґ]", Pattern.CASE_INSENSITIVE);
    private static final Pattern WORD_SPLIT = Pattern.compile("[^a-zA-Zа-яіїєґА-ЯІЇЄҐ'-]", Pattern.CASE_INSENSITIVE);
    private static final HashSet<String> set = new HashSet<>();

    public static void main(String[] argv) throws Exception {

        Files.lines(Paths.get("../../../out/prev/words_spell.txt"))
            .forEach(line -> set.add(line));
        System.err.println("Total words: " + set.size());

        @SuppressWarnings("deprecation")
        Collator collator = Collator.getInstance(new Locale("uk", "UA"));
        
		Files.lines(Paths.get(argv[0]))
            .map(line ->  WORD_SPLIT.split(line.replaceAll("’", "'"))) // Stream<String[]>
            .flatMap(Arrays::stream) // Stream<String>
            .filter(w -> UKR_LETTER.matcher(w).find()
                && ! spell(w))
            .distinct() // Stream<String>
            .sorted(collator)
            .forEach(System.out::println);
    }
            
    private static boolean spell(String w) {
//    	if( w.charAt(0) == '\'' ){
//    		w = w.substring(1);
//    	}
    	return in(w) || splitIn(w);
	}

	private static boolean splitIn(String w) {
        if( ! w.contains("-") )
            return false;
            
        for(String part: w.split("-"))
            if( ! in(part) )
                return false;
        return true;
    }
    
    private static boolean in(String w) {
        return set.contains(w) 
                || set.contains(w.toLowerCase());
    }
}
