package de.ids.mannheim.clarin.teispeech.data;

import net.sf.saxon.BasicTransformerFactory;
import org.jdom2.Namespace;
import org.korpora.useful.LangUtilities;
import org.korpora.useful.Utilities;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.ids.mannheim.clarin.teispeech.data.NameSpaces.TEI_NS;
import static de.ids.mannheim.clarin.teispeech.data.NameSpaces.XML_NS;

public class DocUtilities {

   // private final static Logger LOGGER = LoggerFactory
   //         .getLogger(DocUtilities.class.getName());

    // TODO: Allow exponential notation?
    // TODO: What does "PT" etc. mean?
    // TODO: Treat other measurements (ms, min, h)?
    private static final Pattern TIME_PATTERN = Pattern
            .compile("(?i)P?T?([0-9.]+)s?");

    /**
     * get language of element, ascending the element tree recursively.
     *
     * @param el
     *            the DOM XML Element
     * @param maxComponents
     *            the maximal number of language + locale components
     * @return Optional containing the language code, or empty
     */
    private static Optional<String> getLanguage(Element el, int maxComponents) {
        String lang = el.getAttribute("xml:lang");
        for (Node parent = el.getParentNode(); lang.isEmpty() && parent != null
                && parent.getNodeType() == Node.ELEMENT_NODE; parent = parent
                        .getParentNode()) {
            lang = ((Element) parent).getAttribute("xml:lang");
        }
        if (!lang.isEmpty()) {
            return LangUtilities.getLanguageLocale(lang, maxComponents);
        } else {
            return Optional.empty();
        }
    }

    /**
     * get language of element with default
     *
     * @param el
     *            the DOM XML Element
     * @param defaultL
     *            the default language
     * @param maxComponents
     *            the maximal number of language + locale components
     * @return the corresponding three letter ISO 639-2 language code
     */
    static String getLanguage(Element el, String defaultL,
            int maxComponents) {
        Optional<String> ret = getLanguage(el, maxComponents);
        return ret.orElse(defaultL);
    }

    /**
     * the language of an utterance {@code <u>} is the majority language of its
     * {@code <w>}s or the language of the element.
     *
     * @param el
     *            the utterance element
     * @param defaultL
     *            the default language
     * @param maxComponents
     *            the maximal number of language + locale components
     * @return the determined language
     */
    private static String getUtteranceLanguage(Element el, String defaultL,
                                               int maxComponents) {
        assert "u".equals(el.getTagName());
        String lang = getLanguage(el, defaultL, maxComponents);
        Map<String, Long> freq = Utilities
                .toElementStream(
                        el.getElementsByTagNameNS(TEI_NS, "w"))
                .collect(Collectors.groupingBy(w -> {
                    // words without language tag are counted as having the
                    // language of the {@code <u>}
                    return w.hasAttribute("xml:lang")
                            ? w.getAttribute("xml:lang")
                            : lang;
                }, Collectors.counting()));
        Optional<Entry<String, Long>> maxLang = freq.entrySet().stream()
                .max(Comparator.comparing(Entry::getValue));
        if (maxLang.isPresent()) {
            return maxLang.get().getKey();
        } else {
            return lang;
        }
    }

    /**
     * select elements from {@code doc} by {@code tagName}
     *
     * @param tagName
     *            tag name of the elements
     * @param doc
     *            XML document
     * @param defaultL
     *            default language
     * @param maxComponents
     *            the maximal number of language + locale components
     * @return the elements grouped by language
     */
    public static Map<String, List<Element>> groupByLanguage(String tagName,
            Document doc, String defaultL, int maxComponents) {
        return Utilities
                .toStream(
                        doc.getElementsByTagNameNS(TEI_NS, tagName))
                .map(u -> (Element) u)
                .collect(Collectors.groupingBy(
                        u -> getUtteranceLanguage(u, defaultL, maxComponents),
                        Collectors.toList()));
    }

    static String getTextOrNorm(Element el) {
        if (el.hasAttributeNS(TEI_NS, "norm")) {
            return el.getAttribute("norm");
        } else {
            return el.getTextContent();
        }
    }

    /**
     * add change to {@code <revisionDesc>} in TEI document
     *
     * @param doc
     *            the DOM Document
     * @param change
     *            the change message
     */
    public static void makeChange(Document doc, String change) {
        String stamp = ZonedDateTime.now(ZoneOffset.systemDefault())
                .format(DateTimeFormatter.ISO_INSTANT);
        Element revDesc = Utilities.getElementByTagNameNS(
                doc.getDocumentElement(), TEI_NS, "revisionDesc");
        if (revDesc == null) {
            revDesc = doc.createElementNS(TEI_NS, "revisionDesc");
            Element eDe = Utilities.getElementByTagNameNS(
                    doc.getDocumentElement(), TEI_NS,
                    "encodingDesc");
            if (eDe == null) {
                eDe = doc.createElementNS(TEI_NS, "encodingDesc");
                Element header = Utilities.getElementByTagNameNS(
                        doc.getDocumentElement(), TEI_NS,
                        "teiHeader");
                if (header == null) {
                    header = doc.createElementNS(TEI_NS,
                            "teiHeader");
                    Utilities.insertAtBeginningOf(header,
                            doc.getDocumentElement());
                }
                Utilities.insertAtBeginningOf(eDe, header);
            }
            Utilities.insertAtBeginningOf(revDesc, eDe);
        }
        Element changeEl = doc.createElementNS(TEI_NS, "change");
        changeEl.setAttribute("when", stamp);
        changeEl.appendChild(doc.createTextNode(change));
        Utilities.insertAtBeginningOf(changeEl, revDesc);
    }

