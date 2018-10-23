package de.ids.mannheim.clarin.teispeech.tools;

import org.korpora.useful.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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

    /**
     * make new {@link TEINormalizer} that uses a {@link WordNormalizer}
     *
     * @param wn
     */
    public TEINormalizer(WordNormalizer wn) {
        norm = wn;
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
        DocUtilities.makeChange(doc, "normalized by OrthoNormal");
        return doc;
    }

}
