// IntType.java
public class IntType extends Type {
    private static IntType instance = new IntType();

    private IntType() {}

    public static IntType getI32() {
        return instance;
    }

//    @Override
//    public String getTypeName() {
//        return "int";
//    }
}