    /**
     * add change to {@code <revisionDesc>} in TEI document
     *
     * @param doc
     *            the JDOM Document
     * @param change
     *            the change message
     */
    public static void makeChange(org.jdom2.Document doc,
                                  String change) {
        Namespace TEI_NS = Namespace.getNamespace(NameSpaces.TEI_NS);
        String stamp = ZonedDateTime.now(ZoneOffset.systemDefault())
                .format(DateTimeFormatter.ISO_INSTANT);
        org.jdom2.Element revDesc = Utilities.getElementByTagName(
                doc.getRootElement(), "revisionDesc", TEI_NS);
        if (revDesc == null) {
            revDesc = new org.jdom2.Element("transcriptionDesc", TEI_NS);
            org.jdom2.Element eDe = Utilities.getElementByTagName(
                    doc.getRootElement(), "encodingDesc", TEI_NS);
            if (eDe == null) {
                org.jdom2.Element header = Utilities.getElementByTagName(
                        doc.getRootElement(), "teiHeader", TEI_NS);
                if (header == null) {
                    header = new org.jdom2.Element("teiHeader", TEI_NS);
                    doc.getRootElement().addContent(0, header);
                } else {
                    eDe = new org.jdom2.Element("encodingDesc", TEI_NS);
                    header.addContent(eDe);
                }
            }
            org.jdom2.Element changeEl = new org.jdom2.Element("change",
                    NameSpaces.TEI_NS);
            changeEl.setAttribute("when", stamp);
            changeEl.addContent(new org.jdom2.Text(change));
            revDesc.addContent(changeEl);
        }
    }

    /**
     *
     * add change to {@code <revisionDesc>} in TEI document, name processed and
     * skipped languages
     *
     * @param doc
     *            the document
     * @param change
     *            the change info
     * @param processedLanguages
     *            the languages that could be processed
     * @param skippedLanguages
     *            the languages which had to be skipped
     */
    public static void makeChange(Document doc, String change,
                                  List<String> processedLanguages, List<String> skippedLanguages) {
        String message = change + "; "
                + (processedLanguages.size() > 0
                        ? "processed " + String.join("/", processedLanguages)
                                + ", "
                        : "")
                + (skippedLanguages.size() > 0
                        ? "skipped " + String.join("/", skippedLanguages)
                        : "")
                + ".";
        makeChange(doc, message);
    }

    /**
     * Add a comment at the beginning of a DOM doc's {@code <body>}
     *
     * @param doc
     *            the document
     * @param commentText
     *            the text of the comment
     * @return the document, for chaining
     */
    public static Document addComment(Document doc, String commentText) {
        Element body = (Element) doc.getElementsByTagNameNS(TEI_NS,
                "body");
        Comment comment = doc.createComment(commentText);
        body.getParentNode().insertBefore(comment, body);
        return doc;
    }

    /**
     * test for TEI element, to save typing
     *
     * @param el
     *            Element
     * @param localName
     *            name of the element to test for
     * @return whether el is tei:&lt;localName&gt;
     */
    public static boolean isTEI(Element el, String localName) {
        return (el.getNamespaceURI().equals(TEI_NS)
                && el.getLocalName().equals(localName));
    }

    private static String getAtt(Element el, String nameSpace, String localName) {
        assert el != null;
        assert localName != null;
        String attNS = el.getAttributeNS(nameSpace, localName);
        assert attNS != null;
        return "".equals(attNS) ? el.getAttribute(localName) : attNS;
    }

    /**
     * try to get a TEI attribute, and a namespaceless attribute else
     *
     * @param el
     *            the Element supposed to carry the attribute
     * @param localName
     *            the local name of the attribute
     * @return the value of the attribute, or ""
     */
    public static String getAttTEI(Element el, String localName) {
        return getAtt(el, TEI_NS, localName);
    }

    public static String getAttXML(Element el, String localName) {
        return getAtt(el, XML_NS, localName);
    }

    /**
     * process time, as floating point number
     *
     * @param measurement
     *            e.g. "PT12.2s"
     * @return an optional number, e.g. 12.2d or empty()
     */
    private static Optional<Double> getDuration(String measurement) {
        Matcher matcher = TIME_PATTERN.matcher(measurement);
        if (matcher.matches()) {
            return Optional.of(Double.parseDouble(matcher.group(1)));
        } else {
            return Optional.empty();
        }
    }

