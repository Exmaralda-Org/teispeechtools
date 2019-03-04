package de.ids.mannheim.clarin.teispeech.tools;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.jooq.lambda.Seq;
import org.korpora.useful.Utilities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Pseudo-align an utterance call BAS's web services to transcribe
 * phone[mt]ically
 *
 * <p>
 * TODO: test whether syllable structure improves
 *
 * <p>
 * TODO: cutoff
 *
 * <p>
 * TODO: equidistant syllables
 *
 * <p>
 * TODO: (related) change language normalization to account for Australian
 * languages; use
 * <a href="https://iso639-3.sil.org/code_tables/download_tables">list from
 * SIL</a>.
 */
public class GraphToPhoneme {

    /**
     * list of admissible locales from
     * http://clarin.phonetik.uni-muenchen.de/BASWebServices/services/help
     */
    // TODO: should we map "est" and "ee" to "ekk"?
    // TODO: better: map codes to canonical 639-1/-2 code if possible
    private static final String[] PERMITTED_LOCALES_ARRAY = { "aus-AU", "cat",
            "cat-ES", "deu", "deu-DE", "ekk-EE", "eng", "eng-AU", "eng-GB",
            "eng-NZ", "eng-US", "eus-ES", "eus-FR", "fin", "fin-FI", "fra-FR",
            "gsw-CH", "gsw-CH-BE", "gsw-CH-BS", "gsw-CH-GR", "gsw-CH-SG",
            "gsw-CH-ZH", "guf-AU", "gup-AU", "hat", "hat-HT", "hun", "hun-HU",
            "ita", "ita-IT", "jpn-JP", "kat-GE", "ltz-LU", "mlt", "mlt-MT",
            "nld", "nld-NL", "nor-NO", "nze", "pol", "pol-PL", "ron-RO",
            "rus-RU", "slk-SK", "spa-ES", "sqi-AL", "swe-SE" };

    /*
     * base URL for transcription service
     */
    private final static String BASE_URL = "https://clarin.phonetik.uni-muenchen.de/BASWebServices/services/runG2P";

    /**
     * locale separator
     */
    private static final Pattern LOCALE_SEPARATOR = Pattern.compile("[_-]+");
    /**
     * word separator for transcriptions (tab)
     */
    private static final Pattern WORD_SEPARATOR = Pattern.compile("\t");

    private static Map<String, String> LOCALES = new HashMap<>();
    static {
        List<String> already_permitted = Arrays.asList(PERMITTED_LOCALES_ARRAY);
        for (String loc : PERMITTED_LOCALES_ARRAY) {
            LOCALES.put(loc, loc);
            String[] components = LOCALE_SEPARATOR.split(loc);
            for (int i = 0; i < components.length - 1; i++) {
                String active = String.join("-",
                        Arrays.copyOfRange(components, 0, i));
                if (!already_permitted.contains(active)) {
                    LOCALES.put(active, loc);
                }
                if ("ekk".equals(components[0])) {
                    components[0] = "est";
                    active = String.join("-",
                            Arrays.copyOfRange(components, 0, i));
                    if (!already_permitted.contains(active)) {
                        LOCALES.put(active, loc);
                    }
                }
            }
        }
    }

    public static Optional<String[]> getTranscription(String text, String loc) {
        return getTranscription(text, loc, false);
    }

    public static Optional<String> correspondsTo(String locale) {
        String[] components = LOCALE_SEPARATOR.split(locale);
        Optional<String> ret = Optional.empty();
        for (int i = components.length; i >= 0; i--) {
            String loki = String.join("-",
                    Arrays.copyOfRange(components, 0, i));
            if (LOCALES.containsKey(loki))
                return Optional.of(LOCALES.get(loki));
        }
        return ret;
    }

