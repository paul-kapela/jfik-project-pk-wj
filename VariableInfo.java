public record VariableInfo(String structName, VarType elementType, int size, boolean isArray) {
    public VariableInfo(VarType elementType, int size, boolean isArray) {
        this(null, elementType, size, isArray);
    }

    public boolean isStruct() {
        return elementType == VarType.STRUCT &&
            structName != null;
    }
}
