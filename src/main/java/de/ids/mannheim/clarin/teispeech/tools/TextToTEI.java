/**
 * Converter for simple text format transcriptions to TEI-ISO
 */
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
import java.util.Stack;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import de.ids.mannheim.clarin.teispeech.data.BeginEvent;
import de.ids.mannheim.clarin.teispeech.data.EndEvent;
import de.ids.mannheim.clarin.teispeech.data.Event;
import de.ids.mannheim.clarin.teispeech.data.MarkedEvent;
import de.ids.mannheim.clarin.teispeech.data.SpeechDocument;
import de.ids.mannheim.clarin.teispeech.tools.SimpleExmaraldaParser.ActionContext;
import de.ids.mannheim.clarin.teispeech.tools.SimpleExmaraldaParser.ContentContext;
import de.ids.mannheim.clarin.teispeech.tools.SimpleExmaraldaParser.InfoContext;
import de.ids.mannheim.clarin.teispeech.tools.SimpleExmaraldaParser.MarkedContext;
import de.ids.mannheim.clarin.teispeech.tools.SimpleExmaraldaParser.SpeakerContext;
import de.ids.mannheim.clarin.teispeech.tools.SimpleExmaraldaParser.TranscriptContext;
import de.ids.mannheim.clarin.teispeech.tools.SimpleExmaraldaParser.WordContext;
import net.sf.saxon.om.NameChecker;

/**
 * @author bfi
 *
 */
public class TextToTEI extends SimpleExmaraldaBaseListener {

    private Deque<Event> events = new ArrayDeque<>();
    private Set<String> speakers = new HashSet<>();
    private Map<String, MarkedEvent> markedEvents = new LinkedHashMap<>();
    private Event currentBegin = null;
    private Event currentEnd = null;
    private Optional<Event> lastMark;
    private SpeechDocument spd;

    private static final String TEMPLATE_PATH =
            "/main/xml/NewFile.xml";


    public Document getDocument() {
        return spd.getDocument();
    }

    public void moveEvent(Event e, Event before) {
        Stack<Event> stick = new Stack<>();
        for (Event i = events.pop();
             !events.isEmpty() && i != before;
             i = events.pop()) {
            stick.push(i);
            if (events.isEmpty()) {
                throw new RuntimeException("Event not found!");
            }
        }
        assert events.pop() == before;
        events.push(e);
        events.push(before);
        for (Event i = stick.pop();
             !stick.isEmpty() && i != null;
             i = stick.pop()) {
            events.push(i);
        }
    }

    public TextToTEI() {
        javax.xml.parsers.DocumentBuilder builder;
        try (InputStream templateSource = DictionaryNormalizer.
                    class.getResourceAsStream(TEMPLATE_PATH)) {

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            builder = dbf.newDocumentBuilder();
            Document doc = builder.parse(templateSource);
            spd = new SpeechDocument(doc);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("XML parser broken!");
        } catch (IOException e1) {
            throw new RuntimeException("Template missing!");
        } catch (SAXException e) {
            throw new RuntimeException("Template broken!");
        }
    }

//    public MarkedEvent rememberMarkedEvent(String mark) {
//        if (markedEvents.containsKey(mark)) {
//            return markedEvents.get(mark);
//        } else {
//            MarkedEvent event = new MarkedEvent(mark);
//            markedEvents.put(mark, event);
//            return event;
//        }
//    }

    private void rememberSpeaker(String name) {
        if (NameChecker.isValidNCName(name)) {
            speakers.add(name);
        } else {
            throw new IllegalArgumentException(
                    "'" + name + "' is not a valid name. Start with a letter and use letters, "
                            + "full stops, underscores and digits only!");
        }
    }

    @Override
    public void exitTranscript(TranscriptContext ctx) {
        spd.makeSpeakerList(speakers);
        spd.makeTimeLine(events);
        spd.finish();
    }

    @Override
    public void enterSpeaker(SpeakerContext ctx) {
        String name = ctx.getText();
        rememberSpeaker(name);
        spd.setCurrentSpeaker(name);
    }

    @Override
    public void enterContent(ContentContext ctx) {
        currentBegin = new BeginEvent();
        currentEnd = new EndEvent();
        spd.addBlockUtterance(currentBegin, currentEnd);
        lastMark = Optional.empty();

        events.push(currentBegin);
        spd.addTurn(currentBegin);
    }

    @Override
    public void exitContent(ContentContext ctx) {
        if (lastMark.isPresent()
                // TODO: Check if lastMark already used
                ) {
            moveEvent(currentBegin, lastMark.get());
        }
        events.push(currentEnd);
        spd.endTurn(currentEnd);
    }

    @Override
    public void enterMarked(MarkedContext ctx) {
        String tx = ctx.MWORD().stream().
                map(w -> w.getText()).
                collect(Collectors.joining(" "));
        String mark = ctx.MARK_ID().getText();
        MarkedEvent m;
        if (markedEvents.containsKey(mark)) {

            m = markedEvents.get(mark);
            assert events.contains(m);
            if (!lastMark.isPresent()) {
                lastMark = Optional.of(m);
            }
        } else {
            m = new MarkedEvent();
            markedEvents.put(mark, m);
            events.push(m);
        }
        spd.addMarked(m, tx);
    }

    @Override
    public void enterAction(ActionContext ctx) {
        String tx = ctx.AWORD().stream().
                map(w -> w.getText()).
                collect(Collectors.joining(" "));
        spd.addIncident(currentBegin, currentEnd, tx);
    }

    @Override
    public void enterInfo(InfoContext ctx) {
        String tx = ctx.IWORD().stream().
                map(w -> w.getText()).
                collect(Collectors.joining(" "));
        spd.addComment(currentBegin, currentEnd, tx);
    }

    @Override
    public void enterWord(WordContext ctx) {
        spd.addText(ctx.getText());
    }

    public void makeErrorList(List<String> list) {
        spd.makeErrorList(list);
    };
}
