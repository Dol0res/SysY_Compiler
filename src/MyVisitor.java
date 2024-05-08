import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

public class MyVisitor extends SysYParserBaseVisitor<Void> {
    private Stack<Scope> scopeStack = new Stack<>();
    private Type funcRetType = null;
    private boolean hasError = false;
//    @Override
//    public Void visit(ParseTree tree) {
//        // Initialize the global scope
//        Scope ps = null;
//        if(!scopeStack.isEmpty()) {
//            ps = scopeStack.peek();
//        }
//        scopeStack.push(new Scope(ps));
//        super.visit(tree);
//        scopeStack.pop(); // Pop the global scope after visiting the tree
//        return null;
//    }

    @Override
    public Void visitTerminal(TerminalNode node) {
        // Handle terminal nodes if needed
        return null;
    }

    @Override
    public Void visitProgram(SysYParser.ProgramContext ctx) {
        // Visit the program node

        scopeStack.push(new Scope(null));
        super.visitProgram(ctx);
        scopeStack.pop(); // Pop the global scope after visiting the tree
        if (!hasError) System.err.println("No semantic errors in the program!");
        return null;
    }

    @Override
    public Void visitCompUnit(SysYParser.CompUnitContext ctx) {
        // Visit the compilation unit node
        super.visitCompUnit(ctx);
        return null;
    }

    @Override
    public Void visitFuncDef(SysYParser.FuncDefContext ctx) {
        String funcName = ctx.IDENT().getText();
        Scope curScope = scopeStack.peek();
        scopeStack.push(new Scope(curScope));
        ArrayList<Type> paramsTyList = new ArrayList<>();

        if (curScope.find(funcName) != null) {
            hasError = true;
            OutputHelper.printSemanticError(4, ctx.IDENT().getSymbol().getLine());
            return null;
        }

        Type retType = VoidType.getVoidType();
        String typeStr = ctx.getChild(0).getText();
        if (typeStr.equals("int"))
            retType = IntType.getI32();

        if (ctx.funcFParams() != null) {
            paramsTyList = resolveFuncFParams(ctx.funcFParams());
            // Process function parameters if any
        }

        FunctionType functionType = new FunctionType(retType, paramsTyList);
        curScope.define(funcName, functionType); // Define the function in the current scope
        funcRetType = retType;
        // Create a new scope for the function body

        visit(ctx.block());
        scopeStack.pop(); // Pop the function scope after visiting its block
        funcRetType = null;
        return null;
    }


    public ArrayList<Type> resolveFuncFParams(SysYParser.FuncFParamsContext ctx) {
        ArrayList<Type> paramsTyList = new ArrayList<>();
        Scope curScope = scopeStack.peek();
        for (SysYParser.FuncFParamContext paramCtx : ctx.funcFParam()) {
            String varName = paramCtx.IDENT().getText();
            if (curScope.findCurrent(varName) != null) {
                hasError = true;
                OutputHelper.printSemanticError(3, paramCtx.IDENT().getSymbol().getLine());
            } else {
                Type type = VoidType.getVoidType();
                String typeStr = paramCtx.getChild(0).getText();
                if (typeStr.equals("int")) type = IntType.getI32();
                if(paramCtx.L_BRACKT()!=null && !paramCtx.L_BRACKT().isEmpty()){
                    type = new ArrayType(type, paramCtx.L_BRACKT().size());
                }
                curScope.define(varName, type);
                paramsTyList.add(type);
            }
        }
        return paramsTyList;
    }

    // Implement other visit methods as needed
    @Override
    public Void visitBlock(SysYParser.BlockContext ctx) {
        //新一层作用域
        //System.out.println(ctx.getText());
        Scope curScope = scopeStack.peek();

        Scope newScope = new Scope(curScope); // 添加新作用域
        scopeStack.push(newScope);
        //some code // 将形参添加到作用域里

        ctx.blockItem().forEach(this::visit); // 依次visit block中的节点
        //切换回父级作用域
        scopeStack.pop();
        //curScope = curScope.parent;

        return null;
    }



    @Override
    public Void visitCond(SysYParser.CondContext ctx) {
        super.visitCond(ctx);

        if (ctx.exp() == null && getCondType(ctx) != IntType.getI32()) {
            hasError = true;
            OutputHelper.printSemanticError(6, ctx.getStart().getLine());
            return null;
        }
        return null;
    }


