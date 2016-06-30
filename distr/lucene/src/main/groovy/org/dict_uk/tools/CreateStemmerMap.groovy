package org.dict_uk.tools

import org.dict_uk.expand.Affix
import org.dict_uk.expand.SuffixGroup


Affix affix = new Affix()
affix.load_affixes('../../data/affix')

def outLines = affix.affixMap.collect { String flag, Map<String, SuffixGroup> v ->
	v.collect { String match, SuffixGroup v1 ->
		v1.affixes.findAll { aff ->
		    ! aff.tags.contains(':uncontr')  \
		    && ! aff.tags.contains(':inf:coll') \
		    && ! flag.contains('adj_pron') \
		    && ! (flag.contains('n2adj1') && ! aff.fromm.endsWith('ін') )
		}
		.collect { aff ->
			aff.to + ' ' + aff.fromm + '\t\t# ' + match + ' / ' + flag + ' @ ' + aff.tags
		}
	}
}
.flatten()
.sort()

new File('stemmerMap.txt').text = outLines.join('\n')

System.err.println("Got $outLines.size lines")
