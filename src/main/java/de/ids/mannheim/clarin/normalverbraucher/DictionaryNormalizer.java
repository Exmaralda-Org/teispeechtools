package de.ids.mannheim.clarin.normalverbraucher;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;


import java.nio.charset.Charset;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * A normalizer based on the dictionaries from the EXMARaLDA project,
 * namely
 * <ul>
 * <li>a dictionary of normalizations and frequencies from the FOLK project and</li>
 * <li>a dictionary of capitalized-only-occurring words from Deutsches Referenzkorpus (DeReKo).</li>
 * </ul>
 * @author bfi
 *
 */
public class DictionaryNormalizer implements WordNormalizer {

    // the dictionary:
    private static Map<String, String> folksDict = new ConcurrentHashMap<>();

    private final static Logger LOGGER = LoggerFactory.getLogger(
            DictionaryNormalizer.class.getName());

    // where to load dictionaries from:
    private static final String FOLKS_PATH = "/main/resources/FOLK_Normalization_Lexicon.xml";
    private static String DEREKO_PATH = "/main/resources/dereko_capital_only.txt";

    private static boolean folkLoaded = false;
    private static boolean derekoLoaded = false;
    private static final SAXReader reader = new SAXReader();

    private static boolean debug;

    public final BinaryOperator<String> strCollider =
            (u, v) -> {
                LOGGER.warn("«{}» ignored, already an entry for «{}»", v, u);
                return u;
    };


    private void loadFolksDict() {
        loadFolksDict(false);
    }
    private void loadFolksDict(boolean force) {
        InputStream folkSource = DictionaryNormalizer.class
                .getResourceAsStream(FOLKS_PATH);
        if (folkLoaded && !force) {
            return;
        }
        Document document;
        try {
            document = reader.read(folkSource);
        } catch (DocumentException e) {
            throw new RuntimeException("Dictionary broken! – " + e.getMessage());
        }
        document.getRootElement().elements("entry").parallelStream()
            .forEach(entryN -> {
                    Element entry = (Element) entryN;
                    String from = entry.attributeValue("form");
                    String to = entry.elements("n").parallelStream()
                        .collect(Collectors.maxBy(
                            Comparator.comparing(
                                e -> Integer.parseInt(e.attributeValue("freq")))))
                        .get().attributeValue("corr");
                folksDict.put(from, to);
        });
        LOGGER.info(String.format("FOLK only: %d entries", folksDict.size()));
        folkLoaded = true;
    }

    private static BufferedReader getDerekoReader () {
        InputStream derekoStream = DictionaryNormalizer.class
                .getResourceAsStream(DEREKO_PATH);
        InputStreamReader derekoReader = new InputStreamReader(derekoStream,
                Charset.forName("windows-1252"));
        return new BufferedReader(derekoReader);
    }

    private void loadDerekoDict() {
        loadDerekoDict(false);
    }
    private void loadDerekoDict(boolean force) {
        if (derekoLoaded && !force) {
            return;
        }
        if (debug) {
            // only for statistics at the moment
            Map<String, String> derekoDict = getDerekoReader().lines().parallel()
                .map(String::trim).collect(
                        Collectors.toMap(
                            l -> l.toLowerCase(),
                            Function.identity(),
                            strCollider,
                            ConcurrentHashMap::new
            ));
            LOGGER.info("DEREKO: {} entries", derekoDict.size());
            LOGGER.info("FOLK [before]: {} entries", folksDict.size());
        }
        getDerekoReader().lines()
            // .parallel() // uncomment if order is irrelevant
            .map(String::trim).filter(String::isEmpty).forEach(
                    l -> folksDict.putIfAbsent(l.toLowerCase(), l));
        LOGGER.info("FOLK [finally]: {} entries", folksDict.size());
        derekoLoaded = true;
    }

    /**
     * reload dictionaries
     *
     * TODO: useless at the moment, as dictionaries are loaded from the WAR.
     */
    public void reloadDict() {
        folksDict = new ConcurrentHashMap<>();
        loadFolksDict(true);
        loadDerekoDict(true);
    }

    @Override
    public String getNormalised(String in) {
        return folksDict.get(in);
    }

    public DictionaryNormalizer() {
        this(false);
    }
    public DictionaryNormalizer(boolean debugging) {
        debug = debugging;
        loadFolksDict();
        loadDerekoDict();
    }
}
