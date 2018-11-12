
package de.ids.mannheim.clarin.teispeech.tools;

import java.util.Deque;
import java.util.List;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.korpora.useful.Utilities;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import de.ids.mannheim.clarin.teispeech.data.AnchorSerialization;

/**
 * converter from plain text to TEI ISO
 */
public class GenericParsing {

    /**
     * insert list of parsing errors as comments
     *
     * @param errors
     *            list of errors
     */
    private static void makeErrorList(List<String> errors, Element el) {
        Document doc = el.getOwnerDocument();
        if (errors.size() > 0) {
            for (String error : errors) {
                System.err.println(error);
                Comment comment = doc.createComment(" " + error + " ");
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
    public static void process(Element el) {
        if (el.getTextContent().trim().isEmpty()) {
            return;
        }
        Deque<String> anchors = AnchorSerialization.serializeAnchors(el);
        if (Utilities.toStream(el.getChildNodes())
                .anyMatch(n -> n.getNodeType() == Node.ELEMENT_NODE)) {
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
        GenericConventionParser parser = new GenericConventionParser(tokens);
        parser.addErrorListener(lister);
        ParseTreeWalker walker = new ParseTreeWalker();
        ParseTree tree = parser.text();
        GenericParser gp = new GenericParser(el, anchors);
        walker.walk(gp, tree);
        makeErrorList(lister.getList(), el);
    }

    /**
     * parse all utterances of a TEI-encoded speech transcription for generic
     * conventions
     *
     * @param doc
     *            the TEI document
     */
    public static void process(Document doc) {
        Utilities.toElementStream(doc.getElementsByTagName("u"))
                .forEach(u -> process(u));
        DocUtilities.makeChange(doc,
                "segmented according to generic transcription conventions");
    }

}