    /**
     * get time from XML element, see {@link #getDuration(String)}
     *
     * @param el
     *            an XML element, potentially with a {@code dur} attribute
     * @return optional number representing duration
     */
    public static Optional<Double> getDuration(Element el) {
        return getDuration(el.getAttribute("dur"));
    }

    /**
     * get the tei:timeline from a document
     *
     * @param doc
     *            the document
     * @return the time line
     */
    public static Element getTimeLine(Document doc) {
        Element timeLine = Utilities.getElementByTagNameNS(doc,
                TEI_NS, "timeline");
        if (timeLine == null) {
            throw new RuntimeException(
                    "Cannot process document with no time line!");
        }
        return timeLine;
    }

    public static NodeList getWhens(Document doc) {
        Element timeLine = getTimeLine(doc);
        return getWhens(timeLine);
    }

    private static NodeList getWhens(Element timeLine) {
        NodeList line = timeLine.getElementsByTagNameNS(TEI_NS,
                "when");
        if (line == null || line.getLength() == 0) {
            throw new RuntimeException(
                    "Cannot process document with no time line elements!");
        }
        return line;
    }


    /**
     * get the root Element of the time line, i.e. the first event
     *
     * @param doc
     *            the DOM document
     * @return the first event
     */
    private static Element getTimeRoot(Document doc) {
        NodeList whens = getWhens(doc);
        return (Element) whens.item(0);
    }

    /**
     * remove pound mark (\#) from beginning of String
     *
     * @param id
     *            the String potentially starting with a pound mark
     *
     * @return the cleaned-up String
     */
    public static String unPoundMark(String id) {
        if (id.startsWith("#")) {
            id = id.substring(1);
        }
        return id;
    }

    /**
     * generate new ID following a pattern by adding a number
     *
     * @param doc
     *            the document wherein the ID must be unique
     * @param pattern
     *            the ID follows
     * @return the ID
     */
    private static String generateID(Document doc, String pattern) {
        int i = 0;
        String newId;
        do {
            i++;
            newId = String.format("%s_%d", pattern, i);
        } while (Utilities.getElementByID(doc, newId) != null);
        return newId;
    }

    /**
     * set new ID following a pattern by adding a number
     *
     * @param el
     *            the element to be identified
     * @param pattern
     *            the pattern
     * @return the new ID
     */
    public static String setNewId(Element el, String pattern) {
        String newId = generateID(el.getOwnerDocument(), pattern);
        el.setAttribute("xml:id", newId);
        return newId;
    }

    /**
     * get the time offset of an Element
     *
     * @param el
     *            the Element
     * @return the time offset
     */
    public static Optional<Double> getOffset(Element el) {
        return getOffset(el.getOwnerDocument(),
                el.getAttributeNS(XML_NS, "id"));
    }

    /**
     * get time offset for an ID referring to a timeLine element
     *
     * @param doc
     *            the XML document
     * @param id
     *            the XML ID
     * @return the time offset
     */
    private static Optional<Double> getOffset(Document doc, String id) {
        id = unPoundMark(id);
        Element root = getTimeRoot(doc);
        String rootID = root.getAttributeNS(XML_NS, "id");
        Optional<Double> ret;
        if (id.equals(rootID)) {
            ret = Optional.of(0d);
        } else {
            Element el = Utilities.getElementByID(doc, id);
            Optional<Double> elTime = getDuration(el.getAttribute("interval"));
            String refID = unPoundMark(el.getAttribute("since"));
            Optional<Double> offSet = Optional.empty();
            if (!rootID.equals(refID) && refID.length() > 0) {
                offSet = getOffset(doc, refID);
            }
            ret = elTime;
            if (offSet.isPresent() && elTime.isPresent()) {
                ret = Optional.of(ret.get() + elTime.get());
            }
        }
        return ret;
    }

    private static final DocumentBuilderFactory dbf;
    private static final DocumentBuilder db;
    static {
        dbf = DocumentBuilderFactory.newInstance() ;
        try {
            db = dbf.newDocumentBuilder() ;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("No DocumentBuilder – YOUR JAVA IS VERY BROKEN!");
        }
    }
    private static final TransformerFactory stf = new BasicTransformerFactory();

    private static Templates getTemplate(String path){
        try {
            return stf.newTemplates(
                    new StreamSource(DocUtilities.class.getResourceAsStream(path)));
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(String.format("XSLT broken: «%s»", path));
        }
    }

    public static Document transform(String path, Document inDoc) {
        return transform(getTemplate(path), inDoc);
    }

    private static Document transform(Templates template, Document inDoc){
        try {
            Document doc = db.newDocument() ;
            DOMResult result = new DOMResult(doc);
            DOMSource source = new DOMSource(inDoc);
            template.newTransformer().transform(source, result);
            return doc;
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

}
