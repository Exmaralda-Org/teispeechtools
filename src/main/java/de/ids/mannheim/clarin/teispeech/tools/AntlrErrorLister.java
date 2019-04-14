package de.ids.mannheim.clarin.teispeech.tools;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;

/**
 * accumulate error messages from ANTLR 4 in a List
 *
 * @author bfi
 *
 */
class AntlrErrorLister extends BaseErrorListener {

    /**
     * list of error messages
     */
    private final List<String> errorList = new ArrayList<>();

    private final boolean includeLineNo;

    /**
     * make an AntlrErrorLister
     *
     * @param includeLineNo
     *            whether to include the line node in the error list
     */
    public AntlrErrorLister(boolean includeLineNo) {
        super();
        this.includeLineNo = includeLineNo;
    }

    public AntlrErrorLister() {
        this(true);
    }

    /**
     * catch errors and put a message in the errorList
     */
    @Override
    public void syntaxError(org.antlr.v4.runtime.Recognizer<?, ?> recognizer,
            Object offendingSymbol, int line, int charPositionInLine,
            String msg, RecognitionException e) {
        String tx;
        if (includeLineNo) {
            tx = String.format("line %d, position %d: ", line,
                    charPositionInLine);
        } else {
            tx = String.format("character %d: ", charPositionInLine);
        }
        errorList.add(tx + msg);
    }

    /**
     * @return a list of error messages
     */
    public List<String> getList() {
        return errorList;
    }

}
