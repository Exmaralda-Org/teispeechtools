---
author: Bernhard Fisseni
title: Normalization for TEI XML files
---

# Purpose

This contains services for applying orthographic normalization, mainly to transcripts 
of spoken data in [TEI](http://www.tei-c.org/release/doc/tei-p5-doc/en/html/TS.html)

It basically searches for `<w>` elements and applies normalization to their
[text content](https://www.w3schools.com/xml/prop_element_textcontent.asp). 


# Available Services

Currently, it only contains the `DictionaryNormalizer`, which applies normalization
based on dictionaries from the FOLK and the DeReKo corpora.

- Step 1: The most frequent normalization for a word form in the
  [FOLK](http://agd.ids-mannheim.de/folk.shtml) corpus is applied.
- Step 2: If nothing is found in Step 1, the list of words that occur
  capitalized-only in the [Deutsches Referenzkorpus
  (DeReKo)](http://www1.ids-mannheim.de/kl/projekte/korpora.html)
  is consulted and a normalization is chosen.


# Compilation

    mvn clean install
    
installs the package locally with Maven.  Use as a library then.
