package de.ids.mannheim.clarin.teispeech.tools;

import static de.ids.mannheim.clarin.teispeech.data.NameSpaces.TEI_NS;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jooq.lambda.Seq;
import org.korpora.useful.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import de.ids.mannheim.clarin.teispeech.data.NameSpaces;

/**
 * Pseudo-align documents in the TEI transcription format with the TreeTagger
 *
 * @author bfi
 *
 */
public class PseudoAlign {

    private final static Logger LOGGER = LoggerFactory
            .getLogger(PseudoAlign.class.getName());

    private static NumberFormat NUMBER_FORMAT = NumberFormat
            .getInstance(Locale.ROOT);

    static {
        NUMBER_FORMAT.setMinimumFractionDigits(0);
        NUMBER_FORMAT.setMaximumFractionDigits(1);
    }

    /**
     * default language
     */
    private final String language;

    /**
     * XML DOM document
     */
    private final Document doc;

    /**
     * whether to store the transcriptions in the document
     */
    private boolean phoneticise;

    /**
     * whether to count relative duration in (pseudo)phones if possible
     */
    private boolean usePhones;

    /**
     * whether to force transcription
     */
    private boolean force;

    /**
     * make new {@link PseudoAlign} for
     *
     * @param doc
     *            a DOM XML document
     * @param language
     *            the default document language
     * @param usePhones
     *            whether to count relative duration in (pseudo)phones if
     *            possible
     * @param phoneticise
     *            whether to store the transcriptions in the document
     * @param force
     *            whether to force transcription
     *
     */
    public PseudoAlign(Document doc, String language, boolean usePhones,
            boolean phoneticise, boolean force) {
        this.language = language;
        this.doc = doc;
        this.phoneticise = phoneticise;
        this.usePhones = usePhones;
        this.force = force;
        if (!usePhones && phoneticise) {
            LOGGER.warn(
                    "phoneticise but not usePhones is not useful: phoneticise ignored.");
        }
    }

    /**
     * get duration of element, measured in seconds
     *
     * @param el
     *            the element
     * @return the duration
     */
    public Optional<Double> getUtteranceDuration(Element el) {
        // TODO: check for local anchors and do something about it
        Optional<Double> duration = DocUtilities.getDuration(el);
        LOGGER.info("utterance duration: {}", duration);
        if (!duration.isPresent()) {
            String start = DocUtilities.getTEI(el, "start");
            String end = DocUtilities.getTEI(el, "end");
            if (DocUtilities.isTEI(el, "u")) {
                Element par = ((Element) el.getParentNode());
                if (DocUtilities.isTEI(par, "annotationBlock")) {
                    if ("".equals(start)) {
                        start = DocUtilities.getTEI(par, "start");
                        if (!"".equals(start))
                            el.setAttributeNS(TEI_NS, "start", start);
                    }
                    if ("".equals(end)) {
                        end = DocUtilities.getTEI(par, "end");
                        if (!"".equals(end))
                            el.setAttributeNS(TEI_NS, "end", end);
                    }
                }
            }
            Optional<Double> startTime = DocUtilities
                    .getOffset(Utilities.getElementByID(doc, start));
            Optional<Double> endTime = DocUtilities
                    .getOffset(Utilities.getElementByID(doc, end));
            LOGGER.info("{} -> {}    {} -> {}", start, startTime, end, endTime);
            if (startTime.isPresent() && endTime.isPresent())
                duration = Optional.of(endTime.get() - startTime.get());
        }
        return duration;
    }
//
//    private abstract class Time {
//        protected Number t;
//
//        abstract public Number get();
//
//        public Time(Number t) {
//            this.t = t;
//        }
//    }
//
//    private class AbsoluteTime extends Time {
//        public AbsoluteTime(Double t) {
//            super(t);
//        }
//
//        @Override
//        public Double get() {
//            return (Double) t;
//        }
//    }
//
//    private class RelativeTime extends Time {
//        public RelativeTime(Integer t) {
//            super(t);
//        }
//
//        @Override
//        public Integer get() {
//            return (Integer) t;
//        }
//    }

    /**
     * get duration of pause measured in (pseudo)phones
     *
     * @param el
     *            pause element
     * @return duration
     */
    public double getPausePhoneDuration(Element el) {
        Optional<Double> duration = DocUtilities.getDuration(el);
        if (duration.isPresent()) {
            return duration.get();
        } else {
            double dur = 0;
            String type = DocUtilities.getTEI(el, "type");
            if (!"".equals(type)) {
                if (type.endsWith("long")) {
                    while (type.startsWith("very ")) {
                        dur += 0.3;
                        type = type.substring(5);
                    }
                }
                switch (type) {
                case "long":
                    dur += 0.9;
                    break;
                case "medium":
                    dur = 0.65;
                    break;
                case "short":
                    dur = 0.35;
                    break;
                case "micro":
                    dur = 0.1;
                    break;
                default:
                    throw new RuntimeException("unknown pause type: «"
                            + DocUtilities.getTEI(el, "type") + "»!");
                }
            }
            return dur;
        }
    }

