package de.ids.mannheim.clarin.teispeech.tools;


import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.jooq.lambda.Seq;
import org.korpora.useful.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;

import de.ids.mannheim.clarin.teispeech.data.NameSpaces;

import javax.xml.xpath.*;

import static de.ids.mannheim.clarin.teispeech.data.NameSpaces.TEI_NS;
import static de.ids.mannheim.clarin.teispeech.tools.DocUtilities.getAttXML;
import static de.ids.mannheim.clarin.teispeech.tools.DocUtilities.getAttTEI;

/**
 * Pseudo-align documents in the TEI transcription format with the TreeTagger
 *
 * @author bfi
 */
public class PseudoAlign {

    static {
        Locale.setDefault(Locale.ROOT);
    }
    private final static Logger LOGGER = LoggerFactory
            .getLogger(PseudoAlign.class.getName());

    private static NumberFormat NUMBER_FORMAT = NumberFormat
            .getInstance(Locale.ROOT);

    static {
        NUMBER_FORMAT.setMinimumFractionDigits(0);
        NUMBER_FORMAT.setMaximumFractionDigits(3);
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
     * length of audio in seconds
     */
    private final double timeLength;



    /**
     * whether to store the transcriptions in the document
     */
    private final boolean phoneticise;

    /**
     * whether to count relative duration in (pseudo)phones if possible
     */
    private final boolean usePhones;

    /**
     * whether to force transcription
     */
    private final boolean force;

    private final static XPathExpression interesting;
    private final static XPathExpression blocky;

    static {
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            interesting =
                    xPath.compile(String.format(
                            ".//*[(local-name() = 'w' or local-name() ='pause') and namespace-uri() = '%s']|.//text()",
                            TEI_NS));
            blocky =
                    xPath.compile(String.format(
                            ".//*[(local-name() = 'annotationBlock' or local-name() ='incident') and namespace-uri() = '%s']",
                            TEI_NS));
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * make new {@link PseudoAlign} for
     *
     * @param doc         a DOM XML document
     * @param language    the default document language
     * @param usePhones   whether to count relative duration in (pseudo)phones if
     *                    possible
     * @param phoneticise whether to store the transcriptions in the document
     * @param force       whether to force transcription
     * @param timeLength  length of audio in seconds
     */
    public PseudoAlign(Document doc, String language, boolean usePhones,
                       boolean phoneticise, boolean force, double timeLength) {
        this.language = language;
        this.doc = doc;
        this.phoneticise = phoneticise;
        this.usePhones = usePhones;
        this.force = force;
        this.timeLength = timeLength;
        if (!usePhones && phoneticise) {
            LOGGER.warn(
                    "phoneticise but not usePhones is not useful: phoneticise ignored.");
        }
    }

    /**
     * get duration of element, measured in seconds
     *
     * @param el the element
     * @return the duration
     */
    public Optional<Double> getUtteranceDuration(Element el) {
        // TODO: check for local anchors and do something about it
        Optional<Double> duration = DocUtilities.getDuration(el);
        LOGGER.info("utterance duration: {}", duration);
        if (!duration.isPresent()) {
            String start = getAttTEI(el, "start");
            String end = getAttTEI(el, "end");
            if (DocUtilities.isTEI(el, "u")) {
                Element par = ((Element) el.getParentNode());
                if (DocUtilities.isTEI(par, "annotationBlock")) {
                    if ("".equals(start)) {
                        start = getAttTEI(par, "start");
                        if (!"".equals(start))
                            el.setAttributeNS(TEI_NS, "start", start);
                    }
                    if ("".equals(end)) {
                        end = getAttTEI(par, "end");
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

    /**
     * get duration of pause measured in (pseudo)phones
     *
     * @param el pause element
     * @return duration
     */
    public double getPausePhoneDuration(Element el) {
        Optional<Double> duration = DocUtilities.getDuration(el);
        if (duration.isPresent()) {
            return duration.get();
        } else {
            double dur = 0;
            String type = getAttTEI(el, "type");
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
                                + getAttTEI(el, "type") + "»!");
                }
            }
            return dur;
        }
    }

    /**
     * calculate relative length of utterances the document
     *
     */
    // TODO: Do we need Boolean force?
    // TODO: Do we need to disallow syllabification?
    // TODO: Allow to use normalized orthography, not CA transcription?
    public void calculateUtterances() {

        // aggregate by language to minimise calls to web service
        DocUtilities.groupByLanguage("w", doc, language, 1)
                .forEach((uLanguage, initWords) -> {
                    Optional<String> locale = GraphToPhoneme
                            .correspondsTo(language);
                    String[] transWords;
                    boolean transcribe = usePhones && locale.isPresent();
                    List<Element> words = Seq.seq(initWords)
                            .filter(w -> !("incomprehensible".equals(
                                    getAttTEI(w, "type"))))
                            .toList();
                    if (transcribe) {
                        // only transcribe "normal" words
                        // work with transcriptions
                        String text = Seq.seq(words)
                                .map(el -> el.getTextContent()).toString(" ");
                        Optional<String[]> transcribed = GraphToPhoneme
                                .getTranscription(text, uLanguage, true);
                        transWords = transcribed.get();

                        if (phoneticise) {
                            Seq.seq(words).zip(Arrays.asList(transWords))
                                    .forEach(tup -> {
                                        if (!force || (!tup.v1
                                                .hasAttributeNS(TEI_NS, "phon")
                                                && !tup.v1.hasAttribute(
                                                "phon"))) {
                                            tup.v1.setAttributeNS(TEI_NS,
                                                    "phon", tup.v2);
                                        }
                                    });
                        }
                        // add transcriptions
                        int[] transcribedW = GraphToPhoneme
                                .countSigns(transWords, transcribe);
                        for (int i = 0; i < words.size(); i++) {
                            words.get(i).setAttribute("rel-length",
                                    String.format("%d", transcribedW[i]));
                        }
                    } else {
                        // fall back to counting letters
                        transWords = Seq.seq(words)
                                .map(el -> el.getTextContent())
                                .toArray(String[]::new);
                    }
                    int[] lettered = GraphToPhoneme.countSigns(transWords,
                            transcribe);
                    for (int i = 0; i < words.size(); i++) {
                        words.get(i).setAttribute("rel-length",
                                String.format("%d", lettered[i]));
                    }
                    // treat incomprehensible words:
                    List<Element> unWords = Seq.seq(initWords)
                            .filter(w -> ("incomprehensible".equals(
                                    getAttTEI(w, "type"))))
                            .toList();
                    String[] unStrings = Seq.seq(unWords).
                            map(Element::getTextContent).
                            toArray(String[]::new);
                    int[] unLettered = GraphToPhoneme.countSigns(unStrings,
                            false);
                    for (int i = 0; i < unWords.size(); i++) {
                        unWords.get(i).setAttribute("rel-length",
                                String.format("%d", unLettered[i]));
                    }
                });
        // TODO: calculate!
        Utilities
                .toElementStream(
                        doc.getElementsByTagNameNS(NameSpaces.TEI_NS, "u"))
                .forEach(u -> annotateSingleUtterance(u));
        Optional<Double> itemLength = relItemLength();
        if (itemLength.isPresent())
            applyItemLength(itemLength.get());
        // TODO: remove relative lengths after testing
        // USE XSLT!
        DocUtilities.makeChange(doc, "Pseudo-aligned");
    }

    private static <T> void incAllCounters(Map<T, Integer> map, int addendum) {
        for (Map.Entry<T, Integer> entry : map.entrySet()) {
            entry.setValue(entry.getValue() + addendum);
        }
    }

    private static <T> void incAllCounters(Map<T, Double> map, double addendum) {
        for (Map.Entry<T, Double> entry : map.entrySet()) {
            entry.setValue(entry.getValue() + addendum);
        }
    }

    private class Distance {
        public final int rel;
        public final double abs;
        public final String from;
        public final String to;

        public Distance(int rel, double abs, String from, String to) {
            this.rel = rel;
            this.abs = abs;
            this.from = from;
            this.to = to;
        }

        @Override
        public String toString() {
            return String.format("{Δ<%s, %s> == <%d, -%.3f>}",
                    from, to, rel, abs);
        }
    }

    private List<Distance> distances = new ArrayList<>();
    // private Map<String, Integer> rest = new HashMap<>();
    List<Element> whenList = new ArrayList<>();
    List<String> way = new ArrayList<>();
    Map<Pair<String, String>, Distance> paths = new HashMap<>();

    private void makeDistance(Element u, String from, String to,
                              int relDuration,
                              double absDuration) {
        // Element delta = u.getOwnerDocument().createElement("distance");
        // delta.setAttribute("from", from);
        // delta.setAttribute("to", to);
        // delta.setAttribute("rel", String.valueOf(relDuration));
        // delta.setAttribute("abs", String.valueOf(absDuration));
        // u.appendChild(delta);
        distances.add(new Distance(relDuration, absDuration, from, to));
    }


    public static Map<String, Integer> getOrder(List<Element> whens) {
        Map<String, Integer> order = new HashMap<>();
        for (int i = 0; i < whens.size(); i++) {
            Element el = (Element) whens.get(i);
            String elID = getAttXML(el, "id");
            order.put(elID, i);
        }
        return order;
    }

    private Map<String, LinkedHashSet<String>> accessibleRev = new HashMap<>();

    /**
     * find a way {@code from} from to {@code goal}
     *
     * @param from start of passage
     * @param goal end of passage (first event)
     * @return null or passage
     */
    private List<String> findPathR(String from, String goal) {
        LinkedHashSet<String> possible = accessibleRev.getOrDefault(from,
                new LinkedHashSet<>());
        if (possible.isEmpty())
            return null;
        // System.err.println(possible);
        for (String next : Seq.seq(possible).toList()) {
            if (goal.equals(next)) {
                return Seq.of(next, from).toList();
            } else {
                // System.err.format("@%s: %s -> %s\n", from, next, goal);
                List<String> ret = findPathR(next, goal);
                if (ret != null) {
                    ret.add(from);
                    return ret;
                }
            }
        }
        return null;
    }

    private void annotateSingleUtterance(Element u) {

        List<Element> uChildren;
        try {
            uChildren = Utilities.toStream(
                    (NodeList) interesting.evaluate(u, XPathConstants.NODESET))
                    .filter(n -> n.getNodeType() == Node.ELEMENT_NODE)
                    .map(n -> (Element) n).collect(Collectors.toList());
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
        Map<String, Double> absDuration = new HashMap<>();
        Element par = (Element) u.getParentNode();
        String currentEl = DocUtilities.unPoundMark(getAttTEI(par, "start"));
        String endEl = DocUtilities.unPoundMark(getAttTEI(par, "end"));
        Map<String, Integer> relDuration = new HashMap<>();
        relDuration.put(currentEl, 0);
        absDuration.put(currentEl, 0d);
        // calculate relative durations and time to distribute over them
        for (Element el : uChildren) {
            if (DocUtilities.isTEI(el, "w")) {
                incAllCounters(relDuration, Integer.parseInt(el.getAttribute("rel-length")));
                // count text length in characters for anchors in words
                int textTillNow = 0;
                Map<String, Integer> relRest = new HashMap<>();
                for (Iterator<Node> iNo = Utilities.toIterator(el.getChildNodes());
                     iNo.hasNext(); ) {
                    Node now = iNo.next();
                    // set anchor to text length
                    if (now.getNodeType() == Node.ELEMENT_NODE &&
                            ((Element) now).getLocalName().equals("anchor")) {
                        Element nowEl = ((Element) now);
                        String nowName = DocUtilities.unPoundMark(
                                getAttTEI(nowEl, "synch"));
                        for (String name : relDuration.keySet()) {
                            makeDistance(u, name, nowName, relDuration.get(name) + textTillNow, absDuration.get(name));
                        }
                        relRest.put(nowName, 0);
                    } else if (now.getNodeType() == Node.TEXT_NODE ||
                            (now.getNodeType() == Node.ELEMENT_NODE
                                    && ((Element) now).getLocalName().equals("w"))) {
                        int count = Utilities.removeSpace(
                                now.getTextContent()).length();
                        textTillNow += count;
                        incAllCounters(relRest, count);
                    }
                }
               // for (String name : relRest.keySet()) {
               //     Element delta = u.getOwnerDocument().createElement("rel-rest");
               //     delta.setAttribute("after", name);
               //     delta.setAttribute("rel", String.valueOf(relRest.get(name)));
               //     u.appendChild(delta);
               // }
            } else if (DocUtilities.isTEI(el, "pause")) {
                incAllCounters(absDuration, getPausePhoneDuration(el));
            } else {
                LOGGER.warn("Skipped {}", el.getTagName());
            }
        }
        for (String name : relDuration.keySet()) {
            makeDistance(u, name, endEl, relDuration.get(name), absDuration.get(name));
        }
    }

    // TODO: What about empty incidents?
    private Optional<Double> relItemLength() {
        NodeList whens = DocUtilities.getWhens(doc);
        whenList = Utilities.toElementList(whens);
        NodeList nodes = null;
        try {
            nodes = (NodeList) blocky.evaluate(doc, XPathConstants.NODESET);
            Map<String, Integer> order = getOrder(whenList);

            // elements that are next to each other:
            {
                int i = 0;
                do {
                    Element u = (Element) nodes.item(i);
                    String end = getAttTEI(u, "end");
                    boolean checked = false;
                    do {
                        Element uNext = (Element) nodes.item(i + 1);
                        String startNext = getAttTEI(uNext, "start");
                        if (end.equals(startNext) ||
                                // no overlap:
                                order.get(startNext) > order.get(end)) {
                            accessibleRev.putIfAbsent(startNext, new LinkedHashSet<>());
                            accessibleRev.get(startNext).add(end);
                            distances.add(new Distance(0, 0d, end, startNext));
                            checked = true;
                        }
                        i++;
                    } while ((i < nodes.getLength() - 1) && !checked);
                } while (i < nodes.getLength() - 1);
            }
            // elements that are connected in an utterance
            for (Distance distance : Seq.seq(distances).reverse()) {
                paths.put(Pair.of(distance.from, distance.to), distance);
                accessibleRev.putIfAbsent(distance.to, new LinkedHashSet<>());
                accessibleRev.get(distance.to).add(distance.from);
            }
            // CHECK: Elements that are next to each other should be OK:
            // [1 <2>  ]
            //    [2   ]
            //          [3   ]
            // or
            // [1 <2>      <3>  ]
            //    [2   ]
            //             [3     ]
            // Second do-while loop for the following, which is
            // unfortunately treatable by simple annotation:
            // [1  <2   > ]
            //    [<2   >]
            //             [3   ]
            // treat "empty" <incident>s
            Utilities.toElementStream(nodes)
                    .filter(e -> e.getLocalName().equals("incident"))
                    .forEachOrdered(e -> {
                        String from = getAttTEI(e, "start");
                        String to = getAttTEI(e, "end");
                        accessibleRev.putIfAbsent(to,
                                new LinkedHashSet<>());
                        if (!accessibleRev.get(to).contains(from)){
                            accessibleRev.get(to).add(from);
                            Distance distance = new Distance(0, 0d, from, to);
                            distances.add(distance);
                            paths.put(Pair.of(distance.from, distance.to), distance);
                        }
                    });
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
        String goal = getAttXML(whenList.get(0), "id");
        way = findPathR(getAttXML(whenList.get(whenList.size() - 1), "id"), goal);
        if (way != null) {
            // System.err.println(way);
            int rel = 0;
            double abs = 0d;
            for (int i = 0;  i < way.size() - 1; i++) {
                Distance d = paths.get(Pair.of(way.get(i), way.get(i + 1)));
                if (d != null){
                    rel += d.rel;
                    abs += d.abs;
                }
            }
            // System.err.format(">>> (%f - %f) / %d = %f\n", timeLength, abs, rel,
            //         ((timeLength - abs) / rel));
            return Optional.of((timeLength - abs) / rel);
        }
        return Optional.empty();
    }


    private void applyItemLength (Double itemLength) {
        Comment comment = doc.createComment(
                String.format(" length per item: %.4f ", itemLength));
        Utilities.getElementByTagNameNS(doc, TEI_NS, "body").appendChild(comment);
        Map<String,Double> position = new HashMap<>();
        String start = getAttXML(whenList.get(0), "id");
        position.put(start, 0d);
        Map<String, LinkedHashSet<String>> accessible = new HashMap<>();
        // System.err.println(distances);
        System.err.println(way);
        // System.err.println(accessibleRev);
        for (int i = 1; i < whenList.size(); i++) {
            Element event = whenList.get(i);
            String ref = getAttXML(event, "id");
            // System.err.println(ref);
            Distance dist;
            if (way.contains(ref)) {
                String from = way.get(way.indexOf(ref) - 1);
                // System.err.format("%s -> %s", from, ref);
                dist = paths.get(Pair.of(from, ref));
            } else {
                // elements not on the way, e.g. because of overlap
                dist = Seq.seq(distances).filter(
                        d -> d.to.equals(ref) && position.containsKey(d.from)
                ).minBy(d -> d.rel).get();
            }
            double step = dist.abs + dist.rel * itemLength;
            // System.err.println(position);
            double pos = i < whenList.size() ? position.get(dist.from) + step
                    : timeLength;
            // uncomment to test for rounding error:
            // pos = position.get(dist.from) + step;
            position.put(ref, pos);
            event.setAttributeNS(TEI_NS, "interval", String.format("%.4f", pos));
            event.setAttributeNS(TEI_NS, "since", start);
        }
    }

}
