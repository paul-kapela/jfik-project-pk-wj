public enum VarType {
    INT("i32"),
    REAL("float"),
    REALD("double"),
    STRING("i8*");

    public final String llvmName;

    private VarType(String llvmName) {
        this.llvmName = llvmName;
    }

    @Override
    public String toString() {
        return llvmName;
    }
}