    @Override
    public Void visitVarDef(SysYParser.VarDefContext ctx) {
        String varName = ctx.IDENT().getText(); // c or d
        Scope curScope = scopeStack.peek();
        super.visitVarDef(ctx);
        if (curScope.findCurrent(varName) != null) {
            hasError = true;
            OutputHelper.printSemanticError(3, ctx.IDENT().getSymbol().getLine());
            return null;
        }

        if (ctx.constExp().isEmpty()) {     //非数组
            curScope.put(varName, IntType.getI32());
            if (ctx.ASSIGN() != null) {     // 包含定义语句
                Type lType = IntType.getI32();
                if(ctx.initVal().exp()==null){
                    hasError = true;
                    OutputHelper.printSemanticError(5, ctx.getStart().getLine());//变量未声明
                    return null;
                }
                Type rType = getExpType(ctx.initVal().exp());
                if(rType == ErrorType.getErrorType()){
                    return null;
                }
                if (lType instanceof FunctionType) {
                    hasError = true;
                    OutputHelper.printSemanticError(11, ctx.getStart().getLine());//变量未声明
                    return null;
                }
                if (rType == null) {
                    //hasError=true;
                    // OutputHelper.printSemanticError(1, ctx.ASSIGN().getSymbol().getLine());//变量未声明
                } else {
                    if (!checkType(lType, rType, 5, ctx.getStart().getLine())) {
                        return null;
                    }
                }
                visitInitVal(ctx.initVal()); // 访问定义语句右侧的表达式，如c=4右侧的4
            }
        } else { // 数组
            ArrayType arrType = new ArrayType(IntType.getI32(), ctx.constExp().size());
            curScope.put(varName, arrType);

            if (ctx.ASSIGN() != null) {     // 包含定义语句
                Type lType = arrType;
                Type rType = new ArrayType(IntType.getI32(), 1);
                if(ctx.initVal().exp()!=null) {
                    rType = getExpType(ctx.initVal().exp());
                }
                visitInitVal(ctx.initVal()); // 访问定义语句右侧的表达式，如c=4右侧的4

                if (lType == null) {
                    hasError = true;
                    OutputHelper.printSemanticError(1, ctx.getStart().getLine());//变量未声明
                    //return null;
                } else if (lType instanceof FunctionType) {
                    hasError = true;
                    OutputHelper.printSemanticError(11, ctx.getStart().getLine());//变量未声明
                    return null;

                }
                if (rType == null) {
                    //hasError=true;
                    // OutputHelper.printSemanticError(1, ctx.ASSIGN().getSymbol().getLine());//变量未声明
                } else {
                    if (!checkType(lType, rType, 5, ctx.getStart().getLine())) {
                        return null;
                    }
                }
            }

        }
        return null;
    }
    @Override
    public Void visitConstDecl(SysYParser.ConstDeclContext ctx) {
        //String typeName = ctx.bType().getText();
        Scope curScope = scopeStack.peek();
        //super.visitConstDecl(ctx);

        for (SysYParser.ConstDefContext varDefContext : ctx.constDef()) {
            //Type constType = (Type) curScope.resolve(typeName);
            Type constType = (IntType.getI32());
            if (!varDefContext.constExp().isEmpty()) {
                constType = new ArrayType(constType, varDefContext.constExp().size());
            }
            String constName = varDefContext.IDENT().getText();
            if (curScope.findCurrent(constName) != null) {
                OutputHelper.printSemanticError(3, varDefContext.getStart().getLine());
                continue;
            }
            if (varDefContext.constInitVal() != null) {
                Type initValType = new ArrayType(IntType.getI32(), 1);
                SysYParser.ConstExpContext expContext = varDefContext.constInitVal().constExp();
                if (expContext != null) {
                    initValType = getExpType(expContext.exp());
                }
                    if (initValType == ErrorType.getErrorType()) {
                        return null;//
                    }

                    if (initValType != null) {
                        if (!checkType(initValType, constType, 5, ctx.getStart().getLine())) {
                            return null;
                        }
                    }

            }
                curScope.define(constName, constType);

        }

        return super.visitConstDecl(ctx);
    }
    @Override
    public Void visitStmt(SysYParser.StmtContext ctx) {
        //super.visitStmt(ctx);

        if (ctx.ASSIGN() != null) {
            //String varName = ctx.lVal().IDENT().getText(); // c or d
            //Scope curScope = scopeStack.peek();
            Type lType = getLValType(ctx.lVal());
            Type rType = getExpType(ctx.exp());

            if (lType instanceof FunctionType) {
                hasError = true;
                OutputHelper.printSemanticError(11, ctx.getStart().getLine());//变量未声明
                return null;

            }else if (lType == ErrorType.getErrorType()) {
                return null;

            }
            if (rType == null) {
                //return null;
                //hasError=true;
                // OutputHelper.printSemanticError(1, ctx.ASSIGN().getSymbol().getLine());//变量未声明
            } else if (rType == ErrorType.getErrorType()) {
                return null;
            } else {
                if (!checkType(lType, rType, 5, ctx.getStart().getLine())) {
                    return null;
                }
            }
        } else if (ctx.RETURN() != null) {
            if (ctx.exp() == null) {
                if (funcRetType != VoidType.getVoidType()) {
                    hasError = true;
                    OutputHelper.printSemanticError(7, ctx.RETURN().getSymbol().getLine());//变量未声明
                    return null;

                }
            } else {
                Type type = getExpType(ctx.exp());
                if(type!=null && type == ErrorType.getErrorType()){
                    return null;
                }
                if (funcRetType != type) {
                    hasError = true;
                    OutputHelper.printSemanticError(7, ctx.RETURN().getSymbol().getLine());//变量未声明
                    return null;

                }
            }

        }
        super.visitStmt(ctx);
        return null;
    }

