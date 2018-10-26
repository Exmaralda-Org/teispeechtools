package de.ids.mannheim.clarin.teispeech.tools;

import java.io.IOException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.w3c.dom.Document;

public class TextRunner {
    public static void main(String[] args) {
        try {
//            DocUtilities.setupLanguage();
            CharStream input = CharStreams.fromStream(System.in);
            SimpleExmaraldaLexer lexer = new SimpleExmaraldaLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            SimpleExmaraldaParser parser = new SimpleExmaraldaParser(tokens);
            AntlrErrorLister lister = new AntlrErrorLister();
            parser.addErrorListener(lister);
            ParseTreeWalker walker = new ParseTreeWalker();
            ParseTree tree = parser.transcript();
            TextToTEI tt = new TextToTEI("deu", tokens);
            walker.walk(tt, tree);
            tt.makeErrorList(lister.getList());
            Document doc = tt.getDocument();
//            DOMImplementationLS domImplementation = (DOMImplementationLS) doc
//                    .getImplementation().getFeature("LS", "3.0");
//            LSSerializer lsSerializer = domImplementation.createLSSerializer();
////            lsSerializer.getDomConfig().setParameter("format-pretty-print",
////                    Boolean.TRUE);
//            LSOutput lsOutput = domImplementation.createLSOutput();
//            lsOutput.setEncoding("UTF-8");
//            Writer stringWriter = new StringWriter();
//            lsOutput.setCharacterStream(stringWriter);
//            lsSerializer.write(doc, lsOutput);
//            System.out.println(stringWriter.toString());
         // output DOM XML to console
            TransformerFactory stf = new net.sf.saxon.BasicTransformerFactory();
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(doc);
            StreamResult console = new StreamResult(System.out);
            transformer.transform(source, console);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TransformerConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (TransformerFactoryConfigurationError e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (TransformerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
