import java.util.HashSet;

enum VarType {
    INT,
    REAL,
    REALD
}

class Value {
    VarType type;
    String value;

    public Value(VarType type, String value) {
        this.type = type;
        this.value = value;
    }
}

public class LLVMActions extends ProjektBaseListener {
    HashSet<String> variables = new HashSet<String>();
    
    @Override
    public void exitCode(ProjektParser.CodeContext ctx) {
        System.out.println(LLVMGenerator.generate());
    }
}