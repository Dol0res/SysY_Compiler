import org.bytedeco.llvm.LLVM.LLVMValueRef;

import java.util.HashMap;
import java.util.Map;

public class Scope {
    private Scope parentScope;
    private Map<String, LLVMValueRef> symbols;

    public Scope(Scope parentScope) {
        this.parentScope = parentScope;
        this.symbols = new HashMap<>();
    }

    public Scope getParentScope() {
        return parentScope;
    }

    public void define(String name, LLVMValueRef type) {
        symbols.put(name, type);
    }

    public LLVMValueRef find(String name) {
        LLVMValueRef symbol = symbols.get(name);
        if (symbol != null) {
            return symbol;
        } else if (parentScope != null) {
            return parentScope.find(name);
        } else {
            return null;
        }
    }
    public LLVMValueRef findCurrent(String name) {
        LLVMValueRef symbol = symbols.get(name);
        if (symbol != null) {
            return symbol;
        }
        return null;
    }

    public void put(String varName, LLVMValueRef type) {
        this.symbols.put(varName, type);
    }
}
