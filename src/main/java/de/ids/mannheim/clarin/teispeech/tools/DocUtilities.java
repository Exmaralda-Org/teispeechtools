package de.ids.mannheim.clarin.teispeech.tools;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class DocUtilities {
    /**
     * add a change to the end of the &lt;revisionDesc&gt;
     *
     * @param doc
     *            – the document
     * @return – the document again
     */
    public static Document makeChange(Document doc, String change) {
        String stamp = ZonedDateTime.now(ZoneOffset.systemDefault())
                .format(DateTimeFormatter.ISO_INSTANT);
        NodeList revDescs = doc.getElementsByTagName("revisionDesc");
        if (revDescs.getLength() > 0) {
            Element changeEl = doc.createElement("change");
            changeEl.setAttribute("when", stamp);
            changeEl.appendChild(doc.createTextNode(change));
            revDescs.item(0).appendChild(changeEl);
        }
        return doc;
    }

}
