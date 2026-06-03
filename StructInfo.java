import java.util.LinkedHashMap;

public record StructInfo(
    String name,
    LinkedHashMap<String, VarType> fields,
    LinkedHashMap<String, MethodInfo> methods
) {
    public StructInfo(String name, LinkedHashMap<String, VarType> fields) {
        this(name, fields, new LinkedHashMap<>());
    }

    public String llvmName() {
        return String.format("%%struct.%s", name);
    }

    public int fieldIndex(String fieldName) {
        int i = 0;
        for (String key: fields.keySet()) {
            if (key.equals(fieldName)) return i;
            i++;
        }
        return -1;
    }

    public VarType fieldType(String fieldName) {
        VarType t = fields.get(fieldName);

        if (t == null)
            throw new RuntimeException(
                String.format(
                    "Struct '%s' has no '%s' field",
                    name, fieldName
                )
            );

        return t;
    }

    public MethodInfo method(String methodName) {
        return methods.get(methodName);
    }
}
