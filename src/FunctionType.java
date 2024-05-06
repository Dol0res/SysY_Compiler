import java.util.ArrayList;

public class FunctionType extends Type {
    Type retTy;
    ArrayList<Type> paramsType;
    //···
    public FunctionType(Type retTy, ArrayList<Type> paramsType) {
        this.retTy = retTy;
        this.paramsType = paramsType;
    }

    public Type getRetTy() {
        return retTy;
    }

    public ArrayList<Type> getParamsType() {
        return paramsType;
    }
}
