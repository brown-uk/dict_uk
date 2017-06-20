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
dict += new File('data/dict/base-compound.lst').readLines()
dict += new File('data/dict/twisters.lst').readLines()
dict += new File('data/dict/names.lst').readLines()
dict += new File('data/dict/geo-other.lst').readLines()
dict += new File('data/dict/slang.lst').readLines()
@Field
def media = new File('out/toadd/new_lemmas_find.txt').readLines().collectEntries {
  def parts = it.split('@@@')
  [ (parts[0]): parts.length > 1 ? parts[1..-1] : ["---"] ]
}
@Field
def newWords = []
def newWordsFile = new File('new_words.lst')
//if( newWordsFile.exists() ) {
//	newWords = newWordsFile.readLines()
//}
//
//def newWordsLemmas = newWords.findAll { it }.collect { it.split()[0] }
//data.removeAll {
//	it.split(/ /, 2)[0] in newWordsLemmas
//}

println "Data: ${data.size}, dict: ${dict.size}"

@Field
def expand = new Expand()
expand.affix.load_affixes('data/affix')



UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

def inflect() {
	inflectedList.getModel().clear()


	def txt = text.text	
	if( txt.contains(' /') ) {
		
		try {
			def forms = expand.expand_line(txt)
			forms = new DictSorter().sortEntries(forms)
//			println forms
			
			def inflectedForms = forms.collect{it.word.padRight(30) + it.tagStr}

			if( txt =~ / \/vr?[1-5]/ ) {
				if( inflectedForms.count({ it =~ /:impr/}) == 0 ) {
					inflectedForms.add(0, '-- No imperative --')
				}
			}
			
			inflectedList.getModel().addAll(inflectedForms)
			

		} catch ( e ) {
			inflectedList.getModel().add(e.getMessage())
			e.printStackTrace()
		}
		
	}
}

def findMedia(word) {
	def lst = word in media ? media[word] : []
	mediaList.setModel(new ListWrapperListModel<String>(lst))
}

def addA() {
	text.text = text.text.replaceFirst(/( \/n2[0-9])/, '$1.a')
}

def imperfPerf() {
	if( text.text.contains(":imperf") && ! text.text.contains(":perf") ) {
		text.text = text.text.replace(':imperf', ':imperf:perf')
	}
	else 
	if( text.text.contains(":perf") && ! text.text.contains(":imperf") ) {
		text.text = text.text.replace(':perf', ':imperf:perf')
	}
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

	SwingUtilities.invokeLater( {
		findInDict(word)
		inflect()
	})

	mediaList.setModel(new ListWrapperListModel<String>(['... шукаємо ...']))
	SwingUtilities.invokeLater( {
		findMedia(word)
	})
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
		case ~/.*(олог)$/:
			word_txt += ' /n20.a.p.<'
			break;
		case ~/.*(знавство)$/:
			word_txt += ' /n2n'
			break;
		case ~/.*(метр)$/:
			word_txt += ' /n20.a.p.ke'
			break;
		case ~/.*([^аеєиіїоуюя])$/:
			word_txt += ' /n20.p'
			if( word.endsWith('р') )
				word_txt += '.ke'
			break;

		case ~/.*(ння|ття|сся|ззя|тво|ще)$/:
			word_txt += ' /n2n.p1'
			break;

		case ~/.*(ччя)$/:
			word_txt += ' /n2n'
			break;

		case ~/.*[ую]вати$/:
			word_txt += ' /v1 :imperf'
			break;
		case ~/.*[ую]ватися$/:
			word_txt += ' /vr1 :imperf'
			break;
		case ~/.*ти$/:
			word_txt += ' /v1 :imperf'
			break;
		case ~/.*тися$/:
			word_txt += ' /vr1 :imperf'
			break;
			
			
		case ~/.*([аеєиіїоуюя]ка|[^к]а|ія|я)$/:
			word_txt += ' /n10.p1'
			break;
		case ~/.*([^аеєиіїоуюя]ка)$/:
			word_txt += ' /n10.p2'
			break;
			
		case ~/.*и$/:
			word_txt += ' /np2'
			break;

		case ~/.*о$/:
			word_txt += ' adv'
			break;
	}

	word_txt
}

def findInDict(word) {
	word = word.replaceFirst(/.*-/, '')
	
	def ending = word.replaceFirst(/^(авіа|авто|агро|аеро|анти|аудіо|багато|відео|гео|гепато|геронто|геліо|гідро|гіпер|електро|за|кіно|мега|мета|мікро|мото|нейро|не|пере|під|по|радіо|стерео|спорт|теле|фото|супер|термо)/, '')
    ending = ending.replaceFirst(/(ння|ти)$/, '(ння|ти)')
	if( ending.endsWith('ований') ) {
		ending = ending.replaceFirst(/ований/, '(ованість|ований|овано|увати)')
	}
	else {
		ending = ending.replaceFirst(/(ість|ий|о)$/, '(ість|ий|о)')
	}
    ending = ending.replaceFirst(/(и|і)$/, '(и|і|а)?')
    ending = ending.replaceFirst(/иця$/, '(иця|ик)')
	ending = ending.replaceFirst(/[гґ]/, '[гґ]')
	
	println "searchin for: $ending"
	def ptrn = ~"(?ui).*$ending "
	def similars = dict.findAll{ ptrn.matcher(it) }
//	def similars = dict.findAll{ it =~ "(?i)^[а-яіїєґА-ЯІЇЄҐ'-]*$ending " }
	if( similars.size() > 100 ) {
		similars = similars[0..100]
	}
	def model = new DefaultListModel<String>()
	similars.each{ model.addElement(it) }
	vesumList.setModel( model )
}


