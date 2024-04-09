import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Visitor extends SysYParserBaseVisitor<Void> {
    ArrayList<StringBuilder> list = new ArrayList<>();
    StringBuilder output = new StringBuilder();
    StringBuilder outputWithoutColor = new StringBuilder();
    static boolean underline = false;
    static boolean hasSpace = false;
    static boolean changeLine = false;
    static boolean stat = false;

    static int tab = 0;
    String[] spaceBehind = new String[]{"const","int","void","if","else","while","return"};
    String[] spaceAround = new String[]{"*","/","%","=","==","!=","<",">","<=",">=","&&","||"};
    List<String> spaceBehindList = Arrays.asList(spaceBehind);
    List<String> spaceAroundList = Arrays.asList(spaceAround);

    public String getRuleName(RuleNode node) {
        RuleContext ruleContext = node.getRuleContext();
        int ruleIndex = ruleContext.getRuleIndex();
        return SysYParser.ruleNames[ruleIndex];
    }

    @Override
    public Void visit(ParseTree tree) {

        super.visit(tree);
        System.out.print(output.toString());
        //System.out.println(outputWithoutColor.toString());
        return null;
    }

    @Override
    public Void visitExp(SysYParser.ExpContext ctx) {
        return super.visitExp(ctx);
    }

    @Override
    public Void visitTerminal(TerminalNode node) {
        if(node.getSymbol().getType()!=-1) {
            if(hasSpace)output.append(" ");
            if(hasSpace)outputWithoutColor.append(" ");

            String code = node.getSymbol().getText();
            String ruleNameP ="";
            int childNum=0;
            if(node.getParent()!=null){
                ruleNameP = getRuleName((RuleNode) node.getParent());
                childNum=node.getParent().getChildCount();
            }
            if(changeLine){
                output.append("\n");
                outputWithoutColor.append("\n");
                if(ruleNameP.equals("block") && code.equals("}")) {
                    tab--;
                }
                output.append("    ".repeat(Math.max(0, tab)));
                outputWithoutColor.append("    ".repeat(Math.max(0, tab)));
                changeLine=false;
            }
            if(ruleNameP.equals("block")){
                if(code.equals("{")) {
                    changeLine = true;
                    tab++;
                }
            }

            if(spaceAroundList.contains(code)) {
                if (!hasSpace){
                    output.append(" ");
                    outputWithoutColor.append(" ");
                }
                hasSpace = true;
            } else if (spaceBehindList.contains(code)) {
                hasSpace = true;
                if(code.equals("return") && node.getParent().getChild(1).getText().equals(";"))hasSpace=false;
            } else if ((code.equals("-") || code.equals("+")) && !getRuleName((RuleNode) node.getParent()).equals("unaryOp")) {
                if (!hasSpace){
                    output.append(" ");
                }
                hasSpace = true;
            } else if (code.equals(",")) {
                hasSpace=true;
            } else{
                hasSpace = false;
            }

            if(underline) {
                output.append("\u001B[4m");
                output.append("\u001B[95m");
            } else if (stat) {
                output.append("\u001B[97m");
            }

            //core
            output.append(CharacterHighlighter.getTerminalColor(node));
            super.visitTerminal(node);


            output.append(node.getSymbol().getText());
            outputWithoutColor.append(node.getSymbol().getText());
            output.append("\u001B[0m");



        }
        return null;
    }

    @Override
    public Void visitFuncDef(SysYParser.FuncDefContext ctx) {
        super.visitFuncDef(ctx);
        if(ctx.stop.getType()!=-1)output.append("\n");
        return null;
    }

    //    @Override
//    public Void visitFuncDef(SysYParser.FuncDefContext ctx) {
//        super.visitFuncDef(ctx);
//        if(ctx.stop.getType()!=-1)output.append("\n");
//        return null;
//    }


    @Override
    public Void visitChildren(RuleNode node) {
        if(changeLine && outputWithoutColor.charAt(outputWithoutColor.length()-1)!=' '){
            output.append("\n");
            outputWithoutColor.append("\n");
            output.append("    ".repeat(Math.max(0, tab)));
            outputWithoutColor.append("    ".repeat(Math.max(0, tab)));
            changeLine=false;
        }

        String ruleName = getRuleName(node);
        String ruleNameP ="";
        int childNum=0;
        if(node.getParent()!=null){
            ruleNameP = getRuleName((RuleNode) node.getParent());
            childNum=node.getParent().getChildCount();
        }

        if(ruleName.equals("funcDef") && node.getParent().getChild(0)!=node){
            output.append("\n");
        }

        if(ruleName.equals("block")) {
            //tab++;
//            if(ruleNameP.equals("funcDef") || ruleNameP.equals("loop")) {
            if(ruleNameP.equals("funcDef")) {
                output.append(" ");
                outputWithoutColor.append(" ");
            }
            hasSpace=false;
        }
        if(ruleName.equals("stat") &&ruleNameP.equals("stat")) {
            String head =node.getParent().getChild(0).getText();
            if(head.equals("if")|| head.equals("while")){
                if(node.getChild(0).getText().charAt(0)=='{'){
                    output.append(" ");
                } else if (node.getChild(0).getText().equals("if")) {
                    //output.append(" ");
                } else {
                    output.append("\n");
                    output.append("    ".repeat(Math.max(0, tab+1)));
                }
            }

        }

        //core
        //output.append(CharacterHighlighter.getColor(node));
        CharacterHighlighter.getColor(node);
        super.visitChildren(node);

        if(ruleName.equals("decl")) {
            underline = false;
            changeLine=true;

        }

        output.append("\u001B[0m");
//        if(outputWithoutColor.charAt(outputWithoutColor.length()-1)!='\n') {
            if (ruleName.equals("stat")) {
                changeLine=true;
                stat=false;
            }
//            if (ruleName.equals("block")) {
//                changeLine=true;
//            }
//        }

        return null;
    }

}