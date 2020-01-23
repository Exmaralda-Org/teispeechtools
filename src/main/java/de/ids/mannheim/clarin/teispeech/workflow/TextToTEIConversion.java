package de.ids.mannheim.clarin.teispeech.workflow;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import de.ids.mannheim.clarin.teispeech.data.SpeechDocument;
import de.ids.mannheim.clarin.teispeech.tools.SimpleExmaralda;
import de.ids.mannheim.clarin.teispeech.tools.SimpleExmaraldaBaseListener;
import de.ids.mannheim.clarin.teispeech.tools.SimpleExmaraldaLexer;
import de.ids.mannheim.clarin.teispeech.utilities.AntlrErrorLister;
import net.sf.saxon.om.NameChecker;

/**
 * converter from plain text to TEI ISO
 */
@SuppressWarnings("WeakerAccess")
public class TextToTEIConversion {

    /**
     * convert a plain text document to TEI ISO
     *
     * @param input
     *     the input
     * @param language
     *     the language of the document
     * @return the document
     */
    public static Document process(CharStream input, String language) {
        AntlrErrorLister lister = new AntlrErrorLister();
        SimpleExmaraldaLexer lexer = new SimpleExmaraldaLexer(input);
        lexer.addErrorListener(lister);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SimpleExmaralda parser = new SimpleExmaralda(tokens);
        parser.addErrorListener(lister);
        ParseTreeWalker walker = new ParseTreeWalker();
        ParseTree tree = parser.transcript();
        TextToTEI tt = new TextToTEI(tokens, language);
        walker.walk(tt, tree);
        tt.makeErrorList(lister.getList());
        return tt.getDocument();
    }

    /**
     * converter from Simple EXMARaLDA-encoded document of annotated speech to a
     * TEI-encoded document
     *
     * @author bfi
     *
     */
    @SuppressWarnings("WeakerAccess")
    private static class TextToTEI extends SimpleExmaraldaBaseListener {

        private final Deque<SpeechDocument.Event> events = new ArrayDeque<>();
        private final Set<String> speakers = new HashSet<>();
        private final Map<String, SpeechDocument.MarkedEvent> markedEvents = new LinkedHashMap<>();
        private SpeechDocument.Event currentBegin = null;
        private SpeechDocument.Event currentEnd = null;
        private int currentPos = 0;
        private Optional<SpeechDocument.MarkedEvent> lastMarked;

        /**
         * This records the first overlap mark in an utterance content. It is
         * set if the mark occurs for the second or a later time. Begin events
         * of turns are moved before {@code firstMark} in the timeline later.
         */
        private SpeechDocument spd;
        private final CommonTokenStream tokens;
        private static final String TEMPLATE_PATH = "NewFile.xml";

