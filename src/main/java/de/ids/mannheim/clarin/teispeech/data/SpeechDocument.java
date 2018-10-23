package de.ids.mannheim.clarin.teispeech.data;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import de.ids.mannheim.clarin.teispeech.tools.DocUtilities;

/**
 * @author bfi
 */

public class SpeechDocument {

    private static String TEI_NS = "http://www.tei-c.org/ns/1.0";
    private static String XML_NS = "http://www.w3.org/XML/1998/namespace";

    private Document doc;

    private Element currentBlock;
    private Element currentUtterance;
    private String currentSpeaker;


    public SpeechDocument(Document doc){
        this.doc = doc;
    }

    public Document getDocument() {
        return doc;
    }
    public void makeTimeLine(Deque<Event> events) {
        Element timeLine = (Element) doc.getElementsByTagName("timeline").item(0);
        Iterator<Event> iter = events.descendingIterator();
        while (iter.hasNext()) {
                Event e = iter.next();
                // <when xml:id="TLI_1" interval="6.1" since="TLI_0"/>
//                Element el = doc.createElementNS(TEI_NS, "when");
                Element el = doc.createElement("when");
                el.setAttributeNS(XML_NS, "id", e.mkTime());
                timeLine.appendChild(el);
        }
    }

    public void makeSpeakerList(Collection<String> speakers) {
        // iterate over speakers
        //<person xml:id="LB" n="LB">
        //    <persName>
        //        <abbr>LB</abbr>
        //    </persName>
        //</person>
        Element list = (Element) doc.getElementsByTagName("particDesc").item(0);
        speakers.stream().sorted().forEach(
                s -> {
                    Element person = doc.createElementNS(TEI_NS, "person");
                    Element persName= doc.createElementNS(TEI_NS, "persName");
                    Element abbr = doc.createElementNS(TEI_NS, "abbr");
                    Text tx = doc.createTextNode(s);
                    abbr.appendChild(tx);
                    persName.appendChild(abbr);
                    person.appendChild(persName);
                    person.setAttributeNS(XML_NS, "id", s);
                    person.setAttributeNS(XML_NS, "n", s);
                    list.appendChild(person);
                }
        );
    }

    public Element addBlock(Event from, Event to) {
        Element block = doc.createElement("annotationBlock");
//        Element block = doc.createElementNS(TEI_NS, "annotationBlock");
        block.setAttribute("who", currentSpeaker);
        block.setAttribute("from", from.mkTime());
        block.setAttribute("to", to.mkTime());
        return block;
    }

    public void addBlockUtterance(Event from, Event to) {
        Element block = addBlock(from, to);
        Element utterance = doc.createElementNS(TEI_NS, "u");
        block.appendChild(utterance);
        Element body = (Element) doc.getElementsByTagName("body").item(0);
        body.appendChild(block);
        currentBlock = block;
        currentUtterance = utterance;
    }

    public void addAnchor(Event at, Element parent) {
        Element anc = doc.createElementNS(TEI_NS, "anchor");
        anc.setAttribute("synch", at.mkTime());
        parent.appendChild(anc);
    }
    public void addAnchor(String at, Element parent) {
        Element anc = doc.createElementNS(TEI_NS, "anchor");
        anc.setAttribute("synch", at);
        parent.appendChild(anc);
    }

    public void addTurn(Event from) {
        addAnchor(from, currentUtterance);
    }

    public void addText(String text) {
        Node lc = currentUtterance.getLastChild();
        if (lc != null &&
            lc.getNodeType() != Node.ELEMENT_NODE){
            text = " " + text;
        }
        Text tx = doc.createTextNode(text);
        currentUtterance.appendChild(tx);
    }

    public void endTurn(Event to) {
        addAnchor(to, currentUtterance);
    }
    public void addComment(Event from, Event to, String text) {
        Element comGroup = doc.createElement("spanGrp");
        Element com = doc.createElement("span");
//        Element comGroup = doc.createElementNS(TEI_NS, "spanGrp");
//        Element com = doc.createElementNS(TEI_NS, "span");
        com.setAttribute("type", "comment");
        com.setAttribute("from", from.mkTime());
        com.setAttribute("to", to.mkTime());
        Text tx = doc.createTextNode(text);
        com.appendChild(tx);
        comGroup.appendChild(com);
        currentBlock.appendChild(comGroup);
    }
    public void addMarked(MarkedEvent e, String text) {
        addAnchor(e, currentUtterance);
        Text tx = doc.createTextNode(text);
        currentUtterance.appendChild(tx);
        addAnchor(e.mkEndTime(), currentUtterance);
    }

    public void addIncident(Event from, Event to, String text) {
        Element incident = doc.createElement("incident");
        Element desc = doc.createElement("desc");
//        Element incident = doc.createElementNS(TEI_NS, "incident");
//        Element desc = doc.createElementNS(TEI_NS, "desc");
        Text tx = doc.createTextNode(text);
        desc.appendChild(tx);
        incident.appendChild(desc);
        incident.setAttribute("start", from.mkTime());
        incident.setAttribute("end", to.mkTime());
        currentUtterance.appendChild(incident);
    }


    public void finish() {
        DocUtilities.makeChange(doc, "created from Simple EXMARaLDA plain text transcript");
    }

    public void setCurrentSpeaker(String name) {
        currentSpeaker = name;
    }

}
