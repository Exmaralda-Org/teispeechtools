---
title: Simple EXMARaLDA Examples
author: Bernhard Fisseni, Thomas Schmidt
...

This is based on Thomas Schmidt's *Appendix A: Simple EXMARaLDA
Conventions* in [EXMARaLDA's Partitur-Editor
manual](http://www.exmaralda.org/pdf/Partitur-Editor_Manual.pdf).  The
conventions described here are supported by both the [IDS
TEI-Webstuhl](http://clarin.ids-mannheim.de/webstuhl) web services and by
[EXMARaLDA](http://exmaralda.org/)'s
[Partitur-Editor](http://exmaralda.org/en/partitur-editor-en/).


# Specification and examples


0. The format is line-based. Lines occur in temporal order, except in
   case of **overlap** (see paragraph 8).

    > Note: Square, curly and angle brackets as well as double
    > parentheses have special functions in the format.  Therefore
    > they may only be used as specified below.  They cannot occur
    > within the transcription in any other way.
    
    > Note TS: Lines also occur in temporal order in cases of overlap, 
    > so : "Lines occur in temporal order of their starting points. For cases of overlap, see paragraph 8"

1. Every line starts with a short tag indicating the speaker, which is
   followed by a colon.


    ```
    TOM: .....
    tom: .....
    TIM: .....
    ```

    > Additional information on speakers, names and dates etc. should
    > be provided in a central registry, not in the transcripts.
    >
    > Speaker tags must fulfill the criteria for an XML
    > [NCName](https://www.data2type.de/xml-xslt-xslfo/xml-schema/datentypen-referenz/xs-ncname/),
    > basically it can contain letters, numbers, underscores, dashes.
    > Colons are excluded due to Simple EXMARaLDA syntax.
    >
    > Two speakers are not allowed to share the same abbreviation.
    > Capitalization is relevant for distinguishing speakers, as in
    > the following example with ‘big TOM’ and ‘little tom’.  Using
    > speaker's IDs that only differ in capitalization is generally
    > not recommended.

    ```
    TOM: .....
    tom: .....
    ```

2. Every line should contain exactly **one utterance**.  Lines are
   terminated by line breaks (any number of [CR,
   U+000D](http://www.fileformat.info/info/unicode/char/000D/index.htm)
   and [LF,
   U+000A](http://www.fileformat.info/info/unicode/char/000A/index.htm)).

    ```
    TOM: Hallo, Tim!
    TIM: Hallo, Tom.
    ```

    Spaces and tabs are allowed to improve visual structure, but are
    disregarded when importing the file:

    ```
    TOM:       Hallo,      Timotheus!
    TIMOTHEUS: Hallihallo, Tom      .
    ```

3. Lines that start with whitespace are treated as continuation lines,
   but they are treated as different utterances from the first one.

    ```
    TOM: Ich fang einfach mal an zu reden.
         Aber so richtig weiß ich nicht, was.
    ```

    Lines containing only whitespace are disregarded.

4. A transcription of non-verbal actions that accompany the utterances
   (i.e.  that happen simultaneously), can be placed in square
   brackets between the colon and the verbal utterance.  In the ISO
   format, this becomes its own `<incident>`, which is temporally
   coextensive with the main utterance.

    ```
    TOM: [winkt] Hallo, Tim!
    TIM: [winkt] Hallo, Tom.
    ```
    > Note TS: Yes, but we used to put `<incident>` either inside `<u>`
    > or outside (i.e. between) `<annotationBlock>` whereas
    > you put it inside `<annotationBlock>` but outside `<u>`
    > not sure what the better way is

5. Incomprehensible utterances are transcribed using
   double-parenthesis notation:

    ```
    KIM: ((incomprehensible))
    ```

    Similarly for non-verbal actions that are not accompanied by an utterance:

    ```
    WIM: [nods]
    ```

    > Really similarly? It's round brackets in the first and square 
    > ones in the second. They end up at different places in the TEI.


6. Single incomprehensible words can be indicated by spans of tripple
   pluses, where every span signifies a syllable:

    ```
    TIM: ++++++ +++!
    TOM: ++++++ +++!
    ```
    
    > This annotation will only be resolved when parsing the text for
    > transcription conventions, e.g. with the `generic` parsing.

7. An annotation of the utterance, e.g. a translation or a general
   commentary, can be placed in curly brackets behind the
   utterance. It is treated as temporally coextensive to the
   annotation. In the ISO format, it obtains its own `<spangrp
   type="comment">`.

    ```
    TOM: [winkt] Hallo, Tim! {Salut, Tim!}
    TIM: [winkt] Hallo, Tom. {Salut, Tom!}
    ```


8. Overlapping parts of the utterances of different speakers are
   placed into angle brackets. The closing angle bracket is followed
   by any desired string that indexes the overlapping of the
   utterances, followed by another closing angle bracket.


    ```
    TOM: [winkt] Hallo, <Tim!>1> {Salut, Tim!}
    TIM: [winkt] <Hallo>1>, Tom. {Salut, Tom!}
    ```

    > Indexing  should  be  done  with numbers to simplify the readability.
    > These numbers do not need to be in ascending order (it is necessary,
    > however, that they are unambiguous).
    >
    > Again, overlapping utterances can be aligned with whitespace to
    > improve readability. Whitespace is disregarded in the
    > conversion.

    From the second occurrence, overlap marks must occur at the
    beginning of utterances.  The following is **WRONG** and leads to
    an error message:

    ```
    TIM: [winkt] <Hallo>1>, Tom. {Salut, Tom!}
    TOM: [winkt] Hallo, <Tim!>1> {Salut, Tim!}
    ```


# Example document

Running the example document through IDS TEI-Webstuhl produces the
following TEI ISO document.


```xml
<?xml version="1.0" encoding="UTF-8"?>
<TEI xmlns="http://www.tei-c.org/ns/1.0" xmlns:xs="http://www.w3.org/2001/XMLSchema">
   <teiHeader>
      <profileDesc>
         <particDesc>
            <person id="KIM" n="KIM">
               <persName>
                  <abbr>KIM</abbr>
               </persName>
            </person>
            <person id="TIM" n="TIM">
               <persName>
                  <abbr>TIM</abbr>
               </persName>
            </person>
            <person id="TIMOTHEUS" n="TIMOTHEUS">
               <persName>
                  <abbr>TIMOTHEUS</abbr>
               </persName>
            </person>
            <person id="TOM" n="TOM">
               <persName>
                  <abbr>TOM</abbr>
               </persName>
            </person>
            <person id="WIM" n="WIM">
               <persName>
                  <abbr>WIM</abbr>
               </persName>
            </person>
            <person id="tom" n="tom">
               <persName>
                  <abbr>tom</abbr>
               </persName>
            </person>
         </particDesc>
      </profileDesc>
      <encodingDesc>
         <appInfo>
            <application ident="IDS_TEI_Webstuhl" version="0.1">
               <label>IDS TEI-Webstuhl</label>
               <desc>TEI Conversion Webservices</desc>
            </application>
         </appInfo>
         <transcriptionDesc ident="generic" version="2018">
            <desc>
          <!--Fill me in-->
        </desc>
            <label>
          <!--Fill me in-->
        </label>
         </transcriptionDesc>
      </encodingDesc>
      <revisionDesc>
         <change when="2018-11-13T14:06:25.462Z">
         created from Simple EXMARaLDA
         plain text transcript; language set to «deu»
         </change>
      </revisionDesc>
   </teiHeader>
   <text lang="deu">
      <timeline unit="ORDER">
         <when id="B_1"/>
         <when id="E_1"/>
         <when id="B_2"/>
         <when id="E_2"/>
         <when id="B_3"/>
         <when id="E_3"/>
         <when id="B_4"/>
         <when id="E_4"/>
         <when id="B_5"/>
         <when id="E_5"/>
         <when id="B_6"/>
         <when id="E_6"/>
         <when id="B_7"/>
         <when id="E_7"/>
         <when id="B_8"/>
         <when id="E_8"/>
         <when id="B_9"/>
         <when id="E_9"/>
         <when id="B_10"/>
         <when id="E_10"/>
         <when id="B_11"/>
         <when id="E_11"/>
         <when id="B_12"/>
         <when id="E_12"/>
         <when id="B_13"/>
         <when id="E_13"/>
         <when id="B_14"/>
         <when id="E_14"/>
         <when id="B_15"/>
         <when id="E_15"/>
         <when id="B_16"/>
         <when id="E_16"/>
         <when id="B_17"/>
         <when id="E_17"/>
         <when id="B_18"/>
         <when id="E_18"/>
         <when id="B_19"/>
         <when id="E_19"/>
         <when id="B_20"/>
         <!--marked as ‹1› in the input.-->
         <when id="M_1"/>
         <when id="ME_1"/>
         <when id="E_21"/>
      </timeline>
      <body>
         <annotationBlock from="#B_1" to="#E_1" who="TOM">
            <u>.....</u>
         </annotationBlock>
         <annotationBlock from="#B_2" to="#E_2" who="tom">
            <u>.....</u>
         </annotationBlock>
         <annotationBlock from="#B_3" to="#E_3" who="TIM">
            <u>.....</u>
         </annotationBlock>
         <annotationBlock from="#B_4" to="#E_4" who="TOM">
            <u>.....</u>
         </annotationBlock>
         <annotationBlock from="#B_5" to="#E_5" who="tom">
            <u>.....</u>
         </annotationBlock>
         <annotationBlock from="#B_6" to="#E_6" who="TOM">
            <u>Hallo, Tim!</u>
         </annotationBlock>
         <annotationBlock from="#B_7" to="#E_7" who="TIM">
            <u>Hallo, Tom.</u>
         </annotationBlock>
         <annotationBlock from="#B_8" to="#E_8" who="TOM">
            <u>Hallo, Timotheus!</u>
         </annotationBlock>
         <annotationBlock from="#B_9" to="#E_9" who="TIMOTHEUS">
            <u>Hallihallo, Tom .</u>
         </annotationBlock>
         <annotationBlock from="#B_10" to="#E_10" who="TOM">
            <u>Ich fang einfach mal an zu reden.</u>
         </annotationBlock>
         <annotationBlock from="#B_11" to="#E_11" who="TOM">
            <u>Aber so richtig weiß ich nicht, was.</u>
         </annotationBlock>
         <annotationBlock from="#B_12" to="#E_12" who="TOM">
            <incident end="#E_12" start="#B_12">
               <desc>winkt</desc>
            </incident>
            <u>Hallo, Tim!</u>
         </annotationBlock>
         <annotationBlock from="#B_13" to="#E_13" who="TIM">
            <incident end="#E_13" start="#B_13">
               <desc>winkt</desc>
            </incident>
            <u>Hallo, Tom.</u>
         </annotationBlock>
         <annotationBlock from="#B_14" to="#E_14" who="KIM">
            <u>((incomprehensible))</u>
         </annotationBlock>
         <annotationBlock from="#B_15" to="#E_15" who="WIM">
            <incident end="#E_15" start="#B_15">
               <desc>nods</desc>
            </incident>
         </annotationBlock>
         <annotationBlock from="#B_16" to="#E_16" who="TIM">
            <u>++++++ +++!</u>
         </annotationBlock>
         <annotationBlock from="#B_17" to="#E_17" who="TOM">
            <u>++++++ +++!</u>
         </annotationBlock>
         <annotationBlock from="#B_18" to="#E_18" who="TOM">
            <incident end="#E_18" start="#B_18">
               <desc>winkt</desc>
            </incident>
            <u>Hallo, Tim!</u>
            <spanGrp>
               <span from="#B_18" to="#E_18" type="comment">Salut, Tim!</span>
            </spanGrp>
         </annotationBlock>
         <annotationBlock from="#B_19" to="#E_19" who="TIM">
            <incident end="#E_19" start="#B_19">
               <desc>winkt</desc>
            </incident>
            <u>Hallo, Tom.</u>
            <spanGrp>
               <span from="#B_19" to="#E_19" type="comment">Salut, Tom!</span>
            </spanGrp>
         </annotationBlock>
         <annotationBlock from="#B_20" to="#ME_1" who="TOM">
            <incident end="#ME_1" start="#B_20">
               <desc>winkt</desc>
            </incident>
            <u>Hallo, <anchor synch="#M_1"/>Tim!</u>
            <spanGrp>
               <span from="#B_20" to="#ME_1" type="comment">Salut, Tim!</span>
            </spanGrp>
         </annotationBlock>
         <annotationBlock from="M_1" to="#E_21" who="TIM">
            <incident end="#E_21" start="M_1">
               <desc>winkt</desc>
            </incident>
            <u>Hallo<anchor synch="#ME_1"/>, Tom.</u>
            <spanGrp>
               <span from="#M_1" to="#E_21" type="comment">Salut, Tom!</span>
            </spanGrp>
         </annotationBlock>
      </body>
   </text>
</TEI>
```
