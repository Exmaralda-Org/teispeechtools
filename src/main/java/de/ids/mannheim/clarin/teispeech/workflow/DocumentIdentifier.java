package de.ids.mannheim.clarin.teispeech.workflow;

import net.sf.saxon.om.NameChecker;
import net.sf.saxon.xpath.XPathFactoryImpl;
import org.jdom2.JDOMException;
import org.korpora.useful.XMLUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static de.ids.mannheim.clarin.teispeech.data.NameSpaces.XML_NS;

/**
 * provide IDs für Elements that have none – and remove them
 */
@SuppressWarnings("WeakerAccess")
public class DocumentIdentifier {

    private final static String PREFIX = "CLARIN_ROUNDTRIP_ID";
    private final static Logger LOGGER = LoggerFactory
            .getLogger(DocumentIdentifier.class.getName());
    private final static XPathExpression ID_PATH;
    private final static XPathExpression NO_ID_PATH;
    private final static XPath XPATH = new XPathFactoryImpl().newXPath();

    // note that Java is confused about @xml:id
    static {
        try {
            ID_PATH = XPATH.compile(
                    "//*[@*[local-name() = 'id' and namespace-uri() = '"
                            + XML_NS + "']]");
            String unindentifiedXPath = "//*[not(@*[local-name() = 'id' and "
                    + "namespace-uri() = '" + XML_NS + "'])]";
            NO_ID_PATH = XPATH.compile(unindentifiedXPath);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    private final Document doc;
    private Set<String> IDs = new HashSet<>();
    private int lastId = 0;

    /**
     * a DocumentIdentifier for an XML document
     *
     * @param doc
     *         the DOM document
     */
    @SuppressWarnings("WeakerAccess")
    public DocumentIdentifier(Document doc) {
        this.doc = doc;
    }

    /**
     * add identifiers to a document
     *
     * @param doc
     *         the XML document (DOM)
     */
    public static void makeIDs(Document doc) {
        DocumentIdentifier di = new DocumentIdentifier(doc);
        di.makeIDs();
    }

    /**
     * add identifiers to a document
     *
     * @param jdoc
     *         the XML document (jDOM)
     * @return doc with identifiers DOM
     */
    private static Document makeIDs(org.jdom2.Document jdoc) {
        try {
            DocumentIdentifier di = new DocumentIdentifier(
                    XMLUtilities.convertJDOMToDOM(jdoc));
            return di.makeIDs().getDocument();
        } catch (JDOMException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * add identifiers to a document
     *
     * @param jdoc
     *         the XML document (jDOM)
     * @return doc with identifiers jDOM
     */
    public static org.jdom2.Document jmakeIDs(org.jdom2.Document jdoc) {
        return XMLUtilities.convertDOMtoJDOM(makeIDs(jdoc));
    }

    /**
     * remove identifiers from document
     *
     * @param doc
     *         the XML document (DOM)
     */
    public static void removeIDs(Document doc) {
        System.setProperty("javax.xml.transform.TransformerFactory",
                "net.sf.saxon.TransformerFactoryImpl");
        try {
            XPath xp = new XPathFactoryImpl().newXPath();
            NodeList identified = (NodeList) xp
                    .compile(
                            "//*[./@xml:id[starts-with(., '" + PREFIX + "')] ]")
                    .evaluate(doc, XPathConstants.NODESET);
            XMLUtilities.toElementStream(identified)
                    .forEach(e -> e.removeAttribute("xml:id"));
            LOGGER.info("took the identity of {} elements",
                    identified.getLength());
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * get the document
     *
     * @return the document
     */
    private Document getDocument() {
        return doc;
    }

    /**
     * add identifier to an element
     *
     * @param el
     *         DOM Element
     */
    private void makeID(Element el) {
        makeID(el, PREFIX);
    }

    String makeID(Element el, String prefix) {
        Supplier<String> make = () -> prefix + "_" + (lastId++);
        String candidate = make.get();
        while (IDs.contains(candidate)) {
            candidate = make.get();
        }
        IDs.add(candidate);
        assert NameChecker.isValidNCName(candidate);
        Node n = el.getAttributeNodeNS(XML_NS, "xml:id");
        if (n == null) {
            el.setAttribute("xml:id", candidate);
            return candidate;
        }
        return null;
    }

    /**
     * add identifiers to document
     *
     * @return the DocumentIdentifier instance, for chaining
     */
    private DocumentIdentifier makeIDs() {
        System.setProperty("javax.xml.transform.TransformerFactory",
                "net.sf.saxon.TransformerFactoryImpl");
        try {
            NodeList IDNodes = (NodeList) ID_PATH.evaluate(doc,
                    XPathConstants.NODESET);
            IDs = XMLUtilities.toElementStream(IDNodes)
                    .map(e -> e.getAttributeNS(XML_NS, "xml:id"))
                    .collect(Collectors.toSet());
//            LOGGER.info(IDs.toString());
            NodeList unidentified = (NodeList) NO_ID_PATH.evaluate(doc,
                    XPathConstants.NODESET);
            XMLUtilities.toElementStream(unidentified).forEach(this::makeID);
            LOGGER.info("gave an identity to {} elements",
                    unidentified.getLength());
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
        return this;
    }
}
