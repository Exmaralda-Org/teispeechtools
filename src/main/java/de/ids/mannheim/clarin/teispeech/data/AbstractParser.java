package de.ids.mannheim.clarin.teispeech.data;

import org.jdom2.Document;

/**
 * abstract parser from EXMARaLDA, adjusted to JDOM2
 *
 * @author Thomas Schmidt
 */
abstract class AbstractParser {

    AbstractParser() {
    }

    /**
     * parse document
     *
     * @param doc the XML document
     * @param parseLevel the parse level
     */
    public abstract void parseDocument(Document doc, int parseLevel);

}
