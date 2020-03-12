package de.ids.mannheim.clarin.teispeech.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.jdom2.JDOMException;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.korpora.useful.LangUtilities;
import org.korpora.useful.XMLUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import de.ids.mannheim.clarin.teispeech.data.DocUtilities;
import de.ids.mannheim.clarin.teispeech.data.GATParser;
import de.ids.mannheim.clarin.teispeech.data.LanguageDetect;
import de.ids.mannheim.clarin.teispeech.data.NameSpaces;
import de.ids.mannheim.clarin.teispeech.utilities.VersionProvider;
import de.ids.mannheim.clarin.teispeech.workflow.DictionaryNormalizer;
import de.ids.mannheim.clarin.teispeech.workflow.DocumentIdentifier;
import de.ids.mannheim.clarin.teispeech.workflow.GenericParsing;
import de.ids.mannheim.clarin.teispeech.workflow.PseudoAlign;
import de.ids.mannheim.clarin.teispeech.workflow.TEINormalizer;
import de.ids.mannheim.clarin.teispeech.workflow.TEIPOS;
import de.ids.mannheim.clarin.teispeech.workflow.TextToTEIConversion;
import de.ids.mannheim.clarin.teispeech.workflow.WordNormalizer;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * a command line interface for the annotation processing work flow
 *
 * @author bfi
 *
 */
@SuppressWarnings({ "CanBeFinal", "unused" })
@Command(description = "process documents of speech annotated "
        + "according to TEI/ISO", sortOptions = false, name = "spindel"
                + "", mixinStandardHelpOptions = true, versionProvider = VersionProvider.class)
public class CLI implements Callable<Integer> {

    private final static Logger LOGGER = LoggerFactory
            .getLogger(CLI.class.getName());
    @Option(names = { "-I", "--indent" }, description = "indent")
    private boolean indent = false;

    @Option(names = { "-f", "--force" }, description = "force STEP even if "
            + "the corresponding annotation exists already "
            + "(NOT for text2iso, segmentize)")
    private boolean force = false;
    // @Command() static void normalize

    private enum Step {
        text2iso, segmentize, guess, normalize, pos, align, identify, unidentify
    }

    @Parameters(index = "0", paramLabel = "STEP", description = "Processing "
            + "Step, one of: ${COMPLETION-CANDIDATES}")
    private Step step;

    @Option(names = { "-i",
            "--input" }, description = "input file, by default STDIN")
    private File inputFile;

    @Option(names = { "-o",
            "--output" }, description = "output file, by default STDOUT")
    private File outFile;

    @Option(names = { "-l", "--lang",
            "--language" }, description = "the (default) language "
                    + "of the document, an ISO-639 language code "
                    + "(default: '${DEFAULT-VALUE}'; normalize, pos)")
    private String language = "de";

    @Option(names = { "-E", "--expected" }, description = "comma-separated "
            + "list of expected languages besides the main language; "
            + "by default '${DEFAULT-VALUE}' "
            + "(ONLY guess)", defaultValue = "de,en,tr", split = ",")
    private String[] expected;

    @SuppressWarnings("FieldCanBeLocal")
    @Option(names = { "-k",
            "--keep-case" }, description = "do not convert to lower case "
                    + "when normalizing; effectively, skip capitalized words")
    private boolean keepCase = false;

    @Option(names = { "-L",
            "--level" }, description = "the level of the transcription "
                    + "(segmentize, default: '${DEFAULT-VALUE}')")
    private ProcessingLevel level = ProcessingLevel.generic;

    @SuppressWarnings("FieldCanBeLocal")
    @Option(names = { "-m", "--minimal",
            "--minimal-length" }, description = "the `minimal count` of words so "
                    + "that language detection is even "
                    + "tried (default: ${DEFAULT-VALUE}, "
                    + "which is already pretty low)")
    private int minimalLength = 5;

    @Option(names = { "-p", "--use",
            "--use-graphs" }, description = "use graphs instead of (pseudo)"
                    + "phones "
                    + "for pseudoalignment (default: ${DEFAULT-VALUE})")
    private boolean useGraphs;
    @SuppressWarnings("FieldCanBeLocal")
    @Option(names = { "-t",
            "--transcribe" }, description = "add phonetic canonical "
                    + "transcription "
                    + "in pseudoalignment (default: ${DEFAULT-VALUE}, only used "
                    + "if not --use-graphs)")
    private boolean transcribe = false;

    @SuppressWarnings("FieldCanBeLocal")
    @Option(names = { "-T", "--time" }, description = "audio length in seconds"
            + "(alignment, taken from document if -1, default: ${DEFAULT-VALUE})")
    private double timeLength = -1d;

    @SuppressWarnings("FieldCanBeLocal")
    @Option(names = { "-O",
            "--offset" }, description = "audio offset in seconds"
                    + "(alignment, taken from document if -1, default: ${DEFAULT-VALUE})")
    private double offset = 0d;

