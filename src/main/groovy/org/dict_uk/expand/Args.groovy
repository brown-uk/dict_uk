package org.dict_uk.expand

import java.util.logging.Level
import java.util.logging.Logger
import java.util.regex.*

import org.dict_uk.common.UkDictComparator

import groovy.transform.TypeChecked


@TypeChecked
class Args {
	boolean corp
	boolean mfl
	boolean indent
	boolean time
	boolean stats
	boolean log_usage
	boolean wordlist
	List<String> removeWithTags = ["uncontr"]
	List<String> removeTags = []


	public static Args args = new Args();

	public static parse(String[] argv) {
		if( "--corp" in argv )
			args.corp = true
		if( "--mfl" in argv )
			args.mfl = true
		if( "--indent" in argv )
			args.indent = true
		if( "--time" in argv )
			args.time = true
		if( "--stats" in argv )
			args.stats = true
		if( "--wordlist" in argv )
			args.wordlist = true
		if( "--log_usage" in argv )
			args.log_usage = true
		if( "--uncontr" in argv )
			args.removeWithTags.remove("uncontr")
	}
}
