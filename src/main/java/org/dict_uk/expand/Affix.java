package org.dict_uk.expand;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.*;

public class Affix {
	static Logger log = LogManager.getFormatterLogger(Affix.class);
	//	private final ArrayList<> prefixes = new ArrayList<>();
	private final Map<String, Map<String, SuffixGroup>> affixMap = new HashMap<>();

	private static final Pattern re_alts_slash = Pattern.compile("([^/]+:)([^:]+)(:[^/]+)?");
	private static final Pattern re_alts_vert = Pattern.compile("^(.* )(.*)$");
	private static final Pattern re_alts_dbl_slash = Pattern.compile("^(.* .+?:)((?:.:(?:nv|v_...)(?:/(?:nv|v_...))*)(?://.:(?:nv|v_...)(?:/(?:nv|v_...))*)+)(:[^/]+)?$");
	private static final Pattern re_whitespace = Pattern.compile("[ \t]+");

	public List<String> expand_alts(List<String> lines, String splitter) {
		ArrayList<String> out = new ArrayList<>();

		for(String line: lines ) {

			if( ! line.contains(splitter) ) {
				out.add( line );
				continue;
			}

			Matcher matcher;
			if( splitter.equals("/") ) {
				matcher = re_alts_slash.matcher(line);
			}
			else if( splitter.equals("|") ) {
				if( line.contains("tag=") ) {
					out.add( line );
					continue;
				}
				matcher = re_alts_vert.matcher(line);
			}
			else {
				matcher = re_alts_dbl_slash.matcher(line);
			}
			
			if( ! matcher.matches() )
				throw new IllegalArgumentException("Not found splitter regex " + splitter + " for " + line + " ==~ " + matcher.toString());
			
			String[] split1 = matcher.group(2).split(splitter.equals("|")? "\\|" : splitter);
			
			String base = matcher.group(1);
			String end = "";

			if( matcher.groupCount() > 2 && matcher.group(3) != null )
				end = matcher.group(3);

			for(String split_: split1) {
				out.add( base + split_ + end );
			}
		}

		return out;
	}


	public void log_usage() {
		for(Entry<String, Map<String, SuffixGroup>> affixItem: affixMap.entrySet()) {
			String affixFlag = affixItem.getKey();
			Map<String, SuffixGroup> affixGroups = affixItem.getValue();
			System.err.println(affixFlag + " : " + affixGroups.size());
			for( Entry<String, SuffixGroup> e2: affixGroups.entrySet()) {
				System.err.println("\t" + e2.getKey() + ": " + e2.getValue().counter + "\t\t(" + e2.getValue().getSize() + ")");
			}
		}
	}


	private static String end(String str, int chars) {
		return str.substring(0, str.length() + chars);
	}

	public Map<String, Map<String, SuffixGroup>> load_affix_file(File aff_file) {
		HashMap<String, Map<String, SuffixGroup>> localAffixMap = new HashMap<>();
		
		SuffixGroup affixGroup = null;
		Map<String, SuffixGroup> affixGroupMap = null;
		String affixFlag = null;

		List<String> readAllLines;
		
		try {
			readAllLines = Files.readAllLines(Paths.get(aff_file.getAbsolutePath()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		for(String line: readAllLines) {

			line = line.trim();

			if( line.isEmpty() || line.startsWith("#"))
				continue;

			if( line.contains("group ") ) {
				affixFlag = line.split(" ")[1];
				affixGroupMap = new HashMap<>();
				localAffixMap.put( affixFlag, affixGroupMap );
				continue;
			}

			if( line.endsWith(":") ){
				String match = end(line, -1);

				if( match.contains(" -") ) {
					//					match1, neg_match
					String[] splits = match.split(" -");
					affixGroup = new SuffixGroup(end(splits[0], -1), splits[1]);
				}
				else {
					affixGroup = new SuffixGroup(match);
				}

				if( affixGroupMap.containsKey(match) ) {
					System.err.println("WARNING: overlapping match " + match + " in " + affixFlag + ":\n\t" + line);
					affixGroup = affixGroupMap.get(match);
				}
				else {
					affixGroupMap.put(match, affixGroup);
				}
				continue;
			}


			String[] halfs = line.split("@");
			String affixes = halfs[0].trim();

			if( affixes.contains("#") ) {
				affixes = affixes.split("#")[0].trim();
			}

			String[] parts = re_whitespace.split(affixes);

			if( parts.length > 2 ) {
				String match = parts[2];
				if ( ! affixGroupMap.containsKey(match) ) {
					affixGroup = new SuffixGroup(match);
					affixGroupMap.put(match, affixGroup);
				}
				else {
					affixGroup = affixGroupMap.get(match);
				}
			}


			if( parts.length > 3) {
				System.err.println("WARNING: extra fields in suffix description " + affixes);
			}

			String tags;
			if( halfs.length > 1 ) {
				tags = halfs[1].trim();
			}
			else {
				tags = "";
				System.err.println("Empty tags for " + line);
			}

			String fromm = parts[0];
			String to = parts[1];

			Suffix affixObj = new Suffix(fromm, to, tags);

			affixGroup.appendAffix(affixObj);
		}
		
		affixMap.putAll(localAffixMap);
		return localAffixMap;
	}


	public Map<String, Map<String, SuffixGroup>> load_affixes(String filename) throws IOException {

		File dir = new File(filename);
		if( ! dir.isDirectory() )
			throw new IllegalArgumentException(filename + " is not a directory: " + dir.getAbsolutePath());
			
		//    List<String> aff_files = onlyfiles = [ f for f in os.listdir(filename) if os.path.isfile(os.path.join(filename, f)) and f.endswith(".aff") ]
		System.err.println("Loading affixes from directory " + filename);

		Map<String, Map<String, SuffixGroup>> affixMap = Arrays.asList(dir.listFiles())
				.stream()
				.filter(f -> f.getName().endsWith(".aff"))
				.map(f -> load_affix_file(f))
				.map(Map::entrySet)
				.flatMap(Collection::stream)
				.collect(Collectors.toMap(
						Map.Entry<String, Map<String, SuffixGroup>>::getKey,   // where each entry is based
						Map.Entry<String, Map<String, SuffixGroup>>::getValue));



		if( affixMap.size() == 0 ) {
			throw new RuntimeException("ERROR: Failed to load affixes from " + filename);
		}

		log.debug("Loaded: " + affixMap.keySet());
		
		return affixMap;
	}

	
	public static void main(String[] args) throws IOException {
		Affix affix = new Affix();
//		affix.load_affixes(args[0]);
		System.err.println(affix.expand_alts(Arrays.asList("а conj:coord|part|excl"), "|"));
	}
	
}
