package de.ids.mannheim.clarin.teispeech.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.stream.Stream;

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
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(description = "process documents of annotated speech", name = "spindel", mixinStandardHelpOptions = true, version = "spindel 0.1")
public class CLI implements Runnable {

    // @Option(names = {"-v", "--verbose"}, description = "give more info")
    // private boolean verbose = false;
    //
    @Option(names = { "--indent" }, description = "indent")
    private boolean indent = false;
    // @Command() static void normalize

    @Option(names = "--expected", split = ",", description = "comma-separated list of expected languages besides main language; by default deu,en,tur (ONLY guess)")
    String[] expected = { "deu", "en", "tur" };

    @Option(names = {
            "--force" }, description = "force STEP even if it has been executed already (not for text2iso!)")
    private boolean force = false;
    // @Command() static void normalize

    enum Step {
        text2iso, segmentize, normalize, pos, guess
    };

    @Parameters(index = "0", paramLabel = "STEP", description = "Processing Step, one of: ${COMPLETION-CANDIDATES}")
    private Step step;

    @Option(names = { "-l",
            "--language" }, description = "the (default) language of the document an ISO-639 language code")
    private String language = "deu";

    @Option(names = { "-i",
            "--input" }, description = "file to read from, by default STDIN")
    private File inputFile;

    @Option(names = { "-o",
            "--output" }, description = "file to write to, by default STDOUT")
    private File outFile;

    @Spec
    private CommandSpec spec; // injected by picocli

    private OutputStream outStream = System.out;

    private InputStream inputStream = System.in;

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
        if (inputFile != null) {
            try {
                inputStream = new FileInputStream(inputFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        language = checkLanguage(language);
        expected = Stream.of(expected).map(this::checkLanguage)
                .toArray(String[]::new);
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
        case guess:
            guess();
            break;
        }
    }

    /**
     * check language parameters
     *
     * @param lang
     *            a String given as a language
     * @return
     */
    private String checkLanguage(String lang) {
        if (!DocUtilities.isLanguage(lang)) {
            throw new ParameterException(spec.commandLine(),
                    String.format("«%s» is not a valid language!", lang));
        } else {
            lang = DocUtilities.getLanguage(lang).get();
        }
        return lang;
    }

    public void text2iso() {
        // DocUtilities.setupLanguage();
        CharStream inputCS;
        try {
            inputCS = CharStreams.fromStream(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Document doc = TextToTEIConversion.process(inputCS, language);
        Utilities.outputXML(outStream, doc, indent);

    }

    /**
     * segment an ISO transcription
     */
    public void segmentize() {
        System.err.println("Sorry, Segmentation has not yet been implemented.");
    }

    /**
     * pos-tag an ISO transcription
     */
    public void pos() {
        try {
            Document doc = builder.parse(inputStream);
            TEIPOS teipo = new TEIPOS(doc, language);
            teipo.posTag(force);
            Utilities.outputXML(outStream, doc, indent);
        } catch (IOException | SAXException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * normalize an ISO transcription
     */
    public void normalize() {
        WordNormalizer wn = new DictionaryNormalizer(true);
        TEINormalizer tn = new TEINormalizer(wn, language);
        try {
            Document doc = builder.parse(inputStream);
            System.err.format("Have got %d <w> nodes.\n",
                    doc.getElementsByTagName("w").getLength());
            tn.normalize(doc, force);
            Utilities.outputXML(outStream, doc, indent);
        } catch (IOException | SAXException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * guess languages in an ISO transcription
     */
    public void guess() {
        try {
            Document doc = builder.parse(inputStream);
            LanguageDetect ld = new LanguageDetect(doc, language, expected);
            ld.detect(force);
            Utilities.outputXML(outStream, doc, indent);
        } catch (IOException | SAXException e) {
            throw new RuntimeException(e);
        }

    }
}
