/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.ids.mannheim.clarin.teispeech.tools;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathFactory;
import org.korpora.useful.Utilities;

import com.fasterxml.jackson.databind.ObjectMapper;

/* Big change 04-12-2012
 * allow language codes in order to differentiate between alphabets
 */

/**
 *
 * @author thomas
 */
public class PatternReader {

    Document document;

    public PatternReader(InputStream input) throws JDOMException, IOException {
        document = Utilities.parseXMLviaJDOM(input);
    }

    public Hashtable<String, String> getAllPatterns(int level)
            throws JDOMException {
        return getAllPatterns(level, "default");
    }

    public Hashtable<String, String> getAllPatterns(int level,
            String languageCode) throws JDOMException {
        Hashtable<String, String> result = new Hashtable<String, String>();
        String xp = "//level[@level='" + Integer.toString(level) + "']/pattern";
        for (Element e : XPathFactory.instance().compile(xp, Filters.element())
                .evaluate(document)) {
            String name = e.getAttributeValue("name");
            String pattern = resolveElement(e.getChild("regex"), languageCode);
            if (!("default".equals(languageCode))) {
                String xpath = "descendant::language[@name='" + languageCode
                        + "']/regex";
                Element regexChildOfThisLanguage = XPathFactory.instance()
                        .compile(xpath, Filters.element()).evaluateFirst(e);
                if (regexChildOfThisLanguage != null) {
                    pattern = resolveElement(regexChildOfThisLanguage,
                            languageCode);
                }
            }
            result.put(name, pattern);
        }
        return result;
    }

    public String getPattern(int level, String name) throws JDOMException {
        return getPattern(level, name, "default");
    }

    public String getPattern(int level, String name, String languageCode)
            throws JDOMException {
        String xp = "//level[@level='" + Integer.toString(level)
                + "']/pattern[@name='" + name + "']";
        System.out.println(xp);
        Element e = XPathFactory.instance().compile(xp, Filters.element())
                .evaluateFirst(document);
        String pattern = resolveElement(e.getChild("regex"), languageCode);
        if (!("default".equals(languageCode))) {
            String xpath = "descendant::language[@name='" + languageCode
                    + "']/regex";
            System.out.println(xpath);
            Element regexChildOfThisLanguage = XPathFactory.instance()
                    .compile(xpath, Filters.element()).evaluateFirst(e);
            System.out.println("Null?");
            if (regexChildOfThisLanguage != null) {
                System.out.println("Not null");
                pattern = resolveElement(regexChildOfThisLanguage,
                        languageCode);
            }
        }
        return pattern;
    }

    public String resolveElement(Element e) throws JDOMException {
        return resolveElement(e, "default");
    }

    public String resolveElement(Element e, String languageCode)
            throws JDOMException {
        String result = "";
        List<Content> l = e.getContent();
        for (Object o : l) {
            // System.out.println(o.toString());
            if (o instanceof org.jdom2.Text) {
                result += ((org.jdom2.Text) o).getText();
            } else {
                Element patternRef = ((org.jdom2.Element) o);
                String refName = patternRef.getAttributeValue("ref");
                // System.out.println("---" + refName);
                String xp2 = "ancestor::level/descendant::pattern[@name='"
                        + refName + "']";
                // System.out.println("+++" + xp2);
                Element refEl = XPathFactory.instance()
                        .compile(xp2, Filters.element())
                        .evaluateFirst(patternRef);
                Element theRightRegexElement = refEl.getChild("regex");
                if (!("default".equals(languageCode))) {
                    String xpath = "descendant::language[@name='" + languageCode
                            + "']/regex";
                    Element regexChildOfThisLanguage = XPathFactory.instance()
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

    public static void main(String[] args) {
        FileInputStream input;
        try {
            input = new FileInputStream(args[0]);
            PatternReader r = new PatternReader(input);
            String lang = args[1];
            Map<String, String> patterns = r.getAllPatterns(2, lang);
            ObjectMapper mapper = new ObjectMapper();
            System.out.println(mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(patterns));
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JDOMException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
