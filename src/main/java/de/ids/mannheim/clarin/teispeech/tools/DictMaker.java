package de.ids.mannheim.clarin.teispeech.tools;

/**
 * compile a dictionary from the FOLK and DeReKo dictionaries referenced in
 * {@link DictionaryNormalizer}
 *
 * @author bfi
 *
 */
public class DictMaker {
    public static void main(String[] args) {
        DictionaryNormalizer.loadDictionary(true);
        DictionaryNormalizer.writeDict();
    }
}
