
package de.ids.mannheim.clarin.teispeech.workflow;

import de.ids.mannheim.clarin.teispeech.data.AnchorSerialization;
import de.ids.mannheim.clarin.teispeech.data.DocUtilities;
import de.ids.mannheim.clarin.teispeech.data.NameSpaces;
import de.ids.mannheim.clarin.teispeech.tools.GenericConvention;
import de.ids.mannheim.clarin.teispeech.tools.GenericConventionBaseListener;
import de.ids.mannheim.clarin.teispeech.tools.GenericConventionLexer;
import de.ids.mannheim.clarin.teispeech.utilities.AntlrErrorLister;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.lang3.StringUtils;
import org.jooq.lambda.Seq;
import org.korpora.useful.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;

import java.util.Deque;
import java.util.List;
import java.util.stream.IntStream;

import static de.ids.mannheim.clarin.teispeech.data.NameSpaces.TEI_NS;

/**
 * converter from plain text to TEI ISO
 */
@SuppressWarnings("WeakerAccess")
public class GenericParsing {

    private final static Logger LOGGER = LoggerFactory
            .getLogger(GenericParser.class.getName());

    /**
     * insert list of parsing errors as comments
     *
     * @param errors
     *            list of errors
     */
    private static void makeErrorList(List<String> errors, Element el,
            String tx) {
        Document doc = el.getOwnerDocument();
        if (errors.size() > 0) {
            for (String error : errors) {
                LOGGER.error(error);
                Comment comment = doc.createComment("original input: " + tx);
                Utilities.insertAtBeginningOf(comment, el);
                comment = doc.createComment(" " + error + " ");
                Utilities.insertAtBeginningOf(comment, el);
            }
        }
    }

    /**
     * parse TEI {@code <u>} for generic transcription conventions
     *
     * @param el
     *            the utterance element
     *
     */
    private static void process(Element el) {
        if (StringUtils.strip(el.getTextContent()).isEmpty()) {
            return;
        }
        Deque<String> anchors = AnchorSerialization.serializeAnchors(el);
        if (Utilities.toStream(el.getChildNodes())
                .anyMatch(n -> n.getNodeType() == Node.ELEMENT_NODE
                        && !((Element) n).getTagName().equals("incident"))
                || (Utilities.toStream(el.getChildNodes())
                        .anyMatch(n -> n.getNodeType() == Node.ELEMENT_NODE
                                && ((Element) n).getTagName()
                                        .equals("incident"))
                        && !StringUtils.strip(el.getTextContent()).isEmpty())) {
            Comment comm = el.getOwnerDocument().createComment(
                    "This node was not parsed, as it contains mixed content.");
            el.insertBefore(comm, el.getFirstChild());
            return;
        }
        String tx = el.getTextContent();
        while (el.hasChildNodes()) {
            el.removeChild(el.getFirstChild());
        }
        AntlrErrorLister lister = new AntlrErrorLister(false);
        GenericConventionLexer lexer = new GenericConventionLexer(
                CharStreams.fromString(tx));
        lexer.addErrorListener(lister);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        GenericConvention parser = new GenericConvention(tokens);
        parser.addErrorListener(lister);
        ParseTreeWalker walker = new ParseTreeWalker();
        ParseTree tree = parser.text();
        GenericParser gp = new GenericParser(el, anchors);
        walker.walk(gp, tree);
        makeErrorList(lister.getList(), el, tx);
    }

    /**
     * parse all utterances of a TEI-encoded speech transcription for generic
     * conventions
     *
     * @param doc
     *            the TEI document
     */
    public static void process(Document doc) {
        Utilities
                .toElementStream(
                        doc.getElementsByTagNameNS(NameSpaces.TEI_NS, "u"))
                .forEach(GenericParsing::process);
        DocUtilities.makeChange(doc,
                "segmented according to generic transcription conventions");
    }

    /**
     * parse utterances according to generic conventions
     *
     * @author bfi
     */
    static class GenericParser extends GenericConventionBaseListener {

        private final Document doc;
        private final Element currentUtterance;
        private Element currentParent;

        private final Deque<String> anchors;

        /**
         * make a parser
         *
         * @param current
         *            the current utterance as a DOM XML element, already cleaned of
         *            anchors
         * @param anchors
         *            the anchors (if any) originally contained in {@code current}
         *            and to be restored while parsing.
         */
        public GenericParser(Element current, Deque<String> anchors) {
            this.doc = current.getOwnerDocument();
            currentUtterance = current;
            currentParent = current;
            this.anchors = anchors;
        }

        @Override
        public void enterMicropause(GenericConvention.MicropauseContext ctx) {
            Element pause = doc.createElementNS(TEI_NS, "pause");
            pause.setAttribute("type", "micro");
            currentParent.appendChild(pause);
        }

        @Override
        public void enterPause(GenericConvention.PauseContext ctx) {
            String length;
            int charLength = ctx.getText().length();
            switch (charLength) {
            case 1:
                length = "short";
                break;
            case 2:
                length = "medium";
                break;
            default:
                length = "";
                if (charLength > 3) {
                    length = Seq.of(IntStream.range(3, charLength))
                            .map(i -> "very ").toString();
                }
                length += "long";
                break;
            }
            Element pause = doc.createElementNS(TEI_NS, "pause");
            pause.setAttribute("type", length);
            currentParent.appendChild(pause);
        }

        @Override
        public void enterPunctuation(GenericConvention.PunctuationContext ctx) {
            Element pc = doc.createElementNS(TEI_NS, "pc");
            Text content = doc.createTextNode(ctx.getText());
            pc.appendChild(content);
            currentParent.appendChild(pc);
        }

        @Override
        public void enterWord(GenericConvention.WordContext ctx) {
            Element el = doc.createElementNS(TEI_NS, "w");
            AnchorSerialization.deserializeAnchor(el,
                    StringUtils.strip(ctx.getText()), anchors);
            currentParent.appendChild(el);
        }

        @Override
        public void enterIncomprehensible(GenericConvention.IncomprehensibleContext ctx) {
            Element gap = doc.createElementNS(TEI_NS, "w");
            gap.setAttribute("type", "incomprehensible");
            gap.appendChild(gap.getOwnerDocument().createTextNode(ctx.getText()));
            currentParent.appendChild(gap);
        }

        @Override
        public void enterUncertain(GenericConvention.UncertainContext ctx) {
            currentParent = doc.createElementNS(TEI_NS, "unclear");
        }

        @Override
        public void exitUncertain(GenericConvention.UncertainContext ctx) {
            currentUtterance.appendChild(currentParent);
            currentParent = currentUtterance;
        }
    }
}
