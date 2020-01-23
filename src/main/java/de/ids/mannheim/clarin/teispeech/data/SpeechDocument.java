package de.ids.mannheim.clarin.teispeech.data;

import static de.ids.mannheim.clarin.teispeech.data.NameSpaces.TEI_NS;

import java.text.NumberFormat;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.jooq.lambda.Seq;
import org.korpora.useful.Utilities;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import de.ids.mannheim.clarin.teispeech.workflow.TextToTEIConversion;

/**
 * TEI annotated speech document, mainly for use in {@link TextToTEIConversion}
 *
 * @author bfi
 */

public class SpeechDocument {

    private static final NumberFormat NUMBER_FORMAT = NumberFormat
            .getInstance(Locale.ROOT);
    static {
        Locale.setDefault(Locale.ROOT);
    }

    static {
        NUMBER_FORMAT.setMinimumFractionDigits(0);
        NUMBER_FORMAT.setMaximumFractionDigits(3);
    }

    /**
     * the XML DOM document
     */
    private final Document doc;
    /**
     * the document language
     */
    private final String language;
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

    private Optional<Double> duration;
    private Optional<Double> offset;

    /**
     * a speech document has
     *
     * @param doc
     *     an XML document
     * @param lang
     *     a document language (preferably a ISO 639-1 three letter code)
     */
    public SpeechDocument(Document doc, String lang) {
        language = lang;
        this.doc = doc;
        this.duration = Optional.empty();
        this.offset = Optional.empty();
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
     *     should be an ISO 639-1 three letter code
     */
    private void setLanguage(String language) {
        Element el = Utilities.getElementByTagNameNS(doc, TEI_NS, "text");
        el.setAttribute("xml:lang", language);
    }

    /**
     * set current speaker to name
     *
     * @param name
     */
    public void setCurrentSpeaker(String name) {
        currentSpeaker = name;
    }

    /**
     * set offset
     *
     * @param offset
     */
    public void setOffset(Double offset) {
        this.offset = Optional.of(offset);
    }

    /**
     * set duration
     *
     * @param duration
     */
    public void setDuration(Double duration) {
        this.duration = Optional.of(duration);
    }

    /**
     * get offset
     *
     * @return offset
     */
    public Optional<Double> getOffset() {
        return offset;
    }

    /**
     * get duration
     *
     * @return duration
     */
    public Optional<Double> getDuration() {
        return duration;
    }

    /**
     * insert a time origin T0
     */
    public void insertTimeRoot() {
        DocUtilities.insertTimeRoot(doc);
    }

    /**
     * set duration in XML
     */
    public void applyDuration() {
        DocUtilities.applyDocumentDuration(doc, duration, true);
    }

    /**
     * set offset in XML
     */
    public void applyOffset() {
        DocUtilities.applyDocumentOffset(doc, offset);
    }

    /**
     * insert list of parsing errors as comments
     *
     * @param errors
     *     list of errors
     */
    public void makeErrorList(List<String> errors) {
        if (errors.size() > 0) {
            Element head = (Element) doc
                    .getElementsByTagNameNS(TEI_NS, "teiHeader").item(0);
            Element before = (Element) doc
                    .getElementsByTagNameNS(TEI_NS, "profileDesc").item(0);
            Comment comment = doc
                    .createComment("[ There were errors parsing your text. "
                            + " Please refer to online documentation "
                            + "on how to correct them. ]");
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
     *     {@link Deque} of events
     */
    public void makeTimeLine(Deque<Event> events) {
        Element timeLine = (Element) doc
                .getElementsByTagNameNS(TEI_NS, "timeline").item(0);
        Iterator<Event> iter = events.descendingIterator();
        while (iter.hasNext()) {
            Event e = iter.next();
            // <when xml:id="TLI_1" interval="6.1" since="TLI_0"/>
            Element el = doc.createElementNS(TEI_NS, "when");
            el.setAttribute("xml:id", e.mkTime());
            timeLine.appendChild(el);
            if (e instanceof MarkedEvent) {
                Comment explainMark = doc.createComment("marked as ‹"
                        + ((MarkedEvent) e).getMark() + "› in the input.");
                timeLine.insertBefore(explainMark, el);
                el = doc.createElementNS(TEI_NS, "when");
                el.setAttribute("xml:id", ((MarkedEvent) e).mkEndTime());
                timeLine.appendChild(el);
            }
        }
    }

    /**
     * insert sorted list of speakers
     *
     * @param speakers
     *     the speakers
     */
    public void makeSpeakerList(Collection<String> speakers) {
        // iterate over speakers
        // <person xml:id="LB" n="LB">
        // <persName>
        // <abbr>LB</abbr>
        // </persName>
        // </person>
        Element list = (Element) doc
                .getElementsByTagNameNS(TEI_NS, "particDesc").item(0);
        speakers.stream().sorted().forEach(s -> {
            Element person = doc.createElementNS(TEI_NS, "person");
            Element persName = doc.createElementNS(TEI_NS, "persName");
            Element abbr = doc.createElementNS(TEI_NS, "abbr");
            Text tx = doc.createTextNode(s);
            abbr.appendChild(tx);
            persName.appendChild(abbr);
            person.appendChild(persName);
            person.setAttribute("xml:id", s);
            person.setAttribute("n", s);
            list.appendChild(person);
        });
    }

    private Element addAnnotationBlock(Event from, Event to) {
        Element block = doc.createElementNS(TEI_NS, "annotationBlock");
        // Element block = doc.createElementNS(NameSpaces.TEI_NS,
        // "annotationBlock");
        block.setAttribute("who", currentSpeaker);
        block.setAttribute("start", from.mkTimeRef());
        block.setAttribute("end", to.mkTimeRef());
        return block;
    }

    /**
     * adds {@code <u>} with surrounding block and remembers current Utterance
     *
     * @param from
     *     begin event
     * @param to
     *     end evend
     */
    public void addBlockUtterance(Event from, Event to) {
        Element block = addAnnotationBlock(from, to);
        Element utterance = doc.createElementNS(TEI_NS, "u");
        block.appendChild(utterance);
        Element body = (Element) doc.getElementsByTagNameNS(TEI_NS, "body")
                .item(0);
        body.appendChild(block);
        currentBlock = block;
        currentUtterance = utterance;
    }

    /**
     * change Blockstart from original to from
     *
     * @param original
     * @param from
     */
    public void changeBlockStart(Event original, MarkedEvent from) {
        String mark = from.mkTime();
        currentBlock.setAttribute("start", mark);
        changeOthers(original, from, false);
    }

    private void changeOthers(Event original, MarkedEvent from, boolean isEnd) {
        String mark = isEnd ? from.mkTime() : from.mkEndTime();
        Node sib;
        do {
            sib = currentBlock.getPreviousSibling();
            if (sib.getNodeType() == Node.ELEMENT_NODE
                    && sib.getLocalName().equals("incident")) {
                Element elSib = (Element) sib;
                Seq.of("start", "end").forEach(att -> {
                    if (elSib.getAttribute(att).equals(original.mkTimeRef())) {
                        elSib.setAttribute(att, mark);
                    }
                });
            }
        } while (sib.getNodeType() != Node.ELEMENT_NODE
                || "annotationBlock".equals(sib.getLocalName()));
        Utilities
                .toElementStream(
                        currentBlock.getElementsByTagNameNS(TEI_NS, "span"))
                .forEach(b -> Seq.of("start", "end").forEach(att -> {
                    if (b.getAttribute(att).equals(original.mkTimeRef())) {
                        b.setAttribute(att, mark);
                    }
                }));

    }

    /**
     * add anchor for synchronization
     *
     * @param at
     *     event from timeline
     * @param parent
     *     parent node
     */
    public void addAnchor(Event at, Element parent) {
        addAnchor(at.mkTimeRef(), parent);
    }

    private void addAnchor(String at, Element parent) {
        Element anc = doc.createElementNS(TEI_NS, "anchor");
        parent.appendChild(anc);
        anc.setAttribute("synch", at);
    }

    // public void addTurn(Event from) {
    // // addAnchor(from, currentUtterance);
    // }

    /**
     * if turn ends with marked event, update surrounding
     * {@code <annotationBlock>} {@code @to} to last marked event; remove
     * anchor.
     *
     * @param original
     *     the original end event, to be potentially removed
     * @param to
     *     the end event.
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
                if (mark.equals(lastAnchor.getAttribute("synch"))) {
                    currentUtterance.removeChild(lastAnchor);
                    currentBlock.setAttribute("end", mark);
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
     *     the text
     * @param space
     *     whether to prepend whitespace
     */
    public void addText(String text, boolean space) {
        Node lc = currentUtterance.getLastChild();
        if (lc != null && space) {
            text = " " + text;
        }
        Text tx = doc.createTextNode(text);
        currentUtterance.appendChild(tx);
    }

    /**
     * add space to utterance
     */
    public void addSpace() {
        String text = " ";
        Text tx = doc.createTextNode(text);
        currentUtterance.appendChild(tx);

    }

    /**
     * add a comment concerning the span between two events
     *
     * @param from
     *     the start
     * @param to
     *     the end
     * @param text
     *     the comment
     */
    public void addComment(Event from, Event to, String text) {
        Element comGroup = doc.createElementNS(TEI_NS, "spanGrp");
        Element com = doc.createElementNS(TEI_NS, "span");
        // Element comGroup = doc.createElementNS(NameSpaces.TEI_NS, "spanGrp");
        // Element com = doc.createElementNS(NameSpaces.TEI_NS, "span");
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
     *     the event in the timeline
     * @param text
     *     the labelled text
     * @param startAnchor
     *     whether a start anchor will be placed
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
     *     the start of the current block
     * @param to
     *     the start of the current block
     * @param text
     *     description of the incident
     * @param extraPose
     *     whether to embed into utterance or prepose to annotation block
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
        incident.setAttribute("start", from.mkTimeRef());
        incident.setAttribute("end", to.mkTimeRef());
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

    /**
     * an {@link Event} where temporal overlap between different turns occurs.
     */

    public static class MarkedEvent extends Event {
        private static int lastEvent = 0;
        private final String mark;

        /**
         * create marked event
         *
         * @param mark
         */
        public MarkedEvent(String mark) {
            nr = ++lastEvent;
            this.mark = mark;
        }

        @Override
        protected String mkTime() {
            return "M_" + nr;
        }

        /**
         * generate ID for end time of marked event
         *
         * @return the ID
         */
        String mkEndTime() {
            return "ME_" + nr;
        }

        /**
         * generate ID reference for end time of marked event
         *
         * @return XML-style reference
         */
        String mkEndTimeRef() {
            return mkEndTime();
        }

        String getMark() {
            return mark;
        }

    }

    /**
     * Events in the timeline
     * <p>
     * Events are counted, as their only feature is to be distinct.
     *
     * @author bfi
     */
    public abstract static class Event {

        static int lastEvent = 0;
        int nr;

        /**
         * generate ID for the Event
         *
         * @return the id
         */
        protected abstract String mkTime();

        /**
         * a reference to the event
         *
         * @return XML-style reference
         */
        String mkTimeRef() {
            return mkTime();
        }

    }

    /**
     * an {@link Event} starting a turn
     *
     * @author bfi
     */
    public static class BeginEvent extends Event {
        /**
         * create begin event
         */
        public BeginEvent() {
            nr = ++lastEvent;
        }

        @Override
        public String mkTime() {
            return "B_" + nr;
        }
    }

    /**
     * an {@link Event} ending a turn
     *
     * @author bfi
     */
    public static class EndEvent extends Event {
        /**
         * create end event
         */
        public EndEvent() {
            nr = lastEvent;
        }

        @Override
        public String mkTime() {
            return "E_" + nr;
        }

    }

}
