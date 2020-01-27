package de.ids.mannheim.clarin.teispeech.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.Text;
import org.jdom2.filter.ElementFilter;
import org.jdom2.transform.XSLTransformer;
import org.jdom2.util.IteratorIterable;
import org.jdom2.xpath.XPathFactory;
import org.korpora.useful.Utilities;

/**
 * Parser for cGAT transcription
 *
 * @author Thomas Schmidt
 *
 * slightly adjusted by Bernhard Fisseni
 *
 */
public class GATParser extends AbstractParser {

    private static final XPathFactory xpf = XPathFactory.instance();

    // String PATTERNS_FILE_PATH = "/org/exmaralda/folker/data/Patterns.xml";
    private static final String PATTERNS_FILE_PATH = "Patterns.xml";
    private static final Namespace TEI_NS = Namespace
            .getNamespace(NameSpaces.TEI_NS);

//    private final static Logger LOGGER = LoggerFactory
//            .getLogger(GATParser.class.getName());
    private Map<String, Pattern> minimalPatterns;
    private static final String MINIMAL_TRANSFORMER_FILE_PATH = "transformcontribution.xsl";
    // String MINIMAL_TRANSFORMER_FILE_PATH =
    // "/org/exmaralda/folker/data/transformcontribution.xsl";
    private XSLTransformer minimalTransformer;

    private Map<String, Pattern> basicPatterns;
    private static final String BASIC_TRANSFORMER_FILE_PATH = "transformcontribution_basic.xsl";
    // String BASIC_TRANSFORMER_FILE_PATH =
    // "/org/exmaralda/folker/data/transformcontribution_basic.xsl";
    private XSLTransformer basicTransformer;

    public GATParser() throws JDOMException, IOException {
        this("universal");
        System.setProperty("javax.xml.transform.TransformerFactory",
                "net.sf.saxon.TransformerFactoryImpl");
    }

    private GATParser(
            @SuppressWarnings("SameParameterValue") String languageCode)
            throws JDOMException, IOException {

        PatternReader pr = new PatternReader(GATParser.class.getClassLoader()
                .getResourceAsStream(PATTERNS_FILE_PATH));
        minimalPatterns = pr.getAllPatterns(2, languageCode);
        basicPatterns = pr.getAllPatterns(3, languageCode);

        minimalTransformer = new XSLTransformer(
                Utilities.parseXMLviaJDOM(GATParser.class.getClassLoader()
                        .getResourceAsStream(MINIMAL_TRANSFORMER_FILE_PATH)));

        basicTransformer = new XSLTransformer(
                Utilities.parseXMLviaJDOM(GATParser.class.getClassLoader()
                        .getResourceAsStream(BASIC_TRANSFORMER_FILE_PATH)));

    }

    /**
     * set level and version at appropriate place in &lt;teiHeader&gt;
     *
     * @param doc
     *     JDOM document
     * @param level
     *     the level
     * @param version
     *     the version
     */
    private void setLevel(Document doc, String level,
            @SuppressWarnings("SameParameterValue") String version) {
        Element transDesc = Utilities.getElementByTagName(doc.getRootElement(),
                "transcriptionDesc", TEI_NS);
        if (transDesc == null) {
            transDesc = new Element("transcriptionDesc", TEI_NS);
            // insert after appInfo
            Element ai = Utilities.getElementByTagName(doc.getRootElement(),
                    "appInfo", TEI_NS);
            if (ai != null) {
                int pos = ai.getParent().indexOf(ai);
                ai.getParent().addContent(pos + 1, transDesc);
                // or in encodingDesc
            } else {
                Element eDe = Utilities.getElementByTagName(
                        doc.getRootElement(), "encodingDesc", TEI_NS);
                if (eDe == null) {
                    Element header = Utilities.getElementByTagName(
                            doc.getRootElement(), "teiHeader", TEI_NS);
                    if (header == null) {
                        header = new Element("teiHeader", TEI_NS);
                        doc.getRootElement().addContent(0, header);
                    } else {
                        eDe = new Element("encodingDesc", TEI_NS);
                        header.addContent(eDe);
                    }
                }
                Objects.requireNonNull(eDe).addContent(0, transDesc);
            }
        }
        transDesc.setAttribute("ident", level, null);
        if (version == null) {
            transDesc.removeAttribute("version", null);
        } else {
            transDesc.setAttribute("version", version, null);
        }
    }

