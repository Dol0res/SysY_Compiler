import org.antlr.v4.runtime.*;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;

import java.util.Stack;

import static org.bytedeco.llvm.global.LLVM.*;

public class LLVMIRVisitor extends SysYParserBaseVisitor<LLVMValueRef> {
    private final LLVMModuleRef module = LLVMModuleCreateWithName("module");
    private final LLVMBuilderRef builder = LLVMCreateBuilder();
    private final LLVMTypeRef i32Type = LLVMInt32Type();
    LLVMValueRef result;
    LLVMValueRef zero = LLVMConstInt(i32Type, 0, /* signExtend */ 0);
    private Scope globalScope = null;
    private Scope curScope = null;
    public LLVMIRVisitor() {
        LLVMInitializeCore(LLVMGetGlobalPassRegistry());
        LLVMLinkInMCJIT();
        LLVMInitializeNativeAsmPrinter();
        LLVMInitializeNativeAsmParser();
        LLVMInitializeNativeTarget();

    }
    LLVMValueRef function=null;
    public LLVMModuleRef getModule() {
        return module;
    }

    @Override
    public LLVMValueRef visitFuncDef(SysYParser.FuncDefContext ctx) {
        //生成返回值类型
        LLVMTypeRef returnType = i32Type;
        Scope lastScope = curScope;
        curScope = new Scope(curScope);
        String functionName = ctx.IDENT().getText();
        int argumentCount = 0 ;
        if(ctx.funcFParams()!=null) argumentCount = ctx.funcFParams().funcFParam().size();
        //生成函数参数类型
        PointerPointer<Pointer> argumentTypes = new PointerPointer<>(argumentCount);

        for(int i=0;i<argumentCount;i++) {
            argumentTypes.put(i, i32Type);
        }
        //生成函数类型
        LLVMTypeRef ft = LLVMFunctionType(returnType, argumentTypes, /* argumentCount */ argumentCount, /* isVariadic */ 0);
        //若仅需一个参数也可以使用如下方式直接生成函数类型
        //生成函数，即向之前创建的module中添加函数
        function = LLVMAddFunction(module, /*functionName:String*/functionName, ft);

        LLVMBasicBlockRef block = LLVMAppendBasicBlock(function, functionName+"Entry");
        LLVMPositionBuilderAtEnd(builder, block);
        for(int i=0;i<argumentCount;i++) {
            String varName = ctx.funcFParams().funcFParam(i).IDENT().getText();
            LLVMValueRef arg1 = LLVMBuildAlloca(builder, i32Type, varName);
            //LLVMSetValueName(arg1, varName);
            LLVMValueRef argValue = LLVMGetParam(function, i);
            LLVMBuildStore(builder, argValue, arg1);
            curScope.define(varName, arg1);
        }
        LLVMValueRef r = super.visitFuncDef(ctx);

        curScope=lastScope;
        curScope.define(functionName,function);
        return r;

    }

    @Override
    public LLVMValueRef visitProgram(SysYParser.ProgramContext ctx) {
        // Visit the program node
        globalScope = new Scope(null);
        curScope = globalScope;
        super.visitProgram(ctx);
        return null;
    }

