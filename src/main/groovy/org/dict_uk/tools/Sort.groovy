package org.dict_uk.tools

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream

import org.dict_uk.common.UkDictComparator

class Sort {

	static main(args) {
		Stream.of(args).parallel().each { filename ->
			println "Sorting $filename..."
			sortFile(filename)
		}
	}

	private static sortFile(String filename) {
		def dir = Paths.get(new File("").getAbsolutePath())

		def inFile = dir.resolve(filename)
		def bakFile = dir.resolve(filename + ".bak")

		Files.copy(inFile, bakFile, StandardCopyOption.REPLACE_EXISTING)

		def file = inFile.toFile()
		def text = file.text

		text = text.replaceAll("\n \\+cs", "@&@")

		def lines = text.split("\n")

//		lines = lines.toSorted(new UkDictComparator())
		lines = lines.toSorted {
		    UkDictComparator.getSortKey( it.split()[0] )
		}

		text = lines.join("\n")
		
		text = text.replaceAll("@&@", "\n +cs")
		text += "\n"

		file.text = text
		
		Files.delete(bakFile)
	}

}
