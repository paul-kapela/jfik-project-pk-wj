public record VariableInfo(
    String structName,
    VarType elementType,
    int size,
    boolean isArray,
    boolean isParameter
) {
    public VariableInfo(VarType elementType, int size, boolean isArray) {
        this(null, elementType, size, isArray, false);
    }

    public VariableInfo(VarType elementType, int size, boolean isArray, boolean isParameter) {
        this(null, elementType, size, isArray, isParameter);
    }

    public VariableInfo(String structName, VarType elementType, int size, boolean isArray) {
        this(structName, elementType, size, isArray, false);
    }

    public boolean isStruct() {
        return elementType == VarType.STRUCT && structName != null;
    }
}
