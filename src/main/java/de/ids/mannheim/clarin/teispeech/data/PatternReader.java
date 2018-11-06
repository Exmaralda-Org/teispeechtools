package de.ids.mannheim.clarin.teispeech.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathFactory;
import org.korpora.useful.Utilities;

/* Big change 04-12-2012
 * allow language codes in order to differentiate between alphabets
 */

/**
 *
 * @author Thomas Schmidt; Bernhard Fisseni
 */
public class PatternReader {

    private final Document document;
    private static XPathFactory xpf = XPathFactory.instance();

    /**
     * read Patterns from file
     *
     * @param input
     *            the input file
     * @throws JDOMException
     *             XML broken
     * @throws IOException
     *             file broken
     */
    public PatternReader(File input) throws JDOMException, IOException {
        document = Utilities.parseXMLviaJDOM(new FileInputStream(input));
    }

    /**
     * read Patterns from InputStream
     *
     * @param input
     *            the InputStream
     * @throws JDOMException
     *             XML broken
     * @throws IOException
     *             file broken
     */
    public PatternReader(InputStream input) throws JDOMException, IOException {
        document = Utilities.parseXMLviaJDOM(input);
    }

    /**
     * get all Patterns for a level
     *
     * @param level
     *            the level
     * @return the patterns as a Map: name → Pattern
     * @throws JDOMException
     *             XML broken
     */
    public Map<String, Pattern> getAllPatterns(int level) throws JDOMException {
        return getAllPatterns(level, "default");
    }

    // TODO: Wieso sind die Pattern alle so possessiv?
    // TODO: Ich habe die Muster-Namen auf die *terminologische* Variante
    // umgestellt.
    // TODO: Pattern werden gleich compiliert
    /**
     * get all Patterns for a level
     *
     * @param level
     *            the level
     * @param languageCode
     *            the language code
     * @return the patterns as a Map: name → Pattern
     * @throws JDOMException
     *             XML broken
     */
    public Map<String, Pattern> getAllPatterns(int level, String languageCode)
            throws JDOMException {
        Map<String, Pattern> result = new HashMap<>();
        String xp = "//level[@level='" + Integer.toString(level) + "']/pattern";
        for (Element e : xpf.compile(xp, Filters.element())
                .evaluate(document)) {
            String name = e.getAttributeValue("name");
            String pattern = resolveElement(e.getChild("regex"), languageCode);
            if (!("default".equals(languageCode))) {
                String xpath = "descendant::language[@name='" + languageCode
                        + "']/regex";
                Element regexChildOfThisLanguage = xpf
                        .compile(xpath, Filters.element()).evaluateFirst(e);
                if (regexChildOfThisLanguage != null) {
                    pattern = resolveElement(regexChildOfThisLanguage,
                            languageCode);
                }
            }
            result.put(name,
                    Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
        }
        return result;
    }

    /**
     * get the Pattern by name for a specific level
     *
     * @param level
     *            the level
     * @param name
     *            the Pattern name
     * @return the Pattern
     * @throws JDOMException
     *             XML broken
     */
    public Pattern getPattern(int level, String name) throws JDOMException {
        return getPattern(level, name, "default");
    }

    /**
     * get the Pattern by name for a specific level and language
     *
     * @param level
     *            the level
     * @param name
     *            the Pattern name
     * @param languageCode
     *            the language name
     * @return the Pattern
     * @throws JDOMException
     *             XML broken
     */
    public Pattern getPattern(int level, String name, String languageCode)
            throws JDOMException {
        String xp = "//level[@level='" + Integer.toString(level)
                + "']/pattern[@name='" + name + "']";
        System.out.println(xp);
        Element e = xpf.compile(xp, Filters.element()).evaluateFirst(document);
        Pattern pattern = Pattern.compile(
                resolveElement(e.getChild("regex"), languageCode),
                Pattern.CASE_INSENSITIVE);
        if (!("default".equals(languageCode))) {
            String xpath = "descendant::language[@name='" + languageCode
                    + "']/regex";
            System.out.println(xpath);
            Element regexChildOfThisLanguage = xpf
                    .compile(xpath, Filters.element()).evaluateFirst(e);
            System.out.println("Null?");
            if (regexChildOfThisLanguage != null) {
                System.out.println("Not null");
                pattern = Pattern.compile(
                        resolveElement(regexChildOfThisLanguage, languageCode),
                        Pattern.CASE_INSENSITIVE);
            }
        }
        return pattern;
    }

    /**
     * resolve links to other patterns within element content for the default
     * language
     *
     * @param e
     *            the element whose links to resolve
     * @return the resolved pattern
     * @throws JDOMException
     *             XML broken
     */
    public String resolveElement(Element e) throws JDOMException {
        return resolveElement(e, "default");
    }

    /**
     * resolve links to other patterns within element content
     *
     * @param e
     *            the element whose links to resolve
     * @param languageCode
     *            a language code
     * @return the resolved pattern
     * @throws JDOMException
     *             XML broken
     */
    public String resolveElement(Element e, String languageCode)
            throws JDOMException {
        String result = "";
        List<Content> l = e.getContent();
        for (Object o : l) {
            // System.out.println(o.toString());
            if (o instanceof Text) {
                result += ((Text) o).getText();
            } else {
                Element patternRef = ((Element) o);
                String refName = patternRef.getAttributeValue("ref");
                // System.out.println("---" + refName);
                String xp2 = "ancestor::level/descendant::pattern[@name='"
                        + refName + "']";
                // System.out.println("+++" + xp2);
                Element refEl = xpf.compile(xp2, Filters.element())
                        .evaluateFirst(patternRef);
                Element theRightRegexElement = refEl.getChild("regex");
                if (!("default".equals(languageCode))) {
                    String xpath = "descendant::language[@name='" + languageCode
                            + "']/regex";
                    Element regexChildOfThisLanguage = xpf
                            .compile(xpath, Filters.element())
                            .evaluateFirst(refEl);
                    if (regexChildOfThisLanguage != null) {
                        theRightRegexElement = regexChildOfThisLanguage;
                    }
                }
                result += resolveElement(theRightRegexElement, languageCode);
            }
        }
        return result;
    }

}
