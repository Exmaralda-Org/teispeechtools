package de.ids.mannheim.clarin.teispeech.workflow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ids.mannheim.clarin.teispeech.data.DocUtilities;
import de.ids.mannheim.clarin.teispeech.data.NameSpaces;
import org.annolab.tt4j.TreeTaggerException;
import org.annolab.tt4j.TreeTaggerWrapper;
import org.korpora.useful.Utilities;
import org.korpora.useful.LangUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * POS-tag documents in the TEI transcription format with the TreeTagger
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
    private final String language;

    /**
     * XML DOM document
     */
    private final Document doc;

    /**
     * TreeTagger wrapper
     */
    private TreeTaggerWrapper<Element> treeTagger;

    private static final String TREETAGGER_PATH = "/opt/treetagger";
    static {
        System.setProperty("treetagger.home", TREETAGGER_PATH);
    }
    /**
     * file with TreeTagger model names
     */
    private static final String MODELS_PATH = "treeTagger-languages.json";

    /**
     * models for TreeTagger – three letter language code to model file name
     */
    @SuppressWarnings("CanBeFinal")
    private static Map<String, String> modelMap;

    static {
        ObjectMapper mapper = new ObjectMapper();
        try {
            modelMap = mapper.readValue(
                    TEIPOS.class.getClassLoader()
                            .getResourceAsStream(MODELS_PATH),
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
     * @param modelName the model name
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
     */
    private void tagByLanguage(String lang, List<Element> utterances,
            boolean force) throws IOException {
        String model = modelMap.get(lang);
        assert model != null;
        String modelFName = modelName(model);
        LOGGER.info("model file is: {}", modelFName);
        treeTagger.setModel(modelFName);
        for (Element u : utterances) {
            List<Element> words = Utilities
                    .toElementStream(
                            u.getElementsByTagNameNS(NameSpaces.TEI_NS, "w"))
                    .filter(ut -> !ut.getAttributeNS(NameSpaces.TEI_NS, "type")
                            .equals("incomprehensible"))
                    .collect(Collectors.toList());
            if (!force && words.stream().allMatch(
                    e -> e.hasAttributeNS(NameSpaces.TEI_NS, "pos"))) {
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
     */
    public void posTag(boolean force) {

        // aggregate by language to avoid restarting the tagger all the time
        treeTagger = new TreeTaggerWrapper<>();
        try {
            treeTagger.setAdapter(elly -> elly.hasAttribute("normalizer")
                    ? elly.getAttribute("normalizer")
                    : Utilities.removeSpace(elly.getTextContent()));
            treeTagger.setHandler((token, pos, lemma) -> {
                token.setAttributeNS(NameSpaces.TEI_NS, "pos", pos);
                token.setAttributeNS(NameSpaces.TEI_NS, "lemma", lemma);
            });
            List<String> tagged = new ArrayList<>();
            List<String> untagged = new ArrayList<>();
            DocUtilities.groupByLanguage("u", doc, language, 1)
                    .forEach((uLanguage, utters) -> {
                        uLanguage = LangUtilities.getLanguage(uLanguage, uLanguage);
                        if (modelMap.containsKey(uLanguage)) {
                            try {
                                tagByLanguage(uLanguage, utters, force);
                                tagged.add(uLanguage);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            untagged.add(uLanguage);
                            LOGGER.info("Could not handle {} utterances in {}.",
                                    utters.size(), uLanguage);
                        }
                    });
            DocUtilities.makeChange(doc, "POS-tagged with TreeTagger", tagged,
                    untagged);
        } finally {
            treeTagger.destroy();
        }
    }

}
