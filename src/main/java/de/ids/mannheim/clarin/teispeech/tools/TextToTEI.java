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
import de.ids.mannheim.clarin.teispeech.tools.SimpleExmaraldaParser.TextContext;
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
    private int currentPos = 0;
//    private int lastMarkPos = 0;
    private Optional<MarkedEvent> lastMarked;

    /**
     * This records the first overlap mark in an utterance content. It is set if
     * the mark occurs for the second or a later time. Begin events of turns are
     * moved before <code>firstMark</code> in the timeline later.
     */
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
        currentPos = 0;
//        lastMarkPos = 0;
        lastMarked = Optional.empty();
        currentBegin = new BeginEvent();
        currentEnd = new EndEvent();
        spd.addBlockUtterance(currentBegin, currentEnd);
        events.push(currentBegin);
        spd.addTurn(currentBegin);
    }

    @Override
    public void enterText(TextContext ctx) {
        currentPos++;
    }

    /**
     * one line is finished; mainly event management
     */
    @Override
    public void exitContent(ContentContext ctx) {
        // marked event referring to past contributions
        // present, check if it is the last one
        if (!spd.endTurn(currentEnd, lastMarked)) {
            events.push(currentEnd);
        }
    }

    /**
     * marked event encountered – event management.
     */
    @Override
    public void enterMarked(MarkedContext ctx) {
        List<Token> left = tokens
                .getHiddenTokensToLeft(ctx.getStart().getTokenIndex());
        if (left != null && left.size() > 0 && currentPos > 1) {
            spd.addSpace();
        }
        String tx = ctx.MWORD().stream().map(w -> w.getText())
                .collect(Collectors.joining(" "));
        String mark = ctx.MARK_ID().getText();
        MarkedEvent m;
        // register if unknown; else, use as terminus ante quem for begin
        boolean startAnchor;
        if (markedEvents.containsKey(mark)) {
            if (currentPos == 1) {
                startAnchor = false;
                m = markedEvents.get(mark);
                assert events.contains(m);
                spd.changeBlockStart(currentBegin, m);
                events.remove(currentBegin);
                System.err.println("Removed " + currentBegin.mkTimeRef());
                currentBegin = m;

            } else {
                throw new RuntimeException(String.format(
                        "line %d, char %d: second and latter occurrence "
                                + "of overlap must be at beginning of line!",
                        ctx.getStart().getLine(),
                        ctx.getStart().getCharPositionInLine()));
            }
        } else {
            startAnchor = true;
            m = new MarkedEvent(mark);
            markedEvents.put(mark, m);
            events.push(m);
        }
//        lastMarkPos = currentPos;
        lastMarked = Optional.ofNullable(m);
        spd.addMarked(m, tx, startAnchor);
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