    @Override
    public Void visitExp(SysYParser.ExpContext ctx) {
        super.visitExp(ctx);
        //funcCall
        if (ctx.IDENT() != null) {
            String funcName = ctx.IDENT().getText();
            Scope curScope = scopeStack.peek();
            Type type = curScope.find(funcName);
            if (type == null) {
                hasError = true;
                OutputHelper.printSemanticError(2, ctx.IDENT().getSymbol().getLine());
                return null;
                //return super.visitExp(ctx);

            } else if (!(type instanceof FunctionType)) {
                hasError = true;
                OutputHelper.printSemanticError(10, ctx.IDENT().getSymbol().getLine());
                return null;

            } else {

                FunctionType functionType = (FunctionType) type;
                ArrayList<Type> paramsType = functionType.getParamsType();
                if(!paramsType.isEmpty() && ctx.funcRParams() == null){
                    hasError = true;
                    OutputHelper.printSemanticError(8, ctx.IDENT().getSymbol().getLine());//函数未定义
                    return null;
                }
                if (ctx.funcRParams() != null) {
                    if(ctx.funcRParams().param().size()!=paramsType.size()){
                        hasError = true;
                        OutputHelper.printSemanticError(8, ctx.IDENT().getSymbol().getLine());//函数未定义
                        return null;
                    }
                    int i=0;
                    for (SysYParser.ParamContext paramContext : ctx.funcRParams().param()) {
                        Type lType = getExpType(paramContext.exp());
                        Type rType = paramsType.get(i);
                        if(lType==ErrorType.getErrorType()|| rType==ErrorType.getErrorType()){
                            return null;
                        }
                        if (!checkType(lType, rType, 8, ctx.IDENT().getSymbol().getLine())) {
                            return null;
                        }
                        //paramsType.remove(0);
                        i+=1;
                    }

                }
            }
        }

        //+-*/
        else if (ctx.unaryOp() != null) { // unaryOp exp
            Type expType = getExpType(ctx.exp(0));
            if (expType != IntType.getI32()) {
                hasError = true;
                OutputHelper.printSemanticError(6, ctx.getStart().getLine());
                return null;

            }
        } else if (ctx.MUL() != null || ctx.DIV() != null || ctx.MOD() != null || ctx.PLUS() != null || ctx.MINUS() != null) {
            Type op1Type = getExpType(ctx.exp(0)), op2Type = getExpType(ctx.exp(1));
            if (op1Type == null || op2Type == null) {
                hasError = true;
                //OutputHelper.printSemanticError(1, ctx.getStart().getLine());
                return null;
            } else if (op1Type == IntType.getI32() && op2Type == IntType.getI32()) {
            } else {
                hasError = true;
                OutputHelper.printSemanticError(6, ctx.getStart().getLine());
                return null;

            }
        }
         else if (ctx.lVal()!=null) {
            Type lValType = getLValType(ctx.lVal());
            if(lValType!=null && lValType == ErrorType.getErrorType()){
                return null;
            }
//            if (lValType!=null && lValType != IntType.getI32()) {
//                hasError = true;
//                OutputHelper.printSemanticError(6, ctx.getStart().getLine());
//                return null;
//            }
        }
        //return super.visitExp(ctx);
        return null;
    }

