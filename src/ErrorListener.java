import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.ArrayList;
import java.util.List;

class ErrorListener extends BaseErrorListener {

    public boolean hasErrorInformation;
    List<Integer> lineNo = new ArrayList<>();
    List<String> errmsg = new ArrayList<>();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        //if(hasErrorInformation)return;
        lineNo.add(line);
        //errmsg.add(msg);
        //System.err.println(msg);
        hasErrorInformation = true;
        super.syntaxError(recognizer, offendingSymbol, line, charPositionInLine, msg, e);
    }

    public void printParserErrorInformation() {
        if(!hasErrorInformation)return;
        //int lineNo = this.
        for(int i =0;i<lineNo.size();i++) {
            System.err.println("Error type B at Line " + lineNo.get(i) + ": Wrong .");
        }
    }
}