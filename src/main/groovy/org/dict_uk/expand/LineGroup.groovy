package org.dict_uk.expand

import groovy.transform.CompileStatic

@CompileStatic
class LineGroup {
	String line
	String comment
	List<String> extraLines

	public LineGroup() {
	}

	public LineGroup(String line) {
		this.line = line
	}

	public LineGroup(LineGroup lineGroup) {
		this.line = lineGroup.line
		this.comment = lineGroup.comment
		this.extraLines = lineGroup.extraLines
	}

	public LineGroup(LineGroup lineGroup, String newLine) {
		this.line = newLine
		this.comment = lineGroup.comment
		this.extraLines = lineGroup.extraLines
	}

	public LineGroup(LineGroup lineGroup, String newLine, String comment) {
		this.line = newLine
		this.comment = comment
		this.extraLines = lineGroup.extraLines
	}

	String toString() {
		line + " / " + extraLines + (comment ? " # " + comment : "")
	}
}
