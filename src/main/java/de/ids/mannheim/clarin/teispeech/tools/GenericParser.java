package de.ids.mannheim.clarin.teispeech.tools;

import java.util.Deque;
import java.util.stream.IntStream;

import org.jooq.lambda.Seq;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import de.ids.mannheim.clarin.teispeech.data.AnchorSerialization;
import de.ids.mannheim.clarin.teispeech.tools.GenericConventionParser.AssimilatedContext;
import de.ids.mannheim.clarin.teispeech.tools.GenericConventionParser.IncomprehensibleContext;
import de.ids.mannheim.clarin.teispeech.tools.GenericConventionParser.PauseContext;
import de.ids.mannheim.clarin.teispeech.tools.GenericConventionParser.PunctuationContext;
import de.ids.mannheim.clarin.teispeech.tools.GenericConventionParser.UncertainContext;
import de.ids.mannheim.clarin.teispeech.tools.GenericConventionParser.WordContext;

public class GenericParser extends GenericConventionBaseListener {

    private final Document doc;
    private final Element currentUtterance;
    private Element currentParent;

    private Deque<String> anchors;

    public GenericParser(Element current, Deque<String> anchors) {
        this.doc = current.getOwnerDocument();
        currentUtterance = current;
        currentParent = current;
        this.anchors = anchors;
    }

    @Override
    public void enterPause(PauseContext ctx) {
        String length = "";
        int charLength = ctx.getText().length();
        switch (charLength) {
        case 1:
            length = "short";
            break;
        case 2:
            length = "medium";
            break;
//        case 3:
//            length = "long";
//            break;
        default:
            length = Seq.of(IntStream.range(3, charLength)).map(i -> "very ")
                    .toString();
            length += "long";
            break;
        }
        Element pause = doc.createElement("pause");
        pause.setAttribute("type", length);
        currentParent.appendChild(pause);
    }

    @Override
    public void enterPunctuation(PunctuationContext ctx) {
        System.err.println("PUNCT! " + ctx.getText());
        Element pc = doc.createElement("pc");
        Text content = doc.createTextNode(ctx.getText());
        pc.appendChild(content);
        currentParent.appendChild(pc);
    }

    @Override
    public void enterWord(WordContext ctx) {
        Element el = doc.createElement("w");
        if (ctx.getParent().getRuleContext()
                .getRuleIndex() == new AssimilatedContext(null, 0)
                        .getRuleIndex()) {
            el.setAttribute("type", "assimilated");
        }
        AnchorSerialization.deserializeAnchor(el, ctx.getText().trim(), anchors);
        currentParent.appendChild(el);
    }

    @Override
    public void enterIncomprehensible(IncomprehensibleContext ctx) {
        Element gap = doc.createElement("gap");
        gap.setAttribute("reason", "incomprehensible");
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
