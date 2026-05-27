public record VariableInfo(
    VarType elementType,
    int size,
    boolean isArray,
    boolean isParameter
) {
    public VariableInfo(VarType elementType, int size, boolean isArray) {
        this(elementType, size, isArray, false);
    }
}
