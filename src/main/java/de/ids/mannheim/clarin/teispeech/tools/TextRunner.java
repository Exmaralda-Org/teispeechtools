package de.ids.mannheim.clarin.teispeech.tools;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.w3c.dom.Document;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

public class TextRunner {
    public static void main(String[] args) {
        try {
            CharStream input = CharStreams.fromStream(System.in);
            SimpleExmaraldaLexer lexer = new SimpleExmaraldaLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            SimpleExmaraldaParser parser = new SimpleExmaraldaParser(tokens);
            AntlrErrorLister lister = new AntlrErrorLister();
            parser.addErrorListener(lister);
            ParseTreeWalker walker = new ParseTreeWalker();
            ParseTree tree = parser.transcript();
            TextToTEI tt = new TextToTEI();
            walker.walk(tt, tree);
            tt.makeErrorList(lister.getList());
            Document doc = tt.getDocument();
            DOMImplementationLS domImplementation = (DOMImplementationLS) doc.getImplementation().getFeature("LS", "3.0");;
            LSSerializer lsSerializer = domImplementation.createLSSerializer();
            lsSerializer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
            LSOutput lsOutput =  domImplementation.createLSOutput();
            lsOutput.setEncoding("UTF-8");
            Writer stringWriter = new StringWriter();
            lsOutput.setCharacterStream(stringWriter);
            lsSerializer.write(doc, lsOutput);
            System.out.println(stringWriter.toString());

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
