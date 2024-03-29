package de.ids.mannheim.clarin.teispeech.data;

import static de.ids.mannheim.clarin.teispeech.data.NameSpaces.TEI_NS;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.jooq.lambda.Seq;
import org.korpora.useful.LangUtilities;
import org.korpora.useful.Utilities;
import org.korpora.useful.XMLUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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

    private static final String MODEL_PATH = "langdetect-183.bin";

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

    private static final LanguageDetector languageDetector;
    static {
        // load the trained Language Detector Model file
        try (InputStream modelStream = LanguageDetect.class.getClassLoader()
                .getResourceAsStream(MODEL_PATH)) {

            assert modelStream != null;
            LanguageDetectorModel trainedModel = new LanguageDetectorModel(
                    modelStream);

            // load the model
            languageDetector = new LanguageDetectorME(trainedModel);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // private static int MIN_UTTERANCE_SIZE = 5;
    private final int minUtteranceSize;
    // private static double MIN_CONFIDENCE = 0.1;
    // difference to next-likely language?

    /**
     * make new ;
     *
     * @param doc
     *     XML DOM document
     *
     * @param language
     *     the default language, an ISO language code
     * @param expected
     *     the languages that are expected in the document, for constraining
     *     language identification
     * @param mini
     *     the minimal length of an utterance to attempt language detection
     */
    public LanguageDetect(Document doc, String language, List<String> expected,
            int mini) {
        this.doc = doc;
        // use German as fallback in case nothing else is specified
        // in the document or the expected languages
        this.language = language != null ? language : "deu";
        if (expected != null && expected.size() > 0) {
            expectedLanguages = new HashSet<>();
            if (language != null && !language.isEmpty())
                expectedLanguages.add(language);
            expectedLanguages.addAll(expected);
        }
        this.minUtteranceSize = mini;
    }

    /**
     * Detect languages in document
     *
     * @param doc
     *     XML DOM document
     * @param language
     *     fallback language
     * @param expected
     *     expected languages
     * @param mini
     *     minimal number of words in utterance for language detection
     */
    public LanguageDetect(Document doc, String language, String[] expected,
            int mini) {
        this(doc, "deu", Arrays.asList(expected), mini);
    }

    /**
     * Detect languages in document
     *
     * @param doc
     *     XML DOM document
     */
    private LanguageDetect(Document doc) {
        this(doc, "deu", new String[] { "tur", "en" }, 5);
    }

    /**
     * run detection, do not force
     *
     * @return document with language detection run on utterances
     */
    public Document detect() {
        return detect(false);
    }

    /**
     * count language per {@code <u>}
     *
     * @param force
     *     whether to force language detection, even if a language tag has
     *     already been assigned to {@code <u>}.
     *
     * @return document with language detection run on utterances
     */
    public Document detect(boolean force) {
        long processed = 0;
        long unprocessed = 0;
        Map<String, Integer> changed = new HashMap<>();
        List<Element> utterances = XMLUtilities
                .toElementList(doc.getElementsByTagNameNS(TEI_NS, "u"));
        for (Element utter : utterances) {
            if (!force && utter.hasAttribute("xml:lang")) {
                continue;
            }
            String defaultLanguage = DocUtilities
                    .getLanguage((Element) utter.getParentNode(), language, 1);

            // language by words:
            List<Element> words = XMLUtilities
                    .toElementStream(utter.getElementsByTagNameNS(TEI_NS, "w"))
                    .filter(ut -> !"incomprehensible"
                            .equals(ut.getAttribute("type")))
                    .collect(Collectors.toList());
            boolean already = words.stream()
                    .allMatch(e -> e.hasAttribute("xml:lang"));
            if (already) {
                List<Map.Entry<String, Long>> wordLanguages = words.stream()
                        .filter(e -> e.hasAttribute("xml:lang"))
                        .map(e -> e.getAttribute("xml:lang"))
                        .collect(Collectors.groupingBy(Function.identity(),
                                Collectors.counting()))
                        .entrySet().stream()
                        .sorted(Map.Entry
                                .comparingByValue(Comparator.reverseOrder()))
                        .collect(Collectors.toList());
                List<String> candidates = Seq.seq(wordLanguages.stream())
                        .limitWhile(e -> e.getValue()
                                .equals(wordLanguages.get(0).getValue()))
                        .map(Map.Entry::getKey).collect(Collectors.toList());
                if (candidates.size() == 1) {
                    // majority language:
                    utter.setAttribute("xml:lang",
                            LangUtilities.getLanguageString(candidates.get(0)));
                    continue;
                } else if (candidates.size() > 1
                        && candidates.contains(defaultLanguage)) {
                    // default among major languages:
                    utter.setAttribute("xml:lang", defaultLanguage);
                    continue;
                }
            }
            // haven't found language yet:
            if (words.size() > 0 && words.size() < minUtteranceSize) {
                Comment com = doc.createComment(String.format(
                        "too few words (%d < %d) to make a good language prediction",
                        words.size(), minUtteranceSize));
                utter.getParentNode().insertBefore(com, utter);
                unprocessed++;
                continue;
            }
            String text;
            if (words.isEmpty()) {
                if (utter.getChildNodes().getLength() == 0) {
                    unprocessed++;
                    continue;
                }
                text = utter.getTextContent();
                if (StringUtils.split(text).length < minUtteranceSize) {
                    Comment com = doc.createComment(String.format(
                            "too few words (%d < %d) to make a good language prediction",
                            StringUtils.split(text).length, minUtteranceSize));
                    utter.getParentNode().insertBefore(com, utter);
                    unprocessed++;
                    continue;
                }
            } else {
                text = Seq.seq(words).map(DocUtilities::getTextOrNorm)
                        .toString(" ");
            }
            if (StringUtils.strip(text).isEmpty()) {
                Comment commy = doc.createComment("– EMPTY –");
                utter.getParentNode().insertBefore(commy, utter);
                continue;
            }
            List<Language> languages = Stream
                    .of(languageDetector.predictLanguages(text))
                    .filter(l -> expectedLanguages.contains(
                            LangUtilities.getLanguageString(l.getLang())))
                    .collect(Collectors.toList());
            LOGGER.info("expected: {}; detected:{}", expectedLanguages,
                    languages);
            Comment com = doc.createComment(
                    Seq.seq(languages).filter(l -> l.getConfidence() > 0.005)
                            .map(l -> String.format("%s: %.02f",
                                    LangUtilities.getLanguageString(
                                            l.getLang()),
                                    l.getConfidence()))
                            .toString("; "));
            utter.getParentNode().insertBefore(com, utter);
            if (languages.size() >= 2 && languages.get(0).getConfidence() > 0
                    && languages.get(1).getConfidence() > 0
                    && (languages.get(0).getConfidence() / languages.get(1)
                            .getConfidence() > GOOD_RELATION)) {
                // in clear cases, believe language guess
                String lang = LangUtilities
                        .getLanguageString(languages.get(0).getLang());
                utter.setAttribute("xml:lang", lang);
                Utilities.incCounter(changed, lang);
                processed++;
            } else {
                // prefer defaultLanguage in case of doubt
                if (languages.size() >= 2
                        && languages.get(0).getConfidence() > 0) {
                    double measure = languages.get(0).getConfidence();
                    List<String> similar = Seq.seq(languages)
                            .limitWhile(l -> measure / l.getConfidence() < 1.1)
                            .map(Language::getLang)
                            .collect(Collectors.toList());
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
        String changeMsg = String.format(
                "detected languages in %d utterances; skipped %d (found: %s).",
                processed, unprocessed, changed);
        LOGGER.info(changeMsg);
        DocUtilities.makeChange(doc, changeMsg);
        return doc;
    }

}
