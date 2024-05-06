import java.util.HashMap;
import java.util.Map;

public class Scope {
    private Scope parentScope;
    private Map<String, Type> symbols;

    public Scope(Scope parentScope) {
        this.parentScope = parentScope;
        this.symbols = new HashMap<>();
    }

    public Scope getParentScope() {
        return parentScope;
    }

    public void define(String name, Type type) {
        symbols.put(name, type);
    }

    public Type find(String name) {
        Type symbol = symbols.get(name);
        if (symbol != null) {
            return symbol;
        } else if (parentScope != null) {
            return parentScope.find(name);
        } else {
            return null;
        }
    }

    public void put(String varName, Type type) {
        this.symbols.put(varName, type);
    }
}
