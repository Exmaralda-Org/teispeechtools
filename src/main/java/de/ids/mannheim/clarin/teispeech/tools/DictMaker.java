package de.ids.mannheim.clarin.teispeech.tools;

public class DictMaker {
    public static void main(String[] args) {
        DictionaryNormalizer.loadDictionary(true);
        DictionaryNormalizer.writeDict();
    }
}
