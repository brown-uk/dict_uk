
import java.util.regex.Matcher;

import org.dict_uk.expand.*


def AFFIX_DIR = "../../data/affix"


Affix affixLoader = new Affix()
affixLoader.load_affixes(AFFIX_DIR)

char hunFlag = (int)' '+1

def flagMap = [:].withDefault{ [] }
def revFlagMap = [:]
def toHunFlagMap = [:]


affixLoader.affixMap.each { flag, affixGroupMap ->
	if( flag.startsWith("vr") ) {
		affixLoader.affixMap[ flag.replace('vr', 'v') ].putAll(affixGroupMap)
	}
}


affixLoader.affixMap.each { flag, affixGroupMap ->

	if( flag.contains('.ku') || flag.contains('.u') || flag.contains("patr_pl") || flag.startsWith("vr") )
		return

	//	println flag + " = " + affixGroupMap

	affixGroupMap.each { ending, affixGroup ->

		flagMap[ hunFlag ] << [ending: ending, affixGroup: affixGroup]

		//		flagMap[ [flag: flag, ending: affixGroup.match] ]
	}

	revFlagMap[hunFlag] = flag
	toHunFlagMap[flag] = hunFlag

	hunFlag = (char)((int)hunFlag+1)
	if(hunFlag == '#') {
		hunFlag = (char)((int)hunFlag+1)
	}

}


