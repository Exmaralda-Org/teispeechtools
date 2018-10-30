package de.ids.mannheim.clarin.teispeech.tools;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.w3c.dom.Document;

public class TextToTEIConversion {

    /**
     * convert a plain text document to TEI ISO
     *
     * @param input
     *            the input
     * @param language
     *            the language of the document
     * @return the document
     */
    public static Document process(CharStream input, String language) {
        SimpleExmaraldaLexer lexer = new SimpleExmaraldaLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SimpleExmaraldaParser parser = new SimpleExmaraldaParser(tokens);
        AntlrErrorLister lister = new AntlrErrorLister();
        parser.addErrorListener(lister);
        ParseTreeWalker walker = new ParseTreeWalker();
        ParseTree tree = parser.transcript();
        TextToTEI tt = new TextToTEI(tokens, language);
        walker.walk(tt, tree);
        tt.makeErrorList(lister.getList());
        return tt.getDocument();
    }

}