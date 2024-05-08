public class VoidType extends Type {
    private static final VoidType instance = new VoidType();
    public static VoidType getVoidType() {
        return instance;
    }
}
