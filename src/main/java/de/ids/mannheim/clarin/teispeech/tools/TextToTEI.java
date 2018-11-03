package de.ids.mannheim.clarin.teispeech.tools;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import de.ids.mannheim.clarin.teispeech.data.BeginEvent;
import de.ids.mannheim.clarin.teispeech.data.EndEvent;
import de.ids.mannheim.clarin.teispeech.data.Event;
import de.ids.mannheim.clarin.teispeech.data.MarkedEvent;
import de.ids.mannheim.clarin.teispeech.data.SpeechDocument;
import de.ids.mannheim.clarin.teispeech.tools.SimpleExmaraldaParser.ActionContext;
import de.ids.mannheim.clarin.teispeech.tools.SimpleExmaraldaParser.CommentContext;
import de.ids.mannheim.clarin.teispeech.tools.SimpleExmaraldaParser.ContentContext;
import de.ids.mannheim.clarin.teispeech.tools.SimpleExmaraldaParser.MarkedContext;
import de.ids.mannheim.clarin.teispeech.tools.SimpleExmaraldaParser.SpeakerContext;
import de.ids.mannheim.clarin.teispeech.tools.SimpleExmaraldaParser.TranscriptContext;
import de.ids.mannheim.clarin.teispeech.tools.SimpleExmaraldaParser.WordContext;
import net.sf.saxon.om.NameChecker;

/**
 * converter from Simple EXMARaLDA-encoded document of annotated speech to a
 * TEI-encoded document
 *
 * @author bfi
 *
 */
public class TextToTEI extends SimpleExmaraldaBaseListener {

    private Deque<Event> events = new ArrayDeque<>();
    private Set<String> speakers = new HashSet<>();
    private Map<String, MarkedEvent> markedEvents = new LinkedHashMap<>();
    private Event currentBegin = null;
    private Event currentEnd = null;

    /**
     * This records the first overlap mark in an utterance content. It is set if
     * the mark occurs for the second or a later time. Begin events of turns are
     * moved before <code>firstMark</code> in the timeline later.
     */
    private Optional<Event> firstMark;
    private SpeechDocument spd;
    private CommonTokenStream tokens;
    private static final String TEMPLATE_PATH = "/main/xml/NewFile.xml";

    /**
     * Constructor
     *
     * prepare XML template.
     *
     * @param tokens
     *            the token stream of the document
     * @param language
     *            the language code for the document language
     */
    public TextToTEI(CommonTokenStream tokens, String language) {
        this.tokens = tokens;
        javax.xml.parsers.DocumentBuilder builder;
        try (InputStream templateSource = DictionaryNormalizer.class
                .getResourceAsStream(TEMPLATE_PATH)) {

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            builder = dbf.newDocumentBuilder();
            Document doc = builder.parse(templateSource);
            spd = new SpeechDocument(doc, language);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("XML parser broken!");
        } catch (IOException e1) {
            throw new RuntimeException("Template missing!");
        } catch (SAXException e) {
            throw new RuntimeException("Template broken!");
        }
    }

    /**
     * get XML DOM Document from {@link SpeechDocument}
     *
     * @return the DOM document
     */
    public Document getDocument() {
        return spd.getDocument();
    }

    /**
     * in timeline, move one Event before another.
     *
     * @param e
     *            event to be moved
     * @param before
     *            event before which e shall go
     */
    public void moveEvent(Event e, Event before) {
        Deque<Event> stick = new ArrayDeque<>();
        for (Event i = events.pop(); !events.isEmpty()
                && i != before; i = events.pop()) {
            stick.push(i);
            if (events.isEmpty()) {
                throw new RuntimeException("Event not found!");
            }
        }
        assert events.pop() == before;
        events.push(e);
        events.push(before);
        for (Event i = stick.pop(); !stick.isEmpty()
                && i != null; i = stick.pop()) {
            events.push(i);
        }
    }

    // public MarkedEvent rememberMarkedEvent(String mark) {
    // if (markedEvents.containsKey(mark)) {
    // return markedEvents.get(mark);
    // } else {
    // MarkedEvent event = new MarkedEvent(mark);
    // markedEvents.put(mark, event);
    // return event;
    // }
    // }

