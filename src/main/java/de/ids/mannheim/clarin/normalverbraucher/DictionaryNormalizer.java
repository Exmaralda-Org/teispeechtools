package de.ids.mannheim.clarin.normalverbraucher;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;


import java.nio.charset.Charset;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class DictionaryNormalizer implements WordNormalizer {

    private Map<String, String> folksDict = new ConcurrentHashMap<>();
    private Map<String, String> derekoDict;
    private static final InputStream folkSource =
            DictionaryNormalizer.class.getResourceAsStream("/main/resources/FOLK_Normalization_Lexicon.xml");
    private static String derekoFile = "/main/resources/dereko_capital_only.txt";
    private static final SAXReader reader = new SAXReader();

    public final BinaryOperator<String> strCollider =
            (u, v) -> {
                // throw new IllegalStateException(String.format("Duplicate key «%s»", u));
                System.err.format("«%s» ignored, already an entry for «%s»\n", v, u);
                return u;
    };
    
    private void getFolksDict() {
        Document document;
        try {
            document = reader.read(folkSource);
        } catch (DocumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
        document.getRootElement().elements("entry").parallelStream()
            .forEach(entry -> {
                    String from = entry.attributeValue("form");
                    Stream<Element> str = entry.elements("n").parallelStream();
                    Element maxEl = str.collect(Collectors.maxBy(
                        Comparator.comparing(
                            e -> Integer.parseInt(((e.attributeValue("freq"))))))).get();
                    String to = maxEl.attributeValue("corr");
                folksDict.put(from, to);
        });
        System.err.format("FOLK: %d entries\n", folksDict.size());
    }

    private BufferedReader getDerekoReader () {
        InputStream derekoStream = DictionaryNormalizer.class.getResourceAsStream(derekoFile);
        InputStreamReader derekoReader = new InputStreamReader(derekoStream,
                Charset.forName("windows-1252"));
        return new BufferedReader(derekoReader);

    }
    private void getDerekoDict() {

        derekoDict = getDerekoReader().lines().parallel().map(String::trim).collect(
                Collectors.toMap(
                        l -> l.toLowerCase(),
                        Function.identity(),
                        strCollider,
                        ConcurrentHashMap::new
        ));
        System.err.format("DEREKO: %d entries\n", derekoDict.size());    
        System.err.format("FOLK [before]: %d entries\n", folksDict.size());
        getDerekoReader().lines().parallel().map(String::trim).forEach(
                        l -> folksDict.putIfAbsent(l.toLowerCase(), l));
        System.err.format("FOLK [ after]: %d entries\n", folksDict.size());
    }

    public String getNormalised(String in) {
        return folksDict.get(in);
    }
    
    public DictionaryNormalizer() {
        getFolksDict();
        getDerekoDict();
    }
}
