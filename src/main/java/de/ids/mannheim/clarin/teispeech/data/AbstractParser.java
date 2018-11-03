package de.ids.mannheim.clarin.teispeech.data;

import org.jdom2.Document;

/**
 * abstract parser from EXMARaLDA, adjusted to JDOM2
 *
 * @author Thomas Schmidt
 */
public abstract class AbstractParser {

    public AbstractParser() {
    }

    /**
     * parse document
     *
     * @param doc the XML document
     * @param parseLevel the parse level
     */
    public abstract void parseDocument(Document doc, int parseLevel);

}
