package de.ids.mannheim.clarin.teispeech.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * utility functions related to ISO-639-related language handling
 * 
 * @author bfi
 *
 */

public class LangUtilities {
    private static final String LANGNAMES_PATH = "/main/resources/languages-639-most-tolerant.json";
    private static final String LANGCODES_3_PATH = "/main/resources/three-letters.txt";
    private static final String LANGCODES_2_PATH = "/main/resources/two-letters.txt";

    /**
     * map from language names / letter triples/tuples to terminological
     * ISO-639-2 code.
     */
    private static Map<String, String> languageMap;

    /**
     * valid terminological ISO-639-2 three letter codes
     *
     * see {@link #languageCodesThree} for a list including bibliographic
     * variants
     */
    private static Set<String> languageTriples;

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
     *            the language code
     * @return whether
     *
     */
    public static boolean isLanguageTriple(String lang) {
        return languageCodesThree.contains(lang.toLowerCase());
    }

    /**
     * Is this a terminological ISO 639-2 three letter code (i.e. a key in
     * languageMap)
     *
     * @param lang
     *            the language code
     * @return whether
     */
    public static boolean isTerminologicalLanguageTriple(String lang) {
        return languageTriples.contains(lang.toLowerCase());
    }

    /**
     * Is this an ISO 639-1 two letter code
     *
     * @param lang
     *            the language code
     * @return whether
     */
    public static boolean isLanguageTuple(String lang) {
        return languageCodesTwo.contains(lang.toLowerCase());
    }

}
