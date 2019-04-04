<h1 class="title">Generic Transcription Conventions (DRAFT!)</h1>
<div class="author">Bernhard Fisseni, Thomas Schmidt</div>

# Purpose

This describes the generic conventions for transcripts of spoken
language that are supported by both the [IDS
TEILicht](http://clarin.ids-mannheim.de/teilicht) web services and
this library. They are applied in the `segmentize` step of processing.


# Specification and Examples

0. The segmentation works on un-analysed
   [`<u>`](http://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-u.html)
   elements in a TEI-ISO transcript document, as it can be produced
   from the [Simple EXMARaLDA format](Simple-EXMARaLDA.md).
   
   Utterances `<u>` that contain anything besides text content and
   time `<anchor>`s are not analysed. Comments and processing
   instructions inside `<u>` will be removed. Whitespace will be
   normalized.
   
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


# Example document

The example document from the [Simple EXMARaLDA
format](Simple-Exmaralda.md) documentation is parsed as follows:

```xml
<?xml version="1.0" encoding="UTF-8"?><TEI xmlns="http://www.tei-c.org/ns/1.0" xmlns:xs="http://www.w3.org/2001/XMLSchema">
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
            <application ident="IDS_TEILicht" version="0.1">
               <label>IDS TEILicht</label>
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
         <change when="2018-11-14T10:57:57.294Z">created from Simple EXMARaLDA plain text transcript; language set to «deu»</change>
      <change when="2018-11-14T10:58:00.907Z">segmented according to generic transcription conventions</change></revisionDesc>
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
            <u><pc>.....</pc></u>
         </annotationBlock>
         <annotationBlock from="#B_2" to="#E_2" who="tom">
            <u><pc>.....</pc></u>
         </annotationBlock>
         <annotationBlock from="#B_3" to="#E_3" who="TIM">
            <u><pc>.....</pc></u>
         </annotationBlock>
         <annotationBlock from="#B_4" to="#E_4" who="TOM">
            <u><pc>.....</pc></u>
         </annotationBlock>
         <annotationBlock from="#B_5" to="#E_5" who="tom">
            <u><pc>.....</pc></u>
         </annotationBlock>
         <annotationBlock from="#B_6" to="#E_6" who="TOM">
            <u><w>Hallo</w><pc>,</pc><w>Tim</w><pc>!</pc></u>
         </annotationBlock>
         <annotationBlock from="#B_7" to="#E_7" who="TIM">
            <u><w>Hallo</w><pc>,</pc><w>Tom</w><pc>.</pc></u>
         </annotationBlock>
         <annotationBlock from="#B_8" to="#E_8" who="TOM">
            <u><w>Hallo</w><pc>,</pc><w>Timotheus</w><pc>!</pc></u>
         </annotationBlock>
         <annotationBlock from="#B_9" to="#E_9" who="TIMOTHEUS">
            <u><w>Hallihallo</w><pc>,</pc><w>Tom</w><pc>.</pc></u>
         </annotationBlock>
         <annotationBlock from="#B_10" to="#E_10" who="TOM">
            <u><w>Ich</w><w>fang</w><w>einfach</w><w>mal</w><w>an</w><w>zu</w><w>reden</w><pc>.</pc></u>
         </annotationBlock>
         <annotationBlock from="#B_11" to="#E_11" who="TOM">
            <u><w>Aber</w><w>so</w><w>richtig</w><w>weiß</w><w>ich</w><w>nicht</w><pc>,</pc><w>was</w><pc>.</pc></u>
         </annotationBlock>
         <incident end="#E_12" start="#B_12">
            <desc>winkt</desc>
         </incident>
         <annotationBlock from="#B_12" to="#E_12" who="TOM">
            <u><w>Hallo</w><pc>,</pc><w>Tim</w><pc>!</pc></u>
         </annotationBlock>
         <incident end="#E_13" start="#B_13">
            <desc>winkt</desc>
         </incident>
         <annotationBlock from="#B_13" to="#E_13" who="TIM">
            <u><w>Hallo</w><pc>,</pc><w>Tom</w><pc>.</pc></u>
         </annotationBlock>
         <annotationBlock from="#B_14" to="#E_14" who="KIM">
            <u><!--This node was not parsed, as it contains mixed content.-->
               <incident end="#E_14" start="#B_14">
                  <desc>incomprehensible</desc>
               </incident>
            </u>
         </annotationBlock>
         <incident end="#E_15" start="#B_15">
            <desc>nods</desc>
         </incident>
         <annotationBlock from="#B_16" to="#E_16" who="TIM">
            <u><w dur="2 syl" type="incomprehensible"/><w dur="1 syl" type="incomprehensible"/><pc>!</pc></u>
         </annotationBlock>
         <annotationBlock from="#B_17" to="#E_17" who="TOM">
            <u><w dur="2 syl" type="incomprehensible"/><w dur="1 syl" type="incomprehensible"/><pc>!</pc></u>
         </annotationBlock>
         <annotationBlock from="#B_18" to="#E_18" who="TIM">
            <u><unclear><w>Hallo</w></unclear><w>Tim</w><pc>!</pc></u>
         </annotationBlock>
         <annotationBlock from="#B_19" to="#E_19" who="TOM">
            <u><w dur="2 syl" type="incomprehensible"/><unclear><w>Tom</w><pc>!</pc></unclear></u>
         </annotationBlock>
         <annotationBlock from="#B_20" to="#E_20" who="TIM">
            <u><w>Hallo</w><pause type="long"/><w>Wim</w><pc>!</pc></u>
         </annotationBlock>
         <incident end="#E_21" start="#B_21">
            <desc>winkt</desc>
         </incident>
         <annotationBlock from="#B_21" to="#E_21" who="TOM">
            <u><w>Hallo</w><pc>,</pc><w>Tim</w><pc>!</pc></u>
            <spanGrp>
               <span from="#B_21" to="#E_21" type="comment">Salut, Tim!</span>
            </spanGrp>
         </annotationBlock>
         <incident end="#E_22" start="#B_22">
            <desc>winkt</desc>
         </incident>
         <annotationBlock from="#B_22" to="#E_22" who="TIM">
            <u><w>Hallo</w><pc>,</pc><w>Tom</w><pc>.</pc></u>
            <spanGrp>
               <span from="#B_22" to="#E_22" type="comment">Salut, Tom!</span>
            </spanGrp>
         </annotationBlock>
         <incident end="#E_23" start="#B_23">
            <desc>winkt</desc>
         </incident>
         <annotationBlock from="#B_23" to="#ME_1" who="TOM">
            <u><w>Hallo</w><pc>,</pc><w><anchor synch="#M_1"/>Tim</w><pc>!</pc></u>
            <spanGrp>
               <span from="#B_23" to="#ME_1" type="comment">Salut, Tim!</span>
            </spanGrp>
         </annotationBlock>
         <incident end="#E_24" start="#B_24">
            <desc>winkt</desc>
         </incident>
         <annotationBlock from="M_1" to="#E_24" who="TIM">
            <u><w>Hallo<anchor synch="#ME_1"/></w><pc>,</pc><w>Tom</w><pc>.</pc></u>
            <spanGrp>
               <span from="#M_1" to="#E_24" type="comment">Salut, Tom!</span>
            </spanGrp>
         </annotationBlock>
      </body>
   </text>
</TEI>
```