    /**
     * calculate relative length of utterances the document
     *
     *
     * @return document, for chaining
     */
    // TODO: Do we need Boolean force?
    // TODO: Do we need to disallow syllabification?
    // TODO: Allow to use normalized orthography, not CA transcription?
    public Document calculateUtterances() {

        // aggregate by language to minimise calls to web service
        DocUtilities.groupByLanguage("w", doc, language, 1)
                .forEach((uLanguage, words) -> {
                    Optional<String> locale = GraphToPhoneme
                            .correspondsTo(language);
                    String[] transWords;
                    boolean transcribe = usePhones && locale.isPresent();
                    if (transcribe) {
                        // work with transcriptions
                        String text = Seq.seq(words)
                                .map(el -> el.getTextContent()).toString(" ");
                        Optional<String[]> transcribed = GraphToPhoneme
                                .getTranscription(text, uLanguage, true);
                        transWords = transcribed.get();

                        // TODO: ignore single letter words, or treat
                        // apostrophes specially?
                        // otherwise, we get wouldn'[ti:].
                        if (phoneticise) {
                            Seq.seq(words).zip(Arrays.asList(transWords))
                                    .forEach(tup -> {
                                        if (!force || (!tup.v1
                                                .hasAttributeNS(TEI_NS, "phon")
                                                && !!tup.v1.hasAttribute(
                                                        "phon"))) {
                                            tup.v1.setAttributeNS(TEI_NS,
                                                    "phon", tup.v2);
                                        }
                                    });
                        }
                    } else {
                        // fall back to counting letters
                        transWords = Seq.seq(words)
                                .map(el -> el.getTextContent())
                                .toArray(s -> new String[s]);
                    }
                    int[] transcribed = GraphToPhoneme.countSigns(transWords,
                            transcribe);
                    for (int i = 0; i < words.size(); i++) {
                        words.get(i).setAttribute("rel-length",
                                String.format("%d", transcribed[i]));
                    }
                });
        // TODO: calculate!
        Utilities
                .toElementStream(
                        doc.getElementsByTagNameNS(NameSpaces.TEI_NS, "u"))
                .forEach(u -> calculateSingleUtterance(u));
        // TODO: remove relative lengths after testing
        // Utilities.toElementStream(doc.getElementsByTagNameNS(TEI_NS, "w"))
        // .forEach(el -> el.removeAttributeNS(TEMP_NS, "rel-length"));
        DocUtilities.makeChange(doc, "Pseudo-aligned");
        return doc;
    }

    private void calculateSingleUtterance(Element u) {
        List<Element> uChildren = Utilities.toStream(u.getChildNodes())
                .filter(n -> n.getNodeType() == Node.ELEMENT_NODE)
                .map(n -> (Element) n).collect(Collectors.toList());
        Optional<Double> duration = getUtteranceDuration(u);
        if (!duration.isPresent()) {
            LOGGER.warn("Utterance without duration: {}",
                    u.getTextContent().replace("\n", " "));
            return;
        }
        double absDuration = duration.get();
        int relDuration = 0;
        // calculate relative durations and time to distribute over them
        for (Element el : uChildren) {
            if (DocUtilities.isTEI(el, "w")) {
                relDuration += Integer.parseInt(el.getAttribute("rel-length"));
            } else if (DocUtilities.isTEI(el, "pause")) {
                absDuration -= getPausePhoneDuration(el);
            } else {
                LOGGER.warn("Skipped {}", el.getTagName());
            }
        }
        // what's the unit of 1 relative duration:
        double quantum = absDuration / relDuration;
        LOGGER.info("abs: {}, rel: {}, q: {}", absDuration, relDuration,
                quantum);
        double upToNow = 0;
        Element lastWhen = Utilities.getElementByID(doc,
                DocUtilities.unPoundMark(DocUtilities.getTEI(u, "start")));
        Element refWhen = lastWhen;
        LOGGER.info("WHEN");
        Optional<Double> refOffsetOpt = DocUtilities.getOffset(refWhen);
        if (!refOffsetOpt.isPresent()) {
            throw new RuntimeException("No offset calculable!");
        }
        double refOffset = refOffsetOpt.get();
        String rootID = DocUtilities.getTimeRoot(doc).getAttribute("xml:id");
        String refId = refWhen.getAttribute("xml:id");
        // add when elements to time line
        for (Element el : uChildren) {
            int d = 0;
            double absD = 0;
            if (DocUtilities.isTEI(el, "w")) {
                d = Integer.parseInt(el.getAttribute("rel-length"));
            } else if (DocUtilities.isTEI(el, "pause")) {
                absD += getPausePhoneDuration(el);
            }
            double len = 0;
            if (d > 0) {
                // word
                len = quantum * d;
                upToNow += len;
            } else if (absD > 0) {
                upToNow += absD;
            }
            Element newWhen = doc.createElementNS(TEI_NS, "when");
            String newID = DocUtilities.setNewId(newWhen, refId);
            Utilities.insertAfterMe(newWhen, lastWhen);
            lastWhen = newWhen;
            if (len > 0)
                lastWhen.setAttributeNS(TEI_NS, "dur",
                        NUMBER_FORMAT.format(len));
            lastWhen.setAttributeNS(TEI_NS, "interval",
                    NUMBER_FORMAT.format(refOffset + upToNow));
            lastWhen.setAttributeNS(TEI_NS, "since", rootID);
            Element endAnchor = doc.createElementNS(TEI_NS, "anchor");
            endAnchor.setAttributeNS(TEI_NS, "sync", newID);
            Utilities.insertAfterMe(endAnchor, el);
        }
        if (absDuration - upToNow > 0.05) {
            LOGGER.warn(String.format("%f seconds unused",
                    (absDuration - upToNow)));
        }
    }

}
