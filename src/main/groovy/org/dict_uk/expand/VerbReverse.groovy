#!/usr/bin/env groovy

package org.dict_uk.expand

import java.util.regex.*
import groovy.transform.CompileStatic


//
// Generates affix file for reverse verb forms 
//

//@CompileStatic
class VerbReverse {
    final Pattern RE_GENERIC = Pattern.compile(/.*([аяийіїоуюв]|те)$/)
    final Pattern RE_ADVP = Pattern.compile(/.*[чш]и$/)

	class Re {
    	def match(regex, str) {
			return Pattern.matches(regex, str)
		}
		def sub(regex, repl, str) {
			return str.replaceAll(regex, repl)
		}
	}
	
	final Re re = new Re()
	
	def len(obj) {
		return obj.size()
	}
	
        
    def generate_rev(String line) {
        line = line.trim()

        def line_parts = line.split("#")
		
		if( line_parts.size() == 0 ) {
			line_parts = [""]
		}
		
        String left = line_parts[0]
        
		String comment
        if( len(line_parts) > 1 )
            comment = line_parts[1]
        else
            comment = ""

        if( left.contains("group v") ) {
            line = line.replace("group v", "group vr")
            if( left.startsWith("group v") ) {
                line += "\n\n# Зворотня форма дієслів (-ся та -сь)\n"
                line += "ся\tсь	тися		#  ~тися  ~тись    @ verb:rev:inf\n\n"
            }
            return line
        }

        if( left.trim().endsWith(":") )
            return line.replace("ти:", "тися:")
          
        def columns = left.split()
          
        if( len(columns) < 2 )
            return line
          
        def dual = false
		def suff
        if( comment.contains("advp") ) 
            suff = "сь"
        else if( re.match(/.*те$/, columns[1]) && comment.contains(":s:3") )
            suff = "ться"
        else if( RE_GENERIC.matcher(columns[1]).matches() ) {
            dual = true
            suff = "ся"
        }
        else if( re.match(/.*[еє]$/, columns[1]) )
            suff = "ться"
        else
            suff = "ся"
          
        columns = convert_left(columns, suff)
        
        comment = convert_comment(comment, suff)
        

        def out_line = columns.join("\t") + "\t\t#\t\t" + comment
        
        if( dual ) {
            columns[1] = re.sub(/ся$/, "сь", columns[1])

            out_line += "\n"
            comment = comment.replace("ся\t", "сь\t")
            out_line += columns.join("\t") + "\t\t#\t\t" + comment
        }
        return out_line
    }
        
    def convert_comment(comment, suff) {
        comment = comment.replace("verb:", "verb:rev:")
        comment = comment.replace("advp:", "advp:rev:")
        
        def columns = comment.split("@")
        def left_cols = columns[0].split()
        
        left_cols[0] += suff
        left_cols[1] += suff
        
        comment = left_cols.join("\t") + "\t\t@" + columns[1]
        
        return comment
	}   
        
    def convert_left(columns, suff) {
        if( columns[0] == "0" )
            columns[0] = "ся"
        else
            columns[0] += "ся"

        if( columns[1] == "0" )
            columns[1] = "ся"
        else
            columns[1] += suff
        
        if( len(columns) > 2 )
            columns[2] += "ся"
        
        return columns
    }
    
    
  static void main(argv) {

//if __name__ == "__main__":

    def cnv = new VerbReverse()

    def convert = true
    
    def input = argv.size() > 0 ? new File(argv[0]).newInputStream() : System.in
    //def output = argv.size() > 1 ? new File(argv[1]).newOutputStream() : System.out
    
    new File(argv[1]).withWriter("UTF-8") { output ->
    
    input.readLines("UTF-8").each { String line ->
        
        try {
        if( convert ) {
            line = cnv.generate_rev(line)
            output.println(line)
        }
        else
            if( line.contains("ся ") )
                output.println(line)

        // v5 is special - don't generate reverse affixes for it
        if( line.contains("group ") )
            convert = ! line.contains(" v5") && ! line.contains("shrt")
        }
        catch(ex) {
            throw new Exception("Failed at " + it, ex)
        }
    }
    }
  }
}