    @Override
    public LLVMValueRef visitBlock(SysYParser.BlockContext ctx) {
        //新一层作用域
        //System.out.println(ctx.getText());
        curScope = new Scope(curScope); // 添加新作用域
        //some code // 将形参添加到作用域里

        ctx.blockItem().forEach(this::visit); // 依次visit block中的节点
        //切换回父级作用域
        curScope = curScope.getParentScope();

        return null;
    }
    @Override
    public LLVMValueRef visitStmt(SysYParser.StmtContext ctx) {
        if(ctx.ASSIGN()!=null){
            LLVMValueRef pointer = this.visitLVal(ctx.lVal());
            LLVMValueRef value = this.visit(ctx.exp());
            LLVMBuildStore(builder, value, pointer);
        } else if (ctx.exp() != null) {
            result = visit(ctx.exp());
        }
        if (ctx.RETURN() != null) {
            LLVMBuildRet(builder, result);
        }
        if(ctx.block()!=null){
            return visit(ctx.block());
        }
        if (ctx.IF() != null) {
            LLVMValueRef condVal = this.visit(ctx.cond());
            LLVMValueRef cmpResult = LLVMBuildICmp(builder, LLVMIntNE, zero, condVal, "cmp_result");
            LLVMBasicBlockRef trueBlock = LLVMAppendBasicBlock(function, "true");
            LLVMBasicBlockRef falseBlock = LLVMAppendBasicBlock(function, "false");
            LLVMBasicBlockRef afterBlock = LLVMAppendBasicBlock(function, "entry");

            LLVMBuildCondBr(builder, cmpResult, trueBlock, falseBlock);

            LLVMPositionBuilderAtEnd(builder, trueBlock);
            this.visit(ctx.stmt(0));
            LLVMBuildBr(builder, afterBlock);

            LLVMPositionBuilderAtEnd(builder, falseBlock);
            if (ctx.ELSE() != null) {
                this.visit(ctx.stmt(1));
            }
            LLVMBuildBr(builder, afterBlock);

            LLVMPositionBuilderAtEnd(builder, afterBlock);
            return null;
            //LLVMBasicBlockRef block1 = LLVMAppendBasicBlock(function, /*blockName:String*/"true");
        }
        //将数值存入该内存
        return null;
    }

    @Override
    public LLVMValueRef visitExpCond(SysYParser.ExpCondContext ctx) {
        return visit(ctx.exp());
    }

    @Override
    public LLVMValueRef visitAndCond(SysYParser.AndCondContext ctx) {
        LLVMValueRef l = visit(ctx.cond(0));
        return l;
    }
    @Override
    public LLVMValueRef visitEqCond(SysYParser.EqCondContext ctx) {
        LLVMValueRef l = visit(ctx.cond(0));
        LLVMValueRef r = visit(ctx.cond(1));
        if(ctx.EQ()!=null) {
            return LLVMBuildICmp(builder, LLVMIntEQ, l, r, "eq");
        }else{
            return LLVMBuildICmp(builder, LLVMIntNE, l, r, "neq");
        }
    }


    @Override
    public LLVMValueRef visitLValExp(SysYParser.LValExpContext ctx) {
        LLVMValueRef lValPointer = this.visitLVal(ctx.lVal());
        LLVMValueRef r =  LLVMBuildLoad(builder, lValPointer, ctx.lVal().getText());
        return r;
    }

    @Override
    public LLVMValueRef visitLVal(SysYParser.LValContext ctx) {
        String lValName = ctx.IDENT().getText();
        return curScope.find(lValName);
    }

