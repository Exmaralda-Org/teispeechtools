package de.ids.mannheim.clarin.normalverbraucher;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.korpora.useful.Utilities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a normalizer for the TEI transcription format.
 *
 * Mainly applies the normalizer {@link #norm} to all &lt;w&gt;
 * Elements in a document.
 *
 * @author bfi
 *
 */
public class TEINormalizer {

    private final static Logger LOGGER = LoggerFactory.getLogger(
            TEINormalizer.class.getName());

    WordNormalizer norm;

    public TEINormalizer(WordNormalizer wn) {
        norm = wn;
    }

    /**
     * add a change to the end of the &lt;revisionDesc&gt;
     * @param doc – the document
     * @return – the document again
     */
    public static Document makeChange(Document doc) {
        String stamp = ZonedDateTime
                .now(ZoneOffset.systemDefault())
                .format(DateTimeFormatter.ISO_INSTANT);
        NodeList revDescs = doc.getElementsByTagName("revisionDesc");
        if (revDescs.getLength() > 0) {
            Element changeEl = doc.createElement("change");
            changeEl.setAttribute("when", stamp);
            changeEl.appendChild(
                    doc.createTextNode("normalized by OrthoNormal"));
            revDescs.item(0).appendChild(changeEl);
        }
        return doc;
    }


    /**
     * normalize an XML document using the normalizer {@link #norm}.
     * @param doc – the XML file DOM
     * @return the document again
     */
    public Document normalize(Document doc) {
        Utilities.toStream(doc.getElementsByTagName("w")).forEach(
                e -> {
                    Element el = (Element) e;
                    String tx = el.getTextContent();
                    String normal = norm.getNormalised(tx);
                    if (normal != null) {
                        String before = el.getAttribute("norm");
                        if (!before.isEmpty()) {
                            if (!before.equals(normal)) {
                                LOGGER.info("ReNormalized {} -> {} [was: {}]",
                                        tx, normal, before);
                            }
                        } else {
                            LOGGER.info("Normalized {} -> {}", tx, normal);
                        }
                        el.setAttribute("norm", normal);
                    } else {
                        LOGGER.info("Cannot normalize «{}».", tx);
                    }
                });
        makeChange(doc);
        return doc;
    }

}
