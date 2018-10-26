package de.ids.mannheim.clarin.teispeech.tools;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.ids.mannheim.clarin.teispeech.data.NameSpaces;

public class DocUtilities {
    /**
     * add a change to the end of the &lt;revisionDesc&gt;
     *
     * @param doc
     *            – the document
     * @return – the document again
     */

    private static final String LANGNAMES_PATH = "/main/resources/languages.json";

    public static Map<String, String> languageMap;
    public static Set<String> languageTriples;

    /**
     * get language of element, ascending the element tree recursively.
     *
     * @param el
     * @return Optional containing the language, or an empty Optional
     */
    public static Optional<String> getLanguage(Element el) {
        String lang = el.getAttributeNS(NameSpaces.XML_NS, "lang");
        for (Element parent = (Element) el.getParentNode(); lang == null
                || lang.isEmpty() && parent != null; parent = (Element) parent
                        .getParentNode()) {
            lang = parent.getAttributeNS(NameSpaces.XML_NS, "lang");
        }
        return Optional.ofNullable(lang);
    }

    static {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream str = DocUtilities.class
                .getResourceAsStream(LANGNAMES_PATH)) {
            languageMap = mapper.readValue(str,
                    new TypeReference<Map<String, String>>() {
                    });
            languageTriples = languageMap.values().stream().distinct()
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static boolean isLanguage(String lang) {
        return languageMap.containsKey(lang);
    }

    public static boolean isLanguageTriple(String lang) {
        return languageTriples.contains(lang);
    }

    public static Optional<String> getLanguage(String lang) {
        return Optional.ofNullable(languageMap.get(lang));
    }

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

    public static Document addComment(Document doc, String commentText) {
        Element body = (Element) doc.getElementsByTagName("body");
        Comment comment = doc.createComment(commentText);
        body.getParentNode().insertBefore(comment, body);
        return doc;
    }

}
