<h1 class="title">Simple EXMARaLDA Examples (DRAFT!)</h1>
<div class="author">Bernhard Fisseni, Thomas Schmidt</div>


This is based on Thomas Schmidt's *Appendix A: Simple EXMARaLDA
Conventions* in [EXMARaLDA's Partitur-Editor
manual](http://www.exmaralda.org/pdf/Partitur-Editor_Manual.pdf).  The
conventions described here are supported by both the [IDS
TEI-Webstuhl](http://clarin.ids-mannheim.de/webstuhl) web services,
aby this library, and by [EXMARaLDA](http://exmaralda.org/)'s
[Partitur-Editor](http://exmaralda.org/en/partitur-editor-en/).


# Specification and examples


0. The format is line-based. Lines occur in temporal order, except in
   case of **overlap** (see paragraph 8).

    > Note: Square, curly and angle brackets as well as double
    > parentheses have special functions in the format.  Therefore
    > they may only be used as specified below.  They cannot occur
    > within the transcription in any other way.
    
    > Lines occur in temporal order of their starting points. For
    > cases of overlap, see paragraph 8.

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
    <div class="discussion">
    > Note TS: Yes, but we used to put `<incident>` either inside `<u>`
    > or outside (i.e. between) `<annotationBlock>` whereas
    > you put it inside `<annotationBlock>` but outside `<u>`
    > not sure what the better way is
    >
    > Note BF: Not sure about what the semantics of an `<incident>`
    > inside `<u>` is. I assumed that sibling elements inside an
    > utterance are (by default) consecutive and sibling elements
    > inside an `<annotationBlock>` simultaneous but clearly (somehow)
    > related.  For separate `<annotationBlock>`s, you have to specify
    > the temporal relation (which I do, too, anyway), and the
    > ‘closeness’ gets lost, which is probably not very important.  We
    > should standardize on one.  To avoid temporal ambiguity, I would
    > prefer `<incident>` outside `<u>` for this case.
    > For the case of incomprehensible content, putting it inside an
    > `<u>` feels right.
    > The parser and processor were changed so that `[incidents]` go
    > before the `<annotationBlock>` (same time anchors) and
    > `((incomprehensibles))` go inside the utterance. OK?
    </div>

5. Incomprehensible utterances are transcribed using
   double-parenthesis notation and are treated as utterances:

    ```
    KIM: ((incomprehensible))
    ```

    Similarly for non-verbal actions that are not accompanied by an
    utterance; however, these are not treated as utterances:

    ```
    WIM: [nods]
    ```

6. Single incomprehensible words can be indicated by spans of tripple
   pluses, where every span signifies a syllable:

    ```
    TIM: ++++++ +++!
    TOM: ++++++ +++!
    ```
    
    > This annotation will only be resolved when parsing the text for
    > transcription conventions, e.g. with the `generic` parsing.

7. Uncertain content is placed in single parentheses; parentheses are
   allowed to contain incomprehensible content.

    ```
    TIM: (Hallo) Tim!
    TOM: (++++++ Tom!)
    ```
    
    > This annotation will only be resolved when parsing the text for
    > transcription conventions, e.g. with the `generic` parsing.


8. Pauses are indicated by full stops in parentheses. Pause lengths
   are determined by the number of full stops (short, medium, long,
   very+ long).

    ```
    TIM: Hallo (...) Wim!
    ```
    > This annotation will only be resolved when parsing the text for
    > transcription conventions, e.g. with the `generic` parsing.

9. An annotation of the utterance, e.g. a translation or a general
   commentary, can be placed in curly brackets behind the
   utterance. It is treated as temporally coextensive to the
   annotation. In the ISO format, it obtains its own `<spanGrp
   type="comment">`.

    ```
    TOM: [winkt] Hallo, Tim! {Salut, Tim!}
    TIM: [winkt] Hallo, Tom. {Salut, Tom!}
    ```

10. Overlapping parts of the utterances of different speakers are
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
         <change when="2018-11-14T10:57:57.294Z">
             created from Simple EXMARaLDA plain text transcript; 
             language set to «deu»
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
         <when id="E_20"/>
         <when id="B_21"/>
         <when id="E_21"/>
         <when id="B_22"/>
         <when id="E_22"/>
         <when id="B_23"/>
         <!--marked as ‹1› in the input.-->
         <when id="M_1"/>
         <when id="ME_1"/>
         <when id="E_24"/>
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
         <incident end="#E_12" start="#B_12">
            <desc>winkt</desc>
         </incident>
         <annotationBlock from="#B_12" to="#E_12" who="TOM">
            <u>Hallo, Tim!</u>
         </annotationBlock>
         <incident end="#E_13" start="#B_13">
            <desc>winkt</desc>
         </incident>
         <annotationBlock from="#B_13" to="#E_13" who="TIM">
            <u>Hallo, Tom.</u>
         </annotationBlock>
         <annotationBlock from="#B_14" to="#E_14" who="KIM">
            <u>
               <incident end="#E_14" start="#B_14">
                  <desc>incomprehensible</desc>
               </incident>
            </u>
         </annotationBlock>
         <incident end="#E_15" start="#B_15">
            <desc>nods</desc>
         </incident>
         <annotationBlock from="#B_16" to="#E_16" who="TIM">
            <u>++++++ +++!</u>
         </annotationBlock>
         <annotationBlock from="#B_17" to="#E_17" who="TOM">
            <u>++++++ +++!</u>
         </annotationBlock>
         <annotationBlock from="#B_18" to="#E_18" who="TIM">
            <u>(Hallo) Tim!</u>
         </annotationBlock>
         <annotationBlock from="#B_19" to="#E_19" who="TOM">
            <u>(++++++ Tom!)</u>
         </annotationBlock>
         <annotationBlock from="#B_20" to="#E_20" who="TIM">
            <u>Hallo (...) Wim!</u>
         </annotationBlock>
         <incident end="#E_21" start="#B_21">
            <desc>winkt</desc>
         </incident>
         <annotationBlock from="#B_21" to="#E_21" who="TOM">
            <u>Hallo, Tim!</u>
            <spanGrp>
               <span from="#B_21" to="#E_21" type="comment">Salut, Tim!</span>
            </spanGrp>
         </annotationBlock>
         <incident end="#E_22" start="#B_22">
            <desc>winkt</desc>
         </incident>
         <annotationBlock from="#B_22" to="#E_22" who="TIM">
            <u>Hallo, Tom.</u>
            <spanGrp>
               <span from="#B_22" to="#E_22" type="comment">Salut, Tom!</span>
            </spanGrp>
         </annotationBlock>
         <incident end="#E_23" start="#B_23">
            <desc>winkt</desc>
         </incident>
         <annotationBlock from="#B_23" to="#ME_1" who="TOM">
            <u>Hallo, <anchor synch="#M_1"/>Tim!</u>
            <spanGrp>
               <span from="#B_23" to="#ME_1" type="comment">Salut, Tim!</span>
            </spanGrp>
         </annotationBlock>
         <incident end="#E_24" start="#B_24">
            <desc>winkt</desc>
         </incident>
         <annotationBlock from="M_1" to="#E_24" who="TIM">
            <u>Hallo<anchor synch="#ME_1"/>, Tom.</u>
            <spanGrp>
               <span from="#M_1" to="#E_24" type="comment">Salut, Tom!</span>
            </spanGrp>
         </annotationBlock>
      </body>
   </text>
</TEI>
```
