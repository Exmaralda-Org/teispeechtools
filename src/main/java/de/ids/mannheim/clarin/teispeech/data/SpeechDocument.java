package de.ids.mannheim.clarin.teispeech.data;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Comment;
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

    public SpeechDocument(Document doc) {
        this.doc = doc;
    }

    /**
     * @return XML DOM document
     */
    public Document getDocument() {
        return doc;
    }

    public void setCurrentSpeaker(String name) {
        currentSpeaker = name;
    }

    /**
     * insert list of parsing errors as comments
     *
     * @param errors
     *            list of errors
     */
    public void makeErrorList(List<String> errors) {
        if (errors.size() > 0) {
            Element head = (Element) doc.getElementsByTagName("teiHeader")
                    .item(0);
            Element before = (Element) doc.getElementsByTagName("profileDesc")
                    .item(0);
            Comment comment = doc
                    .createComment("[ There were errors parsing your text: ");
            head.insertBefore(comment, before);
            comment = doc.createComment(
                    "  please refer to online documentation to correct them. ]");
            head.insertBefore(comment, before);
            for (String error : errors) {
                comment = doc.createComment("  - " + error + " ");
                head.insertBefore(comment, before);
            }
            comment = doc.createComment("[ end of parsing errors ]");
            head.insertBefore(comment, before);

        }
    }

    /**
     * insert list of events
     *
     * @param events
     *            {@link Deque} of events
     */
    public void makeTimeLine(Deque<Event> events) {
        Element timeLine = (Element) doc.getElementsByTagName("timeline")
                .item(0);
        Iterator<Event> iter = events.descendingIterator();
        while (iter.hasNext()) {
            Event e = iter.next();
            // <when xml:id="TLI_1" interval="6.1" since="TLI_0"/>
            // Element el = doc.createElementNS(TEI_NS, "when");
            Element el = doc.createElement("when");
            el.setAttributeNS(XML_NS, "id", e.mkTimeRef());
            timeLine.appendChild(el);
            if (e instanceof MarkedEvent) {
                Comment explainMark = doc.createComment("marked as ‹"
                        + ((MarkedEvent) e).getMark() + "› in the input.");
                timeLine.insertBefore(explainMark, el);
                el = doc.createElement("when");
                el.setAttributeNS(XML_NS, "id",
                        ((MarkedEvent) e).mkEndTimeRef());
                timeLine.appendChild(el);
            }
        }
    }

    /**
     * insert sorted list of speakers
     *
     * @param speakers
     */
    public void makeSpeakerList(Collection<String> speakers) {
        // iterate over speakers
        // <person xml:id="LB" n="LB">
        // <persName>
        // <abbr>LB</abbr>
        // </persName>
        // </person>
        Element list = (Element) doc.getElementsByTagName("particDesc").item(0);
        speakers.stream().sorted().forEach(s -> {
            Element person = doc.createElementNS(TEI_NS, "person");
            Element persName = doc.createElementNS(TEI_NS, "persName");
            Element abbr = doc.createElementNS(TEI_NS, "abbr");
            Text tx = doc.createTextNode(s);
            abbr.appendChild(tx);
            persName.appendChild(abbr);
            person.appendChild(persName);
            person.setAttributeNS(XML_NS, "id", s);
            person.setAttributeNS(XML_NS, "n", s);
            list.appendChild(person);
        });
    }

    public Element addAnnotationBlock(Event from, Event to) {
        Element block = doc.createElement("annotationBlock");
        // Element block = doc.createElementNS(TEI_NS, "annotationBlock");
        block.setAttribute("who", currentSpeaker);
        block.setAttribute("from", from.mkTimeRef());
        block.setAttribute("to", to.mkTimeRef());
        return block;
    }

    /**
     * adds &lt;u&gt; with surrounding block and sets {@link #currentUtterance}
     *
     * @param from
     *            begin event
     * @param to
     *            end evend
     */
    public void addBlockUtterance(Event from, Event to) {
        Element block = addAnnotationBlock(from, to);
        // Element utterance = doc.createElementNS(TEI_NS, "u");
        Element utterance = doc.createElement("u");
        block.appendChild(utterance);
        Element body = (Element) doc.getElementsByTagName("body").item(0);
        body.appendChild(block);
        currentBlock = block;
        currentUtterance = utterance;
    }

    /**
     * add anchor for synchronization
     *
     * @param at
     *            event from timeline
     * @param parent
     *            parent node
     */
    public void addAnchor(Event at, Element parent) {
        addAnchor(at.mkTimeRef(), parent);
    }

    public void addAnchor(String at, Element parent) {
        Element anc = doc.createElementNS(TEI_NS, "anchor");
        anc.setAttribute("synch", at);
        parent.appendChild(anc);
    }

    public void addTurn(Event from) {
        addAnchor(from, currentUtterance);
    }

    /**
     * add text and manage whitespace
     *
     * @param text
     */
    public void addText(String text) {
        Node lc = currentUtterance.getLastChild();
        if (lc != null && lc.getNodeType() != Node.ELEMENT_NODE) {
            text = " " + text;
        }
        Text tx = doc.createTextNode(text);
        currentUtterance.appendChild(tx);
    }

    public void endTurn(Event to) {
        addAnchor(to, currentUtterance);
    }

    /**
     * add a comment concerning the span between two events
     *
     * @param from
     *            the start
     * @param to
     *            the end
     * @param text
     *            the comment
     */
    public void addComment(Event from, Event to, String text) {
        Element comGroup = doc.createElement("spanGrp");
        Element com = doc.createElement("span");
        // Element comGroup = doc.createElementNS(TEI_NS, "spanGrp");
        // Element com = doc.createElementNS(TEI_NS, "span");
        com.setAttribute("type", "comment");
        com.setAttribute("from", from.mkTimeRef());
        com.setAttribute("to", to.mkTimeRef());
        Text tx = doc.createTextNode(text);
        com.appendChild(tx);
        comGroup.appendChild(com);
        currentBlock.appendChild(comGroup);
    }

    /**
     * add marked event
     *
     * @param e
     *            the event in the timeline
     * @param text
     */
    public void addMarked(MarkedEvent e, String text) {
        addAnchor(e, currentUtterance);
        Text tx = doc.createTextNode(text);
        currentUtterance.appendChild(tx);
        addAnchor(e.mkEndTimeRef(), currentUtterance);
    }

    /**
     * insert incident in annotationBlock before current block
     *
     * @param from
     *            the start of the current block
     * @param to
     *            the start of the current block
     * @param text
     *            description of the incident
     */
    public void addIncident(Event from, Event to, String text) {
        Element block = addAnnotationBlock(from, to);
        Element incident = doc.createElement("incident");
        Element desc = doc.createElement("desc");
        // Element incident = doc.createElementNS(TEI_NS, "incident");
        // Element desc = doc.createElementNS(TEI_NS, "desc");
        Text tx = doc.createTextNode(text);
        desc.appendChild(tx);
        incident.appendChild(desc);
        incident.setAttribute("start", from.mkTimeRef());
        incident.setAttribute("end", to.mkTimeRef());
        block.appendChild(incident);
        currentUtterance.getParentNode().getParentNode().insertBefore(block,
                currentUtterance.getParentNode());
    }

    public void finish() {
        DocUtilities.makeChange(doc,
                "created from Simple EXMARaLDA plain text transcript");
    }

}