def addWord() {
	def selIdx = mainList.selectionModel.minSelectionIndex
	if( selIdx >= 0 ) {
		
		def txt = text.text
		if( ! (txt =~ /^[а-яіїєґ'-]+ \/?[a-z]/) ) {
			inflectedList.getModel().clear()
			inflectedList.getModel().add('Invalid format')
			return
		}
			
		if( txt.contains(' /') ) {
			try {
				def forms = expand.expand_line(txt)
			} catch ( e ) {
				inflectedList.getModel().clear()
				inflectedList.getModel().add(e.getMessage())
				return
			}
		}
	
		
//		data.removeAt(selIdx)
//		mainList.invalidate()
		
		addedList.getModel().add(text.text.trim())
		
		int sz = addedList.getModel().getSize()
		if( sz > 0 ) {
			addedList.ensureIndexIsVisible(sz-1)
		}

		textlabel.text = "Added ${newWords.size} words."
		
		mainList.setSelectionInterval(selIdx + 1, selIdx + 1)
//		mainList.getSelectionModel().fireValueChanged(selIdx, selIdx)
	}
}


println "starting..."

count = 0
swing.edt {
	def frm = frame(title: 'Frame', defaultCloseOperation: JFrame.EXIT_ON_CLOSE, pack: true, show: true) {
//		vbox {
		splitPane(id:'hsplit', orientation: JSplitPane.VERTICAL_SPLIT) {
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

					label(' ----- ')

					text = textField(
					        //rows: 5
							minimumSize: new Dimension(220, 70)
							)

					textlabel = label("${newWords.size} new words")

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
						
						label('     ')
							
						button(
								text: 'Pers',
								actionPerformed: {
										text.text = text.text.replaceFirst(/( \/n2[0-9]).*/, '$1.a.p.<')
										if( text.text.contains('р /n2') ) {
												text.text = text.text.replace('.<', '.ke.<')
										}
										text.text = text.text.replaceFirst(/ \/n10.*/, '$0.<')
									}
								)
						button(
								text: 'Adjp',
								actionPerformed: {
										text.text = text.text.replaceFirst(/ [^ ]*/, ' /adj :&adjp:pasv:perf')
										inflect()
									}
								)
						button(
								text: 'Impf',
								actionPerformed: {
										text.text = text.text.replaceFirst(/ :(im)?perf/, '.cf.advp :imperf')
										inflect()
									}
								)
						button(
								text: 'Perf',
								actionPerformed: {
										if( text.text =~ / \/v[1-4]/ ) {
											text.text = text.text.replaceFirst(/ \/v[12] :(im)?perf/, ' /v1.is0 :perf')
										}
										else {
											text.text = text.text.replaceFirst(/ \/v[12] :imperf/, ' /v1 :perf')
										}
										inflect()
									}
								)
					}
					
					hbox {
						def btnA = button(
								text: '.A',
								actionPerformed: {
										addA()
										inflect()
									}
								)
								
								KeyStroke keystroke = KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK)
								btnA.registerKeyboardAction(new ActionListener() {
									public void actionPerformed(ActionEvent e) {
										addA()
										inflect()
									}
								}, keystroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
							
						button(
								text: 'Impf/Pf',
								actionPerformed: {
										imperfPerf()
										inflect()
									}
								)
						button(
								text: 'NoP',
								actionPerformed: {
									text.text = text.text.replace('.p', '')
									inflect()
									}
								)
					}
					
					hbox {

//						label('     ')
						def btnInflect = button(
								text: 'Inflect',
								actionPerformed: {
										inflect()
									}
								)
								
								KeyStroke keystroke = KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_MASK)
								btnInflect.registerKeyboardAction(new ActionListener() {
									public void actionPerformed(ActionEvent e) {
										inflect()
									}
								}, keystroke, JComponent.WHEN_IN_FOCUSED_WINDOW);

								
//						label('     ')
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
									new File('new_words.lst') << newWords.join('\n') + '\n'
									newWords.clear()
								}
								)
					}

					scrollPane(verticalScrollBarPolicy:JScrollPane.VERTICAL_SCROLLBAR_ALWAYS ) {

						inflectedList = list(
								model: new ListWrapperListModel<String>([]),
								visibleRowCount: 30,
//								preferredSize: new Dimension(200, 200)
								)
						inflectedList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
					}

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

					addedList = list(
							model: new ListWrapperListModel<String>(newWords),
							visibleRowCount: 30,
//							preferredSize: new Dimension(200, 200)
							)
							addedList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
				}

			}
			
			hbox {
//				minimumSize: new Dimension(100, 100)
				
				scrollPane(verticalScrollBarPolicy:JScrollPane.VERTICAL_SCROLLBAR_ALWAYS ) {
					minimumSize: new Dimension(100, 100)

					mediaList = list(
							minimumSize: new Dimension(100, 100),
							model: new ListWrapperListModel<String>([]),
							visibleRowCount: 10,
							)
							mediaList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
				}
			}
		}
		
	}
}

swing.hsplit.setDividerLocation(0.6)
