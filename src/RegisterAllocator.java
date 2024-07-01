public interface RegisterAllocator {
    void allocate(String variable);

    int getStackSize();

    int getStack(String variable);
}
