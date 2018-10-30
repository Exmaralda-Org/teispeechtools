package de.ids.mannheim.clarin.teispeech.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.annolab.tt4j.TreeTaggerException;
import org.annolab.tt4j.TreeTaggerWrapper;
import org.korpora.useful.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * a normalizer for the TEI transcription format.
 *
 * pos-tags all &lt;w&gt; Elements in a document.
 *
 * @author bfi
 *
 */
public class TEIPOS {

    private final static Logger LOGGER = LoggerFactory
            .getLogger(TEIPOS.class.getName());

    /**
     * default language
     */
    private String language;

    /**
     * XML DOM document
     */
    private Document doc;

    /**
     * TreeTagger wrapper
     */
    TreeTaggerWrapper<Element> treeTagger;

    private static final String TREETAGGER_PATH = "/opt/treetagger";
    static {
        System.setProperty("treetagger.home", TREETAGGER_PATH);
    }
    /**
     * file with TreeTagger model names
     */
    private static final String MODELS_PATH = "/main/resources/treeTagger-languages.json";

    /**
     * models for TreeTagger – three letter language code to model file name
     */
    private static Map<String, String> modelMap = new HashMap<>();

    static {
        ObjectMapper mapper = new ObjectMapper();
        try {
            modelMap = mapper.readValue(
                    TEIPOS.class.getResourceAsStream(MODELS_PATH),
                    new TypeReference<Map<String, String>>() {
                    });
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * make new {@link TEIPOS} for
     *
     * @param doc
     *            a DOM XML document
     * @param language
     *            the language
     */
    public TEIPOS(Document doc, String language) {
        this.language = language;
        this.doc = doc;
        // Point TT4J to the TreeTagger installation directory. The executable
        // is expected
        // in the "bin" subdirectory - in this example at
        // "/opt/treetagger/bin/tree-tagger"
    }

    /**
     * get TreeTagger model file name
     *
     * @param modelName
     * @return file name
     */
    private static String modelName(String modelName) {
        return System.getProperty("treetagger.home") + "/lib/" + modelName
                + ".par:utf-8";
    }

    /**
     * tag all utterances of one language
     *
     * @param lang
     *            the language code
     * @param utterances
     *            the list of &lt;u&gt; elements
     * @throws IOException
     */
    private void tagByLanguage(String lang, List<Element> utterances,
            boolean force) throws IOException {
        String modelLang = DocUtilities.languageMap.get(lang);
        assert modelLang != null;
        String model = modelMap.get(modelLang);
        assert model != null;
        String modelFName = modelName(model);
        LOGGER.info("model file is: {}", modelFName);
        treeTagger.setModel(modelFName);
        for (Element u : utterances) {
            List<Element> words = Utilities
                    .toElementList(u.getElementsByTagName("w"));
            if (!force && words.stream().allMatch(e -> e.hasAttribute("pos"))) {
                continue;
            }
            try {
                treeTagger.process(words);
            } catch (TreeTaggerException | IOException tte) {
                throw new RuntimeException(tte);
            }
        }
        LOGGER.info("Tagged {} utterances in {}.", utterances.size(), lang);
    }

    /**
     * pos-tag the document
     *
     * @param force
     *            whether to force tagging even if utterance already tagged
     *
     * @return current {@link TEIPOS} instance, for chaining
     */
    public Document posTag(boolean force) {

        // aggregate by language to avoid restarting the tagger all the time
        treeTagger = new TreeTaggerWrapper<>();
        try {
            treeTagger.setAdapter(elly -> {
                // TODO Auto-generated method stub
                return elly.hasAttribute("normalizer")
                        ? elly.getAttribute("normalizer")
                        : Utilities.removeSpace(elly.getTextContent());
            });
            treeTagger.setHandler((token, pos, lemma) -> {
                token.setAttribute("pos", pos);
                token.setAttribute("lemma", lemma);
            });
            List<String> tagged = new ArrayList<>();
            List<String> untagged = new ArrayList<>();
            DocUtilities.groupByLanguage("u", doc, language)
                    .forEach((language, utters) -> {
                        if (modelMap.containsKey(language)) {
                            try {
                                tagByLanguage(language, utters, force);
                                tagged.add(language);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            untagged.add(language);
                            LOGGER.info("Could not handle {} utterances in {}.",
                                    utters.size(), language);
                        }
                    });
            DocUtilities.makeChange(doc, "POS-tagged with TreeTagger", tagged,
                    untagged);
        } finally {
            treeTagger.destroy();
        }
        return doc;
    }

}
