
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef;
import org.bytedeco.llvm.LLVM.LLVMModuleRef;
import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;

import static org.bytedeco.llvm.global.LLVM.*;

import java.io.IOException;

public class Main {

    public static SysYLexer lexer(String sourcePath) throws IOException {
        CharStream input = CharStreams.fromFileName(sourcePath);
        return new SysYLexer(input);
    }

    public static SysYParser parser(SysYLexer sysYLexer) {
        CommonTokenStream tokens = new CommonTokenStream(sysYLexer);
        return new SysYParser(tokens);
    }

    public static void main(String[] args) throws IOException {
        String srcPath = "tests/test1.sysy";
        String destPath = "tests/output.txt";

        if (args.length < 2) {
            System.err.println("input path is required");
            //return;
        } else {
            srcPath = args[0];
            destPath = args[1];
        }

        SysYLexer sysYLexer = lexer(srcPath);
        SysYParser sysYParser = parser(sysYLexer);
        ParseTree tree = sysYParser.program();

        LLVMIRVisitor llvmIRVisitor = new LLVMIRVisitor();
        llvmIRVisitor.visit(tree);

        final BytePointer error = new BytePointer();
        LLVMModuleRef module = llvmIRVisitor.getModule();
//        if (LLVMPrintModuleToFile(llvmIRVisitor.getModule(), destPath, error) != 0) {
//            LLVMDisposeMessage(error);
//        }
        AsmBuilder asmBuilder = new AsmBuilder();

        for (LLVMValueRef value = LLVMGetFirstGlobal(module); value != null; value = LLVMGetNextGlobal(value)) {
            //...
            LLVMTypeRef valueType = LLVMTypeOf(value);

            // 检查全局值是否是函数类型
            if (LLVMGetValueKind(value) == LLVMFunctionValueKind) {
                // 将全局值转换为函数类型

            }
        }
        asmBuilder.text("");
        for (LLVMValueRef value = LLVMGetFirstFunction(module); value != null; value = LLVMGetNextFunction(value)) {
            // 获取函数名
            String funcName = LLVMGetValueName(value).getString();
            asmBuilder.globl(funcName);
            asmBuilder.basic(funcName);
            asmBuilder.op2("addi","sp","sp","0");
            for (LLVMBasicBlockRef bb = LLVMGetFirstBasicBlock(value); bb != null; bb = LLVMGetNextBasicBlock(bb)) {
                String basicName = LLVMGetBasicBlockName(bb).getString();
                asmBuilder.basic(basicName);

                for (LLVMValueRef inst = LLVMGetFirstInstruction(bb); inst != null; inst = LLVMGetNextInstruction(inst)) {

                    int opcode = LLVMGetInstructionOpcode(inst);
                    int operandNum = LLVMGetNumOperands(inst);
                    LLVMValueRef op1 = LLVMGetOperand(inst, 0);
                    String op1Str = getOperandString(op1);
                    if(opcode== LLVMRet){
                        asmBuilder.op1("li","a0",op1Str);
                        asmBuilder.op2("addi","sp","sp","0");
                        asmBuilder.op1("li","a7","93");
                        asmBuilder.ecall();

                        continue;
                    }

                    String op = determineOpcode(opcode);
                    String dest = determineDestination(inst);

                    if (operandNum == 2) {
                        LLVMValueRef op2 = LLVMGetOperand(inst, 1);
                        String op2Str = getOperandString(op2);
                        asmBuilder.op2(op, dest, op1Str, op2Str);
                    }else if(operandNum == 1) {
                        asmBuilder.op1(op, dest, op1Str);
                    }else{

                    }
                }

            }
//            String asmCode = asmBuilder.getAsmCode();
//            System.out.println(asmCode);
            asmBuilder.writeTo(destPath);

        }
    }

    public static String getOperandString(LLVMValueRef operand) {
        if (!LLVMIsAConstantInt(operand).isNull()) {
            // 如果操作数是整数常数
            long value = LLVMConstIntGetSExtValue(operand);
            return Long.toString(value);  // 返回常数的值
        } else if (LLVMIsAGlobalValue(operand).isNull()) {
            // 如果操作数是全局值
            String name = String.valueOf(LLVMGetValueName(operand));  // 获取全局值的名称
            return name;  // 返回全局值的字符串表示形式
        } else {
            // 如果操作数是指令的结果
            String name = String.valueOf(LLVMGetValueName(operand));  // 获取指令的结果名称
            return name;  // 返回默认情况下的字符串表示形式
        }
    }

    public static String determineOpcode(int opcode) {
        switch (opcode) {
            case LLVMAdd:
                return "add";
            case LLVMSub:
                return "sub";
            case LLVMMul:
                return "mul";
            case LLVMSDiv:
                return "div";
            case LLVMStore:
                return "store";
            case LLVMLoad:
                return "load";
            // 添加更多操作码的处理
            default:
                return "unknown"; // 如果未识别的操作码，返回 "unknown"
        }
    }

    public static String determineDestination(LLVMValueRef inst) {
        int opcode = LLVMGetInstructionOpcode(inst);

        switch (opcode) {
            case LLVMAdd:
            case LLVMSub:
            case LLVMMul:
            case LLVMSDiv:
                // 对于二元运算指令，第一个操作数通常是目标操作数
                LLVMValueRef destOperand = LLVMGetOperand(inst, 0);
                return LLVMGetValueName(destOperand).getString();

            case LLVMStore:
                // 对于 store 指令，第二个操作数通常是目标操作数
                LLVMValueRef destStoreOperand = LLVMGetOperand(inst, 1);
                return LLVMGetValueName(destStoreOperand).getString();

            // 处理其他指令类型...

            default:
                return "unknown"; // 如果未识别的操作码，返回 "unknown"
        }
    }
}
