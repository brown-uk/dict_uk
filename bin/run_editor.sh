#!/bin/sh
#export JAVA_OPTIONS='-Dawt.useSystemAAFontSettings=gasp -Dswing.defaultlaf=com.sun.java.swing.plaf.gtk.GTKLookAndFeel'
export JAVA_OPTIONS='-Dawt.useSystemAAFontSettings=on -Dswing.aatext=true'
export JAVA_OPTS="$JAVA_OPTIONS"
./gradlew runTool -Ptool=editor/Editor.groovy
