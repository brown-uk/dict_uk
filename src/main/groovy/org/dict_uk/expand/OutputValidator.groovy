package org.dict_uk.expand

import java.util.HashSet
import java.util.List
import java.util.logging.LogManager
import java.util.regex.Pattern

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import org.dict_uk.common.DicEntry

import groovy.transform.TypeChecked;


class OutputValidator {
	static Logger log = LogManager.getFormatterLogger(OutputValidator.class);
	
	static final Pattern WORD_RE = Pattern.compile("[а-яіїєґА-ЯІЇЄҐ][а-яіїєґА-ЯІЇЄҐ']*(-[а-яіїєґА-ЯІЇЄҐ']*)*|[А-ЯІЇЄҐ][А-ЯІЇЄҐ-]+|[а-яіїєґ]+\\.")
	static final Pattern POS_RE = Pattern.compile("(noun:([iu]n)?anim:|noun:.*:&pron|verb(:rev)?:(im)?perf:|advp:(im)?perf|adj:[mfnp]:|adv|numr:|prep|part|intj|conj:|predic|insert|foreign|noninfl).*")
    static final List<String> IGNORED_NOUNS = ["бельмес", "давніх-давен", "основанья", "предку-віку", "роб", "свободівець", "шатер",
            "галай-балай", "вепр", "вихідець", "гратами", "мати-одиночка", "кінця-краю", "усіх-усюд"]

	final List<String> ALLOWED_TAGS = getClass().getResource("tagset.txt").readLines()

	OutputValidator() {
		log.debug("Read %d allowed tags\n", ALLOWED_TAGS.size())
	}
	
	@TypeChecked
	int check_lines(List<String> lines) {
		int fatalErrorCount = 0
		
		for( line in lines ) {
			DicEntry dicEntry = DicEntry.fromLine(line)
			def word = dicEntry.word
			def lemma = dicEntry.lemma
			def tags = dicEntry.tagStr

			if( ! WORD_RE.matcher(word).matches() || ! WORD_RE.matcher(lemma).matches() ) {
				log.error("Invalid pattern in word or lemma: " + line)
				fatalErrorCount++
			}

			if( ! POS_RE.matcher(tags).matches() ) {
				log.error("Invalid main postag in word: " + line)
				fatalErrorCount++
			}

			for( tag in dicEntry.tags) {
				if( ! (tag in ALLOWED_TAGS) ) {
					log.error("Invalid tag " + tag + ": " + line)
					fatalErrorCount++
				}
			}
			def dup_tags = dicEntry.tags.findAll { dicEntry.tags.count(it) > 1 }.unique()
			if( dup_tags) {
				log.error("Duplicate tags " + dup_tags.join(":") + ": " + line)
				if( !("coll" in dup_tags) )
				fatalErrorCount++
			}
		}
		
		return fatalErrorCount
	}

	static final List<String> ALL_V_TAGS = ["v_naz", "v_rod", "v_dav", "v_zna", "v_oru", "v_mis", "v_kly"]
	static final List<String> ADJ_PRON_V_TAGS = ["v_naz", "v_rod", "v_dav", "v_zna", "v_oru", "v_mis"]
	static final List<String> ALL_VERB_TAGS = ["inf",
//			"impr:s:2", "impr:p:1", "impr:p:2", \
			"pres:s:1", "pres:s:2", "pres:s:3", \
			"pres:p:1", "pres:p:2", "pres:p:3", \
			"past:m", "past:f", "past:n", "past:p", \
			 ]
	static final Pattern VERB_CHECK_PATTERN = ~/inf|impr:s:2|impr:p:[12]|(?:pres|futr):[sp]:[123]|past:[mfnp]/

	@TypeChecked
	int check_indented_lines(List<String> lines, List<String> limitedVerbLemmas) {
		String gender = ""
		HashSet<String> subtagSet = new HashSet<String>()
		String lemmaLine
		List<String> lastVerbTags = null
		int nonFatalErrorCount = 0
		
		//		ParallelEnhancer.enhanceInstance(lines)

		lines.each { String line ->
			if( ! line.startsWith(" ") ) {
				if (gender) {
					checkVTagSet(gender, subtagSet, lemmaLine)
				}
				else if( lastVerbTags && ! lemmaLine.contains(". ") ) {
					log.error("verb lemma is missing " + (lastVerbTags) + " for: " + lemmaLine)
					nonFatalErrorCount++
					lastVerbTags = null
				}

				subtagSet.clear()
				gender = ""
				lemmaLine = line
				
				if( line.contains(" verb:") && ! lemmaLine.contains(":inf:dimin") && ! (lemmaLine.split()[0] in limitedVerbLemmas) ) {
					def time = line.contains(":imperf") ? "pres" : "futr"
					lastVerbTags = ALL_VERB_TAGS.collect { it.replace("pres", time) }
				}
				else {
					lastVerbTags = null
				}
			}

			if( ( line.contains(" noun") && ! line.contains("&pron") )
			        || ( line.contains(" adj") && line.contains("&pron") ) ) {
				def parts = line.trim().split(" ")
				def tags = parts[1].split(":")

				def gen = tags.find { it.size() == 1 && "mfnp".contains(it) }
				assert gen : "Cound not find gen in " + tags + " for " + line

				if( gen != gender ) {
					if (gender) {
						checkVTagSet(gender, subtagSet, lemmaLine)
					}
					if( line.contains(":short") ) {
					    gender = ''
					}
					else {
					    gender = gen
					}
					subtagSet.clear()
				}

				String v_tag = tags.find { it.startsWith("v_") }
				subtagSet.add( v_tag )
			}
			else if ( lastVerbTags ) {
				def tagg = VERB_CHECK_PATTERN.matcher(line)
				if( tagg ) {
					lastVerbTags.remove(tagg[0])
				}
			}
		}
		
		return nonFatalErrorCount
	}

	def V_KLY_ONLY = new HashSet(Arrays.asList("v_kly"))
	private checkVTagSet(String gender, HashSet subtagSet, String line) {
		int nonFatalErrorCount = 0
		
		if( ! subtagSet.containsAll(ALL_V_TAGS) && ! line.contains(". ") ) {
			def missingVSet = ALL_V_TAGS - subtagSet

			if( missingVSet == ["v_kly"] && (line.contains(":lname") 
			        || (line.contains(" adj") && line.contains("&pron")) ) )
				return nonFatalErrorCount
			
			if( line.split()[0] in IGNORED_NOUNS )
			    return nonFatalErrorCount
			
			log.error("noun lemma is missing " + missingVSet + " on gender " + gender + " for: " + line)
			nonFatalErrorCount++
		}
		
		return nonFatalErrorCount
	}

	
}