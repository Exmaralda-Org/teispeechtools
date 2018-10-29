package de.ids.mannheim.clarin.teispeech.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.korpora.useful.Utilities;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(description = "process documents of annotated speech", name = "spindel", mixinStandardHelpOptions = true, version = "spindel 0.1")
public class CLI implements Runnable {

    @Option(names = { "-v", "--verbose" }, description = "give more info")
    private boolean verbose = false;

    @Option(names = { "-i", "--indent" }, description = "indent")
    private boolean indent = false;
    // @Command() static void normalize

    enum Step {
        text2iso, segmentize, normalize, pos
    };

    @Parameters(index = "0", paramLabel = "STEP", description = "Processing Step, one of: ${COMPLETION-CANDIDATES}")
    private Step step;

    @Parameters(index = "1", paramLabel = "FILE", description = "File(s) to process.")
    private File inputFile;

    @Option(names = { "-l",
            "--language" }, description = "the (default) language of the document an ISO-639 language code")
    private String language = "deu";

    @Option(names = { "-o",
    "--output" }, description = "the (default) language of the document an ISO-639 language code")
    private File outFile;

    private OutputStream outStream = System.out;

    private static DocumentBuilderFactory factory = DocumentBuilderFactory
            .newInstance();
    private static DocumentBuilder builder;
    static {
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        CommandLine.run(new CLI(), args);
    }

    @Override
    public void run() {
        System.err.println(String.format("STEP is %s with %s and language %s",
                step, inputFile, language));
        if (outFile != null) {
            try {
                outStream = new FileOutputStream(outFile);
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                System.err.println(e.getMessage());
                System.err.println("--> continuing to print to STDOUT");
            }
        }
        switch (step) {
        case text2iso:
            text2iso();
            break;
        case normalize:
            normalize();
            break;
        case pos:
            pos();
            break;
        case segmentize:
            segmentize();
            break;
        }
    }

    public void segmentize() {
        System.err.println("Sorry, Segmentation has not yet been implemented.");
    }


    public void pos() {
        try {
            Document doc = builder.parse(inputFile);
            TEIPOS teipo = new TEIPOS(doc, language);
            teipo.posTag();
            Utilities.outputXML(outStream, doc, indent);
        } catch (IOException | SAXException e) {
            throw new RuntimeException(e);
        }

    }

    public void normalize() {
        WordNormalizer wn = new DictionaryNormalizer(true);
        TEINormalizer tn = new TEINormalizer(wn, language);
        try {
            Document doc = builder.parse(inputFile);
            System.err.format("Have got %d <w> nodes.\n",
                    doc.getElementsByTagName("w").getLength());
            tn.normalize(doc);
            Utilities.outputXML(outStream, doc, indent);
        } catch (IOException | SAXException e) {
            throw new RuntimeException(e);
        }

    }

    public void text2iso() {
        try (InputStream inputStream = new FileInputStream(inputFile)) {
            // DocUtilities.setupLanguage();
            CharStream input = CharStreams.fromStream(inputStream);
            Document doc = TextToTEIConversion.call(input, language);
            Utilities.outputXML(outStream, doc, indent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
