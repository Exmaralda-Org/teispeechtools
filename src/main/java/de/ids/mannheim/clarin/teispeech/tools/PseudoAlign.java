package de.ids.mannheim.clarin.teispeech.tools;

import static de.ids.mannheim.clarin.teispeech.data.NameSpaces.TEI_NS;
import static de.ids.mannheim.clarin.teispeech.data.NameSpaces.TEMP_NS;
import static de.ids.mannheim.clarin.teispeech.data.NameSpaces.XML_NS;

import java.util.Arrays;
import java.util.List;
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

    /**
     * default language
     */
    private final String language;

    /**
     * XML DOM document
     */
    private final Document doc;

    /**
     * make new {@link PseudoAlign} for
     *
     * @param doc
     *            a DOM XML document
     * @param language
     *            the language
     */
    public PseudoAlign(Document doc, String language) {
        this.language = language;
        this.doc = doc;
    }

    public class AnchorSurplusException extends Exception {
        public AnchorSurplusException(String id, int count) {
            super(String.format("%s has %d anchors; cannot get duration", id,
                    count));
        }
    }

    public Optional<Double> getUtteranceDuration(Element el) {
        // TODO: check for local anchors and do something about it
        Optional<Double> duration = DocUtilities.getDuration(el);
        if (!duration.isPresent()) {
            String start = el.getAttributeNS(TEI_NS, "start");
            String end = el.getAttributeNS(TEI_NS, "end");
            if (DocUtilities.isTEI(el, "u")) {
                Element par = ((Element) el.getParentNode());
                if (DocUtilities.isTEI(par, "annotationBlock")) {
                    if (start == "") {
                        start = par.getAttributeNS(TEI_NS, "start");
                        if ("".equals(start))
                            el.setAttributeNS(TEI_NS, "start", start);
                    }
                    if (end == "") {
                        end = par.getAttributeNS(TEI_NS, "end");
                        if ("".equals(end))
                            el.setAttributeNS(TEI_NS, "end", end);
                    }
                }
            }
            try {
                Optional<Double> startTime = DocUtilities.getDuration(start);
                Optional<Double> endTime = DocUtilities.getDuration(end);
                if (startTime.isPresent() && endTime.isPresent())
                    duration = Optional.of(endTime.get() - startTime.get());
            } catch (NullPointerException | NumberFormatException e) {
                // return Optional.empty();
            }
        }
        return duration;
    }

    private abstract class Time {
        protected Number t;

        public Number get() {
            return t;
        }

        public Time(Number t) {
            this.t = t;
        }
    }

    private class AbsoluteTime extends Time {
        public AbsoluteTime(Double t) {
            super(t);
        }

        @Override
        public Double get() {
            return (Double) t;
        }
    }

    private class RelativeTime extends Time {
        public RelativeTime(Integer t) {
            super(t);
        }

        @Override
        public Integer get() {
            return (Integer) t;
        }
    }

    public Time getPausePhoneDuration(Element el) {
        Optional<Double> duration = DocUtilities.getDuration(el);
        if (duration.isPresent()) {
            return new AbsoluteTime(duration.get());
        } else {
            int dur = 0;
            String type = el.getAttributeNS(TEI_NS, "type");
            if (!"".equals(type)) {
                while (type.startsWith("very ")) {
                    dur += 3;
                    type = type.substring(5);
                }
                switch (type) {
                case "long":
                    dur += 3;
                    // NO BREAK
                case "medium":
                    dur += 3;
                    // NO BREAK
                case "short":
                    dur += 3;
                }
            }
            return new RelativeTime(dur);
        }
    }

    /**
     * pos-tag the document
     *
     * @param phoneticise
     *            whether to store the transcriptions in the document
     *
     * @return document, for chaining
     */
    // TODO: Do we need Boolean force
    public Document calculateUtterances(boolean phoneticise) {

        // aggregate by language to minimise calls to web service
        DocUtilities.groupByLanguage("w", doc, language, 1)
                .forEach((uLanguage, words) -> {
                    Optional<String> locale = GraphToPhoneme
                            .correspondsTo(language);
                    String[] transWords;
                    if (locale.isPresent()) {
                        String text = Seq.seq(words)
                                .map(el -> el.getTextContent()).toString(" ");
                        Optional<String[]> transcribed = GraphToPhoneme
                                .getTranscription(text, uLanguage, false);
                        transWords = transcribed.get();
                        if (phoneticise) {
                            Seq.seq(words).zip(Arrays.asList(transWords))
                                    .forEach(tup -> tup.v1.setAttributeNS(
                                            TEI_NS, "phon", tup.v2));
                        }
                    } else {
                        transWords = Seq.seq(words)
                                .map(el -> el.getTextContent())
                                .toArray(s -> new String[s]);
                    }
                    Seq.seq(words)
                            .zip(Arrays.asList(
                                    GraphToPhoneme.countSigns(transWords)))
                            .forEach(tup -> tup.v1.setAttributeNS(
                                    NameSpaces.TEMP_NS, "rel-length",
                                    tup.v2.toString()));
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
            return;
        }
        double absDuration = duration.get();
        int relDuration = 0;
        // calculate relative durations and time to distribute over them
        for (Element el : uChildren) {
            int d = 0;
            if (DocUtilities.isTEI(el, "w")) {
                d = Integer.parseInt(el.getAttributeNS(TEMP_NS, "rel-length"));
            } else if (DocUtilities.isTEI(el, "pause")) {
                Time dur = getPausePhoneDuration(el);
                if (dur instanceof RelativeTime)
                    relDuration += ((RelativeTime) dur).get();
                else
                    absDuration -= ((AbsoluteTime) dur).get();
            } else {
                LOGGER.warn("Skipped {}", el.getTagName());
            }
        }
        // what's the unit of 1 relative duration:
        double quantum = absDuration / relDuration;
        double upToNow = 0;
        Element timeRoot = DocUtilities.getTimeRoot(doc);
        Element lastWhen = doc.getElementById(
                DocUtilities.unPoundMark(u.getAttributeNS(TEI_NS, "start")));
        Element refWhen = lastWhen;
        String refId = refWhen.getAttributeNS(XML_NS, "id");
        // add when elements to time line
        for (Element el : uChildren) {
            int d = 0;
            double absD = 0;
            if (DocUtilities.isTEI(el, "w")) {
                d = Integer.parseInt(el.getAttributeNS(TEMP_NS, "rel-length"));
            } else if (DocUtilities.isTEI(el, "pause")) {
                Time dur = getPausePhoneDuration(el);
                if (dur instanceof RelativeTime)
                    d = ((RelativeTime) dur).get();
                else
                    absD = ((AbsoluteTime) dur).get();
            }
            double len = 0;
            if (d > 0) {
                // word or relative pause or
                len = quantum * d;
                upToNow += len;
            } else if (absD > 0) {
                upToNow += absD;
            }
            Element newWhen = doc.createElementNS(TEI_NS, "when");
            DocUtilities.setNewId(newWhen, refId);
            Utilities.insertAfterMe(lastWhen, newWhen);
            if (len > 0)
                lastWhen.setAttributeNS(TEI_NS, "dur",
                        String.format("%.1g", len));
            lastWhen.setAttributeNS(TEI_NS, "interval",
                    String.format("%.1g", upToNow));
            lastWhen.setAttributeNS(TEI_NS, "since", refId);
            Element endAnchor = doc.createElementNS(TEI_NS, "anchor");
        }
        if (absDuration - upToNow < 0.1) {
            LOGGER.warn(String.format("%.2g seconds unused",
                    absDuration - upToNow));
        }
    }

}