    @Override
    public void parseDocument(Document doc, int parseLevel) {
        if (parseLevel == 0) {
            return;
        }

        if (parseLevel == 1) {
            setLevel(doc, "cGAT minimal", "1.0");
            IteratorIterable<Element> contributionIterator = doc
                    .getDescendants(new ElementFilter("contribution"));
            List<Element> contributions = new ArrayList<>();
            while (contributionIterator.hasNext()) {
                Element con = (contributionIterator.next());
                contributions.add(con);
            }
            for (Element e : contributions) {
                boolean isOrdered = true;
                List<Element> l = e.getChildren("segment");
                String lastEnd = null;
                for (Element ev : l) {
                    if ((lastEnd == null)
                            || (ev.getAttributeValue("start-reference")
                                    .equals(lastEnd))) {
                        // everything ok
                        lastEnd = ev.getAttributeValue("end-reference");
                    } else {
                        isOrdered = false;
                        break;
                    }
                }
                if (isOrdered) {
                    List<Element> l2 = e
                            .removeContent(new ElementFilter("segment"));
                    Element unparsed = new Element("unparsed");
                    for (Object o : l2) {
                        Element ev = (Element) o;
                        unparsed.addContent(ev.getText());
                        if (l2.indexOf(o) < l2.size() - 1) {
                            // do not insert a timepoint for the last segment
                            Element timepoint = new Element("time", TEI_NS);
                            timepoint.setAttribute("timepoint-reference",
                                    ev.getAttributeValue("end-reference"),
                                    null);
                            unparsed.addContent(timepoint);
                        }
                    }
                    e.addContent(unparsed);
                    e.setAttribute("parse-level", "1");
                }
            }
        } else if (parseLevel == 2) {
            setLevel(doc, "cGAT basic", "1.0");
            IteratorIterable<Element> unparsedIterator = doc
                    .getDescendants(new ElementFilter("u"));
            List<Element> unparseds = new ArrayList<>();
            while (unparsedIterator.hasNext()) {
                Element up = unparsedIterator.next();
                if (!up.getTextTrim().isEmpty()) {
                    unparseds.add(up);
                }
            }
            for (Element unparsed : unparseds) {

                List<PositionTimeMapping> timePositions = new ArrayList<>();
                StringBuilder text = new StringBuilder();
                boolean totalParseOK = true;
                for (Content c : unparsed.getContent()) {
                    if (c instanceof Text) {
                        String eventText = ((Text) c).getText();
                        // System.err.println("ETEXT: " + eventText);
                        if (!(minimalPatterns.get("GAT_EVENT")
                                .matcher(eventText).matches())) {
                            // System.err.println(String.format(
                            // "EVENT DID NOT MATCH: «%s»", eventText));
                            totalParseOK = false;
                            break;
                        }
                        // System.err.println("MATCHED!");
                        text.append(eventText);
                    } else {
                        Element e = (Element) c;
                        String timeID = e.getAttributeValue("synch");
                        timePositions.add(
                                new PositionTimeMapping(text.length(), timeID));
                    }
                }
                totalParseOK = totalParseOK
                        && (minimalPatterns.get("GAT_CONTRIBUTION")
                                .matcher(text.toString()).matches());
                if (!totalParseOK) {
                    // System.err.println(
                    // "TOTAL PARSE FAILED: " + unparsed.getText());
                    continue;
                }
                try {
                    text = new StringBuilder(parseText(text.toString(),
                            "GAT_NON_PHO", minimalPatterns));
                    text = new StringBuilder(parseText(text.toString(),
                            "GAT_PAUSE", minimalPatterns));
                    text = new StringBuilder(parseText(text.toString(),
                            "GAT_BREATHE", minimalPatterns));
                    text = new StringBuilder(parseText(text.toString(),
                            "GAT_UNCERTAIN", minimalPatterns));
                    // removed on 06-03-2009
                    // text = parseText(text, "GAT_UNINTELLIGIBLE");
                    text = new StringBuilder(parseText(text.toString(),
                            "GAT_WORD", minimalPatterns));
                    text = new StringBuilder(parseText(text.toString(),
                            "GAT_WORDBOUNDARY", minimalPatterns));

                    // Element contribution = unparsed.getParentElement();
                    Utilities.replaceContentWithParse(unparsed,
                            text.toString());

                    List<Element> l = unparsed.getChildren("GAT_UNCERTAIN"); // drin
                                                                             // lassen
                    List<Element> uncertains = new ArrayList<>();
                    for (Object o : l) {
                        Element uc = (Element) (o);
                        uncertains.add(uc);
                    }
                    for (Element uc : uncertains) {
                        String ucText = uc.getText();
                        ucText = parseText(ucText, "GAT_ALTERNATIVE", // nicht
                                minimalPatterns);
                        ucText = parseText(ucText, "GAT_WORD", minimalPatterns); // anpassen
                                                                                 // auf
                                                                                 // Buchstaben;
                                                                                 // Satzzeichen
                                                                                 // als
                                                                                 // ISO-<punctuation>
                        ucText = parseText(ucText, "GAT_WORDBOUNDARY", // (egal?)
                                minimalPatterns);
                        Utilities.replaceContentWithParse(uc, ucText);
                    }

                    IteratorIterable<Element> i2 = unparsed.getDescendants(
                            new ElementFilter("GAT_ALTERNATIVE"));
                    List<Element> alternatives = new ArrayList<>();
                    while (i2.hasNext()) {
                        Element al = (i2.next());
                        alternatives.add(al);
                    }
                    for (Element al : alternatives) {
                        String alText = al.getText();
                        alText = parseText(alText, "GAT_WORD", minimalPatterns);
                        alText = parseText(alText, "GAT_WORDBOUNDARY",
                                minimalPatterns);
                        Utilities.replaceContentWithParse(al, alText);
                    }
//                    contribution.setAttribute("parse-level", "2");
                    insertTimeReferences(unparsed, timePositions);
                    List<Content> v = new ArrayList<>();
                    v.add(unparsed);
                    // System.err.println(Utilities.elementToString(contribution));
                    List<Content> transformedContribution = minimalTransformer
                            .transform(v);
//                    Element contributionParent = contribution
//                            .getParentElement();
                    unparsed.setContent(transformedContribution);
                } catch (IOException | JDOMException ex) {
                    ex.printStackTrace();
                }
            }
        } else if (parseLevel == 3) {
            // TODO
            // parseDocument(doc, 2);
            IteratorIterable<Element> unparsedIterator = doc
                    .getDescendants(new ElementFilter("unparsed"));
            List<Element> unparseds = new ArrayList<>();
            while (unparsedIterator.hasNext()) {
                Element up = (unparsedIterator.next());
                unparseds.add(up);
            }
            for (Element unparsed : unparseds) {
                List<PositionTimeMapping> timePositions = new ArrayList<>();
                StringBuilder text = new StringBuilder();
                boolean totalParseOK = true;
                for (Object c : unparsed.getContent()) {
                    if (c instanceof Text) {
                        String eventText = ((Text) c).getText();
                        if (!(basicPatterns.get("GAT_EVENT").matcher(eventText)
                                .matches())) {
                            totalParseOK = false;
                            break;
                        }
                        text.append(eventText);
                    } else {
                        Element e = (Element) c;
                        String timeID = e
                                .getAttributeValue("timepoint-reference");
                        timePositions.add(
                                new PositionTimeMapping(text.length(), timeID));
                    }
                }
                if (unparsed.getParentElement()
                        .getAttribute("speaker-reference") != null) {
                    // totalParseOK = totalParseOK &&
                    // (text.matches(basicPatterns.get("GAT_CONTRIBUTION")));
                    // changed 28-03-2012 replace empty boundaries with pipe
                    // symbol boundary
                    totalParseOK = totalParseOK
                            && ((basicPatterns.get("GAT_CONTRIBUTION")
                                    .matcher(text.toString()).matches())
                                    || basicPatterns.get("GAT_CONTRIBUTION")
                                            .matcher(basicPatterns
                                                    .get("GAT_EMPTY_BOUNDARY")
                                                    .matcher(text.toString())
                                                    .replaceAll("| "))
                                            .matches());
                } else {
                    totalParseOK = totalParseOK
                            && (basicPatterns.get("GAT_NO_SPEAKER_CONTRIBUTION")
                                    .matcher(text.toString()).matches());
                }
                if (!totalParseOK) {
                    // System.out.println("TOTAL PARSE FAILED");
                    continue;
                }
                try {

                    // make sure angle brackets do not interfere with the XML
                    // parsing
                    text = new StringBuilder(
                            text.toString().replaceAll("<", "\u2329")
                                    .replaceAll(">", "\u232A"));

                    text = new StringBuilder(parseText(text.toString(),
                            "GAT_PSEUDO_PHRASE_BOUNDARY", basicPatterns));

                    text = new StringBuilder(parseText(text.toString(),
                            "GAT_NON_PHO", basicPatterns));
                    text = new StringBuilder(parseText(text.toString(),
                            "GAT_PAUSE", basicPatterns));
                    text = new StringBuilder(parseText(text.toString(),
                            "GAT_BREATHE", basicPatterns));

                    // patterns specific to basic transcription
                    text = new StringBuilder(parseText(text.toString(),
                            "GAT_COMMENT_START_ESCAPED", basicPatterns));
                    text = new StringBuilder(parseText(text.toString(),
                            "GAT_COMMENT_END_ESCAPED", basicPatterns));
                    text = new StringBuilder(parseText(text.toString(),
                            "GAT_PHRASE_BOUNDARY", basicPatterns));
                    text = new StringBuilder(parseText(text.toString(),
                            "GAT_LATCHING", basicPatterns));
                    // end patterns specific to basic transcription

                    text = new StringBuilder(parseText(text.toString(),
                            "GAT_UNCERTAIN", basicPatterns));
                    // removed on 06-03-2009
                    // text = parseText(text, "GAT_UNINTELLIGIBLE");

                    text = new StringBuilder(parseText(text.toString(),
                            "GAT_WORD", basicPatterns));
                    text = new StringBuilder(parseText(text.toString(),
                            "GAT_WORDBOUNDARY", basicPatterns));

                    Element contribution = unparsed.getParentElement();
                    Utilities.replaceContentWithParse(contribution,
                            text.toString());

                    List<Element> l = contribution.getChildren("GAT_UNCERTAIN");
                    List<Element> uncertains = new ArrayList<>();
                    for (Object o : l) {
                        Element uc = (Element) (o);
                        uncertains.add(uc);
                    }
                    for (Element uc : uncertains) {
                        String ucText = uc.getText();
                        ucText = parseText(ucText, "GAT_ALTERNATIVE",
                                basicPatterns);
                        ucText = parseText(ucText, "GAT_WORD", basicPatterns);
                        ucText = parseText(ucText, "GAT_WORDBOUNDARY",
                                basicPatterns);
                        Utilities.replaceContentWithParse(uc, ucText);
                    }

                    IteratorIterable<Element> i2 = contribution.getDescendants(
                            new ElementFilter("GAT_ALTERNATIVE"));
                    List<Element> alternatives = new ArrayList<>();
                    while (i2.hasNext()) {
                        Element al = (i2.next());
                        alternatives.add(al);
                    }
                    for (Element al : alternatives) {
                        String alText = al.getText();
                        alText = parseText(alText, "GAT_WORD", basicPatterns);
                        alText = parseText(alText, "GAT_WORDBOUNDARY",
                                basicPatterns);
                        Utilities.replaceContentWithParse(al, alText);
                    }

                    // take care of accent markup and lengthening...
                    IteratorIterable<Element> i3 = contribution
                            .getDescendants(new ElementFilter("GAT_WORD"));
                    List<Element> words = new ArrayList<>();
                    while (i3.hasNext()) {
                        Element w = (i3.next());
                        words.add(w);
                    }
                    for (Element w : words) {
                        String wText = w.getText();
                        wText = parseText(wText, "GAT_STRONG_ACCENT_SYLLABLE",
                                basicPatterns);
                        wText = parseText(wText, "GAT_ACCENT_SYLLABLE",
                                basicPatterns);
                        wText = parseText(wText, "GAT_LENGTHENING",
                                basicPatterns);
                        Utilities.replaceContentWithParse(w, wText);
                    }
                    // ... and of lengthening inside accent syllables
                    IteratorIterable<? extends Content> i4 = contribution
                            .getDescendants(
                                    new ElementFilter("GAT_ACCENT_SYLLABLE")
                                            .or(new ElementFilter(
                                                    "GAT_STRONG_ACCENT_SYLLABLE")));
                    List<Element> syllables = new ArrayList<>();
                    while (i4.hasNext()) {
                        Element s = (Element) (i4.next());
                        syllables.add(s);
                    }
                    for (Element s : syllables) {
                        String sText = s.getText();
                        sText = parseText(sText, "GAT_LENGTHENING",
                                basicPatterns);
                        List<Content> newContent4 = Utilities
                                .readJDOMFromString("<X>" + sText + "</X>")
                                .getRootElement().removeContent();
                        s.removeContent();
                        s.setContent(newContent4);
                    }

//                    contribution.setAttribute("parse-level", "3");
                    insertTimeReferences(contribution, timePositions);

                    // transform the pseudo markup into the target markup...
                    List<Content> v = new ArrayList<>();
                    v.add(contribution);
                    Element transformedContribution = (Element) (basicTransformer
                            .transform(v).get(0));

                    // ... hierarchize it...
                    List<Content> content = transformedContribution
                            .removeContent();
                    Element currentLine = new Element("line");
                    for (Object c : content) {
                        Element e = (Element) c;
                        currentLine.addContent(e);
                        if ("boundary".equals(e.getName()) && "final"
                                .equals(e.getAttributeValue("type"))) {
                            transformedContribution.addContent(currentLine);
                            currentLine = new Element("line");
                        }
                    }
                    // this is for speakerless contributions which do not have
                    // to end with a boundary
                    if (currentLine.getContentSize() > 0) {
                        transformedContribution.addContent(currentLine);
                    }

                    // ... and put it in the right place in the document
                    Element contributionParent = contribution
                            .getParentElement();
                    contributionParent.setContent(
                            contributionParent.indexOf(contribution),
                            transformedContribution);
                } catch (IOException | JDOMException ex) {
                    ex.printStackTrace();
                }
            }
        }
        System.err.format("FINISH (%d)!\n", parseLevel);
        DocUtilities.makeChange(doc,
                String.format("parsed for cGAT level %d.", parseLevel));
    }

