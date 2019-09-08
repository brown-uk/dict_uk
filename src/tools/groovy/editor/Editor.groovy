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


@Field
def inputData = new File(args.length >= 1 ? args[0] : 'out/toadd/unknown_lemmas.txt').readLines()
//def inputData = new File('out/toadd/unknown.txt').readLines().collect{ it.replace('\t', '    ') }
@Field
def media = []
@Field
def newWords = []
@Field
def expand = new Expand(false)

@Field
def dictLines = new File('data/dict')
	.listFiles()
	.collect { File file ->
		if( file.name.endsWith('.lst') ) {
//		println "adding file ${file.name}"
			file.readLines()
		}
		else {
			[]
		}
	}.flatten()

def newLemmaFile = new File('out/toadd/media_src.txt')
if( newLemmaFile.exists() ) {
	media = newLemmaFile.readLines().collectEntries {
		def parts = it.split('@@@')
		[ (parts[0]): parts.length > 1 ? parts[1..-1] : ["---"] ]
	}
}

println "Input data: ${inputData.size}, dict lines: ${dictLines.size}"

expand.affix.load_affixes('data/affix')



def swing = new SwingBuilder()

def sharedPanel = {
	swing.panel() { label("Shared Panel") }
}

//UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

def inflect() {
	inflectedList.getModel().clear()


	def txt = text.text	
	if( txt.contains(' /') || txt.contains(' noun:') ) {
		
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
	println "inflected"
	mainList.revalidate()
	mainList.repaint() 
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
//	println '--' + minSelIdx + ' - ' + e.getValueIsAdjusting()
	if( e.getValueIsAdjusting() || minSelIdx < 0 )
		return

	def itemParts = inputData[minSelIdx].split(' ')
	def item = itemParts[0]
	def notes = itemParts.size() > 1 ? itemParts[1] : ''
	
	def word
	def word_txt
	
	if( notes =~ /^(\/[a-z]|noun:.:nv|noninfl|adv)/ ) {
		notesLabel.text = ''
		word = item
		word_txt = inputData[minSelIdx]
	}
	else {
		notesLabel.text = notes

		word = item.startsWith(' ') ? item.trim().split(/    /, 2)[1].trim() : item.split(/ /, 2)[0]
		word_txt = item.contains('невідм.') ? word + " noun:m:nv" : getDefaultTxt(word)
	}

	println "word: $word"

	StringSelection stringSelection = new StringSelection(word);
	Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	clipboard.setContents(stringSelection, null);

	
	text.setText(word_txt)

	SwingUtilities.invokeLater( {
		findInDict(word)
		inflect()
	})

//	if( inputData.contains('lemma') ) {
		mediaList.setModel(new ListWrapperListModel<String>(['... шукаємо ...']))
		SwingUtilities.invokeLater( {
			findMedia(word)
		})
//	}
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
	
	def ending = word.replaceFirst(/^(авіа|авто|агро|аеро|анти|аудіо|багато|взаємо|відео|гео|гепато|геронто|геліо|гідро|гіпер|держ|еко|екстра|електро|євро|за|кібер|кіно|мало|мега|мета|мікро|моно|мото|над|напів|нейро|не|пере|під|по|проти|про|псевдо|радіо|само|спец|спів|стерео|спорт|старо|супер|термо|теле|транс|фото)/, '')
    ending = ending.replaceFirst(/^ав/, 'а[ву]')
    ending = ending.replaceFirst(/(ння|ти)$/, '(ння|ти)')
	if( ending.endsWith('ований') ) {
		ending = ending.replaceFirst(/ований/, '(ованість|ований|овано|увати)')
	}
	else {
		ending = ending.replaceFirst(/(ість|ий|о)$/, '(ість|ий|о)')
	}
    ending = ending.replaceFirst(/(и|і)$/, '(и|і|а)?')
    ending = ending.replaceFirst(/иця$/, '(иця|ик)')
    ending = ending.replaceFirst(/вачка$/, '(вач|вачка)')
	ending = ending.replaceFirst(/[гґ]/, '[гґ]')

	println "searching for existing: $ending in ${dictLines.size}"
	def ptrn = ~"(?ui)^[^#]*$ending "
	def similars = dictLines.findAll{ ptrn.matcher(it) }
//	def similars = dictLines.findAll{ it =~ "(?i)^[а-яіїєґА-ЯІЇЄҐ'-]*$ending " }
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
		if( ! (txt =~ /^[а-яіїєґА-ЯІЇЄҐ'-]+ \/?[a-z]/) ) {
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
	
		
//		inputData.removeAt(selIdx)
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

def defaultFlags() {
	def txt = text.text.replaceFirst(/ .*/, '')
	def word_txt = getDefaultTxt(txt)
	text.setText(word_txt)
	findInDict(txt)
	inflect()
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
							listData: inputData,
							valueChanged: selChange1
							)
					mainList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
					mainList.setVisibleRowCount(50);
//					mainList.setPreferredSize(new Dimension(480, 300))
					mainList.setCellRenderer(new DefaultListCellRenderer() {
						public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
							super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
							String txt = value.replaceFirst('[ 0-9]+', '')
//							println inflectedList.getModel().getClass()
							String[] inflected = inflectedList.getModel().toArray().toList()
							boolean highlight = (inflected.find { it.startsWith(txt+' ') }) != null
//							println "H: $highlight - $txt"
							if( highlight ) {
								setBackground(Color.YELLOW)
								setOpaque(true); // otherwise, it's transparent
							}
							return this;  // DefaultListCellRenderer derived from JLabel, DefaultListCellRenderer.getListCellRendererComponent returns this as well.
						}
					})
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
										inflect()
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
								        if( text.text.contains('adjp') ) {
										    text.text = text.text.replaceFirst(/:perf/, ':imperf')
										}
										else {
										    text.text = text.text.replaceFirst(/ :(im)?perf/, '.cf.advp :imperf')
										}
										inflect()
									}
								)
						button(
								text: 'Perf',
								actionPerformed: {
										if( text.text =~ / \/v1/ ) {
											text.text = text.text.replaceFirst(/( \/v1) :(im)?perf/, '$1.is0 :perf')
										}
										else if( text.text =~ / \/vr[12]/ ) {
											text.text = text.text.replaceFirst(/( \/vr[12]) :imperf/, '$1 :perf')
										}
										else if( text.text =~ / \/v2/ ) {
											text.text = text.text.replaceFirst(/( \/v2) :imperf/, '$1.isNo :perf')
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
								text: 'it1',
								actionPerformed: {
										text.text = text.text.replaceFirst(/ :perf/, '.it1 :perf')
										inflect()
									}
								)
						button(
								text: 'it0',
								actionPerformed: {
										text.text = text.text.replaceFirst(/ :perf/, '.it0 :perf')
										inflect()
									}
								)
						button(
								text: 'NoP',
								actionPerformed: {
									text.text = text.text.replaceFirst(/\.p[123]?/, '')
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
						def btnFind = button(
								text: 'Find',
								actionPerformed: {
										String txt = text.getSelectedText()
										if( txt == null ) txt = text.text.replaceFirst(/ .*/, '')
										findInDict(txt)
									}
								)
								KeyStroke keystrokeF2 = KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK)
								btnFind.registerKeyboardAction(new ActionListener() {
									public void actionPerformed(ActionEvent e) {
										String txt = text.getSelectedText()
										if( txt == null ) txt = text.text.replaceFirst(/ .*/, '')
										findInDict(txt)
									}
								}, keystrokeF2, JComponent.WHEN_IN_FOCUSED_WINDOW);

						def btnFlagSuggest = button(
							text: 'Flags',
							actionPerformed: {
									defaultFlags()
								}
							)
							KeyStroke keystrokeP = KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_MASK)
							btnFlagSuggest.registerKeyboardAction(new ActionListener() {
								public void actionPerformed(ActionEvent e) {
									defaultFlags()
								}
							}, keystrokeP, JComponent.WHEN_IN_FOCUSED_WINDOW);


						label('     ')
						def btnToAdj = button(
							text: 'ToAdj',
							actionPerformed: {
									text.text = text.text.replaceFirst(/(а|у|ої|ого|ому|им|ім|іми|іх) \//, 'ий /')
									defaultFlags()
								}
							)
							KeyStroke keystrokeY = KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_MASK)
							btnToAdj.registerKeyboardAction(new ActionListener() {
								public void actionPerformed(ActionEvent e) {
									text.text = text.text.replaceFirst(/(а|у|ої|ого|ому|им|ім|іми|іх) \//, 'ий /')
									defaultFlags()
								}
							}, keystrokeY, JComponent.WHEN_IN_FOCUSED_WINDOW);


						label('     ')
						button(
								text: 'Save',
								actionPerformed: {
									new File('new_words.lst') << newWords.join('\n') + '\n'
									newWords.clear()
									textlabel.text = "Just saved"
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

					notesLabel = label(horizontalAlignment: SwingConstants.RIGHT)
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
