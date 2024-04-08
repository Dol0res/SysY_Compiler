import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;

class CharacterHighlighter {

    public static String getTerminalColor(TerminalNode node) {
        String code = node.getText();
        String[] cyanKeywords = {"const", "int", "void", "if", "else", "while", "break", "continue", "return"};
        String[] redKeywords = {"+", "-", "*", "/", "%", "=", "==", "!=", "<", ">", "<=", ">=", "!", "&&", "||", ",", ";"};
        List<String> cyanKeywordsList = Arrays.asList(cyanKeywords);
        List<String> redKeywordsList = Arrays.asList(redKeywords);
        if (cyanKeywordsList.contains(code)) {
            return "\u001B[96m";
        } else if (redKeywordsList.contains(code)) {
            return "\u001B[91m";
        }
        String tokenName = SysYLexer.VOCABULARY.getSymbolicName(node.getSymbol().getType());
        if (tokenName.equals("IDENT")) {
            RuleContext ruleContext = ((RuleNode) node.getParent()).getRuleContext();
            int ruleIndex = ruleContext.getRuleIndex();
            String ruleName = SysYParser.ruleNames[ruleIndex];
            if (ruleName.equals("functionDecl")) {
                return "\u001B[93m";
            }
        } else if (tokenName.equals("L_PAREN") | tokenName.equals("R_PAREN") | tokenName.equals("L_BRACE") | tokenName.equals("R_BRACE") | tokenName.equals("L_BRACKT") | tokenName.equals("R_BRACKT")) {
            return colorizeBrackets(code);
        }


        return ""; // 默认颜色
    }

    public static String getColor(RuleNode node) {
        RuleContext ruleContext = node.getRuleContext();
        int ruleIndex = ruleContext.getRuleIndex();
        String ruleName = SysYParser.ruleNames[ruleIndex];
        switch (ruleName) {
            case "number":
                return "\u001B[35m";
            case "stat":
                return "\u001B[0m";
            case "varDecl":
                Visitor.underline=true;
                return "\u001B[95m";
        }
        return "";
    }

    private static final String[] COLORS = {
            "\u001B[91m",  // 亮红色
            "\u001B[92m",  // 亮绿色
            "\u001B[93m",  // 亮黄色
            "\u001B[94m",  // 亮蓝色
            "\u001B[95m",  // 亮品红色
            "\u001B[96m"   // 亮青色
    };
    static Deque<Integer> colorStack = new ArrayDeque<>();
    static int level = 0;

    public static String colorizeBrackets(String input) {
        char c = input.charAt(0);
        if (c == '(' || c == '[' || c == '{') {
            int colorIndex = level % COLORS.length;
            colorStack.push(colorIndex);
            level++;
            return (COLORS[colorIndex]);
        } else if (c == ')' || c == ']' || c == '}') {
            if (!colorStack.isEmpty()) {
                int colorIndex = colorStack.pop();
                level--;
                return (COLORS[colorIndex]);
            }
        }
        return "";
    }
}
