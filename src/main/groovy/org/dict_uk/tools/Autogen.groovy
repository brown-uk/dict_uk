package org.dict_uk.tools

import java.text.Collator
import java.util.regex.Pattern

import org.dict_uk.expand.TaggedWordlist
import org.junit.Test

import groovy.transform.CompileStatic

class Autogen {
	
	private static File file(String name) { new File(name) }
	
	static void main(String[] args) {
		autogen(args[0], args[1], args[2], args[3])
	}

	static String[][] replaceLetters(String[] lines) {
		def outLines = []
		def outReplaceLines = []
		def errors = []
		
		lines
		.findAll { line -> 
			line =~ /^[^#]*(проек[тц]|хіміо|хімі[чкя])|марафон|^(двох|трьох|чотирьох)/ 
		}
		.each { line ->
			if( line.contains('#>') ) {
				println "Skipping line with replace: $line"
				return
			}
			
			if( line =~ /проек[тц]/ ) {
				if( ! line.contains(":ua_1992") ) {
					errors << line
				}
			}

			if( line =~ /^(двох|трьох|чотирьох)/ ) {
				if( ! line.contains(":ua_1992") ) {
					return
				}
			}
			else if( line =~ /^[^#]*хімі[ячк]/ ){
				String newLine = line.replaceFirst(/хімі([ячк])/, 'хемі$1')
				if( ! newLine.contains(":alt") ) {
					if( newLine.contains(" :") ) {
						newLine = newLine.replaceFirst(/^(.*? \/[^ ]+( (:[a-z_0-9-]+)+))( *.*)$/, '$1:alt$4')
					}
					else {
						newLine = newLine.replaceFirst(/^(.*? \/[^ ]+)( *.*)$/, '$1 :alt$2')
					}
//					newLine = newLine.replaceFirst(/(:[a-z_0-9]) (:alt)/, '$1$2')
				}

				outLines << newLine
				return
			}
			else if( line =~ /марафон/ ) {
				String newLine = line.replaceFirst(/марафон/, 'маратон')

				if( newLine.contains(" :") ) {
					newLine = newLine.replaceFirst(/^(.*? \/[^ ]+( (:[a-z_0-9-]+)+))( *.*)$/, '$1:ua_2019$4')
				}
				else {
					newLine = newLine.replaceFirst(/^(.*? \/[^ ]+)( *.*)$/, '$1 :ua_2019$2')
				}

				outLines << newLine
				return
			}
			else {
				String newLine = line.replaceFirst(/проек([тц])/, 'проєк$1').replace('хіміо', 'хіміє')
				newLine = newLine.replace('ua_1992', 'ua_2019')

				outLines << newLine
			}
			
			String newLemma = line.replaceFirst(/(.*?) .*/, '$1')
				.replaceFirst(/проек([тц])/, 'проєк$1')
				.replace('хіміо', 'хіміє')
				.replaceFirst(/^(дво)х'?/, '$1')
				.replaceFirst(/^(тр|чотир)ь?ох'?/, '$1и')
			def newReplLine = line.padRight(64) + "#> $newLemma"
			outReplaceLines << newReplLine
		}
		
		if( errors ) {
			System.err.println "-проект- without :ua_1992\n" + errors.join("\n")
			System.exit(1)
		}

		[ outLines, outReplaceLines ]
	}
	
	static String addReplace(String line, String repl) {
		line.padRight(64) + "#> " + repl
	}

	// Notes:
	// арт - ще від артилерійський, було разом в 1992
	// бод[иі] - зроблено вручну
	// не було унормовано: диско-, економ-
	
	//TODO: етно-
	// етно - (неофіційно), але було разом, бо скор. від етнологічний
	
	// писалися офіційно з дефісом в 1992:
	// віце-, екс-, лейб-, максі-, міді-, міні-, обер-
			
	// писалися без дефісу і з 1992:
	static String lev1prefixes1992 = "анти|архі|архи|етно|мікро|макро|мульти|нано|полі|ультра"
	static String lev1prefixes1992Apo = "гіпер|пан|супер"
	// нове без дефісу з 2019:
	static String lev1prefixes = "віце|диско|екстра|максі|медіа|міді|міні"
	static String lev1prefixesApo = "арт|бліц|веб|економ|екс|камер|кібер|контр|лейб|обер|поп|прес|преміум|смарт|топ|унтер|флеш|фоль?к|штабс"
	
	// two regexes to handle екс-віце-спікер
	static Pattern pattern1 = ~ /([а-яіїєґ']+-)($lev1prefixes)-([^ ]+)(.*)/
	static Pattern pattern2 = ~ /(^|[а-яіїєґ']+-)($lev1prefixes)-([^ ]+)(.*)/
	static Pattern pattern2All = ~ /(^|[а-яіїєґ']+-)($lev1prefixes|$lev1prefixesApo)-([^ ]+)(.*)/
	static Pattern pattern1NoApo = ~ /([а-яіїєґ']+-)($lev1prefixesApo)-([^єїюя][^ ]+)(.*)/
	static Pattern pattern2NoApo = ~ /(^|[а-яіїєґ']+-)($lev1prefixesApo)-([^єїюя][^ ]+)(.*)/
	static Pattern pattern1Apo = ~ /([а-яіїєґ']+-)($lev1prefixesApo)-([єїюя][^ ]+)(.*)/
	static Pattern pattern2Apo = ~ /(^|[а-яіїєґ']+-)($lev1prefixesApo)-([єїюя][^ ]+)(.*)/

	static String removeHyphen(String line) {
		String newLine = pattern1.matcher(line).replaceFirst('$1$2$3$4')
		newLine = pattern2.matcher(newLine).replaceFirst('$1$2$3$4')

		newLine = pattern1NoApo.matcher(newLine).replaceFirst('$1$2$3$4')
		newLine = pattern2NoApo.matcher(newLine).replaceFirst('$1$2$3$4')

		newLine = pattern1Apo.matcher(newLine).replaceFirst('$1$2\'$3$4')
		newLine = pattern2Apo.matcher(newLine).replaceFirst('$1$2\'$3$4')
	}
	

	static String[][] enforceNoDash2019(String[] lines) {
		def outLines = []
		def outReplaceLines = []
		def errors = []
		
		lines
		.findAll { line -> 
			pattern2All.matcher(line).find() 
		}
		.each { line ->
			if( line.contains('#>') ) {
				println "Skipping replace in: $line"
				line = line.replaceFirst(/ *#>.*/, '')
			}

			if( ! line.contains(":ua_1992") ) {
				if( ! (line =~ /прес-(ніж|ножиц)|міді-файл/) ) {
					errors << line
				}
			}

			if( line =~ /проект/ ) {
				// add арт-проєкт
				def newLine = line.replace('проект', 'проєкт').replace('ua_1992', 'ua_2019')
				outLines << newLine

				// repl: арт-проєкт -> артпроєкт
				outReplaceLines << addReplace(newLine, removeHyphen(newLine.replaceFirst(/ .*/, '')))

				// repl: артпроект -> артпроєкт
				outReplaceLines << addReplace(removeHyphen(line), removeHyphen(newLine.replaceFirst(/ .*/, '')))
			}

			String newLine = removeHyphen(line)

			if( newLine =~ /проект/ ) {
				// артпроект still as :ua_1992
				outLines << newLine
				
				newLine = newLine.replace('проект', 'проєкт')
			}
			
			newLine = newLine.replace('ua_1992', 'ua_2019')

			outLines << newLine

			// generate replacements
			
			String replLemma = newLine.replaceFirst(/ .*/, '')

			// repl: арт-проект -> артпроєкт
			replLemma = replLemma.replace('проект', 'проєкт')
			
			def newReplLine = addReplace(line, replLemma)
			outReplaceLines << newReplLine
		}
		
		if( errors ) {
			System.err.println "анти-, архі-... without :ua_1992\n" + errors.join("\n")
			System.exit(1)
		}

		// check old words without dash
		errors = []
		def pattern3 = ~ /(^|[а-яіїєґ']+-)($lev1prefixes1992)-([^ ]+)(.*)/
		
		lines
		.findAll { line -> pattern3.matcher(line).find() }
		.each { line ->
			if( ! (line =~ /ультра-сі|супер-пупер/) )
				errors << line
		}

		if( errors ) {
			System.err.println "should not have dash even in 1992:\n" + errors.join("\n")
			System.exit(1)
		}

		[ outLines, outReplaceLines ]
	}
		

	// forceWithDash2019
	// it's hard to verify words without dash as many are correct, e.g. бізнесовий
	// def pattern2019 = ~ /(^|-)(альфа|бета|дельта|бізнес|блок|генерал|дизель|допінг|інтернет|кіловат|караоке|компакт|крекінг|піар|прем'єр|суші|фан|фітнес)-/
		
	
	static String[][] enforceAlwaysDash(String[] lines) {
		def outLines = []
		def outReplaceLines = []

		// we only generate dash-less for prefixes that happen frequently
		def pattern = ~ /(^|-)(інтернет|компакт|мас|секс|піар|фан|фітнес|шоу)-/

		lines
		.findAll { line -> pattern.matcher(line).find() }
		.each { line ->
			if( line.contains('#>') ) {
				println "Skipping line with replace: $line"
				return
			}

			if( line =~ /авто-?фан/ ) {
				println "TODO: skipping: $line"
				return
			}
			
			String newLine = pattern.matcher(line).replaceFirst('$1$2')
			String newLemma = newLine
			outLines << newLine

			def oldLemma = line.replaceFirst(/^([^ ]+).*/, '$1')
			def newReplLine = newLemma.padRight(64) + "#> " + oldLemma
			outReplaceLines << newReplLine

		}

		[ outLines, outReplaceLines ]
	}
	
	
		
	static void autogen(String baseFile, String compoundFile, String compound1992File, String outputFile) {
		
		String[] lines = file(baseFile).readLines("UTF-8")

	    def repl = replaceLetters(lines)
				
		def outReplaceLines = repl[1]
		String[] outLines1 =  repl[0]

		String[] compoundLines = file(compoundFile).readLines("UTF-8")
		lines += compoundLines
		
		String[] compound1992Lines = new TaggedWordlist().processInput([compound1992File])
		lines += compound1992Lines
		
		repl = enforceNoDash2019(lines)
		
		String[] outLines2 = repl[0]
		outReplaceLines += repl[1]
		
		outLines1 += outLines2

		writeListToFile(file(outputFile), outLines1)
		writeListToFile(new File('data/dict/base-auto-replace.txt'), outReplaceLines)
		
		repl = enforceAlwaysDash(compoundLines)
		
		writeListToFile(new File('data/dict/invalid-autogen.lst'), repl[0])
		writeListToFile(new File('data/dict/invalid-auto-replace.txt'), repl[1])
	}

	private static void writeListToFile(File file, def list) {
		Collator collator = Collator.getInstance(new Locale("uk", "UA"));
		file.setText(list.toSorted(collator).join("\n"), "UTF-8")
	}
	
	
//	@org.junit.jupiter.api.Test
//	void testName() {
//		assertEquals("еспресаташе", replaceMain("екс-прес-аташе")[0][0])
//	}
}