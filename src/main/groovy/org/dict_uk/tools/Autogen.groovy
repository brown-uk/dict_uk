package org.dict_uk.tools

import java.text.Collator

class Autogen {
	
	private static File file(String name) { new File(name) }
	
	static void main(String[] args) {
		autogen(args[0], args[1], args[2])
	}
	
	static void autogen(String inputFile1, String inputFile2, String outputFile) {
		
		String[] lines = file(inputFile1).readLines("UTF-8")
				.findAll { line -> line.contains(":ua_1992") }

		def outReplaceLines = []

		String[] outLines1 = lines
				.findAll { line -> line =~ /проек[тц]|хіміо/ }
				.collect { line ->
					assert ! line.contains('#>')

					String newLine = line.replaceFirst(/проек([тц])/, 'проєк$1').replace('хіміо', 'хіміє')
					newLine = newLine.replace('ua_1992', 'ua_2019')

					String newLemma = line.replaceFirst(/(.*?) .*/, '$1').replace('проек', 'проєк')
					def newReplLine = line.padRight(64) + "#> $newLemma"
					outReplaceLines << newReplLine

					newLine
				}

		def pattern2 = ~ /(^|[а-яіїєґ']+-)(архі|архи|боді|гіпер|диско|екстра|камер|макро|максі|міді|мікро|міні|мульти|нано|полі|преміум|супер|топ|ультра|анти|контр|віце|екс|лейб|обер|штабс|унтер)-([^ ]+)(.*)/

		def lines2 = file(inputFile2).readLines("UTF-8")

		String[] outLines2 = (lines + lines2)
				.findAll { line -> pattern2.matcher(line).find() }
				.collect { line ->
					if( line.contains('#>') ) {
						println "Skipping replace in: $line"
						line = line.replaceFirst(/ *#>.*/, '')
					}

					String newLine = pattern2.matcher(line).replaceFirst('$1$2$3$4')
					// second time for екс-віце-спікер
					newLine = pattern2.matcher(newLine).replaceFirst('$1$2$3$4')
					// TODO: make generic
					newLine = newLine.replace('ua_1992', 'ua_2019')
					newLine = newLine.replace('камерю', 'камер\'ю')

					String newLemma = pattern2.matcher(line).replaceFirst('$1$2$3')
					def newReplLine = line.padRight(64) + "#> $newLemma"
					outReplaceLines << newReplLine

					newLine
				}

		outLines1 += outLines2


		lines = file(inputFile2).readLines("UTF-8")
		//            .findAll { line -> line.contains(":ua_1992") }

		def pattern3 = ~ /(^|-)(арт|бліц|веб|етно|кібер|компакт|мас|медіа|поп|прес|флеш|фоль?к|шоу)-/

		String[] outLines3 = lines
				.findAll { line -> pattern3.matcher(line).find() }
				.collect { line ->
					if( line.contains('#>') ) {
						println "Skipping replace in: $line"
						line = line.replaceFirst(/ *#>.*/, '')
					}

					String newLine = pattern3.matcher(line).replaceFirst('$1$2')
					newLine = newLine.replace('ua_1992', 'ua_2019')
				}

		outLines1 += outLines3

		// allow both forms

		def pattern4 = ~ /(^|-)(інтернет|піар|секс|фан|фітнес)-/

		String[] outLines4 = lines
				.findAll { line -> pattern4.matcher(line).find() }
				.collect { line ->
					if( line.contains('#>') ) {
						println "Skipping replace in: $line"
						line = line.replaceFirst(/ *#>.*/, '')
					}

					String newLine = pattern4.matcher(line).replaceFirst('$1$2')
					//                newLine = newLine.replace('ua_1992', 'ua_2019')

					String newLemma = pattern4.matcher(line).replaceFirst('$1$2')
					def newReplLine = newLemma.padRight(64) + "#> " + line.replaceFirst(/^([^ ]+).*/, '$1')
					outReplaceLines << newReplLine

					newLine
				}

		outLines1 += outLines4


		Collator collator = Collator.getInstance(new Locale("uk", "UA"));
		file(outputFile).setText(outLines1.toSorted(collator).join("\n"), "UTF-8")

		new File('data/dict/base-auto-replace.txt').setText(outReplaceLines.toSorted(collator).join('\n'), "UTF-8")

	}

}