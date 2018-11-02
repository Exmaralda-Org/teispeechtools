package de.ids.mannheim.clarin.teispeech.tools;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.korpora.useful.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.codepoetics.protonpack.StreamUtils;
import java.util.Arrays;

import opennlp.tools.langdetect.Language;
import opennlp.tools.langdetect.LanguageDetector;
import opennlp.tools.langdetect.LanguageDetectorME;
import opennlp.tools.langdetect.LanguageDetectorModel;

/**
 * guess language of utterances in TEI transcriptions.
 *
 *
 * @author bfi
 *
 */
public class LanguageDetect {

    private final static Logger LOGGER = LoggerFactory
            .getLogger(LanguageDetect.class.getName());

    private static final String MODEL_PATH = "/main/resources/langdetect-183.bin";

    /**
     * an acceptable ratio between the "best" and the second-best language
     */
    private static final double GOOD_RELATION = 1.5;

    /**
     * the document language per request; "deu" if nothing is given
     */
    private String language;

    private Set<String> expectedLanguages;

    private Document doc;

    private static LanguageDetector languageDetector;
    static {
        // load the trained Language Detector Model file
        try (InputStream modelStream = LanguageDetect.class
                .getResourceAsStream(MODEL_PATH)) {

            LanguageDetectorModel trainedModel = new LanguageDetectorModel(
                    modelStream);

            // load the model
            languageDetector = new LanguageDetectorME(trainedModel);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
    }

    /**
     * make new {@link LanguageDetect};
     *
     * @param doc
     *            XML DOM document
     *
     * @param language
     *            the default language, an ISO language code
     * @param expected
     *            the languages that are expected in the document, for
     *            constraining language identification
     */
    public LanguageDetect(Document doc, String language, String[] expected) {
        this.doc = doc;
        this.language = language != null ? language : "deu";
        if (expected != null && expected.length > 0) {
            expectedLanguages = new HashSet<>();
            expectedLanguages.add(language);
            expectedLanguages.addAll(Arrays.asList(expected));

        }
    }

    public LanguageDetect(Document doc) {
        this(doc, "deu", new String[] { "tur", "en" });
    }

    // TODO: consider threshold for utterance length and confidence
    private static int MIN_UTTERANCE_SIZE = 5;
    // private static double MIN_CONFIDENCE = 0.1;
    // difference to next-likely language?

    public Document detect() {
        return detect(false);
    }

    /**
     * detect language per {@code <u>}
     *
     * @param force
     *            force processing even if {@code <u>} has already been
     *            language-tagged
     *
     * @return the document again
     */
    @SuppressWarnings("deprecation")
    public Document detect(boolean force) {
        long processed = 0;
        long unprocessed = 0;
        Map<String, Integer> changed = new HashMap<>();
        List<Element> utterances = Utilities
                .toElementList(doc.getElementsByTagName("u"));
        for (Element utter : utterances) {
            if (!force && utter.hasAttribute("xml:lang")) {
                continue;
            }
            String defaultLanguage = DocUtilities
                    .getLanguage((Element) utter.getParentNode(), language);
            NodeList words = utter.getElementsByTagName("w");
            if (words.getLength() > 0
                    && words.getLength() < MIN_UTTERANCE_SIZE) {
                Comment com = doc.createComment(
                        "too few words to make a good language prediction");
                utter.getParentNode().insertBefore(com, utter);
                unprocessed++;
                continue;
            }
            String text;
            // TODO: What to do about mixed content without <w>?
            if (words.getLength() == 0) {
                if (utter.getChildNodes().getLength() == 0) {
                    text = utter.getTextContent();
                } else {
                    text = "";
                }
            } else {
                text = Utilities.toStream(words).map(e -> (Element) e)
                        .map(e -> DocUtilities.getTextOrNorm(e))
                        .collect(Collectors.joining(" "));
            }
            LOGGER.info("{} {}", Utilities.stripSpace(text),
                    Utilities.stripSpace(text).isEmpty());
            if (Utilities.stripSpace(text).isEmpty()) {
                Comment commy = doc.createComment("– EMPTY –");
                utter.getParentNode().insertBefore(commy, utter);
                continue;
            }
            List<Language> languages = Stream
                    .of(languageDetector.predictLanguages(text))
                    .filter(l -> expectedLanguages.contains(l.getLang()))
                    .collect(Collectors.toList());
            Comment com = doc.createComment(
                    languages.stream().filter(l -> l.getConfidence() > 0.005)
                            .map(l -> String.format("%s: %.02f", l.getLang(),
                                    l.getConfidence()))
                            .collect(Collectors.joining("; ")));
            utter.getParentNode().insertBefore(com, utter);
            if (languages.size() >= 2 && languages.get(0).getConfidence() > 0
                    && languages.get(1).getConfidence() > 0
                    && (languages.get(0).getConfidence() / languages.get(1)
                            .getConfidence() > GOOD_RELATION)) {
                // in clear cases, believe language guess
                String lang = languages.get(0).getLang();
                utter.setAttribute("xml:lang", lang);
                Utilities.incCounter(changed, lang);
                processed++;
            } else {
                // prefer defaultLanguage in case of doubt
                if (languages.size() >= 2
                        && languages.get(0).getConfidence() > 0) {
                    double measure = languages.get(0).getConfidence();
                    List<String> similar = StreamUtils
                            .takeWhile(languages.stream(),
                                    l -> measure / l.getConfidence() < 1.1)
                            .map(l -> l.getLang()).collect(Collectors.toList());
                    if (similar.contains(defaultLanguage)) {
                        utter.setAttribute("xml:lang", defaultLanguage);
                    }
                    Utilities.incCounter(changed, defaultLanguage);
                    processed++;
                } else {
                    // or give up
                    Comment commy = doc.createComment(
                            "– Sorry, no idea what language this is! –");
                    utter.getParentNode().insertBefore(commy, utter);
                    unprocessed++;
                }
            }

        }
        DocUtilities.makeChange(doc,
                String.format(
                        "detected languages in %d utterances; skipped %d (found: %s).",
                        processed, unprocessed, changed));
        return doc;
    }

}
