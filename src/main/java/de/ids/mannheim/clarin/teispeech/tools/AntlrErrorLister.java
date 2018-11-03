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
public class AntlrErrorLister extends BaseErrorListener {

    /**
     * list of error messages
     */
    private List<String> errorList = new ArrayList<>();

    /**
     * catch errors and put a message in {@link #errorList}
     */
    @Override
    public void syntaxError(org.antlr.v4.runtime.Recognizer<?, ?> recognizer,
            Object offendingSymbol, int line, int charPositionInLine,
            String msg, RecognitionException e) {
        errorList.add(String.format("line %d, position %d: %s", line,
                charPositionInLine, msg));
    }

    /**
     * @return {@link #errorList}
     */
    public List<String> getList() {
        return errorList;
    }

}
