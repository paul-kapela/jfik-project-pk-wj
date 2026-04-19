public class Value {
    private VarType type;
    private ValueKind kind;
    private String value;

    public Value(VarType type, String value, ValueKind kind) {
        this.type = type;
        this.value = value;
        this.kind = kind;
    }

    public Value(VarType type, String value) {
        this(type, value, ValueKind.LITERAL);
    }

    public VarType type() {
        return type;
    }

    public String value() {
        return value;
    }

    public ValueKind kind() {
        return kind;
    }

}
