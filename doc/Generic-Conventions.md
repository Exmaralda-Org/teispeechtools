<h1 class="title">Generic Transcription Conventions (DRAFT!)</h1>
<div class="author">Bernhard Fisseni, Thomas Schmidt</div>

# Purpose

This describes the generic conventions for transcripts of spoken
language that are  supported by both the [IDS
TEI-Webstuhl](http://clarin.ids-mannheim.de/webstuhl) web services and
this library.


# Specification and Examples

0. This works on un-analysed
   [`<u>`](http://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-u.html)
   elements in a TEI-ISO transcript document, as it can be produced
   from the [Simple EXMARaLDA format](Simple-Exmaralda.md).
   
   Utterances `<u>` that contain anything besides text content and
   time `<anchor>`s are not analysed.
   
1. An utterance `<u>` is mainly split into words that will be
   annotated as
   [`<w>`](http://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-w.html)
   elements and punctuation, which will become
   [`pc`](http://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-pc.html)
   elements.

2. Uncertain content is placed in single parentheses; parentheses are
   allowed to contain incomprehensible content.

    ```
    TIM: (Hallo) Tim!
    TOM: (++++++ Tom!)
    ```

3. Pauses are indicated by full stops in parentheses. Pause lengths
   are determined by the number of full stops (short, medium, long,
   very+ long).

    `TIM: Hallo (...) Wim!`
