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

        //System.out.println(RainbowBrackets.colorizeBrackets(output.toString()));
        System.out.println(output.toString());
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


            output.append(CharacterHighlighter.getTerminalColor(node));
            if(underline) {
                output.append("\u001B[4m");
            }
            output.append(node.getSymbol().getText());
            outputWithoutColor.append(node.getSymbol().getText());
            output.append("\u001B[0m");

            if(hasSpace)output.append(" ");
            if(hasSpace)outputWithoutColor.append(" ");

        }
        return super.visitTerminal(node);
    }

    @Override
    public Void visitChildren(RuleNode node) {

        // 打印当前节点的文本表示
        // 继续访问子节点
        output.append(CharacterHighlighter.getColor(node));
        String ruleName = getRuleName(node);
        String ruleNameP ="";
        int childNum=0;
        if(node.getParent()!=null){
            ruleNameP = getRuleName((RuleNode) node.getParent());
            childNum=node.getParent().getChildCount();
        }

        if(ruleName.equals("block")) {
//            if(ruleNameP.equals("functionDecl") || ruleNameP.equals("loop")) {
                if(!hasSpace) {
                    output.append(" ");
                    outputWithoutColor.append(" ");
                }
//            }else{
//                output.append("\n");
//                outputWithoutColor.append("\n");
//            }
            hasSpace=false;
        }
        if(ruleNameP.equals("block")){
            if( childNum>1&&node.getParent().getChild(1)==node) {
                output.append("\n");
                tab++;
            }
            output.append("    ".repeat(Math.max(0, tab)));
            if( childNum>1 && node.getParent().getChild(childNum-2)==node) {
                tab--;
            }
        }


        super.visitChildren(node);


        if(ruleName.equals("varDecl")) underline = false;
        output.append("\u001B[0m");
        if(ruleName.equals("stat") ) {
            output.append("\n");
            outputWithoutColor.append("\n");
        }

        return null;
    }

}
