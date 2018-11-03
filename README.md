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

```sh
    mvn install dependency:copy-dependencies
```

installs the package locally with Maven and copies the dependencies to
`target/dependency`.  You can use this package as a library then, or
use the CLI (see below).  If you only want to use the CLI, you can run:

```sh
    mvn package dependency:copy-dependencies
```

This will pull in the sources and make a runnable [JAR](https://en.wikipedia.org/wiki/JAR_%28file_format%29)



# Compile Dictionary

To speed up loading time, one can compile the dictionary.  From the root directory 
of the project, execute:

```sh
java -cp 'target/teispeechtools-0.1-SNAPSHOT.jar:target/dependency/*' \
    de.ids.mannheim.clarin.teispeech.tools.DictMaker
```

# Run CLI

All functions are also accessible from the command line.  Try:

```sh
java -cp 'target/dependency/*' -jar target/teispeechtools-0.1-SNAPSHOT.jar
```

and follow the help.


# Check Pattern files

To check the files with regular expressions, you can run:

```sh
java -cp 'target/dependency/*:target/teispeechtools-0.1-SNAPSHOT.jar' \
    de.ids.mannheim.clarin.teispeech.tools.PatternReaderRunner -h
```

to get help. E.g.,

```sh
java -cp 'target/dependency/*:target/teispeechtools-0.1-SNAPSHOT.jar' \
    de.ids.mannheim.clarin.teispeech.tools.PatternReaderRunner \
    -i src/main/xml/Patterns.xml -l universal -L 2
```
