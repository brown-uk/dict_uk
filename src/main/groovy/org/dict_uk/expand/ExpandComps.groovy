#!/usr/bin/env groovy

package org.dict_uk.expand


import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import java.util.regex.*

import org.dict_uk.common.DicEntry
import org.dict_uk.expand.Expand
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@CompileStatic
class ExpandComps {
	private static final Logger log = LoggerFactory.getLogger(ExpandComps.class);

	private static final Pattern tags_re = Pattern.compile("(.*:)[mfnp]:v_...(?::r(?:in)?anim)?(.*)")
	private static final Pattern gen_vidm_pattern = Pattern.compile(":(.):(v_...(:r(in)?anim)?)")
	final Expand expand


	public ExpandComps(Expand expand) {
		this.expand = expand
	}

	List<DicEntry> matchComps(List<DicEntry> lefts, List<DicEntry> rights, String vMisCheck) {
		
		List<DicEntry> outs = []
        
        Set<String> leftGenders = lefts.collect { entry ->
            def m = gen_vidm_pattern.matcher(entry.tagStr)
            assert m, "Not found vidm for $entry"
            m.group(1)
        } as Set
        Set<String> rightGenders = rights.size() == 1 ? [] as Set : rights.collect { entry ->
            def m = gen_vidm_pattern.matcher(entry.tagStr)
            assert m, "Not found vidm for $entry"
            m.group(1)
        } as Set

        def genderMix = leftGenders.size() == 1 || leftGenders.size() == 1
        Map<String, List<DicEntry>> leftEntryByVidm = [:].withDefault{ [] }
        
		for(DicEntry entry in lefts) {
			def rrr = gen_vidm_pattern.matcher(entry.tagStr)
            rrr.find()
            def key = /*genderMix ? rrr.group(2) :*/ rrr.group(0)
			leftEntryByVidm[key] << entry 
		}

		if( rights.size() == 1 ) {
			return lefts.collect { DicEntry left ->
				new DicEntry(left.word+"-"+rights[0].word, left.lemma+"-"+rights[0].word, left.tagStr) 
			}
		}

        List<String> pVnazForms = []
        
		for(DicEntry rightEntry in rights) {
			def rrr = gen_vidm_pattern.matcher(rightEntry.tagStr)
            rrr.find()
            def gender = rrr.group(1)

            def key = rrr.group(0)
            
            if( ! (gender in leftGenders) ) {
                key = ":" + leftGenders[0] + key[2..-1]
            }
            
			if( !(key in leftEntryByVidm) ) {
                boolean kly = key.contains("v_kly")
                if( ! kly ) {
                    log.warn("skipping $key for $rightEntry")
                }
				continue
			}

            def genVidm = key[1..-1]
            
			for(DicEntry leftEntry in leftEntryByVidm[key]) {
				String w_infl = leftEntry.word + "-" + rightEntry.word
				String lemma = leftEntry.lemma + "-" + rightEntry.lemma

				if( vMisCheck && key =~ /[nm]:v_mis/ && ! isMascVMisMatch(leftEntry.word, rightEntry.word, vMisCheck) )
					continue
                
                if( (rightEntry.comment == Expand.Z_V_U_COMMENT || leftEntry.comment == Expand.Z_V_U_COMMENT)
                        &&  rightEntry.comment != leftEntry.comment ) {
                    continue
                }
    
				String tagStr = tags_re.matcher(leftEntry.tagStr).replaceAll('$1'+genVidm+'$2')
				DicEntry entry = new DicEntry(w_infl, lemma, tagStr)
				outs.add(entry)
			}
		}
        
        assert outs, "Could not pair $lefts and $rights"

		return outs
	}

    private static boolean isAnimPVZnaMatch(left, right) {
        boolean leftVov = left =~ /[аяиі]$/
        boolean rightVov = right =~ /[аяиі]$/
        return leftVov == rightVov
    }
    
	private static boolean isMascVMisMatch(left, right, String vMisCheck) {
		if( vMisCheck.startsWith("u-") && left =~ /[ую]$/ && ! (right =~ /[ую]$/) )
			return false
		if( vMisCheck.endsWith("-u") && right =~ /[ую]$/ && ! (left =~ /[ую]$/) )
			return false
//		if( left =~ /[еєо]ві$/ && ! (right =~ /([ії]|[еєо]ві)$/) )
//			return false
//		if( left =~ /[ії]$/ && ! (right =~ /([ії]|[еєо]ві)$/) )
//			return false
		return true
	}
	
	List<DicEntry> expand_composite_line(String line) {
		if( ! line.contains(" - ") )
//			return [line]
			throw new IllegalArgumentException("Only composite lines are supported here, was: " + line)

		String[] parts_all
		if( line.contains(" :") ) {
			parts_all = line.split(" :")
			line = parts_all[0]
			parts_all[1] = ":" + parts_all[1]
		}
		else if( line.contains(" ^") ) {
			parts_all = line.split(/ \^/)
			line = parts_all[0]
			parts_all[1] = "^" + parts_all[1]
		}
		else {
			parts_all = [line]
		}
		
		String[] parts = line.split(" - ", 2)
		assert parts.length == 2, "Line does not have \" - \":\n$line"

		if( parts_all.size() > 1 ) {
			def extra_tags = parts_all[1]
			parts[0] += " " + extra_tags
			
			if( parts[1].contains("/") ) {
				parts[1] += " " + extra_tags
			}
		}

		List<DicEntry> lefts = expand.expand_line(parts[0]) //, true)

		List<DicEntry> rights = parts[1].contains("/") ? expand.expand_line(parts[1]) : [new DicEntry(parts[1], parts[1], null)]

		Matcher vmisCheckMatch = line =~ ('!([u*]-[*u])')
		String vmisCheck = ""
		if( vmisCheckMatch ) {
			vmisCheck = vmisCheckMatch.group(1)
		}
		else if( line =~ ' /adj.* /adj') {
			vmisCheck = "u-u"
		}
		return matchComps(lefts, rights, vmisCheck)
	}
	
	List<DicEntry> process_input(List<String> lines) {
		List<DicEntry> out = []

		for(String line in lines) {

            line = line.replaceFirst(/ *#.*$/, '')

			line = line.trim()
			if( ! line || line[0] == "#" )
				continue

			try {
				List<DicEntry> comps = expand_composite_line(line)
				
				if( Character.isUpperCase(line.charAt(0)) && ! line.contains("<") ) {
				    comps.each { it.tagStr += ":prop" }
				}
				
				out.addAll(comps)
			}
			catch(e) {
				log.error("Failed composite at {}: {}\n", line, e.getMessage())
				throw e
			}
		}

		return out
	}

}
