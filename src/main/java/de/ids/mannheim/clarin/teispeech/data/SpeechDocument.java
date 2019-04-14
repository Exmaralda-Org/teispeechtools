package de.ids.mannheim.clarin.teispeech.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.ids.mannheim.clarin.teispeech.workflow.TextToTEIConversion;
import opennlp.tools.langdetect.Language;
import opennlp.tools.langdetect.LanguageDetector;
import opennlp.tools.langdetect.LanguageDetectorME;
import opennlp.tools.langdetect.LanguageDetectorModel;
import org.apache.commons.lang3.StringUtils;
import org.jooq.lambda.Seq;
import org.korpora.useful.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import de.ids.mannheim.clarin.teispeech.tools.DocUtilities;

import static de.ids.mannheim.clarin.teispeech.data.NameSpaces.TEI_NS;

/**
 * TEI annotated speech document, mainly for use in {@link TextToTEIConversion.TextToTEI}
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
    private final String language;

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
    private void setLanguage(String language) {
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
                    "[ There were errors parsing your text. "
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
            person.setAttribute("n", s);
            list.appendChild(person);
        });
    }

    private Element addAnnotationBlock(Event from, Event to) {
        Element block = doc.createElementNS(TEI_NS,
                "annotationBlock");
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
        currentBlock.setAttribute("start", mark);
        changeOthers(original, from, false);
    }

    private void changeOthers(Event original, MarkedEvent from,
                              boolean isEnd){
        String mark = isEnd ? from.mkTime() : from.mkEndTime();
        Node sib;
        do {
            sib = currentBlock.getPreviousSibling();
            if (sib.getNodeType() == Node.ELEMENT_NODE &&
                    sib.getLocalName().equals("incident")) {
                Element elSib = (Element) sib;
                Seq.of("start", "end").forEach(att -> {
                    if (elSib.getAttribute(att).equals(original
                            .mkTimeRef())) {
                        elSib.setAttribute(att, mark);
                    }
                });
            }
        } while (sib.getNodeType() != Node.ELEMENT_NODE ||
                "annotationBlock".equals(sib.getLocalName()));
        Utilities
                .toElementStream(currentBlock
                        .getElementsByTagNameNS(TEI_NS, "span"))
                .forEach(b -> Seq.of("start", "end").forEach(att -> {
                    if (b.getAttribute(att).equals(original
                            .mkTimeRef())) {
                        b.setAttribute(att, mark);
                    }
                }));

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

    private void addAnchor(String at, Element parent) {
        Element anc = doc.createElementNS(TEI_NS, "anchor");
        parent.appendChild(anc);
        anc.setAttribute("synch", at);
    }

    // public void addTurn(Event from) {
    //     // addAnchor(from, currentUtterance);
    // }

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
        private final String mark;
        private static int lastEvent = 0;

        public MarkedEvent(String mark) {
            nr = ++lastEvent;
            this.mark = mark;
        }

        @Override
        public String mkTime() {
            return "M_" + nr;
        }

        /**
         * generate ID for end time of marked event
         *
         * @return the ID
         */
        public String mkEndTime() {
            return "ME_" + nr;
        }

        /**
         * generate ID reference for end time of marked event
         *
         * @return XML-style reference
         */
        public String mkEndTimeRef() {
            return mkEndTime();
        }

        public String getMark() {
            return mark;
        }

    }

    /**
     * Events in the timeline
     *
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
        public abstract String mkTime();

        /**
         * a reference to the event
         *
         * @return XML-style reference
         */
        public String mkTimeRef() {
            return mkTime();
        }

    }

    /**
     * an {@link Event} starting a turn
     *
     * @author bfi
     */
    public static class BeginEvent extends Event {
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
        public EndEvent() {
            nr = lastEvent;
        }

        @Override
        public String mkTime() {
            return "E_" + nr;
        }

    }

    /**
     * guess language of utterances in TEI transcriptions.
     *
     *
     * @author bfi
     *
     */
    @SuppressWarnings("WeakerAccess")
    public static class LanguageDetect {

        private final static Logger LOGGER = LoggerFactory
                .getLogger(LanguageDetect.class.getName());

        private static final String MODEL_PATH = "langdetect-183.bin";

        /**
         * an acceptable ratio between the "best" and the second-best language
         */
        private static final double GOOD_RELATION = 1.5;

        /**
         * the document language per request; "deu" if nothing is given
         */
        private String language;

        private Set<String> expectedLanguages;

        private Document doc;

        private static final LanguageDetector languageDetector;
        static {
            // load the trained Language Detector Model file
            try (InputStream modelStream = LanguageDetect.class.getClassLoader()
                    .getResourceAsStream(MODEL_PATH)) {

                assert modelStream != null;
                LanguageDetectorModel trainedModel = new LanguageDetectorModel(
                        modelStream);

                // load the model
                languageDetector = new LanguageDetectorME(trainedModel);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // TODO: consider threshold for utterance length and confidence
        // private static int MIN_UTTERANCE_SIZE = 5;
        private final int minUtteranceSize;
        // private static double MIN_CONFIDENCE = 0.1;
        // difference to next-likely language?

        /**
         * make new {@link LanguageDetect};
         *
         * @param doc
         *            XML DOM document
         *
         * @param language
         *            the default language, an ISO language code
         * @param expected
         *            the languages that are expected in the document, for
         *            constraining language identification
         * @param mini
         *            the minimal length of an utterance to attempt language
         *            detection
         */
        public LanguageDetect(Document doc, String language, String[] expected,
                int mini) {
            this.doc = doc;
            this.language = language != null ? language : "deu";
            if (expected != null && expected.length > 0) {
                expectedLanguages = new HashSet<>();
                expectedLanguages.add(language);
                expectedLanguages.addAll(Arrays.asList(expected));
            }
            this.minUtteranceSize = mini;
        }

        private LanguageDetect(Document doc) {
            this(doc, "deu", new String[] { "tur", "en" }, 5);
        }

        public Document detect() {
            return detect(false);
        }

        /**
         * count language per {@code <u>}
         *
         * @param force
         *            whether to force language detection, even if a language tag
         *            has already been assigned to {@code <u>}.
         *
         * @return the document again
         */
        public Document detect(boolean force) {
            long processed = 0;
            long unprocessed = 0;
            Map<String, Integer> changed = new HashMap<>();
            List<Element> utterances = Utilities
                    .toElementList(doc.getElementsByTagNameNS(TEI_NS, "u"));
            for (Element utter : utterances) {
                if (!force && utter.hasAttribute("xml:lang")) {
                    continue;
                }
                String defaultLanguage = DocUtilities
                        .getLanguage((Element) utter.getParentNode(), language, 1);

                // language by words:
                List<Element> words = Utilities
                        .toElementStream(utter.getElementsByTagNameNS(TEI_NS, "w"))
                        .filter(ut -> !"incomprehensible"
                                .equals(ut.getAttribute("type")))
                        .collect(Collectors.toList());
                boolean already = words.stream()
                        .allMatch(e -> e.hasAttribute("xml:lang"));
                if (already) {
                    List<Map.Entry<String, Long>> wordLanguages = words.stream()
                            .filter(e -> e.hasAttribute("xml:lang"))
                            .map(e -> e.getAttribute("xml:lang"))
                            .collect(Collectors.groupingBy(Function.identity(),
                                    Collectors.counting()))
                            .entrySet().stream()
                            .sorted(Map.Entry
                                    .comparingByValue(Comparator.reverseOrder()))
                            .collect(Collectors.toList());
                    List<String> candidates = Seq.seq(wordLanguages.stream())
                            .limitWhile(e -> e.getValue()
                                    .equals(wordLanguages.get(0).getValue()))
                            .map(Map.Entry::getKey).collect(Collectors.toList());
                    if (candidates.size() == 1) {
                        // majority language:
                        utter.setAttribute("xml:lang", candidates.get(0));
                        continue;
                    } else if (candidates.size() > 1
                            && candidates.contains(defaultLanguage)) {
                        // default among major languages:
                        utter.setAttribute("xml:lang", defaultLanguage);
                        continue;
                    }
                }
                // haven't found language yet:
                if (words.size() > 0 && words.size() < minUtteranceSize) {
                    Comment com = doc.createComment(
                            "too few words to make a good language prediction");
                    utter.getParentNode().insertBefore(com, utter);
                    unprocessed++;
                    continue;
                }
                String text;
                // TODO: What to do about mixed content without <w>?
                if (words.isEmpty()) {
                    if (utter.getChildNodes().getLength() == 0) {
                        unprocessed++;
                        continue;
                    }
                    text = utter.getTextContent();
                    if (StringUtils.split(text).length < minUtteranceSize) {
                        Comment com = doc.createComment(
                                "too few words to make a good language prediction");
                        utter.getParentNode().insertBefore(com, utter);
                        unprocessed++;
                        continue;
                    }
                } else {
                    text = Seq.seq(words).map(DocUtilities::getTextOrNorm)
                            .toString(" ");
                }
                if (StringUtils.strip(text).isEmpty()) {
                    Comment commy = doc.createComment("– EMPTY –");
                    utter.getParentNode().insertBefore(commy, utter);
                    continue;
                }
                List<Language> languages = Stream
                        .of(languageDetector.predictLanguages(text))
                        .filter(l -> expectedLanguages.contains(l.getLang()))
                        .collect(Collectors.toList());
                Comment com = doc
                        .createComment(
                                Seq.seq(languages)
                                        .filter(l -> l.getConfidence() > 0.005)
                                        .map(l -> String.format("%s: %.02f",
                                                l.getLang(), l.getConfidence()))
                                        .toString("; "));
                utter.getParentNode().insertBefore(com, utter);
                if (languages.size() >= 2 && languages.get(0).getConfidence() > 0
                        && languages.get(1).getConfidence() > 0
                        && (languages.get(0).getConfidence() / languages.get(1)
                                .getConfidence() > GOOD_RELATION)) {
                    // in clear cases, believe language guess
                    String lang = languages.get(0).getLang();
                    utter.setAttribute("xml:lang", lang);
                    Utilities.incCounter(changed, lang);
                    processed++;
                } else {
                    // prefer defaultLanguage in case of doubt
                    if (languages.size() >= 2
                            && languages.get(0).getConfidence() > 0) {
                        double measure = languages.get(0).getConfidence();
                        List<String> similar = Seq.seq(languages)
                                .limitWhile(l -> measure / l.getConfidence() < 1.1)
                                .map(Language::getLang)
                                .collect(Collectors.toList());
                        if (similar.contains(defaultLanguage)) {
                            utter.setAttribute("xml:lang", defaultLanguage);
                        }
                        Utilities.incCounter(changed, defaultLanguage);
                        processed++;
                    } else {
                        // or give up
                        Comment commy = doc.createComment(
                                "– Sorry, no idea what language this is! –");
                        utter.getParentNode().insertBefore(commy, utter);
                        unprocessed++;
                    }
                }

            }
            String changeMsg = String.format(
                    "detected languages in %d utterances; skipped %d (found: %s).",
                    processed, unprocessed, changed);
            LOGGER.info(changeMsg);
            DocUtilities.makeChange(doc, changeMsg);
            return doc;
        }

    }
}
