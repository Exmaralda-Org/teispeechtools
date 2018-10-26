package de.ids.mannheim.clarin.teispeech.tools;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final String FOLKS_PATH = "/main/resources/FOLK_Normalization_Lexicon.xml";
    private static String DEREKO_PATH = "/main/resources/dereko_capital_only.txt";
    private static String DICT_PATH = "/main/resources/dict.tsv";
    private static String DICT_PATH_FILE = "src" + DICT_PATH;

    private static boolean folkLoaded = false;
    private static boolean derekoLoaded = false;
    private static final SAXReader reader = new SAXReader();

    private static boolean debug;

    public static final BinaryOperator<String> strCollider = (u, v) -> {
        LOGGER.warn("«{}» ignored, already an entry for «{}»", v, u);
        return u;
    };

    public static void loadFolksDict() throws IOException {
        loadFolksDict(false);
    }

    private static void loadFolksDict(boolean force) throws IOException {
        if (folkLoaded && !force) {
            return;
        }
        try (InputStream folkSource = DictionaryNormalizer.class
                .getResourceAsStream(FOLKS_PATH)) {
            Document document;
            try {
                document = reader.read(folkSource);
            } catch (DocumentException e) {
                throw new RuntimeException(
                        "Dictionary broken! – " + e.getMessage());
            }
            document.getRootElement().elements("entry").parallelStream()
                    .forEach(entryN -> {
                        Element entry = entryN;
                        String from = entry.attributeValue("form");
                        String to = entry.elements("n").parallelStream()
                                .collect(Collectors.maxBy(Comparator
                                        .comparing(e -> Integer.parseInt(
                                                e.attributeValue("freq")))))
                                .get().attributeValue("corr");
                        dict.put(from, to);
                    });
            LOGGER.info(String.format("FOLK only: %d entries", dict.size()));
            folkLoaded = true;
        }
    }

    private static BufferedReader getDerekoReader() {
        InputStream derekoStream = DictionaryNormalizer.class
                .getResourceAsStream(DEREKO_PATH);
        InputStreamReader derekoReader = new InputStreamReader(derekoStream,
                Charset.forName("windows-1252"));
        return new BufferedReader(derekoReader);
    }

    public static void loadDerekoDict() throws IOException {
        loadDerekoDict(false);
    }

    private static void loadDerekoDict(boolean force) throws IOException {
        if (derekoLoaded && !force) {
            return;
        }
        if (debug) {
            try (BufferedReader derekoReader = getDerekoReader()) {
                // only for statistics at the moment
                Map<String, String> derekoDict = derekoReader.lines().parallel()
                        .map(String::trim)
                        .collect(Collectors.toMap(l -> l.toLowerCase(),
                                Function.identity(), strCollider,
                                ConcurrentHashMap::new));
                LOGGER.info("DEREKO: {} entries", derekoDict.size());
            }
        }
        try (BufferedReader derekoReader = getDerekoReader()) {
            derekoReader.lines()
            // .parallel() // uncomment if order is irrelevant
            .map(String::trim).filter(s -> !s.isEmpty())
            .forEach(l -> dict.putIfAbsent(l.toLowerCase(), l));
        }
        LOGGER.info("Final dictionary: {} entries", dict.size());
        derekoLoaded = true;
    }

    private static void loadCompiledDict() {
        InputStream dictSource = DictionaryNormalizer.class
                .getResourceAsStream(DICT_PATH);
        InputStreamReader dictReader = new InputStreamReader(dictSource);
        BufferedReader dictBReader = new BufferedReader(dictReader);
        dict = dictBReader.lines().parallel().map(l -> l.split("\t"))
                .collect(Collectors.toMap(l -> l[0], l -> l[1], strCollider,
                        ConcurrentHashMap::new));
        derekoLoaded = true;
        folkLoaded = true;
    }

    // /**
    // * reload dictionaries
    // *
    // * TODO: useless at the moment, as dictionaries are loaded from the WAR.
    // */
    // public static void reloadDict() {
    // dict = new ConcurrentHashMap<>();
    // try {
    // loadFolksDict(true);
    // loadDerekoDict(true);
    // } catch (IOException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // }
    // }

    public static void writeDict() {
        try (FileWriter outF = new FileWriter(DICT_PATH_FILE)) {
            PrintWriter out = new PrintWriter(outF);
            dict.forEach((k, v) -> out.println(String.format("%s\t%s", k, v)));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
        System.err.format("Wrote dictionary to <%s>.\n", DICT_PATH_FILE);
    }

    public static void loadDictionary() {
        loadDictionary(false);
    }

    public static void loadDictionary(boolean force) {
        if (!force) {
            try {
                loadCompiledDict();
            } catch (NullPointerException e) {
                force = true;
                LOGGER.warn(
                        "Internal dictionary not available – forcing reload");
            }
        }
        if (force) {
            try {
                LOGGER.warn(
                        "Load (force = {} / folkLoaded = {}, derekoLoaded = {})",
                        force, folkLoaded, derekoLoaded);
                loadFolksDict(force);
                loadDerekoDict(force);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    @Override
    public String getNormalised(String in) {
        return dict.get(in);
    }

    public DictionaryNormalizer() {
        this(false);
    }

    public DictionaryNormalizer(boolean debugging) {
        debug = debugging;
        loadDictionary();
    }
}
