package de.ids.mannheim.clarin.teispeech.tools;

import java.util.Deque;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.jooq.lambda.Seq;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import de.ids.mannheim.clarin.teispeech.data.AnchorSerialization;
import de.ids.mannheim.clarin.teispeech.tools.GenericConventionParser.IncomprehensibleContext;
import de.ids.mannheim.clarin.teispeech.tools.GenericConventionParser.PauseContext;
import de.ids.mannheim.clarin.teispeech.tools.GenericConventionParser.PunctuationContext;
import de.ids.mannheim.clarin.teispeech.tools.GenericConventionParser.UncertainContext;
import de.ids.mannheim.clarin.teispeech.tools.GenericConventionParser.WordContext;

/**
 * parse utterances according to generic conventions
 *
 * @author bfi
 */
public class GenericParser extends GenericConventionBaseListener {

    private final Document doc;
    private final Element currentUtterance;
    private Element currentParent;

    private Deque<String> anchors;

    /**
     * make a parser
     *
     * @param current
     *            the current utterance as a DOM XML element, already cleaned of
     *            anchors
     * @param anchors
     *            the anchors (if any) originally contained in {@code current}
     *            and to be restored while parsing.
     */
    public GenericParser(Element current, Deque<String> anchors) {
        this.doc = current.getOwnerDocument();
        currentUtterance = current;
        currentParent = current;
        this.anchors = anchors;
    }

    @Override
    public void enterPause(PauseContext ctx) {
        String length;
        int charLength = ctx.getText().length();
        switch (charLength) {
        case 1:
            length = "short";
            break;
        case 2:
            length = "medium";
            break;
        default:
            length = "";
            if (charLength > 3) {
                length = Seq.of(IntStream.range(3, charLength))
                        .map(i -> "very ").toString();
            }
            length += "long";
            break;
        }
        Element pause = doc.createElement("pause");
        pause.setAttribute("type", length);
        currentParent.appendChild(pause);
    }

    @Override
    public void enterPunctuation(PunctuationContext ctx) {
        Element pc = doc.createElement("pc");
        Text content = doc.createTextNode(ctx.getText());
        pc.appendChild(content);
        currentParent.appendChild(pc);
    }

    @Override
    public void enterWord(WordContext ctx) {
        Element el = doc.createElement("w");
        AnchorSerialization.deserializeAnchor(el,
                StringUtils.strip(ctx.getText()), anchors);
        currentParent.appendChild(el);
    }

    @Override
    public void enterIncomprehensible(IncomprehensibleContext ctx) {
        Element gap = doc.createElement("w");
        gap.setAttribute("type", "incomprehensible");
        gap.setAttribute("dur",
                String.format("%d syl", ctx.getText().length() / 3));
        currentParent.appendChild(gap);
    }

    @Override
    public void enterUncertain(UncertainContext ctx) {
        currentParent = doc.createElement("unclear");
    }

    @Override
    public void exitUncertain(UncertainContext ctx) {
        currentUtterance.appendChild(currentParent);
        currentParent = currentUtterance;
    }
}
