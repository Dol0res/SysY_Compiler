import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class AsmBuilder {
    StringBuffer buffer;

    public AsmBuilder() {
        buffer = new StringBuffer(); // Initialize StringBuffer
    }
    public void op2(String op, String dest, String lhs, String rhs) {
        buffer.append(String.format("  %s %s, %s, %s\n", op, dest, lhs, rhs));
    }
    public void op1(String op, String dest, String lhs) {
        buffer.append(String.format("  %s %s, %s\n", op, dest, lhs));
    }
    public void basic(String basicName) {
        buffer.append(String.format("%s:\n", basicName));
    }
    public void globl(String name) {
        buffer.append(String.format("  .globl %s\n", name));
    }
    public void text(String functionName) {
        buffer.append("  .text\n");
//        buffer.append(".align 2\n");
//        buffer.append(functionName).append(":\n");
    }

    //...
    public String getAsmCode() {
        return buffer.toString();
    }
    public void writeTo(String destPath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(destPath))) {
            writer.write(buffer.toString());
        } catch (IOException e) {
            e.printStackTrace(); // Handle exception properly in your application
        }
    }
    public void ecall() {
        buffer.append("  ecall\n");
    }
}
