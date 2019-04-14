package de.ids.mannheim.clarin.teispeech.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.ids.mannheim.clarin.teispeech.tools.DocUtilities;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.ids.mannheim.clarin.teispeech.data.NameSpaces;

/**
 * a normalizer for the TEI transcription format.
 *
 * Mainly applies the normalizer to all &lt;w&gt; Elements in a document.
 *
 * @author bfi
 *
 */
@SuppressWarnings("WeakerAccess")
public class TEINormalizer {

    private final static Logger LOGGER = LoggerFactory
            .getLogger(TEINormalizer.class.getName());

    /**
     * the document language per request; "deu" if nothing is given
     */
    private final String language;

    private final WordNormalizer normalizer;

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

    private TEINormalizer(WordNormalizer wn) {
        this(wn, "deu");
    }

    private TEINormalizer(String language) {
        normalizer = new DictionaryNormalizer(); // TODO: currently, only
                                                 // German,
        // anyway!
        this.language = language != null ? language : "deu";
    }

    /**
     * normalize an XML document using the normalizer
     *
     * @param doc
     *            the XML file DOM
     * @param force
     *            whether to force normalization even if already normalized
     */
    public void normalize(Document doc, boolean force) {
        Map<String, List<Element>> words = DocUtilities.groupByLanguage("w",
                doc, language, 1);

        List<String> processed = new ArrayList<>();
        List<String> unprocessed = new ArrayList<>();
        // TODO: currently, we only support German normalization!
        words.forEach((lang, ws) -> {
            if ("deu".equals(lang)) {
                ws.forEach(el -> {
                    if (!force
                            && el.hasAttributeNS(NameSpaces.TEI_NS, "norm")) {
                        return;
                    }
                    String tx = StringUtils.strip(el.getTextContent());
                    if (tx.isEmpty()) {
                        return;
                    }
                    String normal = normalizer.getNormalised(tx);
                    if (normal != null) {
                        String before = el.getAttributeNS(NameSpaces.TEI_NS,
                                "norm");
                        if (!before.isEmpty()) {
                            if (!before.equals(normal)) {
                                LOGGER.info("ReNormalized {} -> {} [was: {}]",
                                        tx, normal, before);
                            }
                        } else {
                            LOGGER.info("Normalized {} -> {}", tx, normal);
                        }
                        el.setAttributeNS(NameSpaces.TEI_NS, "norm", normal);
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
    }

}
