#!/usr/bin/env groovy

package org.dict_uk.expand


import groovy.transform.TypeChecked;
import java.util.regex.*;

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.dict_uk.common.DicEntry
import org.dict_uk.expand.Expand

class ExpandComps {
	static Logger log = LogManager.getFormatterLogger(ExpandComps.class);

	final Pattern tags_re = Pattern.compile("(.*:)[mfnp]:v_...(.*)")
	final Pattern gen_vidm_pattern = Pattern.compile(":(.:v_...(:r(in)?anim)?)")
	final Expand expand


	public ExpandComps(Expand expand) {
		this.expand = expand
	}

	@TypeChecked
	List<DicEntry> match_comps(List<DicEntry> lefts, List<DicEntry> rights) {
		List<DicEntry> outs = []
		Map<String, List<String>> left_v = [:]
		String left_gen = ""
		boolean mixed_gen = false

		def left_wn
		def left_tags


		for(DicEntry ln in lefts) {
//			def parts = ln.split(" ")
			def rrr = gen_vidm_pattern.matcher(ln.tagStr)
			if( ! rrr.find() ) {
				log.warn("ignoring left %s", ln)
				continue
			}

			def vidm = rrr.group(1)
			if( "mfn".contains(vidm[0]) ) {
				if( !left_gen )
					left_gen = vidm[0]
				else
				if( left_gen != vidm[0] )
					mixed_gen = true
			}
			if( ! (vidm in left_v) )
				left_v[vidm] = []

			left_v[vidm].add(ln.word)
			left_wn = ln.lemma
			left_tags = ln.tagStr
		}

		if( mixed_gen ) {
			left_gen = ""
		}

		if( rights.size() == 1 ) {
			return lefts.collect { DicEntry left ->
				new DicEntry(left.word+"-"+rights[0].word, left.lemma+"-"+rights[0].word, left.tagStr) 
			}
		}

		for(DicEntry rn in rights ) {
			def rrr = gen_vidm_pattern.matcher(rn.tagStr)
			if( ! rrr.find() ) {
				log.warn("composite: ignoring right %s", rn)
				continue
			}

			def vidm = rrr.group(1)
			if( left_gen != "" && "mfn".contains(vidm[0]) )
				vidm = left_gen + vidm[1..-1]

			if( !(vidm in left_v) )
				continue

			for(String left_wi in left_v[vidm] ) {
				String w_infl = left_wi + "-" + rn.word
				String lemma = left_wn + "-" + rn.lemma
				
				String tagStr = tags_re.matcher(left_tags).replaceAll('$1'+vidm+'$2')
				DicEntry entry = new DicEntry(w_infl, lemma, tagStr)
				outs.add(entry)
			}
		}

		return outs
	}

	@TypeChecked
	List<DicEntry> expand_composite_line(String line) {
		if( ! line.contains(" - ") )
//			return [line]
			throw new IllegalArgumentException("Only composite lines are supported here, was: " + line)

		def parts_all
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

		String[] parts = line.split(" - ")

		if( parts_all.size() > 1 ) {
			def extra_tags = parts_all[1]
			parts[0] += " " + extra_tags
			
			if( parts[1].contains("/") ) {
				parts[1] += " " + extra_tags
			}
		}

		List<DicEntry> lefts = expand.expand_line(parts[0]) //, true)

		List<DicEntry> rights = parts[1].contains("/") ? expand.expand_line(parts[1]) : [new DicEntry(parts[1], parts[1], null)]

		return match_comps(lefts, rights)
	}

	@TypeChecked
	def process_input(List<String> lines) {
		List<DicEntry> out = []

		for(String line in lines) {

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
				System.err.printf("Failed composite at %s: %s\n", line, e.getMessage())
				throw e
			}
		}

		return out
	}

}
