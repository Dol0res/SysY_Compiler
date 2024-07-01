public class LinearScanAllocator implements RegisterAllocator {
    // 实现线性扫描算法需要的具体逻辑，这里只是示例，实际实现可能更复杂
    @Override
    public void allocate(String variable) {
        // 实现线性扫描算法的寄存器分配策略
        System.out.println("Allocating register for variable: " + variable);
    }

    @Override
    public int getStackSize() {
        return 0; // 线性扫描算法可能不需要栈空间大小
    }

    @Override
    public int getStack(String variable) {
        return 0;
    }
}
