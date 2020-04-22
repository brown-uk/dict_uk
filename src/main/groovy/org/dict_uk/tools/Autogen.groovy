package org.dict_uk.tools

import java.text.Collator

import org.dict_uk.expand.TaggedWordlist
import org.junit.Test

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

	static String[][] replaceMain(String[] lines) {
		def outLines = []
		def outReplaceLines = []
		def errors = []
		
		// notes:
		// арт - ще від артилерійський, було разом в 1992
		// етно - (неофіційно), але було разом, бо скор. від етнологічний
		// бод[иі] - зроблено вручну
		// не було унормовано: диско-, економ-
		
		//TODO: етно-, кібер-, медіа-

		// писалися офіційно з дефісом в 1992:
		// віце-, екс-, лейб-, максі-, міді-, міні-, обер-
				
		// писалися без дефісу і з 1992:
		def lev1prefixes_1992 = "анти|архі|архи|гіпер|етно|мікро|макро|мульти|нано|пан|полі|супер|ультра"
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

			if( ! (line =~ /прес-(ніж|ножиц)/) ) {
				if( ! line.contains(":ua_1992") ) {
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
			if( ! (line =~ /ультра-сі/) )
				errors << line
		}

		if( errors ) {
			System.err.println "should not have dashes even in 1992:\n" + errors.join("\n")
			System.exit(1)
		}

		[ outLines, outReplaceLines ]
	}
		

	static String[][] onlyWithDash2019(String[] lines) {
		def outLines = []
		def outReplaceLines = []
		def errors = []
		
		// ?? блок-пост, блок-флейта, бізнес-вуман, мінівен/мінібус, блок-шот, поп-корн
		// етно-культурний, етно-історичний, етно-конфесійний
		
		def pattern2019 = ~ /(^|-)(альфа|бета|дельта|бізнес|блок|генерал|дизель|допінг|інтернет|кіловат|караоке|компакт|крекінг|піар|прем'єр|суші|фан|фітнес)-/
		
		lines
				.findAll { line -> pattern2019.matcher(line).find() }
				.each { line ->
					if( line.contains('#>') ) {
						println "Skipping replace in: $line"
						line = line.replaceFirst(/ *#>.*/, '')
					}

					if( ! line.contains("-") )
						errors << line

					outLines << newLine
				}

		if( errors ) {
			System.err.println "WARN: бізнес-, бліц-... without dash\n" + errors.join("\n")
			System.exit(1)
		}

		[ outLines, outReplaceLines ]
	}

	
	static String[][] onlyWithDashSoft(String[] lines) {
		def outLines = []
		def outReplaceLines = []
		def errors = []
		
		def pattern = ~ /(^|-)(мас|секс|шоу)-/
		
		lines
				.findAll { line -> pattern.matcher(line).find() }
				.each { line ->
					if( line.contains('#>') ) {
						println "Skipping replace in: $line"
						line = line.replaceFirst(/ *#>.*/, '')
					}

					if( ! line.contains(":ua_1992") )
						errors << line

					String newLine = pattern.matcher(line).replaceFirst('$1$2')
					newLine = newLine.replace('ua_1992', 'ua_2019')

					outLines << newLine
				}

		if( errors ) {
			System.err.println "WARN: арт-, бліц-... without :ua_1992\n" + errors.join("\n")
//			System.exit(1)
		}

		[ outLines, outReplaceLines ]
	}

//	static String[][] replaceAllowingBoth(String[] lines) {
//		def outLines = []
//		def outReplaceLines = []
//
//		def pattern4 = ~ /(^|-)(інтернет|піар|секс|фан|фітнес)-/
//		
//		lines.findAll { line -> pattern4.matcher(line).find() }
//		.each { line ->
//			if( line.contains('#>') ) {
//				println "Skipping replace in: $line"
//				line = line.replaceFirst(/ *#>.*/, '')
//			}
//
//			String newLine = pattern4.matcher(line).replaceFirst('$1$2')
//			//                newLine = newLine.replace('ua_1992', 'ua_2019')
//
//			String newLemma = pattern4.matcher(line).replaceFirst('$1$2')
//			def newReplLine = newLemma.padRight(64) + "#> " + line.replaceFirst(/^([^ ]+).*/, '$1')
//			outReplaceLines << newReplLine
//
//			outLines << newLine
//		}
//
//		[ outLines, outReplaceLines ]
//	}
	
		
	static void autogen(String inputFile1, String inputFile2, String inputFile3, String outputFile) {
		
		String[] lines = file(inputFile1).readLines("UTF-8")
				//.findAll { line -> line.contains(":ua_1992") }

	    def repl = replaceLetters(lines)
				
		def outReplaceLines = repl[1]
		String[] outLines1 =  repl[0]

		lines += file(inputFile2).readLines("UTF-8")
		
		String[] compound1992Lines = new TaggedWordlist().processInput([inputFile3])
		lines += compound1992Lines
		
		repl = replaceMain(lines)
		
		String[] outLines2 = repl[0]
		outReplaceLines += repl[1]
		
		outLines1 += outLines2

		if( false ) {
			lines = file(inputFile2).readLines("UTF-8")
			//            .findAll { line -> line.contains(":ua_1992") }

			repl = replaceArtBlitz(lines)

			String[] outLines3 = repl[0]
			outReplaceLines += repl[1]

			outLines1 += outLines3
		}

		// allow both forms

		if( false ) {
			repl = replaceAllowingBoth(lines)
			String[] outLines4 = repl[0]
			outReplaceLines += repl[1]

			outLines1 += outLines4
		}


		Collator collator = Collator.getInstance(new Locale("uk", "UA"));
		file(outputFile).setText(outLines1.toSorted(collator).join("\n"), "UTF-8")

		new File('data/dict/base-auto-replace.txt').setText(outReplaceLines.toSorted(collator).join('\n'), "UTF-8")
	}

	
//	@org.junit.jupiter.api.Test
//	void testName() {
//		assertEquals("еспресаташе", replaceMain("екс-прес-аташе")[0][0])
//	}
}