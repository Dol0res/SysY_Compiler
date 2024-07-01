
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
        AsmBuilder asmBuilder = new AsmBuilder();
        RegisterAllocator allocator2 = new MemoryOnlyAllocator();
        //RegisterAllocator allocator2 = new LinearScanAllocator();
        int t = 0;
        for (LLVMValueRef value = LLVMGetFirstGlobal(module); value != null; value = LLVMGetNextGlobal(value)) {
            //...
            LLVMTypeRef valueType = LLVMTypeOf(value);
            String name = LLVMGetValueName(value).getString();
            LLVMValueRef op1 = LLVMGetOperand(value, 0);
            String op1Str = getOperandString(op1);
            asmBuilder.data(name, op1Str);
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
            asmBuilder.op2("addi", "sp", "sp", "0");
            for (LLVMBasicBlockRef bb = LLVMGetFirstBasicBlock(value); bb != null; bb = LLVMGetNextBasicBlock(bb)) {
                String basicName = LLVMGetBasicBlockName(bb).getString();
                asmBuilder.basic(basicName);

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
                            if (opcode == LLVMRet) asmBuilder.op1("li", "a0", op1Str);
                            else if (opcode != LLVMAlloca) asmBuilder.op1("li", "t" + String.valueOf(i), op1Str);

                        } else if (LLVMIsAGlobalValue(op1) != null) {
                            // 如果操作数是全局值
                            op1Str = LLVMGetValueName(op1).getString();
                            asmBuilder.op1("la", "t" + String.valueOf(i), op1Str);
                            if(i==0)asmBuilder.op1("lw", "t" + String.valueOf(i), "0(t"+String.valueOf(i)+")");
                            //else asmBuilder.op1("sw", "t" + String.valueOf(i), "0(t"+String.valueOf(i)+")");
                        } else {
                            if(opcode==LLVMStore&& i==1)break;

                            // 如果操作数是指令的结果
                            if (opcode == LLVMRet) {
                                //asmBuilder.op1("la","a0",op1Str);
                                asmBuilder.op1("lw", "a0", allocator2.getStack(name) + "(sp)");

                            } else asmBuilder.op1("lw", "t" + String.valueOf(i), allocator2.getStack(name) + "(sp)");

                        }
                    }

                    String name = LLVMGetValueName(LLVMGetOperand(inst, 0)).getString();
                    if (opcode == LLVMRet) {
                        //asmBuilder.op1("li","a0",op1Str);
                        asmBuilder.op2("addi", "sp", "sp", "0");
                        asmBuilder.op1("li", "a7", "93");
                        asmBuilder.ecall();

                        continue;
                    }
                    if (opcode == LLVMAlloca) {
                        name = LLVMGetValueName(inst).getString();
                        allocator2.allocate(name);
                        continue;
                    }

                    if (opcode == LLVMStore) {

                        if(LLVMIsAGlobalValue(LLVMGetOperand(inst, 1)) != null){
                            asmBuilder.op1("sw", "t0", "0(t1)");
                        }
                        else{
                            name = LLVMGetValueName(LLVMGetOperand(inst, 1)).getString();
                            if(allocator2.getStack(name)==-1) allocator2.allocate(name);

                            asmBuilder.op1("sw", "t0" , allocator2.getStack(name) + "(sp)");
                        }
                        continue;
                    }
                    if (opcode == LLVMLoad) {
                        //asmBuilder.op1("lw", "t0" , allocator2.getStackSize() + "(sp)");
                        name = LLVMGetValueName(inst).getString();
                        if(allocator2.getStack(name)==-1) allocator2.allocate(name);
//                        t++;
                        asmBuilder.op1("sw", "t0" , allocator2.getStack(name) + "(sp)");
                        continue;
                    }
                    String op = determineOpcode(opcode);
//                    String dest = determineDestination(inst);

                    if (operandNum == 2) {
                        asmBuilder.op2(op, "t0", "t0", "t1");
                        name = LLVMGetValueName(inst).getString();
                        if(allocator2.getStack(name)==-1) allocator2.allocate(name);

                        asmBuilder.op1("sw", "t0", allocator2.getStack(name) + "(sp)");
                    }
                    else if (operandNum == 1) {
                        asmBuilder.op1(op, "t0", "t0");
                    } else {

                    }
                }

            }
//            String asmCode = asmBuilder.getAsmCode();
//            System.out.println(asmCode);
            asmBuilder.writeTo(destPath);

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
                LLVMValueRef storedValue = LLVMGetOperand(inst, 0);

                // 如果存储的值是一个指针类型，则进一步获取它指向的名称
//                if (LLVMTypeOf(storedValue).getTypeKind() == LLVMPointerTypeKind) {
                LLVMValueRef pointedToValue = LLVMGetOperand(storedValue, 0);
                return LLVMGetValueName(pointedToValue).getString();
            //}

            // 如果存储的值不是指针类型，则直接返回其名称
            //return LLVMGetValueName(storedValue).getString();

            // 处理其他指令类型...

            default:
                return "unknown"; // 如果未识别的操作码，返回 "unknown"
        }
    }
}
