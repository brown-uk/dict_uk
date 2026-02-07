#!/bin/env groovy

package editor

import java.awt.*
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.InputEvent
import java.awt.event.KeyEvent

import javax.swing.*

import groovy.swing.SwingBuilder
import groovy.swing.impl.ListWrapperListModel
import groovy.transform.Field

import org.dict_uk.expand.DictSorter
import org.dict_uk.expand.LineGroup


@Field
def EditorData data = new EditorData(args)

def swing = new SwingBuilder()

def sharedPanel = {
	swing.panel() { label("Shared Panel") }
}

//UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

def inflect() {
	inflectedList.getModel().clear()

	String txt = text.text	
	if( txt.contains(' /') || txt.contains(' noun:') ) {
		
		try {
            def parts = txt.split(/#/)
            def comment = null
            txt = parts[0].trim()
            if( parts.size() > 1 ) comment = parts[1].trim()
            println "Expanding $txt"
			def forms = data.expand.expand_line(txt)
            
			forms = new DictSorter().sortEntries(forms)
//			println forms
			
			def inflectedForms = forms.collect{it.word.padRight(30) + it.tagStr}

			if( txt =~ / \/vr?[1-6]/ ) {
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

def copyToClipboard(word) {
    StringSelection stringSelection = new StringSelection(word);
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(stringSelection, null);
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

	def entry = data.inputData[minSelIdx] //.split(' +')
	
	def word = entry.word
	def word_txt = entry.flags ? "${entry.word} ${entry.flags}" : entry.word

    notesLabel.text = word
	
    if( entry.comment ) {
        word_txt += "    # ${entry.comment}"
    }
    if( ! entry.flags ) {
        word_txt = EditorData.getDefaultTxt(word)
    }

	println "word: $word"

    copyToClipboard(word)

	text.setText(word_txt)

	SwingUtilities.invokeLater( {
		findInDict(word)
		inflect()
	})

    mediaList.setModel(new ListWrapperListModel<String>(entry.context))
    
    if( false ) {
		mediaList.setModel(new ListWrapperListModel<String>(['... шукаємо ...']))
		SwingUtilities.invokeLater( {
			findMedia(word)
		})
    }
}



//@CompileStatic
def findInDict(String word) {
	word = word.replaceFirst(/.*-/, '')
    word = word.replace(/ .*/, '')
	
	def ending = word.replaceFirst(/^(авіа|авто|агро|аеро|анти|аудіо|багато|без|взаємо|ви|від|високо|відео|гео|гепато|геронто|геліо|гідро|гіпер|держ|еко|екстра|електро|етно|євро|за|кібер|кіно|мало|мега|мета|мікро|моно|мото|над|напів|нейро|не|пере|під|по|проти|про|псевдо|радіо|само|спец|спів|стерео|спорт|старо|супер|термо|теле|транс|фото)/, '')
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
    ending = ending.replaceFirst(/ка$/, '(ка)?')
	ending = ending.replaceFirst(/[гґ]/, '[гґ]')

	println "searching for existing: $ending in ${data.dictLines.size()}"
	def ptrn = ~"(?ui)^[^#]*$ending "
	def similars = data.dictLines.findAll{ k,v -> ptrn.matcher(k) }
//	def similars = dictLines.findAll{ it =~ "(?i)^[а-яіїєґА-ЯІЇЄҐ'-]*$ending " }
	if( similars.size() > 100 ) {
		similars = similars[0..100]
	}
	
	similars = similars.collect { it,tag ->
	    def parts = it.split(/ /, 2)
	    def right = parts.length > 1 ? parts[1] : ""
	    if( tag ) {
	        right += " - $tag"
	    }
	    parts[0].padRight(25) + right
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
				def forms = data.expand.expand_line(txt.replaceFirst(/#.*/, ''))
			} catch ( e ) {
				inflectedList.getModel().clear()
				inflectedList.getModel().add(e.getMessage())
				return
			}
		}
	
		
//		inputData.removeAt(selIdx)
//		mainList.invalidate()
		
        def lineToAdd = text.text.trim().replace(' #>', '            #>').replace( '# rv', '            # rv')
		addedList.getModel().add(lineToAdd)
		
		int sz = addedList.getModel().getSize()
		if( sz > 0 ) {
			addedList.ensureIndexIsVisible(sz-1)
		}

		textlabel.text = "Added ${data.newWords.size()} words."
		
		mainList.setSelectionInterval(selIdx + 1, selIdx + 1)
//		mainList.getSelectionModel().fireValueChanged(selIdx, selIdx)
	}
}

//@CompileStatic
def lname() {
    if( text.text.contains('єв ') ) {
        text.text = text.text.replaceFirst(/(єв ).*/, '$1/n2adj2.<+')
    }
    else if( text.text.contains(' /adj') ) {
        text.text = text.text.replaceFirst(' /adj.*', ' /n2adj1.<+')
    }
    else if( text.text.contains(' /n2') ) {
        text.text = text.text.replaceFirst(/([^о] \/n2[0-9]).*/, '$1.a.<+')
        text.text = text.text.replaceFirst(/(о \/n2[0-9]).*/, '$1.<+')

        if( text.text.contains('р /n2') ) {
            text.text = text.text.replace('.<', '.ke.<')
         }
    }
    else if( text.text.contains('/n10') ) {
        text.text = text.text.replaceFirst(/( \/n10).*/, '$1.<+')
    }
    inflect()
    text.text = text.text + "    #=> names-anim"
}

def defaultFlags() {
    def txt = text.text.replaceFirst(/ .*/, '')
    println "Default flags for $txt"
    def word_txt = EditorData.getDefaultTxt(txt)
    text.setText(word_txt)
    findInDict(txt)
    inflect()
}

def openUrl(url) {
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        Desktop.getDesktop().browse(new URI(url));
    }
    else {
        "xdg-open $url".execute()
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
							listData: data.inputData.collect { it.flags ? "${it.word} ${it.flags}" : it.word },
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
//				sp.setMaximumSize(new Dimension(200, 0))
				sp.setMinimumSize(new Dimension(200, 0))

				label('  ---  ')

				vbox {

					label(' ----- ')

					text = textField(
					        //rows: 5
							minimumSize: new Dimension(220, 70)
							)

					textlabel = label("${data.newWords.size()} new words")

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
								text: 'F-lem',
								actionPerformed: {
										text.text = text.text.replaceFirst(/(ою|у|и|і|ій)( .*|$)/, 'а')
										text.text = text.text.replaceFirst(/([еє]ю|ю|ї|їй)( .*|$)/, 'я')
										defaultFlags()
										findInDict(text.text)
										inflect()
									}
								)
	
						def btnL = button(
								text: 'lname',
								actionPerformed: {
							            lname()
							        }
								)

								KeyStroke keystrokeL = KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_MASK)
								btnL.registerKeyboardAction(new ActionListener() {
									public void actionPerformed(ActionEvent e) {
										lname()
									}
								}, keystrokeL, JComponent.WHEN_IN_FOCUSED_WINDOW);

						button(
								text: 'Adjp',
								actionPerformed: {
										text.text = text.text.replaceFirst(/ [^ ]*/, ' /adj :&adjp:pasv:perf')
										if( text.text =~ /[ауюя]ючий / ) {
										    text.text = text.text.replaceFirst(/pasv:perf/, 'actv:imperf')
										}
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
								text: '1992',
								actionPerformed: {
									if( text.text =~ /^екс-/ ) {
										text.text = text.text.replaceFirst(/^(екс)([а-яїієґ])(.*)/, '$1-$2$3 :ua_1992')
									}
									else {
										if (text.text.contains(':') ) {
											text.text = text.text.replaceFirst(/:[^ ]+/, '$0:ua_1992')
										}
										else {
											text.text = text.text.replaceFirst(/\/[^ ]+/, '$0 :ua_1992')
										}
									}
									inflect()
									findInDict(text.text)
								}
								)
						button(
								text: 'NoP',
								actionPerformed: {
									text.text = text.text.replaceFirst(/\.(p[123]?|is0|isNo|isTo)/, '')
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
										text.text = text.text.padRight(30) + '#=> geo-other'
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
							    if( text.text =~ /(ост[ію]|істю)( [\/a-z]| *$)/ ) {
									text.text = text.text.replaceFirst(/(ост[ію]|істю)( [\/a-z]| *$)/, 'ість /n30')
							    }
							    else {
									text.text = text.text.replaceFirst(/(а|е|у|ої|ою|ого|ому|ій?|[иії]м|[иії]ми|[иії]х)( [\/a-z]| *$)/, 'ий /')
							    }
									defaultFlags()
									copyToClipboard(text.text.replaceAll(/ .*/, ''))
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
                                    data.save()
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

                    label('ВЕСУМ:')
					scrollPane(verticalScrollBarPolicy:JScrollPane.VERTICAL_SCROLLBAR_ALWAYS ) {
						def data2 = []

						vesumList = list(
								listData: data2,
								constraints: BorderLayout.EAST
								)
						vesumList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
						vesumList.setVisibleRowCount(50);
						def font = vesumList.getFont()
						vesumList.setFont(new Font("monospaced", font.getStyle(), font.getSize()+1));
//							vesumList.setPreferredSize(new Dimension(200, 300))
					}

					notesLabel = label(horizontalAlignment: SwingConstants.RIGHT)
				}

				vbox {
					scrollPane(verticalScrollBarPolicy:JScrollPane.VERTICAL_SCROLLBAR_ALWAYS ) {

						addedList = list(
							model: new ListWrapperListModel<String>(data.newWords),
							visibleRowCount: 30,
							minimumSize: new Dimension(220, 500)
//							preferredSize: new Dimension(200, 200)
							)
							addedList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

					}

					button(
						text: 'leipzig.de',
						actionPerformed: { 
							def encWord = java.net.URLEncoder.encode(text.text.replaceFirst(/ .*/, ''), "UTF-8")
							def url = "http://corpora.uni-leipzig.de/de/res?corpusId=ukr_mixed_2014&word=${encWord}"
							openUrl(url)
						}
					)

					button(
						text: 'GRAC',
						actionPerformed: {
							def encWord = java.net.URLEncoder.encode(text.text.split(" ", 2)[0], "UTF-8")
							def url = "http://www.parasolcorpus.org/bonito/run.cgi/first?corpname=grac6&queryselector=cqlrow&lemma=&phrase=&word=&char=&cql=%5Bword%3D%22${encWord}%22%5D" 
							openUrl(url)
						}
					)

					button(
						text: 'GBooks',
						actionPerformed: {
							def encWord = java.net.URLEncoder.encode(text.text.split(" ", 2)[0], "UTF-8")
							def url = "https://www.google.com/search?lr=lang_uk&hl=uk&tbo=p&tbm=bks&q=${encWord}&num=10" 
							openUrl(url)
						}
					)
					
					button(
						text: 'Google',
						actionPerformed: {
							def encWord = java.net.URLEncoder.encode(text.text.split(" ", 2)[0], "UTF-8")
							def url = "https://www.google.com/search?q=${encWord}&num=10&source=lnt&tbs=lr:lang_1uk&lr=lang_uk"
							openUrl(url)
						}
					)

					button(
						text: 'Wiki.uk',
						actionPerformed: {
							def encWord = java.net.URLEncoder.encode(text.text.split(" ", 2)[0], "UTF-8")
							def url = "https://uk.wikipedia.org/w/index.php?search=${encWord}" 
							openUrl(url)
						}
					)
					
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
