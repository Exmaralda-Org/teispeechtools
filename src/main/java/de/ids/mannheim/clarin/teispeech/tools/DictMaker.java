package de.ids.mannheim.clarin.teispeech.tools;

import de.ids.mannheim.clarin.teispeech.workflow.DictionaryNormalizer;

/**
 * compile a dictionary from the FOLK and DeReKo dictionaries referenced in
 * {@link DictionaryNormalizer}
 *
 * @author bfi
 *
 */

class DictMaker {
    public static void main(String[] args) {
        DictionaryNormalizer.loadDictionary(true);
        DictionaryNormalizer.writeDict();
    }
}
