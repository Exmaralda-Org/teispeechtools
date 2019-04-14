package de.ids.mannheim.clarin.teispeech.data;

import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.jooq.lambda.Seq;
import org.korpora.useful.Utilities;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import de.ids.mannheim.clarin.teispeech.tools.DocUtilities;
import de.ids.mannheim.clarin.teispeech.tools.TextToTEI;

import static de.ids.mannheim.clarin.teispeech.data.NameSpaces.TEI_NS;
import static de.ids.mannheim.clarin.teispeech.tools.DocUtilities.getAttTEI;

/**
 * TEI annotated speech document, mainly for use in {@link TextToTEI}
 *
 * @author bfi
 */

public class SpeechDocument {

    /**
     * the XML DOM document
     */
    private final Document doc;

    /**
     * the current {@code <annotationBlock>}
     */
    private Element currentBlock;

    /**
     * the current {@code <u>}
     */
    private Element currentUtterance;

    /**
     * the ID of the current speaker
     */
    private String currentSpeaker;

    /**
     * the document language
     */
    private String language;

    /**
     * a speech document has
     *
     * @param doc
     *            an XML document
     * @param lang
     *            a document language (preferably a ISO 639-1 three letter code)
     */
    public SpeechDocument(Document doc, String lang) {
        language = lang;
        this.doc = doc;
        setLanguage(lang);
    }

    /**
     * @return the XML DOM document
     */
    public Document getDocument() {
        return doc;
    }

    /**
     * set the language to code specified by {@code language}
     *
     * @param language
     *            should be an ISO 639-1 three letter code
     */
    public void setLanguage(String language) {
        Element el = Utilities.getElementByTagNameNS(doc, TEI_NS,
                "text");
        el.setAttribute("xml:lang", language);
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
            Element head = (Element) doc
                    .getElementsByTagNameNS(TEI_NS, "teiHeader")
                    .item(0);
            Element before = (Element) doc
                    .getElementsByTagNameNS(TEI_NS, "profileDesc")
                    .item(0);
            Comment comment = doc.createComment(
                    String.format("[ There were errors parsing your text. "
                            + " Please refer to online documentation "
                            + "on how to correct them. ]"));
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
        Element timeLine = (Element) doc
                .getElementsByTagNameNS(TEI_NS, "timeline").item(0);
        Iterator<Event> iter = events.descendingIterator();
        while (iter.hasNext()) {
            Event e = iter.next();
            // <when xml:id="TLI_1" interval="6.1" since="TLI_0"/>
            Element el = doc.createElementNS(TEI_NS, "tei:when");
            el.setAttribute("xml:id", e.mkTime());
            timeLine.appendChild(el);
            if (e instanceof MarkedEvent) {
                Comment explainMark = doc.createComment("marked as ‹"
                        + ((MarkedEvent) e).getMark() + "› in the input.");
                timeLine.insertBefore(explainMark, el);
                el = doc.createElementNS(TEI_NS, "tei:when");
                el.setAttribute("xml:id", ((MarkedEvent) e).mkEndTime());
                timeLine.appendChild(el);
            }
        }
    }

    /**
     * insert sorted list of speakers
     *
     * @param speakers
     *            the speakers
     */
    public void makeSpeakerList(Collection<String> speakers) {
        // iterate over speakers
        // <person xml:id="LB" n="LB">
        // <persName>
        // <abbr>LB</abbr>
        // </persName>
        // </person>
        Element list = (Element) doc
                .getElementsByTagNameNS(TEI_NS, "particDesc")
                .item(0);
        speakers.stream().sorted().forEach(s -> {
            Element person = doc.createElementNS(TEI_NS, "person");
            Element persName = doc.createElementNS(TEI_NS,
                    "persName");
            Element abbr = doc.createElementNS(TEI_NS, "abbr");
            Text tx = doc.createTextNode(s);
            abbr.appendChild(tx);
            persName.appendChild(abbr);
            person.appendChild(persName);
            person.setAttribute("xml:id", s);
            person.setAttributeNS(TEI_NS, "n", s);
            list.appendChild(person);
        });
    }

    public Element addAnnotationBlock(Event from, Event to) {
        Element block = doc.createElementNS(TEI_NS,
                "annotationBlock");
        // Element block = doc.createElementNS(NameSpaces.TEI_NS,
        // "annotationBlock");
        block.setAttributeNS(TEI_NS, "who", currentSpeaker);
        block.setAttributeNS(TEI_NS, "start", from.mkTimeRef());
        block.setAttributeNS(TEI_NS, "end", to.mkTimeRef());
        return block;
    }

    /**
     * adds {@code <u>} with surrounding block and remembers current Utterance
     *
     * @param from
     *            begin event
     * @param to
     *            end evend
     */
    public void addBlockUtterance(Event from, Event to) {
        Element block = addAnnotationBlock(from, to);
        Element utterance = doc.createElementNS(TEI_NS, "u");
        block.appendChild(utterance);
        Element body = (Element) doc
                .getElementsByTagNameNS(TEI_NS, "body").item(0);
        body.appendChild(block);
        currentBlock = block;
        currentUtterance = utterance;
    }

    public void changeBlockStart(Event original, MarkedEvent from) {
        String mark = from.mkTime();
        currentBlock.setAttributeNS(TEI_NS, "start", mark);
        changeOthers(original, from, false);
    }

