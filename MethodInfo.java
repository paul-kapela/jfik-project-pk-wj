import java.util.List;

public record MethodInfo(
    VarType returnType,
    List<String> paramNames,
    List<VarType> paramTypes
) {
    public String llvmName(String structName, String methodName) {
        return structName + "_" + methodName;
    }
}
