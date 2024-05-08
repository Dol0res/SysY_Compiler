public class ErrorType extends Type {
    private static final ErrorType instance = new ErrorType();
    public static ErrorType getErrorType() {
        return instance;
    }
}