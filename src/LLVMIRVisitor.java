import org.antlr.v4.runtime.*;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;

import static org.bytedeco.llvm.global.LLVM.*;

public class LLVMIRVisitor extends SysYParserBaseVisitor<LLVMValueRef> {
    private final LLVMModuleRef module = LLVMModuleCreateWithName("module");
    private final LLVMBuilderRef builder = LLVMCreateBuilder();
    private final LLVMTypeRef i32Type = LLVMInt32Type();
    LLVMValueRef result;
    boolean is_global = true;
    LLVMValueRef zero = LLVMConstInt(i32Type, 0, /* signExtend */ 0);

    public LLVMIRVisitor() {
        LLVMInitializeCore(LLVMGetGlobalPassRegistry());
        LLVMLinkInMCJIT();
        LLVMInitializeNativeAsmPrinter();
        LLVMInitializeNativeAsmParser();
        LLVMInitializeNativeTarget();

    }

    public LLVMModuleRef getModule() {
        return module;
    }

    @Override
    public LLVMValueRef visitFuncDef(SysYParser.FuncDefContext ctx) {
        //生成返回值类型
        LLVMTypeRef returnType = i32Type;
        boolean is_global_origin = is_global;
        is_global = false;
        //生成函数参数类型
        PointerPointer<Pointer> argumentTypes = new PointerPointer<>(0);

        //生成函数类型
        LLVMTypeRef ft = LLVMFunctionType(returnType, argumentTypes, /* argumentCount */ 0, /* isVariadic */ 0);
        //若仅需一个参数也可以使用如下方式直接生成函数类型
        //ft = LLVMFunctionType(returnType, i32Type, /* argumentCount */ 0, /* isVariadic */ 0);

        //生成函数，即向之前创建的module中添加函数
        LLVMValueRef function = LLVMAddFunction(module, /*functionName:String*/"main", ft);
        LLVMBasicBlockRef block = LLVMAppendBasicBlock(function, "mainEntry");
        LLVMPositionBuilderAtEnd(builder, block);
        LLVMValueRef r = super.visitFuncDef(ctx);
        is_global = is_global_origin;

        return r;

    }

    @Override
    public LLVMValueRef visitStmt(SysYParser.StmtContext ctx) {
        if (ctx.exp() != null) {
            result = visit(ctx.exp());
        }
        if (ctx.RETURN() != null) {
            LLVMBuildRet(builder, result);
        }

        return null;
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

//    @Override
//    public LLVMValueRef visitLValExp(SysYParser.LValExpContext ctx) {
//        //Scope currentScope =
//        LLVMValueRef lValPointer = currentScope.resolve(ctx.lVal().IDENT().getText());
//        return LLVMBuildLoad(builder, lValPointer, ctx.lVal().getText());
//    }

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

    public String getRuleName(RuleContext ctx) {
        RuleContext ruleContext = ctx.getRuleContext();
        int ruleIndex = ruleContext.getRuleIndex();
        return SysYParser.ruleNames[ruleIndex];
    }

    @Override
    public LLVMValueRef visitDecl(SysYParser.DeclContext ctx) {
        String ruleNameP = getRuleName(ctx.parent);
        if (ruleNameP.equals("compUnit")) {
//创建一个常量,这里是常数0
            LLVMValueRef zero = LLVMConstInt(i32Type, 0, /* signExtend */ 0);

            //创建名为globalVar的全局变量
            LLVMValueRef globalVar = LLVMAddGlobal(module, i32Type, /*globalVarName:String*/"globalVar");

            //为全局变量设置初始化器
            LLVMSetInitializer(globalVar, /* constantVal:LLVMValueRef*/zero);
        }

        return super.visitDecl(ctx);
    }

    @Override
    public LLVMValueRef visitVarDecl(SysYParser.VarDeclContext ctx) {
        if (is_global) {
            for (SysYParser.VarDefContext varDef : ctx.varDef()) {
//            if (zeroDef.ASSIGN() != null) {
//
//                LLVMValueRef v = visit(zeroDef.initVal().exp());
//                zero = LLVMConstInt(i32Type, v, /* signExtend */ 0);
//            }
                String varName = varDef.IDENT().getText();
                //创建名为globalVar的全局变量
                LLVMValueRef globalVar = LLVMAddGlobal(module, i32Type, /*globalVarName:String*/varName);

                //为全局变量设置初始化器
                LLVMSetInitializer(globalVar, /* constantVal:LLVMValueRef*/zero);
            }
        } else {
            for (SysYParser.VarDefContext varDef : ctx.varDef()) {
                String varName = varDef.IDENT().getText();
                //int型变量
                //申请一块能存放int型的内存
                LLVMValueRef pointer = LLVMBuildAlloca(builder, i32Type, /*pointerName:String*/varName);

                //将数值存入该内存
                LLVMBuildStore(builder, zero, pointer);
            }
        }
        return super.visitVarDecl(ctx);
    }
}