    @Override
    public LLVMValueRef visitNumExp(SysYParser.NumExpContext ctx) {
        String num = ctx.number().getText();
        int val;

        //处理十六进制和八进制
        if (num.startsWith("0x") || num.startsWith("0X")) {
            val = Integer.parseInt(num.substring(2), 16);
        } else if (num.startsWith("0") && num.length() > 1) {
            val = Integer.parseInt(num, 8);
        } else {
            val = Integer.parseInt(num);
        }

        return LLVMConstInt(i32Type, val, 0);
    }
    @Override
    public LLVMValueRef visitFuncCallExp(SysYParser.FuncCallExpContext ctx){
        String functionName = ctx.IDENT().getText();
        LLVMValueRef function = curScope.find(functionName);
        PointerPointer<Pointer> args = null;
        int argsCount = 0;
        if (ctx.funcRParams() != null) {
            argsCount = ctx.funcRParams().param().size();
            args = new PointerPointer<>(argsCount);
            for (int i = 0; i < argsCount; ++i) {
                SysYParser.ParamContext paramContext = ctx.funcRParams().param(i);
                SysYParser.ExpContext expContext = paramContext.exp();
                args.put(i, this.visit(expContext));
            }
        }
        return LLVMBuildCall(builder, function, args, argsCount, "");
    }
    @Override
    public LLVMValueRef visitParenExp(SysYParser.ParenExpContext ctx) {
        return this.visit(ctx.exp());
    }
    @Override
    public LLVMValueRef visitAddExp(SysYParser.AddExpContext ctx) {
        LLVMValueRef valueRef1 = visit(ctx.exp(0));
        LLVMValueRef valueRef2 = visit(ctx.exp(1));
        if (ctx.PLUS() != null) {
            return LLVMBuildAdd(builder, valueRef1, valueRef2, "tmp_");
        } else {
            return LLVMBuildSub(builder, valueRef1, valueRef2, "tmp_");
        }
    }
    @Override
    public LLVMValueRef visitMulExp(SysYParser.MulExpContext ctx) {
        LLVMValueRef valueRef1 = visit(ctx.exp(0));
        LLVMValueRef valueRef2 = visit(ctx.exp(1));
        if (ctx.MUL() != null) {
            return LLVMBuildMul(builder, valueRef1, valueRef2, "tmp_");
        } else if (ctx.DIV() != null) {
            return LLVMBuildSDiv(builder, valueRef1, valueRef2, "tmp_");
        } else {
            return LLVMBuildSRem(builder, valueRef1, valueRef2, "tmp_");
        }
    }
    @Override
    public LLVMValueRef visitUnaryExp(SysYParser.UnaryExpContext ctx) {
        LLVMValueRef exp1 = this.visit(ctx.exp());
        if (ctx.unaryOp().PLUS() != null) {
            return exp1;
        } else if (ctx.unaryOp().MINUS() != null) {
            return LLVMBuildNeg(builder, exp1, "tmp_");
        } else {
            // 生成icmp
            LLVMValueRef tmp_ = LLVMBuildICmp(builder, LLVMIntNE, LLVMConstInt(i32Type, 0, 0), exp1, "tmp_");
// 生成xor
            tmp_ = LLVMBuildXor(builder, tmp_, LLVMConstInt(LLVMInt1Type(), 1, 0), "tmp_");
// 生成zext
            tmp_ = LLVMBuildZExt(builder, tmp_, i32Type, "tmp_");
            return tmp_;
        }

    }

    @Override
    public LLVMValueRef visitVarDef(SysYParser.VarDefContext ctx) {
        String varName = ctx.IDENT().getText();
        LLVMValueRef value = zero;

        if (curScope == globalScope) {
            LLVMValueRef var = LLVMAddGlobal(module, i32Type, /*varName:String*/varName);

            if(ctx.ASSIGN()!=null){
                value = this.visit(ctx.initVal());
            }
            //创建名为globalVar的全局变量
            //为全局变量设置初始化器
            LLVMSetInitializer(var, /* constantVal:LLVMValueRef*/value);
            curScope.define(varName,var);
        } else {
            LLVMValueRef pointer = LLVMBuildAlloca(builder, i32Type, /*pointerName:String*/varName);
            if(ctx.ASSIGN()!=null){
                value = this.visit(ctx.initVal());
                LLVMBuildStore(builder, value, pointer);
            }
            //将数值存入该内存
            curScope.define(varName,pointer);
        }
        //return super.visitVarDef(ctx);
        return value;
    }

    @Override
    public LLVMValueRef visitConstDef(SysYParser.ConstDefContext ctx) {
        String varName = ctx.IDENT().getText();
        LLVMValueRef value = this.visit(ctx.constInitVal());

//        if(ctx.ASSIGN()!=null){
////            LLVMValueRef intValue = this.visit(ctx.constInitVal());
////            int signedValue = (int)LLVMConstIntGetSExtValue(intValue);
////
////            value = LLVMConstInt(i32Type, signedValue, /* signExtend */ 0);
//        }

        if (curScope == globalScope) {
            LLVMValueRef var = LLVMAddGlobal(module, i32Type, /*varName:String*/varName);

            //为全局变量设置初始化器
            LLVMSetInitializer(var, /* constantVal:LLVMValueRef*/value);
            curScope.define(varName,var);
        } else {
            LLVMValueRef pointer = LLVMBuildAlloca(builder, i32Type, /*pointerName:String*/varName);
            LLVMBuildStore(builder, value, pointer);
            curScope.define(varName,pointer);
        }
        //return super.visitConstDef(ctx);
        return value;
    }
}