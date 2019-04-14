package de.ids.mannheim.clarin.teispeech.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jdom2.JDOMException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.ids.mannheim.clarin.teispeech.data.PatternReader;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

/**
 * CLI to read the pattern file and show the expanded Patterns
 *
 * @author bfi
 *
 */
@Command(description = "read patterns from file", name = "patternreader", mixinStandardHelpOptions = true, versionProvider = de.ids.mannheim.clarin.teispeech.tools.VersionProvider.class)
class PatternReaderRunner implements Runnable {
    /**
     * @param args
     *            the command line arguments ;-).
     */
    public static void main(String[] args) {
        CommandLine.run(new PatternReaderRunner(), args);
    }

    private static final List<Integer> potentialLevels = Arrays
            .asList(2, 3);

    @Option(names = { "-i",
            "--input" }, description = "file to read from, by default STDIN")
    private File inputFile;
    @Option(names = { "-l",
            "--language" }, description = "the language (default: universal)")
    private final String language = "universal";
    @Option(names = { "-L",
            "--level" }, description = "the parsing level (default: 2)")
    private final int level = 2;

    @Spec
    private CommandSpec spec; // injected by picocli

    @Override
    public void run() {
        InputStream inputStream = System.in;
        if (!potentialLevels.contains(level)) {
            throw new ParameterException(spec.commandLine(),
                    "Level must be either 2 or 3");
        }
        try {
            if (inputFile != null) {
                inputStream = new FileInputStream(inputFile);
            }

            PatternReader r = new PatternReader(inputStream);
            Map<String, Pattern> patterns = r.getAllPatterns(level, language);
            ObjectMapper mapper = new ObjectMapper();
            System.out.println(mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(patterns));
        } catch (JDOMException | IOException e) {
            e.printStackTrace();
        }
    }

}
