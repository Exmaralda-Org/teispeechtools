package de.ids.mannheim.clarin.teispeech.workflow;


import de.ids.mannheim.clarin.teispeech.data.NameSpaces;
import de.ids.mannheim.clarin.teispeech.tools.DocUtilities;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpEntity;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.jooq.lambda.Seq;
import org.korpora.useful.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.ids.mannheim.clarin.teispeech.data.NameSpaces.TEI_NS;
import static de.ids.mannheim.clarin.teispeech.tools.DocUtilities.getAttXML;
import static de.ids.mannheim.clarin.teispeech.tools.DocUtilities.getTimeLine;

/**
 * Pseudo-align documents in the TEI transcription format with the TreeTagger
 *
 * @author bfi
 */
@SuppressWarnings("WeakerAccess")
public class PseudoAlign {

    static {
        Locale.setDefault(Locale.ROOT);
    }

    private final static Logger LOGGER = LoggerFactory
            .getLogger(PseudoAlign.class.getName());

    private static final NumberFormat NUMBER_FORMAT = NumberFormat
            .getInstance(Locale.ROOT);

    static {
        NUMBER_FORMAT.setMinimumFractionDigits(0);
        NUMBER_FORMAT.setMaximumFractionDigits(3);
    }

    /**
     * default language
     */
    private final String language;
    private final double offset;

    /**
     * XML DOM document
     */
    private Document doc;

