package de.ids.mannheim.clarin.teispeech.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.korpora.useful.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * a normalizer for the TEI transcription format.
 *
 * Mainly applies the normalizer {@link #normalizer} to all &lt;w&gt; Elements
 * in a document.
 *
 * @author bfi
 *
 */
public class TEINormalizer {

    private final static Logger LOGGER = LoggerFactory
            .getLogger(TEINormalizer.class.getName());

    /**
     * the document language per request; "deu" if nothing is given
     */
    private String language;

    private WordNormalizer normalizer;

    /**
     * make new {@link TEINormalizer} that uses a {@link WordNormalizer}
     *
     * @param wn
     *            the normalizer
     * @param language
     *            an ISO language code
     */
    public TEINormalizer(WordNormalizer wn, String language) {
        normalizer = wn;
        this.language = language != null ? language : "deu";
    }

    public TEINormalizer(WordNormalizer wn) {
        this(wn, "deu");
    }

    public TEINormalizer(String language) {
        normalizer = new DictionaryNormalizer(); // TODO: currently, only
                                                 // German,
        // anyway!
        this.language = language != null ? language : "deu";
    }

    /**
     * normalize an XML document using the normalizer {@link #normalizer}.
     *
     * @param doc
     *            the XML file DOM
     * @param force
     *            whether to force normalization even if already normalized
     * @return the document again
     */
    public Document normalize(Document doc, boolean force) {
        Map<String, List<Element>> words = DocUtilities.groupByLanguage("w",
                doc, language);

        List<String> processed = new ArrayList<>();
        List<String> unprocessed = new ArrayList<>();
        // TODO: currently, we only support German normalization!
        words.forEach((lang, ws) -> {
            if (lang == "deu") {

                ws.forEach(e -> {
                    if (!force && e.hasAttribute("norm")) {
                        return;
                    }
                    Element el = e;
                    String tx = Utilities.removeSpace(el.getTextContent());
                    String normal = normalizer.getNormalised(tx);
                    if (normal != null) {
                        String before = el.getAttribute("normalizer");
                        if (!before.isEmpty()) {
                            if (!before.equals(normal)) {
                                LOGGER.info("ReNormalized {} -> {} [was: {}]",
                                        tx, normal, before);
                            }
                        } else {
                            LOGGER.info("Normalized {} -> {}", tx, normal);
                        }
                        el.setAttribute("normalizer", normal);
                    } else {
                        LOGGER.info("Cannot normalize «{}».", tx);
                    }
                });
                processed.add(lang);
            } else {
                unprocessed.add(lang);
            }
        });

        // TODO: currently, we only support German normalization!
        DocUtilities.makeChange(doc,
                "normalized words using the OrthoNormal dictionaries",
                processed, unprocessed);
        return doc;
    }

}
