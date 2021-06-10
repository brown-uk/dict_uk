package org.dict_uk.common;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

public class UkDictComparatorTest {

	@Test
	public void test() {
		Scanner scanner = new Scanner(getClass().getResourceAsStream("/uk_UA.in"));
		scanner.useDelimiter("\\Z");

		List<String> lineList = new ArrayList<>();
		while (scanner.hasNextLine()) {
			lineList.add(scanner.nextLine());
		}
		scanner.close();

		assertTrue(lineList.size() > 10);

		ArrayList<String> outputList = new ArrayList<>(lineList);

		Collections.sort(outputList, new UkDictComparator());

		if (!lineList.equals(outputList)) {
			for (int i = 0; i < lineList.size(); i++) {
				System.out.println(lineList.get(i) + "\t\t" + outputList.get(i) + "\t\t"
						+ UkDictComparator.getSortKey(lineList.get(i)) + "\t\t"
						+ UkDictComparator.getSortKey(outputList.get(i)));
			}
		}

		assertEquals(lineList, outputList);
	}

	private static final List<String> lines = Arrays.asList(
			"А-Ба-Ба-Га-Ла-Ма-Г", 
			"а-ба-ба-га-ла-ма-г",
			"А-Ба-Ба-Га-Ла-Ма-Га", 
			"а-ба-ба-га-ла-ма-га", 
			"азотистоводневий", // азотистоводневий adj:m:v_naz",
			"азотисто-водневий", // азотисто-водневий adj:m:v_naz",
			"Єремія",
			"зав", 
			"зав."
	);

	@Test
	public void test2() {
		ArrayList<String> outputList = new ArrayList<>(lines);

		Collections.sort(outputList, new UkDictComparator());

		if (!lines.equals(outputList)) {
			for (int i = 0; i < lines.size(); i++) {
				System.out.println(
						lines.get(i) + "\t\t" + outputList.get(i) + "\t\t" + UkDictComparator.getSortKey(lines.get(i))
								+ "\t\t" + UkDictComparator.getSortKey(outputList.get(i)));
			}
		}

		assertEquals(lines, outputList);

	}
}
