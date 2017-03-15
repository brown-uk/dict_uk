#!/bin/env groovy

import groovy.swing.SwingBuilder
import javax.swing.*
import java.awt.*
import java.awt.BorderLayout
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent

import org.dict_uk.expand.DictSorter
import org.dict_uk.expand.Expand

import groovy.swing.impl.ListWrapperListModel
import groovy.transform.Field
import java.awt.event.ActionListener
import java.awt.event.InputEvent


def swing = new SwingBuilder()

def sharedPanel = {
	swing.panel() { label("Shared Panel") }
}

println "reading..."

@Field
def data = new File('out/toadd/new_lemmas_main.txt').readLines()
@Field
def dict = new File('data/dict/base.lst').readLines()
@Field
def media = new File('out/toadd/new_lemmas_find.txt').readLines().collectEntries {
  def parts = it.split('@@@')
  [ (parts[0]): parts[1..-1] ]
}
@Field
def newWords = new File('new_words.lst').readLines()

def newWordsLemmas = newWords.findAll { it }.collect { it.split()[0] }
data.removeAll {
	it.split(/ /, 2)[0] in newWordsLemmas
}

println "Data: ${data.size}, dict: ${dict.size}"

@Field
def expand = new Expand()
expand.affix.load_affixes('data/affix')



UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

def inflect() {
	inflectedList.getModel().clear()

	if( text.text.contains(' /') ) {
		
		try {
			
			def forms = expand.expand_line(text.text)
			forms = new DictSorter().sortEntries(forms)
//			println forms
		
			inflectedList.getModel().addAll(forms.collect{it.word.padRight(30) + it.tagStr})
		} catch ( e ) {
			inflectedList.getModel().add(e.getMessage())
		}
	}
}

def findMedia(word) {
	def lst = word in media ? media[word] : []
	mediaList.setModel(new ListWrapperListModel<String>(lst))
}


Closure selChange1 = { e ->
	def minSelIdx = e.source.selectionModel.minSelectionIndex
	println '--' + minSelIdx + ' - ' + e.getValueIsAdjusting()
	if( e.getValueIsAdjusting() || minSelIdx < 0 )
		return

	def item = data[minSelIdx]

	def word = item.split(/ /, 2)[0]

	StringSelection stringSelection = new StringSelection(word);
	Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	clipboard.setContents(stringSelection, null);

	def word_txt = item.contains('невідм.') ? word + " noun:m:nv" : getDefaultTxt(word)
	
	text.setText(word_txt)

	findInDict(word)
		
	inflect()
	
	findMedia(word)
}


def getDefaultTxt(word) {
	def word_txt = word
	switch( word ) {
		case ~/.*[иі]й$/:
			word_txt += ' /adj'
			break;
			
		case ~/.*(ість)$/:
			word_txt += ' /n30'
			break;
		case ~/.*([еє]ць)$/:
			word_txt += ' /n22.a.p'
			break;
		case ~/.*(метр)$/:
			word_txt += ' /n20.a.p.ke'
			break;
		case ~/.*([^аеєиіїоуюя])$/:
			word_txt += ' /n20.p'
			if( word.endsWith('р') )
				word_txt += '.ke'
			break;

		case ~/.*([аеєиіїоуюя]ка|[^к]а|ія|ця)$/:
			word_txt += ' /n10.p1'
			break;
		case ~/.*([^аеєиіїоуюя]ка)$/:
			word_txt += ' /n10.p2'
			break;

		case ~/.*(ння|ття|сся|ззя|тво|ще)$/:
			word_txt += ' /n2n.p1'
			break;

		case ~/.*[ую]вати$/:
			word_txt += ' /v1 :imperf'
			break;
		case ~/.*[ую]ватися$/:
			word_txt += ' /vr1 :imperf'
			break;
		case ~/.*[аіия]ти$/:
			word_txt += ' /v2 :imperf'
			break;
		case ~/.*[аіия]тися$/:
			word_txt += ' /vr2 :imperf'
			break;

		case ~/.*и$/:
			word_txt += ' /np2'
			break;
	}

	word_txt
}

