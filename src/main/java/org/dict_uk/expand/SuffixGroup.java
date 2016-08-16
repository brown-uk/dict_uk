package org.dict_uk.expand;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

class SuffixGroup {
	private final String match;
	private final String neg_match;
	private final List<Suffix> affixes = new ArrayList<>();
	private final Pattern match_ends_re;
	private final Pattern neg_match_ends_re;
	int counter;
	
    protected SuffixGroup(String match_) {
    	this(match_, null);
    }
    
    protected SuffixGroup(String match_, String neg_match_) {
        this.match = match_;
        this.neg_match = neg_match_;
        
        try {
            this.match_ends_re = Pattern.compile(match_+"$");
            this.neg_match_ends_re = neg_match_ != null ? Pattern.compile(neg_match_+"$") : null; 
        } catch(Exception e) {
            throw new IllegalArgumentException("Failed to compile match " + match_+"$", e);
        }
        this.counter = 0;
    }

    public boolean matches(String word) {
        return match_ends_re.matcher(word).find()
          && (neg_match == null || ! neg_match_ends_re.matcher(word).find());
    }
    
    public void appendAffix(Suffix affixObj) {
    	affixes.add(affixObj);
    }

	public int getSize() {
		return affixes.size();
	}

	@Override
	public String toString() {
		return "SuffixGroup [match=" + match + ", neg_match=" + neg_match + ", affixes=" + affixes + "]";
	}
	
}
