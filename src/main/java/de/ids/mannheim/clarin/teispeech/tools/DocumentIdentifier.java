package de.ids.mannheim.clarin.teispeech.tools;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.jdom2.JDOMException;
import org.korpora.useful.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.ids.mannheim.clarin.teispeech.data.NameSpaces;
import net.sf.saxon.om.NameChecker;
import net.sf.saxon.xpath.XPathFactoryImpl;

/**
 * provide IDs für Elements that have none – and remove them
 */
public class DocumentIdentifier {

    private final static String PREFIX = "CLARIN_ROUNDTRIP_ID";
    private final Document doc;
    private Set<String> IDs = new HashSet<>();
    private int lastId = 0;

    private final static Logger LOGGER = LoggerFactory
            .getLogger(DocumentIdentifier.class.getName());

    public DocumentIdentifier(Document doc) {
        this.doc = doc;
    }

    public void makeID(Element el) {
        Supplier<String> make = () -> PREFIX + "_" + (lastId++);
        String candidate = make.get();
        while (IDs.contains(candidate)) {
            candidate = make.get();
        }
        IDs.add(candidate);
        NameChecker.isValidNCName(candidate);
        Node n = el.getAttributeNode("id");
        if (n == null || NameSpaces.XML_NS.equals(n.getNamespaceURI())) {
            el.setAttributeNS("http://www.w3.org/XML/1998/namespace", "id",
                    candidate);
        } else {
            LOGGER.warn("skipped ID attribute with wrong namespace");
        }
    }

    public Document getDocument() {
        return doc;
    }

    public DocumentIdentifier makeIDs() {
        System.setProperty("javax.xml.transform.TransformerFactory",
                "net.sf.saxon.TransformerFactoryImpl");
        try {
            XPath xpf = new XPathFactoryImpl().newXPath();
            // note that Java is confused about @xml:id
            NodeList IDNodes = (NodeList) xpf.compile("//*[@id]").evaluate(doc,
                    XPathConstants.NODESET);
            IDs = Utilities.toElementStream(IDNodes).map(e -> {
                String n = e.getAttributeNode("id").getNamespaceURI();
                assert n == null || NameSpaces.XML_NS.equals(n);
                return e.getAttribute("id");
            }).collect(Collectors.toSet());
            System.err.println(IDs);
            XPath xPath = XPathFactory.newInstance().newXPath();
            // note that Java is confused about @xml:id
            String unindentifiedXPath = "//*[not(@id)]";
            NodeList unidentified = (NodeList) xPath.compile(unindentifiedXPath)
                    .evaluate(doc, XPathConstants.NODESET);
            Utilities.toElementStream(unidentified).forEach(this::makeID);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public static Document makeIDs(Document doc) {
        DocumentIdentifier di = new DocumentIdentifier(doc);
        return di.makeIDs().getDocument();
    }

    public static Document makeIDs(org.jdom2.Document jdoc) {
        try {
            DocumentIdentifier di = new DocumentIdentifier(
                    Utilities.convertJDOMToDOM(jdoc));
            return di.makeIDs().getDocument();
        } catch (JDOMException e) {
            throw new RuntimeException(e);
        }
    }

    public static org.jdom2.Document jmakeIDs(org.jdom2.Document jdoc) {
        return Utilities.convertDOMtoJDOM(makeIDs(jdoc));
    }

    public static void removeIDs(Document doc) {
        System.setProperty("javax.xml.transform.TransformerFactory",
                "net.sf.saxon.TransformerFactoryImpl");
        try {
            XPath xp = new XPathFactoryImpl().newXPath();
            NodeList identified = (NodeList) xp.compile("//*[@id]")
                    .evaluate(doc, XPathConstants.NODESET);
            Utilities.toElementStream(identified).forEach(e -> {
                if (StringUtils.startsWith(
                        e.getAttributeNS("http://www.w3.org/XML/1998/namespace",
                                "xml:id"),
                        PREFIX)) {
                    e.removeAttribute("xml:id");
                }
            });
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }
}