        /**
         * Constructor:
         *
         * prepare XML template.
         *
         * @param tokens
         *     the token stream of the document
         * @param language
         *     the language code for the document language
         */
        public TextToTEI(CommonTokenStream tokens, String language) {
            this.tokens = tokens;
            try (InputStream templateSource = TextToTEI.class.getClassLoader()
                    .getResourceAsStream(TEMPLATE_PATH)) {
                DocumentBuilderFactory dbf = DocumentBuilderFactory
                        .newInstance();
                dbf.setNamespaceAware(true);
                DocumentBuilder builder = dbf.newDocumentBuilder();
                Document doc = builder
                        .parse(Objects.requireNonNull(templateSource));
                spd = new SpeechDocument(doc, language);
            } catch (IOException e1) {
                throw new RuntimeException("Template missing!");
            } catch (SAXException e) {
                throw new RuntimeException("Template broken!");
            } catch (ParserConfigurationException e) {
                // TODO Auto-generated catch block
                throw new RuntimeException(e);
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
         *     the potential ID
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
        public void exitTranscript(SimpleExmaralda.TranscriptContext ctx) {
            spd.makeSpeakerList(speakers);
            spd.makeTimeLine(events);
            spd.insertTimeRoot();
            spd.applyDuration();
            spd.applyOffset();
            spd.finish();
        }

        /**
         * Speaker: make sure speaker is registered and set as current
         */
        @Override
        public void enterSpeaker(SimpleExmaralda.SpeakerContext ctx) {
            String name = ctx.getText();
            rememberSpeaker(name);
            spd.setCurrentSpeaker(name);
        }

        /**
         * Content of a contribution: register events and set currentUtterance
         */
        @Override
        public void enterContent(SimpleExmaralda.ContentContext ctx) {
            currentPos = 0;
            lastMarked = Optional.empty();
            currentBegin = new SpeechDocument.BeginEvent();
            currentEnd = new SpeechDocument.EndEvent();
            spd.addBlockUtterance(currentBegin, currentEnd);
            events.push(currentBegin);
            // spd.addTurn(currentBegin);
        }

        @Override
        public void enterText(SimpleExmaralda.TextContext ctx) {
            currentPos++;
        }

        /**
         * one line is finished; mainly event management
         */
        @Override
        public void exitContent(SimpleExmaralda.ContentContext ctx) {
            // marked event referring to past contributions
            // present, check if it is the last one
            if (!spd.endTurn(currentEnd, lastMarked)) {
                events.push(currentEnd);
            }
            spd.cleanUtterance();
        }

        /**
         * marked event encountered – event management.
         */
        @Override
        public void enterMarked(SimpleExmaralda.MarkedContext ctx) {
            List<Token> left = tokens
                    .getHiddenTokensToLeft(ctx.getStart().getTokenIndex());
            if (left != null && left.size() > 0 && currentPos > 1) {
                spd.addSpace();
            }
            String tx = ctx.MWORD().stream().map(ParseTree::getText)
                    .collect(Collectors.joining(" "));
            String mark = ctx.MARK_ID().getText();
            SpeechDocument.MarkedEvent m;
            // register if unknown; else, use as terminus ante quem for begin
            boolean startAnchor;
            if (markedEvents.containsKey(mark)) {
                if (currentPos == 1) {
                    startAnchor = false;
                    m = markedEvents.get(mark);
                    assert events.contains(m);
                    spd.changeBlockStart(currentBegin, m);
                    events.remove(currentBegin);
                    // System.err.println("Removed " +
                    // currentBegin.mkTimeRef());
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
                m = new SpeechDocument.MarkedEvent(mark);
                markedEvents.put(mark, m);
                events.push(m);
            }
            lastMarked = Optional.of(m);
            spd.addMarked(m, tx, startAnchor);
        }

        /**
         * found offset declaration
         */
        @Override
        public void enterOffset(SimpleExmaralda.OffsetContext ctx) {
            String tx = ctx.timeData().getText();
            Double time = Double.parseDouble(tx);
            spd.setOffset(time);
        }

        /**
         * found duration declaration
         */
        @Override
        public void enterDuration(SimpleExmaralda.DurationContext ctx) {
            String tx = ctx.timeData().getText();
            Double time = Double.parseDouble(tx);
            spd.setDuration(time);
        }

        /**
         * found language declaration
         */
        @Override
        public void enterLanguage(SimpleExmaralda.LanguageContext ctx) {
            String tx = ctx.lang_code().getText();
            spd.setLanguage(tx);
        }

        /**
         * incident encountered
         */
        @Override
        public void enterAction(SimpleExmaralda.ActionContext ctx) {
            String tx = ctx.aword().stream().map(RuleContext::getText)
                    .collect(Collectors.joining(" "));
            spd.addIncident(currentBegin, currentEnd, tx, true);
        }

        /**
         * incident encountered
         */
        @Override
        public void enterCaction(SimpleExmaralda.CactionContext ctx) {
            String tx = ctx.aword().stream().map(RuleContext::getText)
                    .collect(Collectors.joining(" "));
            spd.addIncident(currentBegin, currentEnd, tx, false);
        }

        /**
         * comment encountered
         */
        @Override
        public void enterComment(SimpleExmaralda.CommentContext ctx) {
            String tx = ctx.IWORD().stream().map(ParseTree::getText)
                    .collect(Collectors.joining(" "));
            spd.addComment(currentBegin, currentEnd, tx);
        }

        /**
         * word encountered; consign to white space management
         */
        @Override
        public void enterWord(SimpleExmaralda.WordContext ctx) {
            List<Token> left = tokens
                    .getHiddenTokensToLeft(ctx.getStart().getTokenIndex());
            boolean space = (left != null);
            spd.addText(ctx.getText(), space);
        }

        /**
         * prepare list of errors, delegate do {@link SpeechDocument}
         *
         * @param list
         *     the list of error messages
         */
        public void makeErrorList(List<String> list) {
            spd.makeErrorList(list);
        }
    }
}
