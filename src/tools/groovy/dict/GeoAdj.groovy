#!/bin/env groovy

import groovy.transform.*

def geoLines = new File('geo-ukr-koatuu.lst').readLines()
def baseLines = new File('base.lst').readLines()
def ulif = new File('vtssum_lemmas.txt').readLines().toSorted()


def baseAdjs = baseLines.findAll { it.contains(' /adj') }.collect { it.split(' ', 2)[0] }

@Field
def unknown = []

def derivAdjs =
    geoLines
    .findAll { ! it.startsWith('#') \
            && ! it.contains(' /adj') \
            && ! it.contains(':nv') }
    .collectEntries {
        def (word, flags) = it.split(' ', 2)
        
        def derived = deriveAdj(word, flags)
        if( derived instanceof String ) {
            derived = [ derived ]
        }
        [ (word) : derived ]
    }


println "Unknown: ${unknown.size}"
println unknown.toSorted { it.reverse() }.join("\n")

new File("geo_deriv").text = derivAdjs.collect { k,v-> "$k - $v" }.join("\n")


def adjs = geoLines
    .findAll { it.contains(' /adj') }
    .collect { it.split(' ', 2)[0] }


def bases = []

derivAdjs.each { k, v ->
    if( v && ! (v.intersect(adjs)) ) {
        
        if( v.intersect(baseAdjs) ) {
          v.eachWithIndex { word, i ->
            if( baseAdjs.contains(word) && ! ulif.contains(word) ) {
                bases << word
            }
          }
        }
        else {
           println "$k - " + v.join(", ")
        }
    }
    else {
//        println "ok: $v - $k"
    }
}

new File('base_with_geo').text = bases.join(' \n')


//new File('geo-ukr-koatuu.lst.new').text = newLines.join('\n')