def out = ''
flagMap.each{ flag, affixGroupItems ->

	def body = ''
	def cnt = 0
	affixGroupItems.each { item ->

		if( item.affixGroup.neg_match ) {
			println "TODO: not handling negative match $item.affixGroup.neg_match for " + item.affixGroup.match + " in " + revFlagMap[flag]
		}
		
		item.affixGroup.affixes.each { affix ->

			if( affix.fromm && ! (affix.fromm ==~ /[а-яіїєґА-ЯІЇЄҐ']+/ ) ) {
//				println "regex in $flag ("+revFlagMap[flag]+") $affix.fromm -> $affix.to @ $item.ending"

				Matcher matcher = item.ending =~ /\[([а-яіїєґ']+)\]/

				if( ! matcher || matcher.size() > 2 ) {
					println "TODO: not handling $item.ending /$affix.fromm -> $affix.to/ for " + revFlagMap[flag]
					return
				}

				if( matcher.size() == 2 && (! affix.fromm.startsWith('.') || affix.fromm.contains('(')) ) {
//				println "regex in $flag ("+revFlagMap[flag]+") $affix.fromm -> $affix.to @ $item.ending"
					
					matcher[0][1].toCharArray().each { it1 ->
						matcher[1][1].toCharArray().each { it2 ->
							def fromm
							def to

							if( affix.fromm.contains('(..)') ) {
								fromm = affix.fromm.replace('(..)', ''+it1 + it2)
								to = affix.to.replaceFirst(/\$1/, ''+it1 + it2)
							}
							else if( affix.fromm.contains('.(.)') ) {
								fromm = affix.fromm.replace('.(.)', ''+it1 + it2)
								to = affix.to.replaceFirst(/\$1/, ''+it2)
							}
							else {
 								println "TODO: not handling $item.ending /$affix.fromm -> $affix.to/ for " + revFlagMap[flag]
								return
							}

							def ending = item.ending.replaceFirst(/\[/+matcher[0][1]+/\]/, ''+it1)
							ending = ending.replaceFirst(/\[/+matcher[0][2]+/\]/, ''+it2)
//													println "\t $fromm -> $to @ $ending"

							body += "SFX $flag $fromm $to $ending"
							body += '\n'
							cnt += 1
						}
					}
					return
	
				}

//				if( matcher.size() == 2 && affix.fromm.startsWith('.') && ! affix.fromm.contains('(') ) {
//					println "regex in $flag ("+revFlagMap[flag]+") $affix.fromm -> $affix.to @ $item.ending"
//				}
				
				def matcherGroup = matcher.size()==1 ? matcher[0][1] : matcher[1][1]
				
				matcherGroup.toCharArray().each {
					def fromm
					if( affix.fromm.contains('(.)') ) {
						fromm = affix.fromm.replaceFirst(/\(\.\)/, ''+it)
					}
					else if (affix.fromm.contains('.') ) {
						fromm = affix.fromm.replaceFirst(/\./, ''+it)
					}
					else if (affix.fromm.contains(matcher[0][0])) {
						fromm = affix.fromm.replace(matcher[0][0], ''+it)
					}
					else if ( ! affix.fromm ) {
						fromm = '0'
					}
					
					def to = affix.to ? affix.to.replaceFirst(/\$1/, ''+it) : 0
					def ending = item.ending.replaceFirst(/\[chars\]/, ''+it)
//					if( matcher.size() == 2 ) {
//						println "\t $fromm -> $to @ $ending"
//					}
					body += "SFX $flag $fromm $to $ending"
					body += '\n'
					cnt += 1
				}
				
			}
			else {
				def fromm = affix.fromm ? affix.fromm : '0'
				def to = affix.to ? affix.to : '0'
				def ending = item.affixGroup.match

				
				body += "SFX $flag $fromm $to $ending"
				body += '\n'
				cnt += 1
			}

		}
	}

	def header = "SFX $flag Y $cnt\t\t# " + revFlagMap[flag]
	out += header
	out += '\n'
	out += body
}

def fileHeader = new File('header/affix_header.txt').text
new File('build/hunspell/uk_UA.aff').text = fileHeader + '\n' + out



// word list


def dictDir = new File("../../data/dict")

def files = dictDir.listFiles().findAll {
	it.name.endsWith('.lst') && ! (it.name =~ /composite|dot-abbr|twisters|ignored|alt/)
}

println("Dict files: " + files*.name)


def MULTIFLAG_PATTERN = ~ ' /[nv][^#]+ /[nv]'

def superlatives = []

def lines = files.collect {
	it.text.split("\n")
}
.flatten()
.collect {
	if( MULTIFLAG_PATTERN.matcher(it) ) {
//		System.err.println('got dual flag ' + it)
		def parts = it.split(' ')
		return [parts[0] + ' ' + parts[1], parts[0] + ' ' + parts[2]]
	}
	return it
}
.flatten()
.collect {
	it = it.replaceFirst(/ *#.*/, '').trim()
	if( ! it )
		return ''

	if( ! it.contains(" /") ) {
		if( it.startsWith("+cs") ) {
			it = it.replaceFirst(/\+cs=([^ ]*).*/, '$1/' + toHunFlagMap['adj'])
			superlatives << 'най' + it
			superlatives << 'щонай' + it
			superlatives << 'якнай' + it
		}
		else {
			it = it.split()[0]
		}
		return it
	}
	else if( it.contains("/<") ) {
		it = it.split()[0]
		return it
	}
	else {
		def parts = it.split()
		
		if( parts[1].contains("/n10") || parts[1].contains("/n3") ) {
			if( ! parts[1].contains(".k") && ! parts[0].contains("ще ") ) {
				parts[1] += parts[1].contains("/n10") ? ".ko" : ".ke"
			}
		}
		else if( parts[1] =~ '/n2[0-4]' && ! parts[1].contains(".k") ) {
			if( Expand.isDefaultKlyE(parts[0], parts[1]) ) {
			    parts[1] += ".ke"
		    }
		}
		

		def allFlags = parts[1][1..-1]

		if( allFlags.startsWith('vr') ) {
			allFlags = allFlags.replace('vr', 'v')
		}

		def flgs = allFlags.split(/\./)

		def mainFlg = flgs[0]

		def outFlags = ''

		flgs.each { flg ->
			if( flg.startsWith('<') || flg == 'u'|| flg == 'ku' || flg == '@' )
				return

			def f = flg == mainFlg ? flg : mainFlg + '.' + flg
			if( flg == 'patr' ) {
				f = 'n.patr'
			}
			else if( f.contains('.cf') || f.contains('.imprs') ) {
				f = f.replaceFirst('v[0-9]', 'v')
			}

			if( ! toHunFlagMap.containsKey(f) ) {
				println 'cant find ' + parts[0] + '/' + f
			}

			outFlags += toHunFlagMap[f]
		}

		return parts[0] + '/' + outFlags
	}


	if( it ) {
		it = it.replaceFirst(/[. ].*/, '')
	}
}.grep {
	it
}

lines.addAll(superlatives)

Args.args = new Args()

def dic_file = new File("../../data/dict/composite.lst" )
def expand = new Expand()
expand.affix.load_affixes(AFFIX_DIR)
def expand_comps = new ExpandComps(expand)

def comps
def dic_file_reader = dic_file.withReader("utf-8") { reader ->
	comps = expand_comps.process_input(reader.readLines())
}
println "Got $comps.size comps"

lines.addAll(comps.collect{ it.split()[0] })


println "Found ${lines.size} total lines"

def words = lines //.toSorted().unique()

println "Found ${lines.size} unique lines"

def txt = "${words.size}\n"
txt += words.join("\n")

new File("build/hunspell/uk_UA.dic").text = txt
