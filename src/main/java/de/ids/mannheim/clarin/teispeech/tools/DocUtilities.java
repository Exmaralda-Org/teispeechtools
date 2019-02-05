package de.ids.mannheim.clarin.teispeech.tools;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jdom2.Namespace;
import org.korpora.useful.LangUtilities;
import org.korpora.useful.Utilities;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import de.ids.mannheim.clarin.teispeech.data.NameSpaces;

public class DocUtilities {

    /**
     * get language of element, ascending the element tree recursively.
     *
     * @param el
     *            the DOM XML Element
     * @return Optional containing the language code, or empty
     */
    public static Optional<String> getLanguage(Element el) {
        String lang = el.getAttribute("xml:lang");
        for (Node parent = el.getParentNode(); lang.isEmpty() && parent != null
                && parent.getNodeType() == Node.ELEMENT_NODE; parent = parent
                        .getParentNode()) {
            lang = ((Element) parent).getAttribute("xml:lang");
        }
        if (!lang.isEmpty()) {
            return LangUtilities.getLanguage(lang);
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
     * @return the corresponding three letter ISO 639-2 language code
     */
    public static String getLanguage(Element el, String defaultL) {
        Optional<String> ret = getLanguage(el);
        if (ret.isPresent()) {
            return ret.get();
        } else {
            return defaultL;
        }
    }

    /**
     * the language of an utterance {@code <u>} is the majority language of its
     * {@code <w>}s or the language of the element.
     *
     * @param el
     *            the utterance element
     * @param defaultL
     *            the default language
     * @return the determined language
     */
    public static String getUtteranceLanguage(Element el, String defaultL) {
        assert "u".equals(el.getTagName());
        String lang = getLanguage(el, defaultL);
        Map<String, Long> freq = Utilities
                .toElementStream(el.getElementsByTagNameNS("*", "w"))
                .collect(Collectors.groupingBy(w -> {
                    // words without language tag are counted as having the
                    // language of the {@code <u>}
                    return w.hasAttribute("lang") ? w.getAttribute("lang")
                            : lang;
                }, Collectors.counting()));
        Optional<Entry<String, Long>> maxLang = freq.entrySet().stream()
                .collect(Collectors
                        .maxBy(Comparator.comparing(c -> c.getValue())));
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
     * @return the elements grouped by language
     */
    public static Map<String, List<Element>> groupByLanguage(String tagName,
            Document doc, String defaultL) {
        return Utilities.toStream(doc.getElementsByTagNameNS(NameSpaces.TEI_NS, tagName))
                .map(u -> (Element) u)
                .collect(Collectors.groupingBy(
                        u -> getUtteranceLanguage(u, defaultL),
                        Collectors.toList()));
    }

    public static String getTextOrNorm(Element el) {
        if (el.hasAttribute("norm")) {
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
     * @return the document, for chaining
     */
    public static Document makeChange(Document doc, String change) {
        String stamp = ZonedDateTime.now(ZoneOffset.systemDefault())
                .format(DateTimeFormatter.ISO_INSTANT);
        Element revDesc = Utilities
                .getElementByTagNameNS(doc.getDocumentElement(), NameSpaces.TEI_NS, "revisionDesc");
        if (revDesc == null) {
            revDesc = doc.createElementNS(NameSpaces.TEI_NS, "revisionDesc");
            Element eDe = Utilities.getElementByTagNameNS(
                    doc.getDocumentElement(), NameSpaces.TEI_NS, "encodingDesc");
            if (eDe == null) {
                eDe = doc.createElementNS(NameSpaces.TEI_NS, "encodingDesc");
                Element header = Utilities.getElementByTagNameNS(
                        doc.getDocumentElement(), NameSpaces.TEI_NS, "teiHeader");
                if (header == null) {
                    header = doc.createElementNS(NameSpaces.TEI_NS,
                            "teiHeader");
                    Utilities.insertAtBeginningOf(header,
                            doc.getDocumentElement());
                }
                Utilities.insertAtBeginningOf(eDe, header);
            }
            Utilities.insertAtBeginningOf(revDesc, eDe);
        }
        Element changeEl = doc.createElementNS(NameSpaces.TEI_NS, "change");
        changeEl.setAttributeNS(NameSpaces.TEI_NS, "when", stamp);
        changeEl.appendChild(doc.createTextNode(change));
        Utilities.insertAtBeginningOf(changeEl, revDesc);
        return doc;
    }

    /**
     * add change to {@code <revisionDesc>} in TEI document
     *
     * @param doc
     *            the JDOM Document
     * @param change
     *            the change message
     * @return the document, for chaining
     */
    public static org.jdom2.Document makeChange(org.jdom2.Document doc,
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
        return doc;
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
     * @return the document, for chaining
     */
    public static Document makeChange(Document doc, String change,
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
        return makeChange(doc, message);
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
        Element body = (Element) doc.getElementsByTagNameNS("*", "body");
        Comment comment = doc.createComment(commentText);
        body.getParentNode().insertBefore(comment, body);
        return doc;
    }

}
