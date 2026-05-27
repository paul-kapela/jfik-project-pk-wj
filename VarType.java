public enum VarType {
    INT("i32"),
    REAL("float"),
    REALD("double"),
    STRING("i8*"),
    VOID("void");

    public final String llvmName;

    private VarType(String llvmName) {
        this.llvmName = llvmName;
    }

    public static VarType fromString(String s) {
        return switch (s.toLowerCase()) {
            case "int" -> INT;
            case "real" -> REAL;
            case "double" -> REALD;
            case "string" -> STRING;
            case "void" -> VOID;
            default -> throw new IllegalArgumentException("Unknown type: " + s);
        };
    }

    @Override
    public String toString() {
        return llvmName;
    }
}