    private String parseText(String text, String patternName,
            Map<String, Pattern> patterns) throws JDOMException, IOException {
        String docString = "<X>" + text + "</X>";
        // System.out.println("=== " + docString);
        Element e = Utilities.readJDOMFromString(docString).getRootElement();
        StringBuilder returnText = new StringBuilder();
        for (Object o : e.getContent()) {
            if (!(o instanceof Text)) {
                returnText.append(Utilities.elementToString((Element) o));
                continue;
            }
            Pattern p = patterns.get(patternName);
            String thisText = ((Text) o).getText();
            Matcher m = p.matcher(thisText);
            int fromWhere = 0;
            while (m.find(fromWhere)) {
                int i1 = m.start();
                int i2 = m.end();
                thisText = thisText.substring(0, i1) + "<" + patternName + ">"
                        + thisText.substring(i1, i2) + "</" + patternName + ">"
                        + thisText.substring(i2);
                m = p.matcher(thisText);
                fromWhere = i2 + 2 * patternName.length() + 5;
            }
            returnText.append(thisText);
        }
        return returnText.toString();
    }

    private void insertTimeReferences(Element contribution,
            List<PositionTimeMapping> timePositions) {
        /*
         * for (PositionTimeMapping ptm : timePositions){
         * System.out.println(ptm.position + " / " + ptm.timeID); }
         */
        IteratorIterable<Content> i = contribution.getDescendants();
        List<Text> texts = new ArrayList<>();
        while (i.hasNext()) {
            Object o = i.next();
            if (!(o instanceof Text)) {
                continue;
            }
            Text textElement = (Text) o;
            texts.add(textElement);
        }

        int timePositionCount = 0;
        int offsetCount = 0;
        for (Text textElement : texts) {
            List<PositionTimeMapping> localTimePositions = new ArrayList<>();
            if (timePositionCount >= timePositions.size()) {
                break;
            }
            int positionWanted = timePositions.get(timePositionCount).position;
            String text = textElement.getText();
            while ((positionWanted >= 0) && (offsetCount <= positionWanted)
                    && (offsetCount + text.length() >= positionWanted)) {
                localTimePositions.add(
                        new PositionTimeMapping(positionWanted - offsetCount,
                                timePositions.get(timePositionCount).timeID));
                timePositionCount++;
                if (timePositionCount < timePositions.size()) {
                    positionWanted = timePositions
                            .get(timePositionCount).position;
                } else {
                    positionWanted = -1;
                }
            }
            offsetCount += text.length();
            if (localTimePositions.size() > 0) {
                Element parent = textElement.getParentElement();
                int index = parent.indexOf(textElement);
                textElement.detach();
                List<Content> newContent = new ArrayList<>();
                int offsetCount2 = 0;
                for (PositionTimeMapping ptm : localTimePositions) {
                    Text t = new Text(
                            text.substring(offsetCount2, ptm.position));
                    newContent.add(t);
                    Element e = new Element("anchor", NameSpaces.TEI_NS);
                    e.setAttribute("synch", ptm.timeID);
                    newContent.add(e);
                    offsetCount2 = ptm.position;
                }
                if (offsetCount2 < text.length()) {
                    Text t = new Text(text.substring(offsetCount2));
                    newContent.add(t);
                }
                parent.addContent(index, newContent);
            }
        }
    }

    public boolean isFullyParsedOnLevel(Document doc, int level) {
        return (xpf.compile("//contribution[not(@parse-level='" + level + "')]",
                new ElementFilter()).evaluateFirst(doc) == null);
    }

}
