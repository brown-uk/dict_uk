package org.dict_uk.spell;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;
import java.util.*;
import java.nio.file.*;
import java.util.regex.*;

public class Spell {
    private static final Pattern UKR_LETTER = Pattern.compile("[а-яіїєґ]", Pattern.CASE_INSENSITIVE);
    private static final HashSet<String> set = new HashSet<>();

    public static void main(String[] argv) throws Exception {

        Files.lines(Paths.get("../../../out/prev/words.txt"))
            .forEach(line -> set.add(line));
        System.err.println("Total words: " + set.size());

        Files.lines(Paths.get(argv[0]))
            .map(line ->  line.replaceAll("’", "'").split("[^а-яіїєґА-ЯІЇЄҐ'-]")) // Stream<String[]>
            .flatMap(Arrays::stream) // Stream<String>
            .filter(w -> UKR_LETTER.matcher(w).find()
                && ! in(w)
                && ! splitIn(w))
            .distinct() // Stream<String>
            .forEach(System.out::println);
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
