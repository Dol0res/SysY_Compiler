import org.antlr.v4.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Main
{
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("input path is required");
        }
        //String source = args[0];
        String source = "tests/test1.sysy";
        CharStream input = CharStreams.fromFileName(source);
        SysYLexer sysYLexer = new SysYLexer(input);
        sysYLexer.removeErrorListeners();
        ErrorListener myErrorListener = new ErrorListener();
        sysYLexer.addErrorListener(myErrorListener);
        CommonTokenStream tokens = new CommonTokenStream(sysYLexer);
        SysYParser sysYParser = new SysYParser(tokens);

        ParseTree tree = sysYParser.program();
        //Visitor extends SysYParserBaseVisitor<Void>
        Visitor visitor = new Visitor();
        visitor.visit(tree);


//        if (myErrorListener.hasErrorInformation) {
//            // 假设myErrorListener有一个错误信息输出函数printLexerErrorInformation.
//            myErrorListener.printLexerErrorInformation();
//        } else {
//            for (Token t : tokens) {
//                printSysYTokenInformation(t);
//            }
//        }

    }

    private static void printSysYTokenInformation(Token t) {
        //System.err.println(t);
        String tokenName = SysYLexer.VOCABULARY.getSymbolicName(t.getType());
        String text =t.getText();
        if(Objects.equals(tokenName, "INTEGER_CONST")){
            if(text.length()>1 && text.charAt(0)=='0'){
                if(text.charAt(1) == 'x' || text.charAt(1) == 'X'){
                    text = String.valueOf(Integer.parseInt(text.substring(2), 16));
                }else{
                    text = String.valueOf(Integer.parseInt(text.substring(1), 8));
                }
            }
        }
        System.err.println(tokenName + " " + text + " at Line " + t.getLine()+'.');
    }

}
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

    public void printLexerErrorInformation() {
        if(!hasErrorInformation)return;
        //int lineNo = this.
        for(int i =0;i<lineNo.size();i++) {
            System.err.println("Error type A at Line " + lineNo.get(i) + ": Wrong .");
        }
    }
}

class Visitor extends SysYParserBaseVisitor<Void>{
    @Override
    public Void visit(ParseTree tree) {
        return super.visit(tree);
    }

    @Override
    public Void visitExp(SysYParser.ExpContext ctx) {
        return super.visitExp(ctx);
    }

    @Override
    public Void visitTerminal(TerminalNode node) {
        return super.visitTerminal(node);
    }

    @Override
    public Void visitChildren(RuleNode node) {
        return super.visitChildren(node);
    }
}