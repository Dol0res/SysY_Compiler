import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef;
import org.bytedeco.llvm.LLVM.LLVMModuleRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;
import org.bytedeco.llvm.global.LLVM;

import java.util.HashMap;

import static org.bytedeco.llvm.global.LLVM.*;

public class MemoryOnlyAllocator implements RegisterAllocator {
    private int stackSize; // 当前函数需要的栈内存大小
    private HashMap<String, Integer> stackMap= new HashMap<>();
    public MemoryOnlyAllocator() {
        stackSize = 64;
    }
    // asmBuilder = new AsmBuilder();

    @Override
    public void allocate(String variable) {
        // 在完全使用内存的策略中，每次分配变量都增加栈空间大小
        stackSize -= 4; // 假设每个变量占用 4 字节的栈空间
        stackMap.put(variable,stackSize);
    }

    @Override
    public int getStackSize() {
        return stackSize;
    }

    @Override
    public void init(LLVMBasicBlockRef bb) {

    }


    @Override
    public int getStack(String variable) {
        Integer value = stackMap.get(variable);
        if (value == null) {
            return -1;
        }
        return value;
    }

    @Override
    public void storeNew(String name) {
        if(getStack(name)==-1) allocate(name);
        AsmBuilder.op1("sw", "t0" , getStack(name) + "(sp)");
    }

    @Override
    public void storeNew(String name1, String name2) {

    }

    @Override
    public void loadNew(String name,int i) {
        if(getStack(name)==-1) allocate(name);
        AsmBuilder.op1("lw", "t" + String.valueOf(i), getStack(name) + "(sp)");

    }

    @Override
    public void op2(LLVMValueRef inst) {
        int opcode = LLVMGetInstructionOpcode(inst);
        String op = determineOpcode(opcode);
        AsmBuilder.op2(op, "t0", "t0", "t1");
        String name = LLVMGetValueName(inst).getString();
        storeNew(name);
    }

    @Override
    public void ret(String name) {
        AsmBuilder.op1("lw", "a0", getStack(name) + "(sp)");

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
}
