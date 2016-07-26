
import org.dict_uk.expand.*



Affix affixLoader = new Affix()
affixLoader.load_affixes("../../data/affix")

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
	
	if( flag.contains('.ku') || flag.contains("patr_pl") || flag.startsWith("vr") )
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
		item.affixGroup.affixes.each { affix ->
			def fromm = affix.fromm ? affix.fromm : '0'  
			def to = affix.to ? affix.to : '0'
			body += "SFX $flag $fromm $to $item.ending"
			body += '\n'
			cnt += 1
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
    it.name.endsWith('.lst') && ! (it.name =~ /composite|dot-abbr|twisters|ignored|alt|rare/)
}

println("Dict files: " + files*.name)

def lines = files.collect {
    it.text.split("\n")
}
.flatten()
.collect {
    it = it.replaceFirst(/ *#.*/, '').trim()
    if( ! it )
        return ''
    
    if( ! it.contains(" /") ) {
        if( it.startsWith("+cs") ) {
            it = it.replaceFirst(/\+cs=([^ ]*).*/, '$1/ac')
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
	
		def allFlags = parts[1][1..-1]

		if( allFlags.startsWith('vr') ) {
			allFlags = allFlags.replace('vr', 'v')
		}

		def flgs = allFlags.split(/\./)
		
		def mainFlg = flgs[0]

		def outFlags = ''
		
		flgs.each { flg ->
			if( flg.startsWith('<') || flg == 'ku' || flg == '@' )
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

println "Found ${words.size} $lines.size"

def words = lines.unique().toSorted()

def txt = "${words.size}\n"
txt += words.join("\n")

new File("build/hunspell/uk_UA.dic").text = txt
