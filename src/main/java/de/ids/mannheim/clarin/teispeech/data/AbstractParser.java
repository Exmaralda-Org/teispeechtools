/**
 * abstract parser from EXMARaLDA, adjusted to JDOM2
 * @author Thomas Schmidt
 */
package de.ids.mannheim.clarin.teispeech.data;

import org.jdom2.Document;

/**
 *
 * @author thomas
 */
public abstract class AbstractParser {

    public AbstractParser() {
    }

    public abstract void parseDocument(Document doc, int parseLevel);

}
