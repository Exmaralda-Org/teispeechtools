package de.ids.mannheim.clarin.teispeech.data;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.korpora.useful.Utilities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * This contains a slightly dirty trick to treat anchors in words
 *
 * @author bfi
 *
 */
public class AnchorSerialization {
    /**
     * start of the escape string – composed of Tibetan, Georgian and Coptic
     * characters. Hopefully no-one will ever use this to transcribe speech.
     */
    private static final String ANCHOR_START = "ཏჾ፼ཏჾ፼";

    private static final Pattern anchor_pat = Pattern
            .compile(AnchorSerialization.ANCHOR_START);
    // .compile(DirtyTricks.ANCHOR_START + ".*?" + DirtyTricks.ANCHOR_END);

    /**
     * replace escaped anchors in text with {@code <anchor> elements}
     *
     * @param parent
     *            the element that will receive the text and anchors
     * @param from
     *            text with serialized anchors
     * @param anchors
     *            list of anchor {@code @synch}s
     */
    public static void deserializeAnchor(Element parent, String from,
            Deque<String> anchors) {
        List<Node> ret = new ArrayList<>();
        Matcher mat = anchor_pat.matcher(from);
        int lastEnd = 0;
        while (mat.find(lastEnd)) {
            if (mat.start() > 0) {
                ret.add(parent.getOwnerDocument()
                        .createTextNode(from.substring(lastEnd, mat.start())));
            }
            lastEnd = mat.end();
            Element anchor = parent.getOwnerDocument().createElementNS(NameSpaces.TEI_NS, "anchor");
            String anchorText = anchors.removeFirst();
            if (!anchorText.isEmpty()) {
                anchor.setAttributeNS(NameSpaces.TEI_NS, "synch", anchorText);
            }
            ret.add(anchor);
        }
        String remaining = from.substring(lastEnd);
        if (!remaining.isEmpty()) {
            ret.add(parent.getOwnerDocument().createTextNode(remaining));
        }
        for (Node n : ret) {
            parent.appendChild(n);
        }
    }

    /**
     * replace anchors with escapes in text with {@code <anchor> elements}
     *
     * @param el
     *            the element contains text and anchors
     * @return list of anchor {@code @synch} values
     */
    public static Deque<String> serializeAnchors(Element el) {
        NodeList anchors = el.getElementsByTagNameNS(NameSpaces.TEI_NS, "anchor");
        Deque<String> anchorQ = new ArrayDeque<>();
        for (int i = anchors.getLength() - 1; i >= 0; i--) {
            Element a = (Element) anchors.item(i);
            anchorQ.push(a.getAttribute("synch"));
            Text tx = el.getOwnerDocument().createTextNode(ANCHOR_START);
            a.getParentNode().replaceChild(tx, a);
        }
        return anchorQ;
    }

    /**
     * replace anchors with escapes in doc with {@code <anchor> elements}
     *
     * @param doc
     *            the document that contains {@code u} and anchors
     */
    public static void serializeAnchors(Document doc) {
        Utilities.toElementStream(doc.getElementsByTagNameNS(NameSpaces.TEI_NS, "u"))
                .forEach(AnchorSerialization::serializeAnchors);
    }
}
