package org.dict_uk.tools

import java.text.Collator

import org.dict_uk.expand.TaggedWordlist
import org.junit.Test

import groovy.transform.CompileStatic

class Autogen {
	
	private static File file(String name) { new File(name) }
	
	static void main(String[] args) {
		autogen(args[0], args[1], args[2], args[3])
	}
	
	static String[][] replaceLetters_2019(String[] lines) {
		def outLines = []
		def outReplaceLines = []
		def errors = []
		
		lines
		.findAll { line -> line =~ /проек[тц]|хіміо/ }
		.each { line ->
			if( line.contains('#>') )
				return
			
			if( line =~ /проек[тц]/ ) {
				if( ! line.contains(":ua_1992") ) {
					errors << line
				}
			}

			String newLine = line.replaceFirst(/проек([тц])/, 'проєк$1').replace('хіміо', 'хіміє')
			newLine = newLine.replace('ua_1992', 'ua_2019')

			String newLemma = line.replaceFirst(/(.*?) .*/, '$1').replaceFirst(/проек([тц])/, 'проєк$1').replace('хіміо', 'хіміє')
			def newReplLine = line.padRight(64) + "#> $newLemma"
			outReplaceLines << newReplLine

			outLines << newLine
		}
		
		if( errors ) {
			System.err.println "-проект- without :ua_1992\n" + errors.join("\n")
			System.exit(1)
		}

		[ outLines, outReplaceLines ]
	}

	static String[][] enforceNoDash_2019(String[] lines) {
		def outLines = []
		def outReplaceLines = []
		def errors = []
		
		// notes:
		// арт - ще від артилерійський, було разом в 1992
		// бод[иі] - зроблено вручну
		// не було унормовано: диско-, економ-
		
		//TODO: етно-
		// етно - (неофіційно), але було разом, бо скор. від етнологічний
		
		// писалися офіційно з дефісом в 1992:
		// віце-, екс-, лейб-, максі-, міді-, міні-, обер-
				
		// писалися без дефісу і з 1992:
		def lev1prefixes_1992 = "анти|архі|архи|гіпер|етно|мікро|макро|мульти|нано|пан|полі|супер|ультра"
		// нове без дефісу з 2019:
		def lev1prefixes = "арт|бліц|веб|віце|диско|економ|екс|екстра|камер|кібер|контр|лейб|максі|медіа|міді|міні|обер|поп|прес|преміум|смарт|топ|унтер|флеш|фоль?к|штабс"
		
		// two regexes to handle екс-віце-спікер
		def pattern1 = ~ /([а-яіїєґ']+-)($lev1prefixes)-([^ ]+)(.*)/
		def pattern2 = ~ /(^|[а-яіїєґ']+-)($lev1prefixes)-([^ ]+)(.*)/
		
		lines
		.findAll { line -> pattern2.matcher(line).find() }
		.each { line ->
			if( line.contains('#>') ) {
				println "Skipping replace in: $line"
				line = line.replaceFirst(/ *#>.*/, '')
			}

			if( ! line.contains(":ua_1992") ) {
				if( ! (line =~ /прес-(ніж|ножиц)/) ) {
					errors << line
				}
			}
			
			String newLine = pattern1.matcher(line).replaceFirst('$1$2$3$4')
			newLine = pattern2.matcher(newLine).replaceFirst('$1$2$3$4')
			// TODO: make generic
			newLine = newLine.replace('ua_1992', 'ua_2019')
			newLine = newLine.replace('камерю', 'камер\'ю')

			String newLemma = pattern2.matcher(line).replaceFirst('$1$2$3')
			def newReplLine = line.padRight(64) + "#> $newLemma"
			outReplaceLines << newReplLine

			outLines << newLine
		}
		
		if( errors ) {
			System.err.println "анти-, архі-... without :ua_1992\n" + errors.join("\n")
			System.exit(1)
		}

		// check old words without dash
		errors = []
		def pattern3 = ~ /(^|[а-яіїєґ']+-)($lev1prefixes_1992)-([^ ]+)(.*)/
		
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
			def oldLemma = line.replaceFirst(/^([^ ]+).*/, '$1')
			def newReplLine = newLemma.padRight(64) + "#> " + oldLemma
			outReplaceLines << newReplLine

			outLines << newLine
		}

		[ outLines, outReplaceLines ]
	}
	
	
		
	static void autogen(String baseFile, String compoundFile, String compound1992File, String outputFile) {
		
		String[] lines = file(baseFile).readLines("UTF-8")

	    def repl = replaceLetters_2019(lines)
				
		def outReplaceLines = repl[1]
		String[] outLines1 =  repl[0]

		String[] compoundLines = file(compoundFile).readLines("UTF-8")
		lines += compoundLines
		
		String[] compound1992Lines = new TaggedWordlist().processInput([compound1992File])
		lines += compound1992Lines
		
		repl = enforceNoDash_2019(lines)
		
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