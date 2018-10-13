package de.ids.mannheim.clarin.normalverbraucher;

/**
 * A WordNormalizer can normalize single words.
 * @author bfi
 *
 */
public interface WordNormalizer {

    /**
     * normalize a single word
     * @param word
     * @return normalized form
     */
    String getNormalised(String word);

}
