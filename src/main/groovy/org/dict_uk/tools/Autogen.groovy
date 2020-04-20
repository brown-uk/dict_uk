package org.dict_uk.tools

import java.text.Collator

import org.junit.Test

class Autogen {
	
	private static File file(String name) { new File(name) }
	
	static void main(String[] args) {
		autogen(args[0], args[1], args[2])
	}
	
	static String[][] replaceLetters(String[] lines) {
		def outLines = []
		def outReplaceLines = []
		def errors = []
		
		lines
		.findAll { line -> line =~ /проек[тц]|хіміо/ }
		.each { line ->
			assert ! line.contains('#>')
			
			if( line =~ /проек[тц]/ ) {
				if( ! line.contains(":ua_1992") ) {
					errors << line
				}
			}

			String newLine = line.replaceFirst(/проек([тц])/, 'проєк$1').replace('хіміо', 'хіміє')
			newLine = newLine.replace('ua_1992', 'ua_2019')

			String newLemma = line.replaceFirst(/(.*?) .*/, '$1').replace('проек', 'проєк')
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
		
		// two regexes to handle екс-віце-спікер
		def pattern1 = ~ /([а-яіїєґ']+-)(анти|архі|архи|боді|віце|гіпер|диско|екс|екстра|камер|макро|контр|лейб|максі|міді|мікро|міні|мульти|нано|обер|полі|прес|преміум|супер|топ|ультра|штабс|унтер)-([^ ]+)(.*)/
		def pattern2 = ~ /(^|[а-яіїєґ']+-)(анти|архі|архи|боді|віце|гіпер|диско|екс|екстра|камер|макро|контр|лейб|максі|міді|мікро|міні|мульти|нано|обер|полі|прес|преміум|супер|топ|ультра|штабс|унтер)-([^ ]+)(.*)/
		
		lines
		.findAll { line -> pattern2.matcher(line).find() }
		.each { line ->
			if( line.contains('#>') ) {
				println "Skipping replace in: $line"
				line = line.replaceFirst(/ *#>.*/, '')
			}

			if( ! (line =~ /прес-(ніж|ножиц)|ультра-сі/) )
				if( ! line.contains(":ua_1992") )
					errors << line
			
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

		[ outLines, outReplaceLines ]
	}
		

	static String[][] replaceArtBlitz(String[] lines) {
		def outLines = []
		def outReplaceLines = []
		def errors = []
		
		def pattern3 = ~ /(^|-)(арт|бліц|веб|етно|кібер|компакт|мас|медіа|поп|флеш|фоль?к|шоу)-/
		
		lines
				.findAll { line -> pattern3.matcher(line).find() }
				.each { line ->
					if( line.contains('#>') ) {
						println "Skipping replace in: $line"
						line = line.replaceFirst(/ *#>.*/, '')
					}

					if( ! line.contains(":ua_1992") )
						errors << line

					String newLine = pattern3.matcher(line).replaceFirst('$1$2')
					newLine = newLine.replace('ua_1992', 'ua_2019')

					outLines << newLine
				}

		if( errors ) {
			System.err.println "WARN: арт-, бліц-... without :ua_1992\n" + errors.join("\n")
//			System.exit(1)
		}

		[ outLines, outReplaceLines ]
	}

	
	
	static String[][] replaceAllowingBoth(String[] lines) {
		def outLines = []
		def outReplaceLines = []

		def pattern4 = ~ /(^|-)(інтернет|піар|секс|фан|фітнес)-/
		
		lines.findAll { line -> pattern4.matcher(line).find() }
		.each { line ->
			if( line.contains('#>') ) {
				println "Skipping replace in: $line"
				line = line.replaceFirst(/ *#>.*/, '')
			}

			String newLine = pattern4.matcher(line).replaceFirst('$1$2')
			//                newLine = newLine.replace('ua_1992', 'ua_2019')

			String newLemma = pattern4.matcher(line).replaceFirst('$1$2')
			def newReplLine = newLemma.padRight(64) + "#> " + line.replaceFirst(/^([^ ]+).*/, '$1')
			outReplaceLines << newReplLine

			outLines << newLine
		}

		[ outLines, outReplaceLines ]
	}
	
		
	static void autogen(String inputFile1, String inputFile2, String outputFile) {
		
		String[] lines = file(inputFile1).readLines("UTF-8")
				//.findAll { line -> line.contains(":ua_1992") }

	    def repl = replaceLetters(lines)
				
		def outReplaceLines = repl[1]
		String[] outLines1 =  repl[0]

		def lines2 = file(inputFile2).readLines("UTF-8")

		lines += lines2
		
		repl = replaceMain(lines)
		
		String[] outLines2 = repl[0]
		outReplaceLines += repl[1]
		
		outLines1 += outLines2

		lines = file(inputFile2).readLines("UTF-8")
		//            .findAll { line -> line.contains(":ua_1992") }

		repl = replaceArtBlitz(lines)

		String[] outLines3 = repl[0]
		outReplaceLines += repl[1]
		
		outLines1 += outLines3

		// allow both forms


		repl = replaceAllowingBoth(lines)
		String[] outLines4 = repl[0]
		outReplaceLines += repl[1]
		
		outLines1 += outLines4


		Collator collator = Collator.getInstance(new Locale("uk", "UA"));
		file(outputFile).setText(outLines1.toSorted(collator).join("\n"), "UTF-8")

		new File('data/dict/base-auto-replace.txt').setText(outReplaceLines.toSorted(collator).join('\n'), "UTF-8")
	}

	
//	@org.junit.jupiter.api.Test
//	void testName() {
//		assertEquals("еспресаташе", replaceMain("екс-прес-аташе")[0][0])
//	}
}