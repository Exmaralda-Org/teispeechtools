package de.ids.mannheim.clarin.normalverbraucher;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.korpora.useful.Utilities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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

    WordNormalizer norm;
    boolean debug;

    public TEINormalizer(WordNormalizer wn) {
        this(wn, false);
    }

    public TEINormalizer(WordNormalizer wn, boolean debugging) {
        norm = wn;
        debug = debugging;
    }

    /**
     * add a change to the end of the &lt;revisionDesc&gt;
     * @param doc
     * @return
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
     * @return
     */
    public Document normalize(Document doc) {
        Utilities.toStream(doc.getElementsByTagName("w")).forEach(
                e -> {
                    Element el = (Element) e;
                    String tx = el.getTextContent();
                    String normal = norm.getNormalised(tx);
                    if (normal != null) {
                        if (debug) {
                            String before = el.getAttribute("norm");
                            if (!before.equals(normal)) {
                                System.err.format(
                                        "%20s -> %-20s [was: %s]\n",
                                        tx, normal, before);
                            }
                        }
                        System.err.format("%20s -> %s\n", tx, normal);
                        el.setAttribute("norm", normal);
                    } else if (debug) {
                        System.err.format("Cannot normalize «%s»\n", tx);
                    }
                });
        makeChange(doc);
        return doc;
    }

}
