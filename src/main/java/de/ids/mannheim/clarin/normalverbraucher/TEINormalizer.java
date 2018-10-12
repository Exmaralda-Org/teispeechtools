package de.ids.mannheim.clarin.normalverbraucher;

import org.dom4j.Document;

public class TEINormalizer {

    WordNormalizer norm;

    public TEINormalizer(WordNormalizer wn) {
        norm = wn;
    }

    public Document normalize(Document doc) {
        doc.getRootElement().elements("w").parallelStream().forEach(
                el -> {
                    String normal = norm.getNormalised(el.getText());
                    if (normal != null) {
                        el.addAttribute("norm", normal);
                    }
                });
        return doc;
    }

}