    private Type getLValType(SysYParser.LValContext ctx) {
        Scope curScope = scopeStack.peek();
        String varName = ctx.IDENT().getText();
        Type type = curScope.find(varName);
        if (type == null) {
                hasError = true;
                OutputHelper.printSemanticError(1, ctx.getStart().getLine());//变量未声明
                return ErrorType.getErrorType();

        }
//        if(type instanceof FunctionType){
//            hasError = true;
//            OutputHelper.printSemanticError(5, ctx.getStart().getLine());
//            return ErrorType.getErrorType();
//        }
        if (type instanceof ArrayType) {
            int d1 = ctx.exp().size();
            int d2 = ((ArrayType) type).getNum_elements();
            if (d1 == d2) {
                return IntType.getI32();
            }
            if (d1 > d2) {
                hasError = true;
                OutputHelper.printSemanticError(9, ctx.getStart().getLine());
                return ErrorType.getErrorType();
            }
            return new ArrayType(IntType.getI32(), d2 - d1);
        } else if (ctx.exp() != null && !ctx.exp().isEmpty()) {
            hasError = true;
            OutputHelper.printSemanticError(9, ctx.getStart().getLine());
            return ErrorType.getErrorType();

        }
        return type;
    }

    private Type getExpType(SysYParser.ExpContext ctx) {
        Scope curScope = scopeStack.peek();
        if (ctx.IDENT() != null) { // func
            //assert ctx.IDENT().getText() != null : "funcName 为空";
            String funcName = ctx.IDENT().getText();
            Type type = curScope.find(funcName);
            if (type instanceof FunctionType) {
                return ((FunctionType) type).getRetTy();
            }
            return null;
        } else if (ctx.L_PAREN() != null) { // L_PAREN exp R_PAREN
            return getExpType(ctx.exp(0));
        } else if (ctx.unaryOp() != null) { // unaryOp exp
            return getExpType(ctx.exp(0));
        } else if (ctx.lVal() != null) { // lVal
            return getLValType(ctx.lVal());
        } else if (ctx.number() != null) { // number
            return IntType.getI32();
        } else if (ctx.MUL() != null || ctx.DIV() != null || ctx.MOD() != null || ctx.PLUS() != null || ctx.MINUS() != null) {
            Type op1Type = getExpType(ctx.exp(0));
            Type op2Type = getExpType(ctx.exp(1));
            if (op1Type == IntType.getI32() && op2Type == IntType.getI32()) {
                return op1Type;
            }
        }
        return null;
    }

    private Type getCondType(SysYParser.CondContext ctx) {
        if (ctx.exp() != null) {
            return getExpType(ctx.exp());
        }

        Type cond1 = getCondType(ctx.cond(0));
        Type cond2 = getCondType(ctx.cond(1));
        if (cond1 == IntType.getI32() && cond2 == IntType.getI32()) {
            return cond1;
        }
        return null;
    }

    private boolean checkType(Type lType, Type rType, int ErrorType, int line) {
        if (lType instanceof ArrayType && rType instanceof ArrayType) {
//                if(((ArrayType) lType).getContained() != IntType.getI32()){
//                    hasError=true;
//                    OutputHelper.printSemanticError(2, ctx.ASSIGN().getSymbol().getLine());//函数未定义
//                }
            if (((ArrayType) lType).getNum_elements() != ((ArrayType) lType).getNum_elements()) {
                hasError = true;
                OutputHelper.printSemanticError(ErrorType, line);//函数未定义
                return false;
            }

        } else if (lType != rType) {
            hasError = true;
            OutputHelper.printSemanticError(ErrorType, line);//函数未定义
            return false;
        }
        return true;
    }

}
//5