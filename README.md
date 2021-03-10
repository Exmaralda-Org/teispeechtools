---
current version: 0.6.1
---


# Purpose

This documentation describes the tools for processing transcripts of
spoken data in
[TEI](http://www.tei-c.org/release/doc/tei-p5-doc/en/html/TS.html). Some
of the tools can also be applied to TEI documents which are at least
`<w>`-annotated.

In principle, target documents are those conforming to the ISO standard
[ISO 24624:2016(E)](https://www.iso.org/standard/37338.html) ‘Language
resource management -- Transcription of spoken language’.


# Availability

This is the library containing the tools. A [companion
suite of webservices called
<b>TEILicht</b>](https://clarin.ids-mannheim.de/teilicht)
([source](https://github.com/Exmaralda-Org/teilicht)) 
offers the functionality to be accessed as
[REST](https://en.wikipedia.org/wiki/Representational_state_transfer)ful
[web services](https://en.wikipedia.org/wiki/Web_service), i.e. you can
upload documents and download them in the processed form. The web
services are also available via
[WebLicht](https://weblicht.sfs.uni-tuebingen.de/), but by processing
TEI-encoded files they break with WebLicht’s convention of processing
[TCF](https://weblicht.sfs.uni-tuebingen.de/weblichtwiki/index.php/The_TCF_Format)
files.

## Run CLI

All functions are also accessible from the command line. Try:

(after building in root directory)

``` sh
java -cp 'target/dependency/*' -jar target/teispeechtools-VERSION.jar
```

(in the directory containing the jar)

``` sh
java -cp 'dependency/*' -jar teispeechtools-VERSION.jar
```

and follow the help. Together with this description, you should get
along well. If not, contact [Bernhard
Fisseni](mailto:bernhard.fisseni@uni-due.de?subect=TEI+Transcription+tools)

A simple [wrapper script](spindel.sh) is available.

The names of CLI commands corresponds to those of the TEILicht web
services.

The names of the options CLI correspond to those of the parameters
used in the Java library.  However, some abbreviations are possible
and `CamelCase` was converted to
[`kebab-case`](https://stackoverflow.com/questions/11273282/whats-the-name-for-hyphen-separated-case).
To see all options, execute the wrapper script once.


# Tools

## Plain Text to ISO-TEI-annotated texts (CLI command `text2iso`)

  - Input  
    a [plain text](https://en.wikipedia.org/wiki/Plain_text) file
    containing a transcription in the [Simple EXMARaLD...A
    format](doc/Simple-EXMARaLDA.md). This
    format permits to encode utterances and overlap between them as well
    as incidents occurring independently or simultaneously to an
    utterance and a commentary (e.g. a translation) on utterances or
    incidents.

  - Output  
    a transcription file conforming to the TEI specification which is
    split into utterances: `<annotationBlock>` and `<u>`,
    `<incident>` elements together with `<spanGrp>`s containing the
    commentary. A `<timeline>` is derived from the text, and all
    annotation is situated with respect to the `<timeline>`.

  - Parameters  
    
      - the `language` of the utterance.  If the header of the document
        specifies a language, the header value will take precedence.


## Segmentation according to transcription convention (CLI command `segmentize`)

  - Input  
    a TEI-conformant XML document containing `<u>` elements which
    contain plain text formatted according to a transcription convention
    ([generic](doc/Generic-Conventions.md),
    [cGAT](https://ids-pub.bsz-bw.de/frontdoor/index/index/docId/4616)
    minimal,
    [cGAT](https://ids-pub.bsz-bw.de/frontdoor/index/index/docId/4616)
    basic) and potentially `<anchor>` elements referring to the
    `<timeline>`. This can be the output of the previously described
    plain text conversion.

  - Output  
    a TEI-conformant XML document in which the `<u>` elements have been
    segmented into words `<w>` on the one hand and conventions have been
    resolved to XML markup like `<pause>`, `<gap>` etc.

  - Parameters  
    
      - the `language` of the document (if there is language information
        in the document, it will be preferred),
      - the transcription convention which the text contents of the
        `<u>` follows. Currently `generic`, (cGAT) `minimal` and (cGAT)
        `basic` are supported.


## Language-detection (CLI command `guess`)

  - Input  
    a TEI-conformant XML document containing `<u>` elements which
    contain either plain text and `<anchor>` elements only or have been
    analysed into `<w>` (other contents possible). In the first case,
    the whole text content (excluding) will be processed; in the latter
    case, only the contents of `<w>` elements will be processed.

  - Output  
    a TEI-conformant XML document where the `<u>` have been annotated with
    `@xml:lang` attributes where the
    [algorithm](https://opennlp.apache.org/docs/1.9.0/manual/opennlp.html#tools.langdetect)
    reached a decision. Cases of doubt are reported in XML comments. If
    languages are equally probable, the document language is preferred.

  - Parameters  
    
      - the `language` of the document (if there is language information
        in the document, it will be preferred),
      - the `expected languages` of the document, to constrain the
        search space, [which contains 103
        languages](http://opennlp.apache.org/models.html), for language
        detection. The more precisely you know which languages are
        expected, the better detection will work.

        The web service variant also  accepts parameters `exspected1`,
        `exspected2`,… `exspected5` which contain single language codes.

      - the `minimal count` of words so that language detection is even
        tried (default: 5, which is already pretty low).
      - whether to `force` language detection, even if a language tag
        has already been assigned to `<u>`.


## OrthoNormal-like Normalization (command `normalize`)

  - Input  
    a TEI-conformant XML document containing `<u>` elements which have
    been analysed into `<w>` (other contents possible).

  - Output  
    a TEI-conformant XML document where the `<w>` have been annotated with
    a `@norm` attribute containing the normalized form. Currently,
    normalization is only applied to text in German.

  - Parameters  
    
      - the `language` of the document (if there is language information
        in the document, it will be preferred).
      - whether to `force` normalization, even if `<w>`s already have
        `@norm` attributes.

This service is based on the algorithm in [OrthoNormal (German
description only)](http://exmaralda.org/de/orthonormal-de/).
[`DictionaryNormalizer`](target/apidocs/de/ids/mannheim/clarin/teispeech/tools/class-use/DictionaryNormalizer.html),
which applies normalization based on dictionaries from the
[FOLK](http://agd.ids-mannheim.de/folk.shtml) and the
[DeReKo](http://www1.ids-mannheim.de/kl/projekte/korpora.html) corpora.

It basically searches for `<w>` elements and applies normalization to
their [text
content](https://www.w3schools.com/xml/prop_element_textcontent.asp).

Normalization:

  - Step 1: The most frequent normalization for a word form in the
    [FOLK](http://agd.ids-mannheim.de/folk.shtml) corpus is applied.
  - Step 2: If nothing is found in Step 1, the list of words that occur
    capitalized-only in the [Deutsches Referenzkorpus
    (DeReKo)](http://www1.ids-mannheim.de/kl/projekte/korpora.html) is
    consulted and a normalization is chosen.
  - Step 3: Out-of-dictionary words are left as is.


## POS-Tagging with the TreeTagger (command `pos`)

  - Input  
    a TEI-conformant XML document containing `<u>` elements which have
    been analysed into `<w>` (other contents possible).

  - Output  
    a TEI-conformant XML document where the `<w>` have been pos-tagged
    (`@pos` attribute) and lemmatized (`@lemma` attribute) with the
    [TreeTagger](http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/).

  - Parameters  
    
      - the `language` of the document (if there is language information
        in the document, it will be preferred).
      - whether to `force` tagging, even if a pos tag has already been
        assigned to `<w>`.


## Pseudo-alignment using Phonetic Transcription or Orthographic Information (command `align`)

  - Input  
    a TEI-conformant XML document 
    - containing `<u>` elements which have
    been analysed into `<w>` (other contents possible) and
    - a timeline that conforms to the following constraints:
        - if no `--time` parameter is given,
          it must be specified in the document as follows:
          the `<tei:timeline>` contains a leading `<tei:when>`
          that has `@interval` of 0 to itself; there is a last
          `<tei:when>` element that refers to this element
          (`@since`) and the `@interval` will be the duration.
        - if no `--offset` parameter is given, the `@interval`
          between the first and second `<tei:when>` will be
          considered the offset.  Offsets are always positive.

  - Output  
    a TEI-conformant XML document where 
    - the `<w>` have been assigned a
      proportion of utterance corresponding to the number of letters or
      IPA signs in the `<w>`.
    - duration information for `<u>` will be in the `<tei:timeline>`.

  - Parameters  
    
      - the `language` of the document (if there is language information
        in the document, it will be preferred)
      - whether to `force` tagging, even if a pos tag has already been
        assigned to `<w>`
      - whether to `transcribe` using [BAS Web Services](https://clarin.phonetik.uni-muenchen.de/BASWebServices/interface).
        See the [BAS documentation](http://clarin.phonetik.uni-muenchen.de/BASWebServices/help)
        ("runG2P") for the supported locales (non-ISO-639 codes like
        `nze` are not supported here).  The service will do some
        adjustment to be able to transcribe (e.g., accept `ltz` and not
        just the full `ltz-LU` for Luxemburgish).  Transcription is only
        used if phones are used for pseudoalignment, see next option
      - whether to `usePhones` for pseudoalignment. If transcription
        using BAS' web service is possible and `usePhones` is true, the
        transcription will be used to guess the proportion of utterance
        duration to assign to the `<w>`. If no transcription
        is possible, or transcription is disabled, the number of letters
        will be used to pseudo-align, and no transcription will be added.
      - the `time` duration of the utterance. Setting the time to -1 (default)
        means that it will be derived from the document as described above.
      - the `offset` of the utterance, i.e. the time of the first
        timeline event. Setting the offset to -1 (default) means that it will
        be derived from the document as described above.
      - `every`, a number of items after which to insert anchors


## Adding and removing `xml:id` (command `identify` and `unidentify`)

For some operations, it must be possible to address all structural
elements with an `@xml:id` attribute.  `identify` adds `@xml:id`
attributes to all TEI elements that do not have one, and `@unidentify`
removes such attributes whose form suggests they have been added by
`identify`.



# Building and inspecting


## Dependencies

Besides the dependencies available via
[Maven](https://maven.apache.org/), needs [some utility
functions](https://github.com/teoric/java-utilities). These can be
locally [`mvn
install`ed](https://maven.apache.org/plugins/maven-install-plugin/usage.html).


## Compilation

``` sh
    mvn install dependency:copy-dependencies
```

installs the package locally with Maven and copies the dependencies to
`target/dependency`. You can use this package as a library then, or use
the CLI (see below). If you only want to use the CLI, you can run:

``` sh
    mvn package dependency:copy-dependencies
```

This will pull in the sources and make a runnable
[JAR](https://en.wikipedia.org/wiki/JAR_%28file_format%29)


## Compile Dictionary

To speed up loading time, one can compile the dictionary, which combines
the FOLK and DeReKo dictionaries described above. The combined
dictionary is contained in downloads. From the root directory of the
project, execute:

``` sh
java -cp 'target/teispeechtools-VERSION.jar:target/dependency/*' \
    de.ids.mannheim.clarin.teispeech.tools.DictMaker
```


## Check Pattern files

To check the files with regular expressions for transcription
conventions by running:

``` sh
java -cp 'target/dependency/*:target/teispeechtools-VERSION.jar' \
    de.ids.mannheim.clarin.teispeech.tools.PatternReaderRunner -h
```

to get help. E.g.,

``` sh
java -cp 'target/dependency/*:target/teispeechtools-VERSION.jar' \
    de.ids.mannheim.clarin.teispeech.tools.PatternReaderRunner \
    -i src/main/xml/Patterns.xml --language universal --level 2
```

(only levels 2 and 3 are available)
