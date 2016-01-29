def list = new File('dict_rules_lt.txt').text.split(/\n/).collect {
    def s = it.split()[0]; 
    if( s.size() >= 4 && s == s.reverse() )
        return s;
}.unique()

println list.join("\n")
