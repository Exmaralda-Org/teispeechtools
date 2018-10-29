package de.ids.mannheim.clarin.teispeech.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.korpora.useful.Utilities;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DocUtilities {
    /**
     * add a change to the end of the {@code <revisionDesc>}
     *
     * @param doc
     *            – the document
     * @return – the document again
     */

    private static final String LANGNAMES_PATH = "/main/resources/languages-639-most-tolerant.json";
    private static final String LANGCODES_3_PATH = "/main/resources/three-letters.txt";
    private static final String LANGCODES_2_PATH = "/main/resources/two-letters.txt";

    /**
     * map from language names / letter triples/tuples to terminological
     * ISO-639-2 code.
     */
    public static Map<String, String> languageMap;

    /**
     * valid terminological ISO-639-2 three letter codes
     *
     * @see {@link #languageCodesThree} for a list including bibliographic
     *      variants
     */
    public static Set<String> languageTriples;

    /*
     * prepare variables
     */
    static {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream str = DocUtilities.class
                .getResourceAsStream(LANGNAMES_PATH)) {
            languageMap = mapper.readValue(str,
                    new TypeReference<Map<String, String>>() {
                    });
            languageTriples = languageMap.keySet().stream().distinct()
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
    /**
     * valid terminological ISO-639-2 three letter codes, including
     * bibliographic variants
     */
    private static Set<String> languageCodesThree = new HashSet<>();
    static {
        try (InputStream str = DocUtilities.class
                .getResourceAsStream(LANGCODES_3_PATH)) {
            InputStreamReader strR = new InputStreamReader(str);
            BufferedReader strRR = new BufferedReader(strR);
            strRR.lines().forEach(l -> languageCodesThree.add(l));
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
    /**
     * valid terminological ISO-639-1 two letter codes
     */
    private static Set<String> languageCodesTwo = new HashSet<>();
    static {
        try (InputStream str = DocUtilities.class
                .getResourceAsStream(LANGCODES_2_PATH)) {
            InputStreamReader strR = new InputStreamReader(str);
            BufferedReader strRR = new BufferedReader(strR);
            strRR.lines().forEach(l -> languageCodesTwo.add(l));
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * get language of element, ascending the element tree recursively.
     *
     * @param el
     * @return Optional containing the language code, or empty
     */
    public static Optional<String> getLanguage(Element el) {
        String lang = el.getAttribute("xml:lang");
        for (Node parent = el.getParentNode(); lang.isEmpty()
                && parent != null && parent.getNodeType() == Node.ELEMENT_NODE; parent = parent.getParentNode()) {
            lang = ((Element) parent).getAttribute("xml:lang");
        }
        if (!lang.isEmpty()) {
            return getLanguage(lang);
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
        assert el.getTagName() == "u";
        String lang = getLanguage(el, defaultL);
        Map<String, Long> freq = Utilities
                .toStream(el.getElementsByTagName("w"))
                .collect(Collectors.groupingBy(w -> {
                    // words without language tag are counted as having the
                    // language of the {@code <u>}
                    Element wEl = (Element) w;
                    return wEl.hasAttribute("lang") ? wEl.getAttribute("lang")
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
     * @return
     */
    public static Map<String, List<Element>> groupByLanguage(String tagName,
            Document doc, String defaultL) {
        return Utilities.toStream(doc.getElementsByTagName("u"))
                .map(u -> (Element) u).collect(Collectors.groupingBy(
                        u -> getUtteranceLanguage(u, defaultL), Collectors.toList()));
    }

    /**
     * Can we get a language out of {@link #languageMap}?
     *
     * @param lang
     *            the language name / two or three letter code
     * @return whether
     */
    public static boolean isLanguage(String lang) {
        return languageMap.containsKey(lang.toLowerCase());
    }

    /**
     * Get the (terminological) three letter ISO-639-1 code for language
     *
     * @param lang
     *            the language name / two or three letter code
     * @return the three letter code as an Optional
     */
    public static Optional<String> getLanguage(String lang) {
        return Optional.ofNullable(languageMap.get(lang.toLowerCase()));
    }

    /**
     * Get the (terminological) three letter ISO-639-1 code for language
     *
     * @param lang
     *            the language name / two or three letter code
     * @param defaultL
     *            the default code to return if {@code lang} is no discernible
     *            language
     * @return the three letter code, or the default
     */
    public static String getLanguage(String lang, String defaultL) {
        return languageMap.getOrDefault(lang.toLowerCase(), defaultL);
    }

    /**
     * Is this an ISO 639-2 three letter code?
     *
     * @param lang
     * @return whether
     */
    public static boolean isLanguageTriple(String lang) {
        return languageCodesThree.contains(lang.toLowerCase());
    }

    /**
     * Is this a terminological ISO 639-2 three letter code (i.e. a key in
     * languageMap)
     *
     * @param lang
     * @return whether
     */
    public static boolean isTerminologicalLanguageTriple(String lang) {
        return languageTriples.contains(lang.toLowerCase());
    }

    /**
     * Is this an ISO 639-1 two letter code
     *
     * @param lang
     * @return whether
     */
    public static boolean isLanguageTuple(String lang) {
        return languageCodesTwo.contains(lang.toLowerCase());
    }

    /**
     * add change to {@code <revisionDesc>} in TEI document
     *
     * @param doc
     * @param change
     * @return the document, for chaining
     */
    public static Document makeChange(Document doc, String change) {
        String stamp = ZonedDateTime.now(ZoneOffset.systemDefault())
                .format(DateTimeFormatter.ISO_INSTANT);
        NodeList revDescs = doc.getElementsByTagName("revisionDesc");
        if (revDescs.getLength() > 0) {
            Element changeEl = doc.createElement("change");
            changeEl.setAttribute("when", stamp);
            changeEl.appendChild(doc.createTextNode(change));
            revDescs.item(0).appendChild(changeEl);
        }
        return doc;
    }

    /**
     *
     * add change to {@code <revisionDesc>} in TEI document
     *
     * @param doc
     * @param change
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
     * @param commentText
     * @return the document, for chaining
     */
    public static Document addComment(Document doc, String commentText) {
        Element body = (Element) doc.getElementsByTagName("body");
        Comment comment = doc.createComment(commentText);
        body.getParentNode().insertBefore(comment, body);
        return doc;
    }

}
