package de.ids.mannheim.clarin.teispeech.tools;

/**
 * A WordNormalizer can normalize single words.
 *
 * @author bfi
 *
 */
interface WordNormalizer {

    /**
     * normalize a single word
     *
     * @param word
     *            – the form to be normalized
     * @return normalized form
     */
    String getNormalised(String word);

}