    @SuppressWarnings("FieldCanBeLocal")
    @Option(names = { "-e", "--every" }, description = "insert time anchor "
            + "every n items" + "(alignment, default: ${DEFAULT-VALUE})")
    private int every = 20;

    @Spec
    private CommandSpec spec; // injected by picocli

    private OutputStream outStream = System.out;

    private InputStream inputStream = System.in;

    private static final DocumentBuilderFactory factory = DocumentBuilderFactory
            .newInstance();
    private static final DocumentBuilder builder;

    static {
        try {
            factory.setNamespaceAware(true);
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * run CLI
     *
     * @param args
     */
    public static void main(String[] args) {
        System.exit(new CommandLine(new CLI()).execute(args));
    }

    @Override
    public Integer call() {
        if (outFile != null) {
            try {
                outStream = new FileOutputStream(outFile);
            } catch (FileNotFoundException e) {
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
        case segmentize:
            segmentize();
            break;
        case normalize:
            normalize();
            break;
        case pos:
            pos();
            break;
        case guess:
            guess();
            break;
        case identify:
            identify();
            break;
        case unidentify:
            unidentify();
            break;
        case align:
            pseudoAlign();
            break;
        }
        return 0;
    }

    /**
     * check language parameters
     *
     * @param lang
     *     a String given as a language
     * @return normalized language
     */
    private String checkLanguage(String lang) {
        // String origLang = lang;
        if (!LangUtilities.isLanguage(lang)) {
            throw new ParameterException(spec.commandLine(),
                    String.format("«%s» is not a valid language!", lang));
        } else {
            // noinspection OptionalGetWithoutIsPresent
            lang = LangUtilities.getLanguage(lang).get();
        }
        // LOGGER.info("In: {}, Out: {}", origLang, lang);
        return lang;
    }

    /**
     * convert to ISO
     */
    private void text2iso() {
        CharStream inputCS;
        try {
            inputCS = CharStreams.fromStream(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Document doc = TextToTEIConversion.process(inputCS, language);
        XMLUtilities.outputXML(outStream, doc, indent);

    }

    /**
     * pos-tag an ISO transcription
     */
    private void pos() {
        try {
            Document doc = builder.parse(inputStream);
            TEIPOS teipo = new TEIPOS(doc, language);
            teipo.posTag(force);
            XMLUtilities.outputXML(outStream, doc, indent);
        } catch (IOException | SAXException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * normalize an ISO transcription
     */
    private void normalize() {
        WordNormalizer wn = new DictionaryNormalizer(keepCase, true);
        TEINormalizer tn = new TEINormalizer(wn, language);
        try {
            Document doc = builder.parse(inputStream);
            System.err.format("Have got %d <w> nodes.\n",
                    doc.getElementsByTagNameNS(NameSpaces.TEI_NS, "w")
                            .getLength());
            tn.normalize(doc, force);
            XMLUtilities.outputXML(outStream, doc, indent);
        } catch (IOException | SAXException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * guess languages in an ISO transcription
     */
    private void guess() {
        try {
            Document doc = builder.parse(inputStream);
            LanguageDetect ld = new LanguageDetect(doc, language, expected,
                    minimalLength);
            ld.detect(force);
            XMLUtilities.outputXML(outStream, doc, indent);
        } catch (IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * segment an ISO transcription
     */
    private void segmentize() {
        if (level == ProcessingLevel.generic) {
            try {
                Document doc = builder.parse(inputStream);
                GenericParsing.process(doc);
                XMLUtilities.outputXML(outStream, doc, indent);
            } catch (SAXException | IOException e) {
                throw new RuntimeException(e);
            }

        } else {
            try {
                org.jdom2.Document doc = XMLUtilities
                        .parseXMLviaJDOM(inputStream);
                GATParser parser = new GATParser();
                parser.parseDocument(doc, level.ordinal() + 1);
                DocUtilities.makeChange(doc, String.format(
                        "utterances parsed to %s conventions", level.name()));

                XMLOutputter outputter = new XMLOutputter();
                if (indent) {
                    Format outFormat = Format.getPrettyFormat();
                    outputter.setFormat(outFormat);
                }
                outputter.output(doc, outStream);
            } catch (IOException | JDOMException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * add XML IDs
     */
    private void pseudoAlign() {
        try {
            boolean usePhones = !useGraphs;
            Document doc = builder.parse(inputStream);
            PseudoAlign aligner = new PseudoAlign(doc, language, usePhones,
                    transcribe, force, timeLength, offset, every);
            aligner.calculateUtterances();
            XMLUtilities.outputXML(outStream, aligner.getDoc(), indent);
        } catch (IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * add XML IDs
     */
    private void identify() {
        try {
            Document doc = builder.parse(inputStream);
            DocumentIdentifier.makeIDs(doc);
            XMLUtilities.outputXML(outStream, doc, indent);
        } catch (IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * add XML IDs
     */
    private void unidentify() {
        try {
            Document doc = builder.parse(inputStream);
            DocumentIdentifier.removeIDs(doc);
            XMLUtilities.outputXML(outStream, doc, indent);
        } catch (IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

}