def findInDict(word) {
	word = word.replaceFirst(/.*-/, '')
	
	def ending = word.replaceFirst(/^(авіа|авто|агро|аеро|анти|аудіо|багато|відео|гео|гідро|гіпер|електро|кіно|мега|мета|мікро|мото|нейро|не|пере|під|по|радіо|стерео|спорт|теле|фото|супер|термо)/, '')

	def similars = dict.findAll{ it =~ "^[а-яіїєґА-ЯІЇЄҐ'-]*$ending " }
	def model = new DefaultListModel<String>()
	similars.each{ model.addElement(it) }
	vesumList.setModel( model )
}


def addWord() {
	def selIdx = mainList.selectionModel.minSelectionIndex
	if( selIdx >= 0 ) {
		data.removeAt(selIdx)
		mainList.invalidate()
		
		addedList.getModel().add(text.text.trim())
		
		int sz = addedList.getModel().getSize()
		if( sz > 0 ) {
			addedList.ensureIndexIsVisible(sz-1)
		}

		textlabel.text = "Added ${newWords.size} words."
		
		mainList.getSelectionModel().fireValueChanged(selIdx, selIdx)
	}
}

println "starting..."

count = 0
swing.edt {
	def frm = frame(title: 'Frame', defaultCloseOperation: JFrame.EXIT_ON_CLOSE, pack: true, show: true) {
		vbox {
			hbox {

				def sp = scrollPane( verticalScrollBarPolicy:JScrollPane.VERTICAL_SCROLLBAR_ALWAYS ) {
					mainList = list(
							listData: data,
							valueChanged: selChange1
							)
					mainList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
					mainList.setVisibleRowCount(50);
//					mainList.setPreferredSize(new Dimension(480, 300))
				}

				label('  -----  ')

				vbox {

					label('----------')

					text = textField(
							preferredSize: new Dimension(220, 70)
							)

					textlabel = label('Click the button!')

					hbox {
						def btn1 = button(
								text: 'Add',
								actionPerformed: {
									addWord()
								}
								)
								
								KeyStroke keystroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_MASK)
								btn1.registerKeyboardAction(new ActionListener() {
									public void actionPerformed(ActionEvent e) {
										addWord();
									}
								}, keystroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
						
								
						button(
								text: 'Inflect',
								actionPerformed: {
										inflect()

									}
								)
						label('     ')
						button(
								text: 'Geo',
								actionPerformed: {
										text.text = text.text.padRight(30) + '# geo-other'
									}
								)
						label('     ')
						button(
								text: 'Find',
								actionPerformed: {
										findInDict(text.text.replaceFirst(/ .*/, ''))

									}
								)
						label('     ')
						button(
								text: 'Save',
								actionPerformed: {
									new File('new_words.lst').text=newWords.join('\n') + '\n'
								}
								)
					}

					vbox {
						scrollPane(verticalScrollBarPolicy:JScrollPane.VERTICAL_SCROLLBAR_ALWAYS ) {
							def data2 = []

							vesumList = list(
									listData: data2,
									constraints: BorderLayout.EAST
									)
							vesumList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
							vesumList.setVisibleRowCount(50);
//							vesumList.setPreferredSize(new Dimension(200, 300))
						}
					}
					scrollPane(verticalScrollBarPolicy:JScrollPane.VERTICAL_SCROLLBAR_ALWAYS ) {

						inflectedList = list(
								model: new ListWrapperListModel<String>([]),
								visibleRowCount: 30,
//								preferredSize: new Dimension(200, 200)
								)
						inflectedList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
					}

				}

				scrollPane(verticalScrollBarPolicy:JScrollPane.VERTICAL_SCROLLBAR_ALWAYS ) {

					addedList = list(
							model: new ListWrapperListModel<String>(newWords),
							visibleRowCount: 30,
//							preferredSize: new Dimension(200, 200)
							)
							addedList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
				}

			}
			
				scrollPane(verticalScrollBarPolicy:JScrollPane.VERTICAL_SCROLLBAR_ALWAYS ) {

					mediaList = list(
							model: new ListWrapperListModel<String>([]),
							visibleRowCount: 10,
							)
							mediaList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
				}

		}
		
	}
}

