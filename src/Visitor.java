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
//        int l=outputWithoutColor.length();
//        while(l>2&&outputWithoutColor.charAt(l-1)!='\n' && outputWithoutColor.charAt(l-2)!='\n' ) {
//            System.out.print(output.substring(0, output.length()-1));
//        }
//        //System.out.println(RainbowBrackets.colorizeBrackets(output.toString()));
//        else{
            System.out.print(output.toString());
//        }
        System.out.println(outputWithoutColor.toString());
        return null;
    }

    @Override
    public Void visitExp(SysYParser.ExpContext ctx) {
        return super.visitExp(ctx);
    }

    @Override
    public Void visitTerminal(TerminalNode node) {
        if(node.getSymbol().getType()!=-1) {
//            if(node.getText().equals("{")) {
//                String ruleName = getRuleName((RuleNode) node.getParent());
//
//                if(ruleName.equals("")) {}
//            }
            String code = node.getSymbol().getText();
            String ruleNameP ="";
            int childNum=0;
            if(node.getParent()!=null){
                ruleNameP = getRuleName((RuleNode) node.getParent());
                childNum=node.getParent().getChildCount();
            }
            if(code.equals("}") && !ruleNameP.equals("array")){
                output.append("\n");
                outputWithoutColor.append("\n");
                changeLine=true;
                if(ruleNameP.equals("block")){
                    tab--;
                    output.append("    ".repeat(Math.max(0, tab)));
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
                if(code.equals("return") && node.getParent().getChild(node.getParent().getChildCount()-2).equals("return"))hasSpace=false;
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
            }

            output.append(CharacterHighlighter.getTerminalColor(node));
            super.visitTerminal(node);


            output.append(node.getSymbol().getText());
            outputWithoutColor.append(node.getSymbol().getText());
            output.append("\u001B[0m");

            if(hasSpace)output.append(" ");
            if(hasSpace)outputWithoutColor.append(" ");

        }
        return null;
    }

    @Override
    public Void visitFunctionDecl(SysYParser.FunctionDeclContext ctx) {
        super.visitFunctionDecl(ctx);
        if(ctx.stop.getType()!=-1)output.append("\n");
        return null;
    }

    @Override
    public Void visitChildren(RuleNode node) {
        if(changeLine){
            output.append("\n");
            outputWithoutColor.append("\n");
            output.append("    ".repeat(Math.max(0, tab)));
        }


        String ruleName = getRuleName(node);
        String ruleNameP ="";
        int childNum=0;
        if(node.getParent()!=null){
            ruleNameP = getRuleName((RuleNode) node.getParent());
            childNum=node.getParent().getChildCount();
        }
        if(ruleName.equals("block")) {
            tab++;
//            if(ruleNameP.equals("functionDecl") || ruleNameP.equals("loop")) {
            if(!hasSpace && !changeLine) {
                output.append(" ");
                outputWithoutColor.append(" ");
            }
//            }else{
//                output.append("\n");
//                outputWithoutColor.append("\n");
//            }
            hasSpace=false;
        }
        changeLine=false;


        if(ruleNameP.equals("block")){
            if( childNum>1&&node.getParent().getChild(1)==node) {
                changeLine=true;
            }
//            if( childNum>1 && node.getParent().getChild(childNum-2)==node) {
//                tab--;
//            }
        }

        output.append(CharacterHighlighter.getColor(node));
        super.visitChildren(node);

        if(ruleName.equals("varDecl")) {
            underline = false;
            changeLine=true;

        }
        output.append("\u001B[0m");
//        if(outputWithoutColor.charAt(outputWithoutColor.length()-1)!='\n') {
            if (ruleName.equals("stat")) {
                changeLine=true;
            }
            if (ruleName.equals("block")) {
                changeLine=true;
            }
//        }

        return null;
    }

}
