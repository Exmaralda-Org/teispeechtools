package de.ids.mannheim.clarin.teispeech.tools;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.korpora.useful.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import de.ids.mannheim.clarin.teispeech.data.NameSpaces;

/**
 * A normalizer based on the dictionaries from the EXMARaLDA project, namely
 * <ul>
 * <li>a dictionary of normalizations and frequencies from the FOLK project
 * and</li>
 * <li>a dictionary of capitalized-only-occurring words from Deutsches
 * Referenzkorpus (DeReKo).</li>
 * </ul>
 *
 * @author bfi
 *
 */
public class DictionaryNormalizer implements WordNormalizer {

    // the dictionary:
    private static Map<String, String> dict = new ConcurrentHashMap<>();

    private final static Logger LOGGER = LoggerFactory
            .getLogger(DictionaryNormalizer.class.getName());

    // where to load dictionaries from:
    private static final String FOLKS_PATH = "FOLK_Normalization_Lexicon.xml";
    private static String DEREKO_PATH = "dereko_capital_only.txt";
    private static String DICT_PATH = "dict.tsv";
    private static String DICT_PATH_FILE = "src/main/resources/" + DICT_PATH;

    private static boolean folkLoaded = false;
    private static boolean derekoLoaded = false;

    private static boolean debug;

    private static final BinaryOperator<String> strCollider = (u, v) -> {
        LOGGER.warn("«{}» ignored, already an entry for «{}»", v, u);
        return u;
    };

    /**
     * load the dictionary generated from the FOLK data
     *
     * @param force
     *            whether to force loading even if data already loaded
     * @throws IOException
     *             if file (included) broken/unavailable
     */
    private static void loadFolksDict(boolean force) throws IOException {
        if (folkLoaded && !force) {
            return;
        }
        try (InputStream folkSource = DictionaryNormalizer.class
                .getClassLoader().getResourceAsStream(FOLKS_PATH)) {
            Document document;
            try {
                document = Utilities.parseXML(folkSource);
            } catch (SAXException e) {
                throw new RuntimeException(
                        "Dictionary broken! – " + e.getMessage());
            } catch (ParserConfigurationException ex) {
                throw new RuntimeException(
                        "XML parsing broken! – " + ex.getMessage());
            }
            Utilities
                    .toElementStream(document
                            .getElementsByTagNameNS(NameSpaces.TEI_NS, "entry"))
                    .forEach(entry -> {
                        String from = entry.getAttribute("form");
                        String to = Utilities
                                .toElementStream(entry.getElementsByTagNameNS(
                                        NameSpaces.TEI_NS, "n"))
                                .max(Comparator.comparing(e -> Integer
                                        .parseInt(e.getAttribute("freq"))))
                                .get().getAttribute("corr");
                        dict.put(from, to);
                    });
            LOGGER.info(String.format("FOLK only: %d entries", dict.size()));
            folkLoaded = true;
        }
    }

    /**
     * Do something with the DeReKo file
     *
     * @param con
     *            a Consumer that uses the {@link BufferedReader} on the DeReKo
     *            file
     */
    private static void withDerekoReader(Consumer<BufferedReader> con) {
        try (InputStream derekoStream = DictionaryNormalizer.class
                .getClassLoader().getResourceAsStream(DEREKO_PATH);
                InputStreamReader derekoReader = new InputStreamReader(
                        derekoStream, Charset.forName("windows-1252"));
                BufferedReader buf = new BufferedReader(derekoReader)) {
            con.accept(buf);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * load the dictionary generated from DeReKo data
     *
     * @param force
     *            the whether to force loading even if data already loaded
     * @throws IOException
     *             if file (included) broken/unavailable
     */
    private static void loadDerekoDict(boolean force) throws IOException {
        if (derekoLoaded && !force) {
            return;
        }
        if (debug) {
            withDerekoReader(derekoReader -> {
                // only for statistics at the moment
                Map<String, String> derekoDict = derekoReader.lines().parallel()
                        .map(StringUtils::strip)
                        .collect(Collectors.toMap(l -> l.toLowerCase(),
                                Function.identity(), strCollider,
                                ConcurrentHashMap::new));
                LOGGER.info("DEREKO: {} entries", derekoDict.size());
            });
        }
        withDerekoReader(derekoReader -> {
            derekoReader.lines()
                    // .parallel() // uncomment if order is irrelevant
                    .map(StringUtils::strip).filter(s -> !s.isEmpty())
                    .forEach(l -> dict.putIfAbsent(l.toLowerCase(), l));
            LOGGER.info("Final dictionary: {} entries", dict.size());
            derekoLoaded = true;
        });
    }

    /**
     * load the dictionary compiled from both FOLK and DeReKo
     */
    private static void loadCompiledDict() {
        try (InputStream dictSource = DictionaryNormalizer.class
                .getClassLoader().getResourceAsStream(DICT_PATH);
                InputStreamReader dictReader = new InputStreamReader(
                        dictSource);
                BufferedReader dictBReader = new BufferedReader(dictReader);) {
            dict = dictBReader.lines().parallel().map(l -> l.split("\t"))
                    .collect(Collectors.toMap(l -> l[0], l -> l[1], strCollider,
                            ConcurrentHashMap::new));
            derekoLoaded = true;
            folkLoaded = true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * write the combined FOLK/DeReKo dictionary to TSV (will be included in the
     * JAR if available)
     */
    public static void writeDict() {
        try (FileWriter outF = new FileWriter(DICT_PATH_FILE);
                PrintWriter out = new PrintWriter(outF);) {

            Collator collator = Collator.getInstance(Locale.GERMAN);
            collator.setStrength(Collator.PRIMARY);
            dict.entrySet().stream()
                    .sorted(Comparator.comparing(c -> c.getKey(), collator))
                    .forEach(entry -> out.println(String.format("%s\t%s",
                            entry.getKey(), entry.getValue())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.format("Wrote dictionary to <%s>.\n", DICT_PATH_FILE);
    }

    /**
     * load the dictionaries
     *
     * @param force
     *            whether to try loading the compiled dictionary, and whether to
     *            force loading the DeReKo- and FOLK-derived dictionaries
     */
    public static void loadDictionary(boolean force) {
        if (!force) {
            try {
                loadCompiledDict();
            } catch (NullPointerException e) {
                force = true;
                LOGGER.warn("Compiled dictionary not available – "
                        + "forcing reload of sources");
            }
        }
        if (force) {
            try {
                LOGGER.warn(
                        "Load (force = {} / folkLoaded = {}, derekoLoaded = {})",
                        force, folkLoaded, derekoLoaded);
                loadFolksDict(force);
                loadDerekoDict(force);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    private final boolean keepCase;

    /**
     * make a {@link DictionaryNormalizer}
     *
     * @param keepCase
     *            whether to keep upper case letters (and hence leave them
     *            untouched)
     *
     * @param debugging
     *            whether to give more info
     */
    public DictionaryNormalizer(boolean keepCase, boolean debugging) {
        debug = debugging;
        this.keepCase = keepCase;
        loadDictionary(false);
    }

    /**
     * make a non-debugging {@link DictionaryNormalizer}
     */
    public DictionaryNormalizer() {
        this(true, false);
    }

    @Override
    public String getNormalised(String in) {
        String seek;
        if (keepCase)
            seek = in;
        else
            seek = in.toLowerCase();
        return dict.getOrDefault(seek, in);
    }
}
