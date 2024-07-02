
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
        if (LLVMPrintModuleToFile(llvmIRVisitor.getModule(), destPath, error) != 0) {
            LLVMDisposeMessage(error);
        }
        //AsmBuilder AsmBuilder;
        //RegisterAllocator allocator2 = new MemoryOnlyAllocator();
        RegisterAllocator allocator2 = new LinearScanAllocator();
        int t = 0;
        for (LLVMValueRef value = LLVMGetFirstGlobal(module); value != null; value = LLVMGetNextGlobal(value)) {
            //...
            LLVMTypeRef valueType = LLVMTypeOf(value);
            String name = LLVMGetValueName(value).getString();
            LLVMValueRef op1 = LLVMGetOperand(value, 0);
            String op1Str = getOperandString(op1);
            AsmBuilder.data(name, op1Str);
            // 检查全局值是否是函数类型
            if (LLVMGetValueKind(value) == LLVMFunctionValueKind) {
                // 将全局值转换为函数类型

            }
        }
        AsmBuilder.text("");


        for (LLVMValueRef value = LLVMGetFirstFunction(module); value != null; value = LLVMGetNextFunction(value)) {
            // 获取函数名
            String funcName = LLVMGetValueName(value).getString();
            AsmBuilder.globl(funcName);
            AsmBuilder.basic(funcName);
            AsmBuilder.op2("addi", "sp", "sp", String.valueOf(allocator2.getStackSize()));
            for (LLVMBasicBlockRef bb = LLVMGetFirstBasicBlock(value); bb != null; bb = LLVMGetNextBasicBlock(bb)) {
                String basicName = LLVMGetBasicBlockName(bb).getString();
                AsmBuilder.basic(basicName);
                allocator2.init(bb);
                for (LLVMValueRef inst = LLVMGetFirstInstruction(bb); inst != null; inst = LLVMGetNextInstruction(inst)) {

                    int opcode = LLVMGetInstructionOpcode(inst);
                    int operandNum = LLVMGetNumOperands(inst);
                    for(int i=0;i<operandNum;i++) {
                        LLVMValueRef op1 = LLVMGetOperand(inst, i);
                        String name = LLVMGetValueName(LLVMGetOperand(inst, i)).getString();
                        String op1Str = new String();

                        if (LLVMIsAConstantInt(op1) != null) {
                            // 如果操作数是整数常数

                            long v = LLVMConstIntGetSExtValue(op1);
                            op1Str = Long.toString(v);  // 返回常数的值
                            if (opcode == LLVMRet) AsmBuilder.op1("li", "a0", op1Str);
                            else if (opcode != LLVMAlloca) AsmBuilder.op1("li", "t" + String.valueOf(i), op1Str);

                        } else if (LLVMIsAGlobalValue(op1) != null) {
                            // 如果操作数是全局值
                            op1Str = LLVMGetValueName(op1).getString();
                            AsmBuilder.op1("la", "t2", op1Str);
                            if(i==0)AsmBuilder.op1("lw", "t" + String.valueOf(i), "0(t2)");
                        } else {
                            if(opcode==LLVMStore&& i==1)break;

                            // 如果操作数是指令的结果
                            if (opcode == LLVMRet) {
                                allocator2.ret(name);

                            } else allocator2.loadNew(name,i);
                        }
                    }

                    String name = LLVMGetValueName(LLVMGetOperand(inst, 0)).getString();
                    if (opcode == LLVMRet) {
                        //AsmBuilder.op1("li","a0",op1Str);
                        AsmBuilder.op2("addi", "sp", "sp", "0");
                        AsmBuilder.op1("li", "a7", "93");
                        AsmBuilder.ecall();

                        continue;
                    }
                    if (opcode == LLVMAlloca) {
                        name = LLVMGetValueName(inst).getString();
                        allocator2.allocate(name);
                        continue;
                    }

                    if (opcode == LLVMStore) {

                        if(LLVMIsAGlobalValue(LLVMGetOperand(inst, 1)) != null){
                            AsmBuilder.op1("sw", "t0", "0(t2)");
                        }
                        else{
                            name = LLVMGetValueName(LLVMGetOperand(inst, 1)).getString();
                            //allocator2.storeNew(name);
                            allocator2.storeNew(LLVMGetValueName(LLVMGetOperand(inst, 0)).getString(),name);

                        }
                        continue;
                    }
                    if (opcode == LLVMLoad) {
                        name = LLVMGetValueName(inst).getString();
                        if(LLVMIsAGlobalValue(LLVMGetOperand(inst, 0))==null) allocator2.storeNew(LLVMGetValueName(LLVMGetOperand(inst, 0)).getString(), name);
                        else allocator2.storeNew(name);
                        continue;
                    }

                    if (operandNum == 2) {
                        allocator2.op2(inst);
                    }
                }

            }
//            String asmCode = AsmBuilder.getAsmCode();
//            System.out.println(asmCode);
            AsmBuilder.writeTo(destPath);

        }
    }

    public static String getOperandString(LLVMValueRef operand) {
        if (LLVMIsAConstantInt(operand) != null) {
            // 如果操作数是整数常数
            long value = LLVMConstIntGetSExtValue(operand);
            return Long.toString(value);  // 返回常数的值
        } else if (LLVMIsAGlobalValue(operand) != null) {
            // 如果操作数是全局值
            String name = LLVMGetValueName(operand).getString();  // 获取全局值的名称
            return name;  // 返回全局值的字符串表示形式
        } else {
            // 如果操作数是指令的结果
            String name = LLVMGetValueName(operand).getString();  // 获取指令的结果名称
            return name;  // 返回默认情况下的字符串表示形式
        }
    }

}
