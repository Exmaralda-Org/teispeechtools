package de.ids.mannheim.clarin.teispeech.workflow;

/**
 * A WordNormalizer can normalize single words.
 *
 * @author bfi
 *
 */
public interface WordNormalizer {

    /**
     * normalize a single word
     *
     * @param word
     *            – the form to be normalized
     * @return normalized form
     */
    String getNormalised(String word);

}
