public class ArrayType extends Type {
    private Type contained; // type of its elements, may be int or array
    private int num_elements;

    public ArrayType(Type contained, int num_elements) {
        this.contained = contained;
        this.num_elements = num_elements;

    }
    public Type getContained() {
        return contained;
    }
    public int getNum_elements() {
        return num_elements;
    }
}

