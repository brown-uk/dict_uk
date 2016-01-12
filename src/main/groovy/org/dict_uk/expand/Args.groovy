package org.dict_uk.expand

import org.apache.commons.cli.Option;

import groovy.transform.TypeChecked


//@TypeChecked
class Args {
	boolean corp
	boolean rules
	boolean mfl
	boolean indent
	boolean time
	boolean stats
	boolean log_usage
	boolean wordlist
	String affixDir
	String dictDir
	List<String> removeWithTags = []
	List<String> removeTags = []
	List<String> lemmaForTags = []


	public static Args args;

	//    public static parse_old(String[] argv) {
	//		if( "--corp" in argv )
	//			args.corp = true
	//		if( "--mfl" in argv )
	//			args.mfl = true
	//		if( "--indent" in argv )
	//			args.indent = true
	//		if( "--time" in argv )
	//			args.time = true
	//		if( "--stats" in argv )
	//			args.stats = true
	//		if( "--wordlist" in argv )
	//			args.wordlist = true
	//		if( "--log-usage" in argv )
	//			args.log_usage = true
	//		if( "--uncontr" in argv )
	//			args.removeWithTags.remove("uncontr")
	//	}

	public static parse(String[] argv) {
		def cli = new CliBuilder(usage: 'expandAll <flags> or expandAll -h')

		cli._(longOpt:'aff', args:1, required: true, 'Affix dir')
		cli._(longOpt:'dict', args:1, required: true, 'Dictionary dir')

		cli._(longOpt:'corp', 'Generate corpus version of the dictionary (implies: --lemmaWithTags advp,compr,super)')
		cli._(longOpt:'rules', 'Generate check rules version of the dictionary (implies: --removeTags ranim,rianim)')
		cli._(longOpt:'mfl', 'Generate morfologik format of the dictionary')
		
		cli._(longOpt:'indent', 'Generate indented format of the dictionary')

		cli._(longOpt:'stats', 'Generate dictionary statistics')
		cli._(longOpt:'wordlist', 'Generate word, lemma, and tag list files')

		cli._(longOpt:'time', 'Print timing info')
		cli._(longOpt:'log-usage', 'Generate affix usage statistics file')

		cli._(longOpt:'removeWithTags', type: String, args: 1, argName:'tags', 'Remove forms with listed tags (comma separated)')
		cli._(longOpt:'removeTags', type: String, args: 1, argName:'tags', 'Remove listed tags (comma separated) from generated forms')
		cli._(longOpt:'lemmaForTags', type: String, args: 1, argName:'tags', 'Promote generated forms with listed tags (comma separated) to separate lemmas (supported tags: advp, compr)')

		cli.h(longOpt: 'help', 'Help - Usage Information')


		def options = cli.parse(argv)

		if (!options || options.h) {
			cli.usage()
			System.exit(0)
		}

		args = new Args()
		args.with {
			corp = options.corp
			rules = options.rules
			mfl = options.mfl
			indent = options.indent
			time = options.time
			stats = options.stats
			log_usage = options['log-usage']
			wordlist = options.wordlist

			affixDir = options.aff
			dictDir = options.dict

			if( rules ) {
				removeWithTags = ["uncontr"]
				//TODO: we can't handle those yet in LT
				removeTags = ["ranim", "rinanim"]
			}
			
			if( corp ) {
				removeWithTags = ["uncontr"]
				// for corpus advp and comparative forms are separate lemmas
				lemmaForTags = ["advp", "compr", "super"]
			}
			
			if( options.removeWithTags )
				removeWithTags = options.removeWithTags.split(",")

			if( options.removeTags )
				removeTags = options.removeTags.split(",")
				
			if( options.lemmaForTags ) 
				lemmaForTags = options.lemmaForTags.split(",")
		}
	}
}
