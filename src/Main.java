package src;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.IOException;
import java.util.*;

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
//        sysYLexer.removeErrorListeners();
//        ErrorListener myErrorListener = new ErrorListener();
//        sysYLexer.addErrorListener(myErrorListener);
        CommonTokenStream tokens = new CommonTokenStream(sysYLexer);
        SysYParser sysYParser = new SysYParser(tokens);
        sysYParser.removeErrorListeners();
        //ErrorListener myErrorListener = new ErrorListener();
        //sysYParser.addErrorListener(myErrorListener);
        //SysYParser.ExpContext ctx = sysYParser.exp();

        ParseTree tree = sysYParser.program();
        //Visitor extends SysYParserBaseVisitor<Void>



//        if (myErrorListener.hasErrorInformation) {
//            // 假设myErrorListener有一个错误信息输出函数printLexerErrorInformation.
//            //myErrorListener.printLexerErrorInformation();
//            myErrorListener.printParserErrorInformation();
//        } else {
            MyVisitor visitor = new MyVisitor();
            visitor.visit(tree);
            //visitor.visitExp(ctx);
//            for (Token t : tokens) {
//                printSysYTokenInformation(t);
//            }
        //}

    }

//    private static void printSysYTokenInformation(Token t) {
//        //System.err.println(t);
//        String tokenName = SysYLexer.VOCABULARY.getSymbolicName(t.getType());
//        String text =t.getText();
//        if(Objects.equals(tokenName, "INTEGER_CONST")){
//            if(text.length()>1 && text.charAt(0)=='0'){
//                if(text.charAt(1) == 'x' || text.charAt(1) == 'X'){
//                    text = String.valueOf(Integer.parseInt(text.substring(2), 16));
//                }else{
//                    text = String.valueOf(Integer.parseInt(text.substring(1), 8));
//                }
//            }
//        }
//        System.err.println(tokenName + " " + text + " at Line " + t.getLine()+'.');
//    }

}

