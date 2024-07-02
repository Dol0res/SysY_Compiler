import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef;
import org.bytedeco.llvm.LLVM.LLVMModuleRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;

public interface RegisterAllocator {
    int stackSize = 0;
    void allocate(String variable);

    int getStackSize();
    void init(LLVMBasicBlockRef bb);
    int getStack(String variable);
    void storeNew(String name);
    void storeNew(String name1,String name2);
    void loadNew(String name, int i);

    void op2(LLVMValueRef inst);

    void ret(String name);
}