    /**
     * remember speaker and check that name is a valid XML ID
     *
     * @param name
     */
    private void rememberSpeaker(String name) {
        if (NameChecker.isValidNCName(name)) {
            speakers.add(name);
        } else {
            throw new IllegalArgumentException(
                    "«" + name + "» is not a valid name. Names "
                            + "start with a letter and use letters, "
                            + "full stops, underscores and digits only!");
        }
    }

    /**
     * fill in lists of speakers, time line, changes.
     */
    @Override
    public void exitTranscript(TranscriptContext ctx) {
        spd.makeSpeakerList(speakers);
        spd.makeTimeLine(events);
        spd.finish();
    }

    /**
     * Speaker: make sure speaker is registered and set as current
     */
    @Override
    public void enterSpeaker(SpeakerContext ctx) {
        String name = ctx.getText();
        rememberSpeaker(name);
        spd.setCurrentSpeaker(name);
    }

    /**
     * Content of a contribution: register events and set currentUtterance
     */
    @Override
    public void enterContent(ContentContext ctx) {
        currentBegin = new BeginEvent();
        currentEnd = new EndEvent();
        spd.addBlockUtterance(currentBegin, currentEnd);
        firstMark = Optional.empty();

        events.push(currentBegin);
        spd.addTurn(currentBegin);
    }

    /**
     * one line is finished; mainly event management
     */
    @Override
    public void exitContent(ContentContext ctx) {
        // marked event referring to past contributions
        // present, must move begin of this one before it.
        if (firstMark.isPresent()) {
            moveEvent(currentBegin, firstMark.get());
        }
        events.push(currentEnd);
        spd.endTurn(currentEnd);
    }

    /**
     * marked event encountered – event management.
     */
    @Override
    public void enterMarked(MarkedContext ctx) {
        List<Token> left = tokens
                .getHiddenTokensToLeft(ctx.getStart().getTokenIndex());
        if (left != null && left.size() > 0) {
            spd.addSpace();
        }
        String tx = ctx.MWORD().stream().map(w -> w.getText())
                .collect(Collectors.joining(" "));
        String mark = ctx.MARK_ID().getText();
        MarkedEvent m;
        // register if unknown; else, use as terminus ante quem for begin
        if (markedEvents.containsKey(mark)) {

            m = markedEvents.get(mark);
            assert events.contains(m);
            if (!firstMark.isPresent()) {
                firstMark = Optional.of(m);
            }
        } else {
            m = new MarkedEvent(mark);
            markedEvents.put(mark, m);
            events.push(m);
        }
        spd.addMarked(m, tx);
    }

    // @Override
    // public void exitMarked(MarkedContext ctx) {
    // List<Token> right = tokens
    // .getHiddenTokensToRight(ctx.getStop().getTokenIndex());
    // if (right != null && right.size() > 0) {
    // spd.addSpace();
    //
    // }
    // }
    //

    /**
     * incident encountered
     */
    @Override
    public void enterAction(ActionContext ctx) {
        String tx = ctx.AWORD().stream().map(w -> w.getText())
                .collect(Collectors.joining(" "));
        spd.addIncident(currentBegin, currentEnd, tx);
    }

    /**
     * comment encountered
     */
    @Override
    public void enterComment(CommentContext ctx) {
        String tx = ctx.IWORD().stream().map(w -> w.getText())
                .collect(Collectors.joining(" "));
        spd.addComment(currentBegin, currentEnd, tx);
    }

    /**
     * word encountered; consign to white space management
     */
    @Override
    public void enterWord(WordContext ctx) {
        List<Token> left = tokens
                .getHiddenTokensToLeft(ctx.getStart().getTokenIndex());
        boolean space = (left != null);
        spd.addText(ctx.getText(), space);
    }

    /**
     * prepare list of errors, delegate do {@link SpeechDocument}
     *
     * @param list
     *            the list of error messages
     */
    public void makeErrorList(List<String> list) {
        spd.makeErrorList(list);
    };
}
