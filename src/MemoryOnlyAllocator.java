import java.util.HashMap;

public class MemoryOnlyAllocator implements RegisterAllocator {
    private int stackSize; // 当前函数需要的栈内存大小
    private HashMap<String, Integer> stackMap= new HashMap<>();
    public MemoryOnlyAllocator() {
        stackSize = 64;
    }

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
    public int getStack(String variable) {
        Integer value = stackMap.get(variable);
        if (value == null) {
            return -1;
        }
        return value;
    }
}
