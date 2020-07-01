#!/bin/env groovy

def dict_uk_all = new File('../dict_corp_lt.txt').text.split("\n")

def pts = []
def ps = []

dict_uk_all.each { it ->
    if( it.contains('noun') && it.contains(':p:v_naz') && ! it.contains('prop') ) {
        if( it.contains(':ns') ) {
            pts << it.split()[0]
        }
        else {
            ps << it.split()[0]
        }
    }
}

println ps.intersect(pts).join('\n')
