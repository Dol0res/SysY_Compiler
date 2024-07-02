import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public  class AsmBuilder {
    static StringBuffer buffer= new StringBuffer();;


    public static void op2(String op, String dest, String lhs, String rhs) {
        buffer.append(String.format("  %s %s, %s, %s\n", op, dest, lhs, rhs));
    }
    public static void op1(String op, String dest, String lhs) {
        buffer.append(String.format("  %s %s, %s\n", op, dest, lhs));
    }
    public static void basic(String basicName) {
        buffer.append(String.format("%s:\n", basicName));
    }
    public static void globl(String name) {
        buffer.append(String.format("  .globl %s\n", name));
    }
//    public void alloc(String name) {
//
//        buffer.append(String.format("  .globl %s\n", name));
//    }
    public static void text(String functionName) {
        buffer.append("  .text\n");
//        buffer.append(".align 2\n");
//        buffer.append(functionName).append(":\n");
    }
    public static void data(String name, String num) {
        buffer.append("  .data\n");
        buffer.append(String.format("%s:\n", name));
        buffer.append(String.format("  .word %s\n", num));
        buffer.append("\n");
    }
    //...
    public static String getAsmCode() {
        return buffer.toString();
    }
    public void writeTo(String destPath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(destPath))) {
            writer.write(buffer.toString());
        } catch (IOException e) {
            e.printStackTrace(); // Handle exception properly in your application
        }
    }
    public static void ecall() {
        buffer.append("  ecall\n");
    }
}