    /**
     * get a transcription from the web service
     *
     * @param text
     *            running text
     * @param loc
     *            locale
     * @param getSyllables
     *            whether to have the transcription syllabified
     * @return an Optional containing the list of transcribed words or emptiness
     */
    public static Optional<String[]> getTranscription(String text, String loc,
            boolean getSyllables) {
        Optional<String[]> ret = Optional.empty();
        try {
            URIBuilder uriBui = new URIBuilder(BASE_URL);
            boolean extendedFeatures = false; // extended for eng-GB and deu?
            HttpEntity entity = MultipartEntityBuilder.create()
                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                    .setCharset(Charset.forName("UTF-8"))
                    .addBinaryBody("i", text.getBytes(),
                            ContentType.MULTIPART_FORM_DATA, "input")
                    .addTextBody("com", "no")
                    .addTextBody("syl", getSyllables ? "yes" : "no")
                    .addTextBody("outsym", "ipa").addTextBody("oform", "txt")
                    .addTextBody("iform", "list") // txt?
                    .addTextBody("align", "no").addTextBody("lng", loc)
                    .addTextBody("featset",
                            extendedFeatures ? "extended" : "standard")
                    .build();
            String result = Request.Post(uriBui.build()).body(entity).execute()
                    .returnContent().asString();
            Document doc = Utilities.parseXML(result);
            Element link = Utilities.getElementByTagName(doc, "downloadLink");
            if (link != null && !"".equals(link.getTextContent())) {
                String retString = Request.Get(link.getTextContent()).execute()
                        .returnContent().asString(Charset.forName("UTF-8"))
                        .replace(" ", "");
                ret = Optional.of(WORD_SEPARATOR.split(retString.trim()));
            }
        } catch (URISyntaxException | IOException | ParserConfigurationException
                | SAXException e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    /**
     * count signs in transcription
     *
     * @param words
     *            an {@link Optional} array of transcribed words
     * @param syllabified
     *            whether syllable separators (full stops) are present and
     *            should be removed
     * @return the word lengths in signs
     */
    public static int[] countSigns(Optional<String[]> words,
            boolean syllabified) {
        if (words.isPresent())
            return countSigns(words.get());
        else
            return new int[] {};
    }

    /**
     * @see #countSigns(Optional, Boolean)
     * @param words
     *            (not syllabified)
     * @return the word lengths in signs
     */
    public static int[] countSigns(Optional<String[]> words) {
        return countSigns(words, false);
    }

    /**
     * @see #countSigns(Optional, Boolean)
     * @param text
     *            a string to be split in words and transcribed
     * @param separator
     *            the word separator {@link Pattern}
     * @return number of characters
     */
    public static int[] countSigns(String text, Pattern separator) {
        return countSigns(separator.split(text));
    }

    /**
     * @param text
     *            the text to count word lenghts for
     * @see #countSigns(String)
     * @return number of characters
     **/
    public static int[] countSigns(String text) {
        return countSigns(text.split("\\s+"));
    }

    /**
     * count signs in transcription
     *
     * @param words
     *            an array of transcribed words
     * @param syllabified
     *            whether syllable separators (full stops) are present and
     *            should be removed
     * @return the word lengths in signs
     */

    public static int[] countSigns(String[] words, boolean syllabified) {
        Stream<String> wordStream = Stream.of(words);
        if (syllabified) // remove syllable limits
            wordStream = wordStream.map(s -> s.replace(".", ""));
        return wordStream.mapToInt(word -> word.length()).toArray();
    }

    /**
     * @see #countSigns(Optional, Boolean)
     * @param words
     *            an array of transcribed words
     * @return lengths of individual words
     */
    public static int[] countSigns(String[] words) {
        Stream<String> wordStream = Stream.of(words);
        return wordStream.mapToInt(word -> word.length()).toArray();
    }

    public static int[] countSyllables(String[] words) {
        Stream<String> wordStream = Stream.of(words);
        return wordStream.mapToInt(word -> word.split("\\.").length).toArray();
    }

    /**
     * count syllables in transcription
     *
     * @param words
     *            an array of transcribed words
     * @param loc
     *            the locale
     * @return the word lengths in signs
     */
    public static int[] countSyllables(String words, String loc) {
        Optional<String[]> result = getTranscription(words, loc, true);
        if (result.isPresent())
            return countSyllables(result.get());
        else
            return new int[] {};
    }

    public static String printCounts(String text) {
        return printCounts(countSigns(text));
    }

    public static String printCounts(int[] counted) {
        return Seq.seq(Arrays.stream(counted)).map(i -> {
            String format = "% " + i + "d";
            return String.format(format, i);
        }).toString(" ");
    }

    public static void main(String[] args) {
        String text = "Dies Beispiel ist bezaubernd schön – Strumpf!";
        if (args.length > 0) {
            text = String.join(" ", args);
        }
        Optional<String[]> result = getTranscription(text, "deu");
        if (result.isPresent()) {
            text = text.trim();
            System.out.println(String.join(" ", text));
            System.out.println(printCounts(countSigns(text)));
            System.out.println(String.join(" ", result.get()));
            int[] counted = countSigns(result, true);
            System.out.println(printCounts(counted));
            System.out.println(String.format("Syllables: %s",
                    Arrays.toString(countSyllables(text, "deu"))));
        }
    }
}