    public Document getDoc() {
        return doc;
    }

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
                            ".//*[(local-name() = 'w' or local-name() " +
                                    "='pause') and namespace-uri() = '%s']|" +
                                    ".//text()",
                            TEI_NS));
            blocky =
                    xPath.compile(String.format(
                            ".//*[(local-name() = 'annotationBlock' or " +
                                    "local-name() ='incident') and " +
                                    "namespace-uri() = '%s']",
                            TEI_NS));
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * make new PseudoAlign for
     *
     * @param doc
     *         a DOM XML document
     * @param language
     *         the default document language
     * @param usePhones
     *         whether to count relative duration in (pseudo)phones if
     *         possible
     * @param phoneticise
     *         whether to store the transcriptions in the document
     * @param force
     *         whether to force transcription
     * @param timeLength
     *         length of audio in seconds
     * @param offset
     *         the time offset of the first timeline event
     */
    public PseudoAlign(Document doc, String language, boolean usePhones,
                       boolean phoneticise, boolean force, double timeLength,
                       double offset) {
        this.language = language;
        this.doc = doc;
        this.phoneticise = phoneticise;
        this.usePhones = usePhones;
        this.force = force;
        this.timeLength = timeLength;
        this.offset = offset;
        if (!usePhones && phoneticise) {
            LOGGER.warn(
                    "phoneticise but not usePhones is not useful: phoneticise" +
                            " ignored.");
        }
    }

    /**
     * get duration of element, measured in seconds
     *
     * @param el
     *         the element
     * @return the duration
     */
    public Optional<Double> getUtteranceDuration(Element el) {
        // TODO: check for local anchors and do something about it
        Optional<Double> duration = DocUtilities.getDuration(el);
        LOGGER.info("utterance duration: {}", duration);
        if (!duration.isPresent()) {
            String start = el.getAttribute("start");
            String end = el.getAttribute("end");
            if (DocUtilities.isTEI(el, "u")) {
                Element par = ((Element) el.getParentNode());
                if (DocUtilities.isTEI(par, "annotationBlock")) {
                    if ("".equals(start)) {
                        start = par.getAttribute("start");
                        if (!"".equals(start))
                            el.setAttribute("start", start);
                    }
                    if ("".equals(end)) {
                        end = par.getAttribute("end");
                        if (!"".equals(end))
                            el.setAttribute("end", end);
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
     * @param el
     *         pause element
     * @return duration
     */
    private double getPausePhoneDuration(Element el) {
        Optional<Double> duration = DocUtilities.getDuration(el);
        if (duration.isPresent()) {
            return duration.get();
        } else {
            double dur = 0;
            String type = el.getAttribute("type");
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
                                + el.getAttribute("type") + "»!");
                }
            }
            return dur;
        }
    }

    /**
     * calculate relative length of utterances the document
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
                                    w.getAttribute("type"))))
                            .toList();
                    if (transcribe) {
                        // only transcribe "normal" words
                        // work with transcriptions
                        String text = Seq.seq(words)
                                .map(Node::getTextContent).toString(" ");
                        //noinspection OptionalGetWithoutIsPresent
                        transWords = GraphToPhoneme
                                .getTranscription(text, uLanguage, true)
                                .get();

                        if (phoneticise) {
                            Seq.seq(words).zip(Arrays.asList(transWords))
                                    .forEach(tup -> {
                                        if (!force || (!tup.v1
                                                .hasAttributeNS(TEI_NS, "phon")
                                                && !tup.v1.hasAttribute(
                                                "phon"))) {
                                            tup.v1.setAttribute("phon", tup.v2);
                                        }
                                    });
                        }
                        // add transcriptions
                        @SuppressWarnings("ConstantConditions") int[] transcribedW = GraphToPhoneme
                                .countSigns(transWords, transcribe);
                        for (int i = 0; i < words.size(); i++) {
                            words.get(i).setAttribute("rel-length",
                                    String.format("%d", transcribedW[i]));
                        }
                    } else {
                        // fall back to counting letters
                        transWords = Seq.seq(words)
                                .map(Node::getTextContent)
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
                                    w.getAttribute("type"))))
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
                .forEach(this::annotateSingleUtterance);
        Optional<Double> itemLength = relItemLength();
        itemLength.ifPresent(this::applyItemLength);
        cleanUp();
        // TODO: remove relative lengths after testing
        // USE XSLT!
        DocUtilities.makeChange(doc, "Pseudo-aligned");
    }

    /**
     * increase all counters in Map (int version)
     *
     * @param map
     *         the Map
     * @param addendum
     *         the amount to increase
     * @param <T>
     *         the type of the counters
     */
    private static <T> void incAllCounters(Map<T, Integer> map, int addendum) {
        for (Map.Entry<T, Integer> entry : map.entrySet()) {
            entry.setValue(entry.getValue() + addendum);
        }
    }

    /**
     * increase all counters in Map (double version)
     *
     * @param map
     *         the Map
     * @param addendum
     *         the amount to increase
     * @param <T>
     *         the type of the counters
     */
    private static <T> void incAllCounters(Map<T, Double> map,
                                           double addendum) {
        for (Map.Entry<T, Double> entry : map.entrySet()) {
            entry.setValue(entry.getValue() + addendum);
        }
    }

    /**
     * distance between elements
     */
    private class Distance {
        final int rel;
        final double abs;
        final String from;
        final String to;

        /**
         * create a distance between elements
         *
         * @param rel
         *         distance in characters / pseudophones
         * @param abs
         *         distance in time, esp. pauses
         * @param from
         *         first element
         * @param to
         *         second element
         */
        Distance(int rel, double abs, String from, String to) {
            this.rel = rel;
            this.abs = abs;
            this.from = from;
            this.to = to;
        }

        /**
         * generate string representation
         *
         * @return string representation
         */
        @Override
        public String toString() {
            return String.format("{Δ<%s, %s> == <%d, -%.3f>}",
                    from, to, rel, abs);
        }
    }

    /**
     * distances in the document
     */
    private final List<Distance> distances = new ArrayList<>();

    /**
     * events of the timeline
     */
    private List<Element> whenList = new ArrayList<>();

    /**
     * path from first to last event
     */
    private List<String> way = new ArrayList<>();

    /**
     * paths between events in the document
     */
    private final Map<Pair<String, String>, Distance> paths = new HashMap<>();

    /**
     * reverse accessibility: Going *backwards*,
     * there is a path from every Key to the members of its Value.
     */
    private final Map<String, LinkedHashSet<String>> accessibleRev =
            new HashMap<>();


    private void makeDistance(String from, String to,
                              int relDuration,
                              double absDuration) {
        distances.add(new Distance(relDuration, absDuration, from, to));
    }


    /**
     * make a Hash that gives the position for every event
     *
     * @param whens
     *         the events
     * @return the Hash EventName -> Position
     */
    private static Map<String, Integer> getOrder(List<Element> whens) {
        Map<String, Integer> order = new HashMap<>();
        for (int i = 0; i < whens.size(); i++) {
            Element el = whens.get(i);
            String elID = getAttXML(el, "id");
            order.put(elID, i);
        }
        return order;
    }


    /**
     * find a way {@code from} from to {@code goal}
     *
     * @param from
     *         start of passage
     * @param goal
     *         end of passage (first event)
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

    /**
     * annotate a single utterance
     *
     * @param u
     *         the utterance
     */
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
        String currentEl = DocUtilities.unPoundMark(par.getAttribute("start"));
        String endEl = DocUtilities.unPoundMark(par.getAttribute("end"));
        Map<String, Integer> relDuration = new HashMap<>();
        relDuration.put(currentEl, 0);
        absDuration.put(currentEl, 0d);
        // calculate relative durations and time to distribute over them
        for (Element el : uChildren) {
            if (DocUtilities.isTEI(el, "w")) {
                incAllCounters(relDuration, Integer.parseInt(el.getAttribute(
                        "rel-length")));
                // count text length in characters for anchors in words
                int textTillNow = 0;
                Map<String, Integer> relRest = new HashMap<>();
                for (Iterator<Node> iNo =
                     Utilities.toIterator(el.getChildNodes());
                     iNo.hasNext(); ) {
                    Node now = iNo.next();
                    // set anchor to text length
                    if (now.getNodeType() == Node.ELEMENT_NODE &&
                            now.getLocalName().equals("anchor")) {
                        Element nowEl = ((Element) now);
                        String nowName = DocUtilities.unPoundMark(
                                nowEl.getAttribute("synch"));
                        for (String name : relDuration.keySet()) {
                            makeDistance(name, nowName,
                                    relDuration.get(name) + textTillNow,
                                    absDuration.get(name));
                        }
                        relRest.put(nowName, 0);
                    } else if (now.getNodeType() == Node.TEXT_NODE ||
                            (now.getNodeType() == Node.ELEMENT_NODE
                                    && now.getLocalName().equals("w"))) {
                        int count = Utilities.removeSpace(
                                now.getTextContent()).length();
                        textTillNow += count;
                        incAllCounters(relRest, count);
                    }
                }
                // for (String name : relRest.keySet()) {
                //     Element delta = u.getOwnerDocument().createElement
                //     ("rel-rest");
                //     delta.setAttribute("after", name);
                //     delta.setAttribute("rel", String.valueOf(relRest.get
                //     (name)));
                //     u.appendChild(delta);
                // }
            } else if (DocUtilities.isTEI(el, "pause")) {
                incAllCounters(absDuration, getPausePhoneDuration(el));
            } else {
                LOGGER.warn("Skipped {}", el.getTagName());
            }
        }
        for (String name : relDuration.keySet()) {
            makeDistance(name, endEl, relDuration.get(name),
                    absDuration.get(name));
        }
    }

    // TODO: What about empty incidents?

    /**
     * determine the length of a pseudophone
     *
     * @return length in seconds
     */
    private Optional<Double> relItemLength() {
        NodeList whens = DocUtilities.getWhens(doc);
        whenList = Utilities.toElementList(whens);
        NodeList nodes;
        try {
            nodes = (NodeList) blocky.evaluate(doc, XPathConstants.NODESET);
            Map<String, Integer> order = getOrder(whenList);

            // elements that are next to each other:
            {
                int i = 0;
                do {
                    Element u = (Element) nodes.item(i);
                    String end = u.getAttribute("end");
                    boolean checked = false;
                    do {
                        Element uNext = (Element) nodes.item(i + 1);
                        String startNext = uNext.getAttribute("start");
                        if (end.equals(startNext) ||
                                // no overlap:
                                order.get(startNext) > order.get(end)) {
                            accessibleRev.putIfAbsent(startNext,
                                    new LinkedHashSet<>());
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
                        String from = e.getAttribute("start");
                        String to = e.getAttribute("end");
                        accessibleRev.putIfAbsent(to,
                                new LinkedHashSet<>());
                        if (!accessibleRev.get(to).contains(from)) {
                            accessibleRev.get(to).add(from);
                            Distance distance = new Distance(0, 0d, from, to);
                            distances.add(distance);
                            paths.put(Pair.of(distance.from, distance.to),
                                    distance);
                        }
                    });
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
        String goal = getAttXML(whenList.get(0), "id");
        way = findPathR(getAttXML(whenList.get(whenList.size() - 1), "id"),
                goal);
        if (way != null) {
            // System.err.println(way);
            int rel = 0;
            double abs = 0d;
            for (int i = 0; i < way.size() - 1; i++) {
                Distance d = paths.get(Pair.of(way.get(i), way.get(i + 1)));
                if (d != null) {
                    rel += d.rel;
                    abs += d.abs;
                }
            }
            // System.err.format(">>> (%f - %f) / %d = %f\n", timeLength,
            // abs, rel,
            //         ((timeLength - abs) / rel));
            return Optional.of((timeLength - abs) / rel);
        }
        return Optional.empty();
    }

    /**
     * determine lenth of annotationBlocks
     *
     * @param itemLength
     *         seconds per item
     */
    private void applyItemLength(Double itemLength) {
        Comment comment = doc.createComment(
                String.format(" length per item: %.4f seconds", itemLength));
        Utilities.insertAtBeginningOf(comment,
                Utilities.getElementByTagNameNS(doc, TEI_NS, "body"));
        Map<String, Double> position = new HashMap<>();
        String start = getAttXML(whenList.get(0), "id");
        position.put(start, 0d);
        // System.err.println(distances);
        // System.err.println(way);
        // System.err.println(accessibleRev);

        getTimeLine(doc).setAttribute("unit", "s");
        whenList.get(0).setAttribute("absolute", String.format("%.4fs",
                offset));
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
                //noinspection OptionalGetWithoutIsPresent
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
            event.setAttribute("interval",
                    String.format("%.4fs", pos));
            event.setAttribute("since", start);
        }
    }

    /**
     * cleanup document, remove temporary annotation
     */
    private void cleanUp() {
        doc = DocUtilities.transform("/PseudoAlign.xsl", doc);
    }

    /**
     * Pseudo-align an utterance call BAS's web services to transcribe
     * phone[mt]ically
     *
     * <p>
     * TODO: test whether syllable structure improves
     *
     * <p>
     * TODO: cutoff
     *
     * <p>
     * TODO: equidistant syllables
     *
     * <p>
     * TODO: (related) change language normalization to account for Australian
     * languages; use
     * <a href="https://iso639-3.sil.org/code_tables/download_tables">list from
     * SIL</a>.
     */
    @SuppressWarnings("SameParameterValue")
    static
    class GraphToPhoneme {

        /**
         * list of admissible locales from
         * http://clarin.phonetik.uni-muenchen.de/BASWebServices/services/help
         */
        // TODO: should we map "est" and "ee" to "ekk"?
        // TODO: better: map codes to canonical 639-1/-2 code if possible
        private static final String[] PERMITTED_LOCALES_ARRAY = {"aus-AU",
                "cat",
                "cat-ES", "deu", "deu-DE", "ekk-EE", "eng", "eng-AU", "eng-GB",
                "eng-NZ", "eng-US", "eus-ES", "eus-FR", "fin", "fin-FI", "fra" +
                "-FR",
                "gsw-CH", "gsw-CH-BE", "gsw-CH-BS", "gsw-CH-GR", "gsw-CH-SG",
                "gsw-CH-ZH", "guf-AU", "gup-AU", "hat", "hat-HT", "hun", "hun" +
                "-HU",
                "ita", "ita-IT", "jpn-JP", "kat-GE", "ltz-LU", "mlt", "mlt-MT",
                "nld", "nld-NL", "nor-NO", "nze", "pol", "pol-PL", "ron-RO",
                "rus-RU", "slk-SK", "spa-ES", "sqi-AL", "swe-SE"};

        /**
         * base URL for transcription service
         */
        private final static String BASE_URL = "https://clarin.phonetik" +
                ".uni-muenchen.de/BASWebServices/services/runG2P";

        /**
         * locale separator
         */
        private static final Pattern LOCALE_SEPARATOR = Pattern.compile("[_" +
                "-]+");
        /**
         * word separator for transcriptions (tab)
         */
        private static final Pattern WORD_SEPARATOR = Pattern.compile("\t");

        /**
         * locales permitted in annotation, including some substitutions
         */
        private static final Map<String, String> LOCALES = new HashMap<>();

        static {
            List<String> already_permitted =
                    Arrays.asList(PERMITTED_LOCALES_ARRAY);
            for (String loc : PERMITTED_LOCALES_ARRAY) {
                LOCALES.put(loc, loc);
                String[] components = LOCALE_SEPARATOR.split(loc);
                for (int i = 0; i < components.length - 1; i++) {
                    String active = String.join("-",
                            Arrays.copyOfRange(components, 0, i));
                    if (!already_permitted.contains(active)) {
                        LOCALES.put(active, loc);
                    }
                    if ("ekk".equals(components[0])) {
                        components[0] = "est";
                        active = String.join("-",
                                Arrays.copyOfRange(components, 0, i));
                        if (!already_permitted.contains(active)) {
                            LOCALES.put(active, loc);
                        }
                    }
                }
            }
        }

        /**
         * have a text transcribed according to a locale
         *
         * @param text
         *         the text
         * @param loc
         *         the locale
         * @return the transcription
         */
        private static Optional<String[]> getTranscription(String text,
                                                           String loc) {
            return getTranscription(text, loc, false);
        }

        public static Optional<String> correspondsTo(String locale) {
            String[] components = LOCALE_SEPARATOR.split(locale);
            Optional<String> ret = Optional.empty();
            for (int i = components.length; i >= 0; i--) {
                String loki = String.join("-",
                        Arrays.copyOfRange(components, 0, i));
                if (LOCALES.containsKey(loki))
                    return Optional.of(LOCALES.get(loki));
            }
            return ret;
        }

        /**
         * get a transcription from the web service
         *
         * @param text
         *         running text
         * @param loc
         *         locale
         * @param getSyllables
         *         whether to have the transcription syllabified
         * @return an Optional containing the list of transcribed words or
         * emptiness
         */
        public static Optional<String[]> getTranscription(String text,
                                                          String loc,
                                                          boolean getSyllables) {
            Optional<String[]> ret = Optional.empty();
            try {
                URIBuilder uriBui = new URIBuilder(BASE_URL);
                boolean extendedFeatures = false; // extended for eng-GB and
                // deu?
                @SuppressWarnings("ConstantConditions") HttpEntity entity =
                        MultipartEntityBuilder.create()
                                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                                .setCharset(Charset.forName("UTF-8"))
                                .addBinaryBody("i", text.getBytes(),
                                        ContentType.MULTIPART_FORM_DATA,
                                        "input")
                                .addTextBody("com", "no")
                                .addTextBody("syl", getSyllables ? "yes" : "no")
                                .addTextBody("outsym", "ipa").addTextBody(
                                        "oform",
                                "txt")
                                .addTextBody("iform", "list") // txt?
                                .addTextBody("align", "no").addTextBody("lng"
                                , loc)
                                .addTextBody("featset",
                                        extendedFeatures ? "extended" :
                                                "standard")
                                .build();
                String result =
                        Request.Post(uriBui.build()).body(entity).execute()
                                .returnContent().asString();
                Document doc = Utilities.parseXML(result);
                Element link = Utilities.getElementByTagName(doc,
                        "downloadLink");
                if (link != null && !"".equals(link.getTextContent())) {
                    String retString =
                            Request.Get(link.getTextContent()).execute()
                                    .returnContent().asString(Charset.forName("UTF-8"))
                                    .replace(" ", "");
                    ret = Optional.of(WORD_SEPARATOR.split(retString.trim()));
                }
            } catch (URISyntaxException | IOException | ParserConfigurationException
                    | SAXException e) {
                throw new RuntimeException(e);
            }
            return ret;
        }

        /**
         * count signs in transcription
         *
         * @param words
         *         an {@link Optional} array of transcribed words
         * @param syllabified
         *         whether syllable separators (full stops) are present and
         *         should be removed
         * @return the word lengths in signs
         */
        private static int[] countSigns(Optional<String[]> words,
                                        boolean syllabified) {
            if (words.isPresent())
                return countSigns(words.get(), syllabified);
            else
                return new int[]{};
        }

        /**
         * @param words
         *         (not syllabified)
         * @return the word lengths in signs
         * @see #countSigns(Optional, boolean)
         */
        public static int[] countSigns(Optional<String[]> words) {
            return countSigns(words, false);
        }

        /**
         * @param text
         *         a string to be split in words and transcribed
         * @param separator
         *         the word separator {@link Pattern}
         * @return number of characters
         * @see #countSigns(Optional, boolean)
         */
        public static int[] countSigns(String text, Pattern separator) {
            return countSigns(separator.split(text));
        }

        /**
         * @param text
         *         the text to count word lengths for
         * @return number of characters
         * @see #countSigns(String[])
         **/
        private static int[] countSigns(String text) {
            return countSigns(text.split("\\s+"));
        }

        /**
         * count signs in transcription
         *
         * @param words
         *         an array of transcribed words
         * @param syllabified
         *         whether syllable separators (full stops) are present and
         *         should be removed
         * @return the word lengths in signs
         */

        public static int[] countSigns(String[] words, boolean syllabified) {
            Stream<String> wordStream = Stream.of(words);
            if (syllabified) // remove syllable limits
                wordStream = wordStream.map(s -> s.replace(".", ""));
            wordStream = wordStream.map(s ->
                    s.replaceAll("[-\\p{javaWhitespace}]", ""));

            return wordStream.mapToInt(String::length).toArray();
        }

        /**
         * @param words
         *         an array of transcribed words
         * @return lengths of individual words
         * @see #countSigns(Optional, boolean)
         */
        private static int[] countSigns(String[] words) {
            Stream<String> wordStream = Stream.of(words);
            return wordStream.mapToInt(String::length).toArray();
        }

        /**
         * count signs in transcription
         *
         * @param words
         *         the transcribed words
         * @return the number of syllables for each word
         **/
        private static int[] countSyllables(String[] words) {
            Stream<String> wordStream = Stream.of(words);
            return wordStream.mapToInt(word -> word.split("\\.").length).toArray();
        }

        /**
         * count syllables in transcription
         *
         * @param words
         *         an array of transcribed words
         * @param loc
         *         the locale
         * @return the word lengths in signs
         */
        private static int[] countSyllables(String words, String loc) {
            Optional<String[]> result = getTranscription(words, loc, true);
            if (result.isPresent())
                return countSyllables(result.get());
            else
                return new int[]{};
        }

        /**
         * print phone/letter counts in transcription,
         * {@link #printCounts(int[])}
         *
         * @param text
         *         text to be transcribed
         * @return string representation of counts
         */
        public static String printCounts(String text) {
            return printCounts(countSigns(text));
        }

        /**
         * print phone/letter counts in transcription
         *
         * @param counted
         *         the counted letters/phones
         * @return string representation of counts
         */
        private static String printCounts(int[] counted) {
            return Seq.seq(Arrays.stream(counted)).map(i -> {
                String format = "% " + i + "d";
                return String.format(format, i);
            }).toString(" ");
        }

        /**
         * program to test transcription: transcribe all arguments as German
         * text
         *
         * @param args
         *         supposedly single words
         */
        public static void main(String[] args) {
            String text = "Dies Beispiel ist bezaubernd schön – Strumpf!";
            if (args.length > 0) {
                text = String.join(" ", args);
            }
            Optional<String[]> result = getTranscription(text, "deu");
            if (result.isPresent()) {
                text = text.trim();
                System.out.println(String.join(" ", text));
                System.out.println(printCounts(countSigns(text)));
                System.out.println(String.join(" ", result.get()));
                int[] counted = countSigns(result, true);
                System.out.println(printCounts(counted));
                System.out.println(String.format("Syllables: %s",
                        Arrays.toString(countSyllables(text, "deu"))));
            }
        }
    }
}
