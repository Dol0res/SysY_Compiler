import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;
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
        ArrayList<Type> paramsTyList = new ArrayList<>();

        if (curScope.find(funcName) != null) {
            hasError = true;
            OutputHelper.printSemanticError(4, ctx.IDENT().getSymbol().getLine());
            return null;
        }

        Type retType = Type.getVoidType();
        String typeStr = ctx.getChild(0).getText();
        if (typeStr.equals("int"))
            retType = IntType.getI32();

        if (ctx.funcFParams() != null) {
            visit(ctx.funcFParams());
            // Process function parameters if any
        }

        FunctionType functionType = new FunctionType(retType, paramsTyList);
        curScope.define(funcName, functionType); // Define the function in the current scope
        funcRetType = retType;
        // Create a new scope for the function body
        scopeStack.push(new Scope(curScope));
        visit(ctx.block());
        scopeStack.pop(); // Pop the function scope after visiting its block
        funcRetType = null;
        return null;
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
    public Void visitVarDef(SysYParser.VarDefContext ctx) {
        String varName = ctx.IDENT().getText(); // c or d
        Scope curScope = scopeStack.peek();

        if (curScope.findCurrent(varName) != null) {
            hasError = true;
            OutputHelper.printSemanticError(3, ctx.IDENT().getSymbol().getLine());
            return null;
        }

        if (ctx.constExp().isEmpty()) {     //非数组
            if (ctx.ASSIGN() != null) {     // 包含定义语句
                visitInitVal(ctx.initVal()); // 访问定义语句右侧的表达式，如c=4右侧的4
            }
            curScope.put(varName, IntType.getI32());
        } else { // 数组

            ArrayType arrType = new ArrayType(IntType.getI32(), ctx.constExp().size());
            if (ctx.ASSIGN() != null) {     // 包含定义语句
                visitInitVal(ctx.initVal()); // 访问定义语句右侧的表达式，如c=4右侧的4
            }
            curScope.put(varName, arrType);

        }
        return null;
    }

    @Override
    public Void visitCond(SysYParser.CondContext ctx) {
        if (ctx.exp() == null && getCondType(ctx) != IntType.getI32()) {
            hasError = true;
            OutputHelper.printSemanticError(6, ctx.getStart().getLine());
            return null;
        }

        return super.visitCond(ctx);
    }

    @Override
    public Void visitStmt(SysYParser.StmtContext ctx) {
        if (ctx.ASSIGN() != null) {
            String varName = ctx.lVal().getText(); // c or d
            Scope curScope = scopeStack.peek();
            Type lType = curScope.find(varName);
            Type rType = getExpType(ctx.exp());

            if (lType == null) {
                hasError = true;
                OutputHelper.printSemanticError(1, ctx.getStart().getLine());//变量未声明
                return null;
            } else if (lType instanceof FunctionType) {
                hasError = true;
                OutputHelper.printSemanticError(11, ctx.getStart().getLine());//变量未声明
                return null;

            }
            if (rType == null) {
                return super.visitStmt(ctx);
                //hasError=true;
                // OutputHelper.printSemanticError(1, ctx.ASSIGN().getSymbol().getLine());//变量未声明
            } else {
                if(lType != null && !checkType(lType, rType, 5, ctx.getStart().getLine())){
                    return null;
                }
            }
        } else if (ctx.RETURN() != null) {
            if (ctx.exp() == null) {
                if (funcRetType != Type.getVoidType()) {
                    hasError = true;
                    OutputHelper.printSemanticError(7, ctx.RETURN().getSymbol().getLine());//变量未声明
                    return null;

                }
            } else {
                if (funcRetType != getExpType(ctx.exp())) {
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

                //return super.visitExp(ctx);
            } else {

                FunctionType functionType = (FunctionType) type;
                ArrayList<Type> paramsType = functionType.getParamsType(), argsType = new ArrayList<>();
                if (ctx.funcRParams() != null) {
                    for (SysYParser.ParamContext paramContext : ctx.funcRParams().param()) {
                        Type lType = getExpType(paramContext.exp());
                        if (paramsType.isEmpty()) {
                            hasError = true;
                            OutputHelper.printSemanticError(8, ctx.IDENT().getSymbol().getLine());//函数未定义
                            return null;

                        }
                        Type rType = paramsType.get(0);
                        if(!checkType(lType, rType, 8, ctx.IDENT().getSymbol().getLine())){
                            return null;
                        }
                        paramsType.remove(0);
                    }
                    if (!paramsType.isEmpty()) {
                        hasError = true;
                        OutputHelper.printSemanticError(8, ctx.IDENT().getSymbol().getLine());//函数未定义
                        return null;

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
            if (op1Type== null || op2Type== null) {
                hasError = true;
                OutputHelper.printSemanticError(1, ctx.getStart().getLine());
                return null;
            } else if (op1Type == IntType.getI32() && op2Type == IntType.getI32()) {
            } else {
                hasError = true;
                OutputHelper.printSemanticError(6, ctx.getStart().getLine());
                return null;

            }
        }
        return super.visitExp(ctx);
    }

    private Type getLValType(SysYParser.LValContext ctx) {
        Scope curScope = scopeStack.peek();
        String varName = ctx.IDENT().getText();
        Type type = curScope.find(varName);
        if (type == null) {
            return null;
        }
        if (type instanceof ArrayType) {
            int d1 = ctx.exp().size();
            int d2 = ((ArrayType) type).getNum_elements();
            if (d1 == d2) {
                return IntType.getI32();
            }
            if (d1 > d2) {
                return null;
            }
            return new ArrayType(IntType.getI32(), d2 - d1);
        } else if (ctx.exp() != null && !ctx.exp().isEmpty()) {
            hasError = true;
            OutputHelper.printSemanticError(9, ctx.getStart().getLine());
            return null;

        }
        return type;
    }
//    private Type getFuncCallType(SysYParser.LValContext ctx) {
//        Scope curScope = scopeStack.peek();
//        String varName = ctx.IDENT().getText();
//        Type type = curScope.find(varName);
//        if (type == null) {
//            return null;
//        }
//        if(type instanceof ArrayType) {
//            int d1 = ctx.exp().size();
//            int d2 = ((ArrayType)type).getNum_elements();
//            if(d1 == d2) {
//                return IntType.getI32();
//            }
//            if(d1 > d2) {
//                return null;
//            }
//            return new ArrayType(IntType.getI32(), d2-d1 );
//        }
//        return type;
//    }

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
