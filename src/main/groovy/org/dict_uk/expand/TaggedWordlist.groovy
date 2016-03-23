package org.dict_uk.expand

import org.dict_uk.expand.Expand.Re;

class TaggedWordlist {

//def except_base_tag = ""


def extra_tag_map = [
  "base-abbr.lst": ":abbr",
  "dot-abbr.lst": ":abbr",
  "twisters.lst": ":bad",
  "ignored.lst": ":bad",
  "rare.lst": ":rare",
  "slang.lst": ":slang",
  "alt.lst": ":alt"
]

private final Re re = new Re()

static {
	Locale.setDefault(new Locale("uk" , "UA"))
	String.metaClass.isCase = { delegate.contains(it) }	// pythonize
	assert "b" in "abc"
}


def process_line_exceptions(line) {

    if( ! (" " in line) || line ==~ ".*[а-яіїєґА-ЯІЇЄҐ] /.*" )
        return [line]
      
    if( line ==~ /[^ ]+ [^ ]+ [^:]?[a-z].*/ )
        return [line]

	return line
//    if( line.startsWith("# !") ) {
//        except_base_tag = re.findall("![a-z:-]+", line)[0][1:] + ":"
//        return []
//    }
    
//    base = re.findall("^[^ ]+", line)[0]
    
//    except_base_tag2 = except_base_tag
//    if( base.endswith("ся") )
//        except_base_tag2 = except_base_tag.replace("verb:", "verb:rev:")
      
//    out_line = re.sub("([^ ]+) ?", "$1 " + base + " " + except_base_tag2 + "unknown\n", line)
//    
//    if( except_base_tag in ("verb:imperf:", "verb:perf:") ) {
//        out_line = re.sub("(verb:(?:rev:)?)((im)?perf:)", "$1inf:$2", out_line, 1)
//      
//        out_lines = out_line.split("\n")
//        out_lines[0] = out_lines[0].replace(":unknown", "")
//        out_line = "\n".join(out_lines)
//    }
//    return out_line[:-1].split("\n")
}

def process_line(line, extra_tags) {
    line = re.sub(/ *#.*$/, "", line) // remove comments
    
//    line = re.sub(/-$/, "", line)
    
	def out_line
    if( !(" " in line) || re.match('.*[а-яіїєґА-ЯІЇЄҐ] /.*', line) )
        out_line = line
    else if( re.match(/^[^ ]+ [^ ]+ [^:]?[a-z].*$/, line) )
        out_line = line
    else if( re.match(/^[^ ]+ [:^<a-z0-9_].*$/, line) )
        out_line = re.sub(/^([^ ]+) ([^<a-z].*)$/, '$1 $1 $2', line)
    else {
        assert false, "hit unknown tag line: >>" + line + "<<"
//        base = re.findall("^[^ ]+", line)[0]
//        out_line = re.sub("([^ ]+) ?", "$1 " + base + " unknown" + extra_tags + "\n", line)
//        return out_line[0..<-1]
    }
			
    //if extra_tags != "" && not re.match(".* [a-z].*$", out_line):
    if( extra_tags != "" && (! re.search(" [:a-z]", out_line) || "g=" in out_line) )
        extra_tags = " " + extra_tags
    else if( line.startsWith(" +") )
        extra_tags = ""
      
    if( "|" in out_line )
        out_line = out_line.replace("|", extra_tags + "|")
    
    //  if not "/" in out_line && not re.match("^[^ ]+ [^ ]+ [^ ]+$", out_line + extra_tags):
    //    print("bad line:", out_line + extra_tags, file=sys.stderr)
    
    //  if len(out_line)> 100:
    //      print(out_line, file=sys.stderr)
    //      sys.exit(1)
    
    out_line = out_line + extra_tags
    if( " \\ " in out_line )
        out_line = out_line.replace(" \\ ", " ") + " \\"
    else if( " \\:" in out_line )
        out_line = out_line.replace(" \\:", ":") + " \\"
      
    return out_line
}


def process_input(files) {
    def out_lines = []
    for( filename in files ) {

		def detectProperNoun = false
		
        def fn = new File(filename).name
		
		def extra_tags
        if( fn in extra_tag_map ) {
            extra_tags = extra_tag_map[fn]
        }
        else {
			if( fn in ["geography.lst", "names.lst"] ) {
				detectProperNoun = true
			}
			
            extra_tags = ""
        }
    
//		System.err.println("---" + filename)
        new File(filename).withReader("utf-8") { reader ->
            for( line in reader ) {
//				System.err.println("line: " + line)

			    line = line.replaceAll(/ +$/, "")
                if( line ==~ / *(#.*)?/ )
                    continue
                
                if( line.startsWith(" +") ) {
                    if( extra_tags )
                        line += " " + extra_tags
                    out_lines.add( line )
                    continue
                }
				
                if( filename.endsWith( "exceptions.lst" ) ) {
                    def lines = process_line_exceptions(line)
                    if( lines )
                        out_lines.addAll( lines )
                }
			    else {
					
					def extra_tags2 = extra_tags;
					
					if( detectProperNoun ) {
						if( Character.isUpperCase(line.charAt(0))
								&& line =~ " /n| /<|\\.<| ^noun| noun:" ) {
							extra_tags2 += ":prop"
						}
					}
					
                    def out_line = process_line(line, extra_tags2)
                    if( out_line.trim() )
                        out_lines.add( out_line )
                }
            }
        }
    }
    return out_lines
}   


//  static void main(argv) {
//    def out_lines = process_input(sys.argv[1:])
//    for out_line in out_lines ) {
//        print(out_lines)
//    }
//  }

}