def deriveAdj(word_, flags) {
    def word = word_.toLowerCase()

    if( word =~ /[сц]ький|ий$/ ) {
        return word
    }

    if( word =~ /([сц]ьк[еа])$/ && flags.contains('n2adj1') ) {
        return word[0..<-1] + 'ий'
    }

    if( word =~ /([аяоеєії])$/ && flags.contains('n2adj1') ) {
        if( word =~ /(йов[аео])$/ ) {
            return word[0..<-4] + 'ївський'
        }
        if( word =~ /(ьов[аео])$/ ) {
            return word[0..<-4] + 'івський'
        }
        if( word =~ /([є]в[аео])$/ ) {
            return word[0..<-3] + 'ївський'
        }
        if( word =~ /([ео]в[аео])$/ ) {
            return [word[0..<-3] + 'івський', word + 'цький']
        }
        if( word =~ /не$/ ) {
            return [word[0..<-1] + 'ий', word[0..<-1] + 'ський', word + 'нський', word + 'янський']
        }
        if( word =~ /ч[ае]$/ ) {
            return [word[0..<-1] + 'ий', word[0..<-2] + 'цький']
        }
        return word[0..<-1] + 'ський'
    }

    if( word =~ /[сц]ьк$/ ) {
        return word + 'ий'
    }
    if( word =~ /ць$/ ) {
        return word + 'кий'
    }


    if( word =~ /([еєоя]в[ео]|ова|ино)$/ ) {
        if( word =~ /(йов[аео])$/ ) {
            return word[0..<-4] + 'ївський'
        }
        if( word =~ /(ьов[аео])$/ ) {
            return word[0..<-4] + 'івський'
        }
        if( word =~ /([є]в[аео])$/ ) {
            return word[0..<-3] + 'ївський'
        }
        if( word =~ /([ео]в[аео])$/ ) {
            return word[0..<-3] + 'івський'
        }
        return word[0..<-1] + 'ський'
    }

    if( word =~ /оле$/ ) {
        return word[0..<-3] + 'ільський'
    }

    if( word =~ /[цс]ьке|[зч]н[ае]$/ ) {
        return word[0..<-1] + 'ий'
    }

    if( word =~ /[аеєиіїоуюя][бвдлмнпрт]а$/ ) {
        return word[0..<-1] + 'ський'
    }
    if( word =~ /[аеєиіїоуюя][чк]а$/ ) {
        return word[0..<-2] + 'цький'
    }

    if( word =~ /[и]ня$/ ) {
        return word + 'нський'
    }

    if( word =~ /[и]ця$/ ) {
        return word[0..<-1] + 'ький'
    }

    if( word =~ /[иі]я$/ ) {
        return word[0..<-1] + 'йський'
    }

    if( word =~ /[аеиіїоу]лля$/ ) {
        return word[0..<-2] + 'ьський'
    }
    if( word =~ /сся$/ ) {
        return word[0..<-2] + 'ький'
    }
    if( word =~ /(ння|ття)$/ ) {
        return word[0..<-2] + 'ський'
    }
    if( word =~ /(жжя|ззя)$/ ) {
        return word[0..<-3] + 'зький'
    }
    if( word =~ /([иоу]ччя)$/ ) {
        return word[0..<-3] + 'цький'
    }
    if( word =~ /(річчя)$/ ) {
        return word[0..<-2] + 'анський'
    }
    if( word =~ /([^аеєиіїоуюя']я|льня)$/ ) {
        return word + 'нський'
    }
    if( word =~ /([^аеєиіїоуюя]'я)$/ && !(word =~ /ір'я$/) ) {
        return word + 'нський'
    }
    if( word =~ /(р'я)$/ ) {
        return [word[0..<-2] + 'нянський', word[0..<-2] + 'ський']
    }

    if( word =~ /[бвдймнрт]ка$/ ) {
        return word[0..<-2] + 'ський'
    }
    if( word =~ /[зж]ка$/ ) {
        return word[0..<-3] + 'зький'
    }
    if( word =~ /[с]ка$/ ) {
        return word[0..<-2] + 'ький'
    }
    if( word =~ /[ч]ка$/ ) {
        return word[0..<-3] + 'цький'
    }
    if( word =~ /лка$/ ) {
        return word[0..<-2] + 'ьський'
    }
    if( word =~ /([ьш]ка|ко)$/ ) {
        return word[0..<-1] + 'івський'
    }

    if( word =~ /[^аеєиіїоуюя]ва$/ ) {
        return word[0..<-1] + 'янський'
    }

    if( word =~ /[гжз]а$/ ) {
        return word[0..<-2] + 'зький'
    }
    if( word =~ /[схш]а$/ ) {
        return word[0..<-2] + 'ський'
    }

    if( word =~ /(ча|че|ща|ще)$/ ) {
        return word + 'нський'
    }

    if( word =~ /[аеєиіїоуюя][ш]і$/ ) {
        return word + 'вський'
    }

    if( word =~ /ько$/ ) {
        return word[0..<-1] + 'ий'
    }


    if( word =~ /[ч]і$/ ) {
        return [ word[0..<-2] + 'цький', word + 'вський']
    }
    if( word =~ /[внп]ці$/ ) {
        if( word =~ /[ое]вці$/ ) {
            return [word[0..<-4] + 'івецький', word + 'вський']
        }
        if( word =~ /івці$/ ) {
            return [word[0..<-4] + 'овецький', word + 'вський']
        }
        return [word[0..<-2] + 'ецький', word + 'вський']
    }
    if( word =~ /ельці$/ ) {
        return [word[0..<-1] + 'івський', word[0..<-3] + 'ецький']
    }
    if( word =~ /и$/ ) {
        return [word[0..<-1] + 'івський', word[0..<-1] + 'ський']
    }
    if( word =~ /[ії]$/ ) {
        return [word + 'вський', word[0..<-1] + 'анський', word[0..<-1] + 'янський', word[0..<-1] + 'инський']
    }
    if( word =~ /'є$/ ) {
        return word[0..<-1] + 'ївський'
    }


    if( word =~ /[гжз]$/ ) {
        return word[0..<-1] + 'зький'
    }
    if( word =~ /чок$/ ) {
        return word[0..<-3] + 'цький'
    }
    if( word =~ /[кцч]$/ ) {
        return word[0..<-1] + 'цький'
    }
    if( word =~ /[схш]$/ ) {
        return word[0..<-1] + 'ський'
    }
    if( word =~ /л$/ ) {
        return word[0..<-1] + 'ьський'
    }
    if( word =~ /([бвдмнпрстфщьй])$/ ) {
        return word + 'ський'
    }

    if( word =~ /[бдн]а$/ ) {
        return [word[0..<-1] + 'ський', word[0..<-1] + 'янський', word[0..<-1] + 'івський']
    }
    if( word =~ /[м]а$/ ) {
        return [word[0..<-1] + 'ський', word[0..<-1] + 'анський', word[0..<-1] + 'івський']
    }
    if( word =~ /[р]а$/ ) {
        return [word[0..<-1] + 'янський']
    }
    if( word =~ /(пка|та)$/ ) {
        return [word[0..<-1] + 'инський', word[0..<-2] + 'ський']
    }
    if( word =~ /па$/ ) {
        return [word[0..<-1] + 'янський', word[0..<-1] + 'инський', word[0..<-1] + 'івський']
    }

    if( word =~ /[аеєиіїоуюя][лн]о$/ ) {
        return word[0..<-1] + 'ьський'
    }
    if( word =~ /[^аеєиіїоуюя]ло$/ ) {
        return [word[0..<-1] + 'янський', word[0..<-1] + 'івський']
    }
    if( word =~ /[^аеєиіїоуюя]но$/ ) {
        return [word[0..<-1] + 'янський', word[0..<-1] + 'евський', word[0..<-1] + 'івський']
    }

    if( word =~ /[аеоу]я$/ ) {
        return [word[0..<-1] + 'янський', word[0..<-1] + 'ївський']
    }

    
    unknown << word_
    
    return null
}