    public void changeOthers(Event original, MarkedEvent from,
                             boolean isEnd){
        String mark = isEnd ? from.mkTime() : from.mkEndTime();
        Node sib;
        do {
            sib = currentBlock.getPreviousSibling();
            if (sib.getNodeType() == Node.ELEMENT_NODE &&
                    sib.getLocalName().equals("incident")) {
                Element elSib = (Element) sib;
                Seq.of("start", "end").forEach(att -> {
                    if (getAttTEI(elSib, att).equals(original
                            .mkTimeRef())) {
                        elSib.setAttributeNS(TEI_NS, att, mark);
                    }
                });
            }
        } while (sib.getNodeType() != Node.ELEMENT_NODE ||
                "annotationBlock".equals(sib.getLocalName()));
        Utilities
                .toElementStream(currentBlock
                        .getElementsByTagNameNS(TEI_NS, "span"))
                .forEach(b -> {
                    Seq.of("start", "end").forEach(att -> {
                        if (getAttTEI(b, att).equals(original
                                .mkTimeRef())) {
                            b.setAttributeNS(TEI_NS, att, mark);
                        }
                    });
                });

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
        parent.appendChild(anc);
        anc.setAttributeNS(TEI_NS, "synch", at);
    }

    public void addTurn(Event from) {
        // addAnchor(from, currentUtterance);
    }

    /**
     * if turn ends with marked event, update surrounding
     * {@code <annotationBlock>} {@code @to} to last marked event; remove
     * anchor.
     *
     * @param original
     *            the original end event, to be potentially removed
     * @param to
     *            the end event.
     * @return whether a replacement was done
     */
    public boolean endTurn(Event original, Optional<MarkedEvent> to) {
        // addAnchor(to, currentUtterance);
        if (to.isPresent()) {
            Node lastNode = currentUtterance.getLastChild();
            while (lastNode.getNodeType() == Node.TEXT_NODE
                    && StringUtils.strip(lastNode.getTextContent()).isEmpty()) {
                lastNode = lastNode.getPreviousSibling();
            }
            if (lastNode.getNodeType() == Node.ELEMENT_NODE
                    && "anchor".equals(((Element) lastNode).getTagName())) {
                MarkedEvent toM = to.get();
                Element lastAnchor = (Element) lastNode;
                String mark = toM.mkEndTimeRef();
                if (mark.equals(lastAnchor.getAttributeNS(TEI_NS,
                        "synch"))) {
                    currentUtterance.removeChild(lastAnchor);
                    currentBlock.setAttributeNS(TEI_NS, "end", mark);
                    changeOthers(original, toM, true);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * add text and manage whitespace
     *
     * @param text
     *            the text
     * @param space
     *            whether to prepend whitespace
     */
    public void addText(String text, boolean space) {
        Node lc = currentUtterance.getLastChild();
        if (lc != null && space) {
            text = " " + text;
        }
        Text tx = doc.createTextNode(text);
        currentUtterance.appendChild(tx);
    }

    public void addSpace() {
        String text = " ";
        Text tx = doc.createTextNode(text);
        currentUtterance.appendChild(tx);

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
        Element comGroup = doc.createElementNS(TEI_NS, "spanGrp");
        Element com = doc.createElementNS(TEI_NS, "span");
        // Element comGroup = doc.createElementNS(NameSpaces.TEI_NS, "spanGrp");
        // Element com = doc.createElementNS(NameSpaces.TEI_NS, "span");
        com.setAttributeNS(TEI_NS, "type", "comment");
        com.setAttributeNS(TEI_NS, "from", from.mkTimeRef());
        com.setAttributeNS(TEI_NS, "to", to.mkTimeRef());
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
     *            the labelled text
     * @param startAnchor
     *            whether a start anchor will be placed
     */
    public void addMarked(MarkedEvent e, String text, boolean startAnchor) {
        if (startAnchor) {
            addAnchor(e.mkTimeRef(), currentUtterance);
        }
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
     * @param extraPose
     *            whether to embed into utterance or prepose to annotation block
     */
    public void addIncident(Event from, Event to, String text,
            boolean extraPose) {
        Element incident = doc.createElementNS(TEI_NS, "incident");
        Element desc = doc.createElementNS(TEI_NS, "desc");
        // Element incident = doc.createElementNS(NameSpaces.TEI_NS,
        // "incident");
        // Element desc = doc.createElementNS(NameSpaces.TEI_NS, "desc");
        Text tx = doc.createTextNode(text);
        desc.appendChild(tx);
        incident.appendChild(desc);
        incident.setAttributeNS(TEI_NS, "start", from.mkTimeRef());
        incident.setAttributeNS(TEI_NS, "end", to.mkTimeRef());
        if (extraPose) {
            currentUtterance.getParentNode().getParentNode()
                    .insertBefore(incident, currentUtterance.getParentNode());
        } else {
            Utilities.insertAtBeginningOf(incident, currentUtterance);
        }
    }

    /**
     * final words
     */
    public void finish() {
        DocUtilities.makeChange(doc,
                "created from Simple EXMARaLDA plain text transcript; "
                        + "language set to «" + language + "»");
    }

    /**
     * remove utterance or annotation block if empty.
     */
    public void cleanUtterance() {
        if (!currentUtterance.hasChildNodes()) {
            currentBlock.removeChild(currentUtterance);
        }
        if (!currentBlock.hasChildNodes()) {
            currentBlock.getParentNode().removeChild(currentBlock);
        }
    }

    private Map<String, Double> timedEvents = new HashMap<>();
    private Element timeLineRoot = null;

    public void processTimeLine() {
        timeLineRoot = DocUtilities.getTimeLine(doc);
        Utilities.toElementStream(DocUtilities.getWhens(timeLineRoot)).forEach(
                e -> {
                    Optional<Double> offset = DocUtilities.getOffset(e);
                    timedEvents.put(e.getAttribute("xml:id"),
                            offset.orElse(0d));
                }
        );
    }

}
