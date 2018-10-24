package de.ids.mannheim.clarin.teispeech.tools;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;

public class AntlrErrorLister extends BaseErrorListener {
    private List<String> errorList = new ArrayList<>();

    @Override
    public void syntaxError(org.antlr.v4.runtime.Recognizer<?, ?> recognizer,
            Object offendingSymbol, int line, int charPositionInLine,
            String msg, RecognitionException e) {
        errorList.add(String.format("line %d, position %d: %s", line,
                charPositionInLine, msg));
    }

    public List<String> getList() {
        return errorList;
    }

}
