import java.util.HashMap;
import java.util.Stack;
import java.util.ArrayList;
import java.util.List;

public class LLVMActions extends ProjektBaseListener {
    HashMap<String, VariableInfo> variables = new HashMap<>();
    Stack<Value> valuesStack = new Stack<>();
    
    // To store elements of array literals temporarily
    HashMap<String, List<Value>> arrayLiterals = new HashMap<>();
    int literalId = 0;

    @Override
    public void exitCode(ProjektParser.CodeContext ctx) {
        System.out.println(LLVMGenerator.generate());
    }

    @Override
    public void exitInt(ProjektParser.IntContext ctx) {
        valuesStack.push(new Value(VarType.INT, ctx.INT().getText()));
    }

    @Override
    public void exitReal(ProjektParser.RealContext ctx) {
        valuesStack.push(new Value(VarType.REAL, ctx.REAL().getText()));
    }

    @Override
    public void exitId(ProjektParser.IdContext ctx) {
        String id = ctx.ID().getText();

        if (variables.containsKey(id)) {
            VariableInfo info = variables.get(id);
            if (info.isArray()) {
                valuesStack.push(new Value(info.elementType(), id, ValueKind.ARRAY_VARIABLE));
            } else {
                valuesStack.push(new Value(info.elementType(), id, ValueKind.VARIABLE));
            }
        } else {
            System.err.println(String.format("Error: unknown variable %s", id));
            System.exit(1);
        }
    }

    @Override
    public void exitAssign(ProjektParser.AssignContext ctx) {
        Value val = valuesStack.pop();
        
        if (ctx.lvalue() instanceof ProjektParser.IdLvalContext idLval) {
            String id = idLval.ID().getText();
            
            if (variables.containsKey(id)) {
                VariableInfo info = variables.get(id);
                if (info.isArray()) {
                    System.err.println("Error: cannot assign scalar to array variable " + id);
                    System.exit(1);
                }
                if (info.elementType() != val.type()) {
                    System.err.println("Type mismatch for variable " + id);
                    System.exit(1);
                }
            } else {
                if (val.kind() == ValueKind.ARRAY_LITERAL) {
                    String litId = val.value();
                    List<Value> elements = arrayLiterals.get(litId);
                    VarType type = elements.isEmpty() ? VarType.INT : elements.get(0).type();
                    int size = elements.size();
                    
                    variables.put(id, new VariableInfo(type, size, true));
                    LLVMGenerator.declareArray(id, type, size);
                    
                    for (int i = 0; i < size; i++) {
                        LLVMGenerator.storeArrayElement(id, new Value(VarType.INT, String.valueOf(i)), elements.get(i), size);
                    }
                } else if (val.kind() == ValueKind.REGISTER && val.value().startsWith("INIT_")) {
                    String[] parts = val.value().split("_");
                    // The key is "INIT_VarType_Size"
                    // VarType.toString() returns the LLVM name (e.g., "i32")
                    // We need to map it back to the enum.
                    VarType type = null;
                    String typeName = parts[1];
                    if (typeName.equals("i32")) type = VarType.INT;
                    else if (typeName.equals("float")) type = VarType.REAL;
                    else if (typeName.equals("double")) type = VarType.REALD;
                    else if (typeName.equals("i8*")) type = VarType.STRING;
                    
                    int size = Integer.parseInt(parts[2]);
                    
                    variables.put(id, new VariableInfo(type, size, true));
                    LLVMGenerator.declareArray(id, type, size);
                    
                    for (int i = 0; i < size; i++) {
                       Value defVal = new Value(type, "0");
                       LLVMGenerator.storeArrayElement(id, new Value(VarType.INT, String.valueOf(i)), defVal, size);
                    }
                } else {
                    variables.put(id, new VariableInfo(val.type(), 1, false));
                    LLVMGenerator.declare(id, val.type());
                    LLVMGenerator.assign(id, val);
                }
            }
        } else if (ctx.lvalue() instanceof ProjektParser.IndexLvalContext indexLval) {
            String id = indexLval.ID().getText();
            Value index = valuesStack.pop();
            
            if (!variables.containsKey(id)) {
                System.err.println("Error: unknown array " + id);
                System.exit(1);
            }
            VariableInfo info = variables.get(id);
            if (!info.isArray()) {
                System.err.println("Error: " + id + " is not an array");
                System.exit(1);
            }
            
            LLVMGenerator.storeArrayElement(id, index, val, info.size());
        }
    }

    @Override
    public void exitArrayLit(ProjektParser.ArrayLitContext ctx) {
        if (ctx.arrayLiteral() instanceof ProjektParser.LitContext litCtx) {
            List<Value> elements = new ArrayList<>();
            int numExprs = litCtx.expr().size();
            for (int i = 0; i < numExprs; i++) {
                elements.add(0, valuesStack.pop());
            }
            
            String lid = "LIT_" + (literalId++);
            arrayLiterals.put(lid, elements);
            VarType type = elements.isEmpty() ? VarType.INT : elements.get(0).type();
            valuesStack.push(new Value(type, lid, ValueKind.ARRAY_LITERAL));
        }
    }

    @Override
    public void exitArrayInitVal(ProjektParser.ArrayInitValContext ctx) {
        if (ctx.arrayInit() instanceof ProjektParser.InitContext initCtx) {
            String typeStr = initCtx.type().getText();
            VarType type = VarType.fromString(typeStr);
            Value sizeValue = valuesStack.pop();
            
            int sizeVal = Integer.parseInt(sizeValue.value());
            String initKey = "INIT_" + type + "_" + sizeVal;
            valuesStack.push(new Value(type, initKey, ValueKind.REGISTER));
        }
    }

    @Override
    public void exitIndexRval(ProjektParser.IndexRvalContext ctx) {
        String id = ctx.ID().getText();
        Value index = valuesStack.pop();

        if (!variables.containsKey(id)) {
            System.err.println("Error: unknown array " + id);
            System.exit(1);
        }
        VariableInfo info = variables.get(id);
        if (!info.isArray()) {
            System.err.println("Error: " + id + " is not an array");
            System.exit(1);
        }

        valuesStack.push(LLVMGenerator.loadArrayElement(id, index, info.size(), info.elementType()));
    }

    @Override
    public void exitRead(ProjektParser.ReadContext ctx) {
        LLVMGenerator.read(valuesStack.pop());
    }

    @Override
    public void exitWrite(ProjektParser.WriteContext ctx) {
        Value v = valuesStack.pop();
        if (v.kind() == ValueKind.ARRAY_VARIABLE) {
            String id = v.value();
            VariableInfo info = variables.get(id);
            LLVMGenerator.writeArray(v, info.size(), info.elementType());
        } else if (v.kind() == ValueKind.ARRAY_LITERAL) {
            String litId = v.value();
            List<Value> elements = arrayLiterals.get(litId);
            VarType type = elements.isEmpty() ? VarType.INT : elements.get(0).type();
            int size = elements.size();
            
            // To print a literal as an array, we need it to actually be in memory 
            // because writeArray uses loadArrayElement.
            // We create a temporary hidden array for this.
            String tempId = "tmp_arr_" + literalId; // This needs a counter
            LLVMGenerator.declareArray(tempId, type, size);
            for (int i = 0; i < size; i++) {
                LLVMGenerator.storeArrayElement(tempId, new Value(VarType.INT, String.valueOf(i)), elements.get(i), size);
            }
            Value tempVal = new Value(type, tempId, ValueKind.ARRAY_VARIABLE);
            LLVMGenerator.writeArray(tempVal, size, type);
        } else {
            LLVMGenerator.write(v);
        }
    }

    @Override
    public void exitAdd(ProjektParser.AddContext ctx) {
        Value b = valuesStack.pop();
        Value a = valuesStack.pop();
        valuesStack.push(LLVMGenerator.add(a, b));
    }

    @Override
    public void exitSub(ProjektParser.SubContext ctx) {
        Value b = valuesStack.pop();
        Value a = valuesStack.pop();
        valuesStack.push(LLVMGenerator.sub(a, b));
    }

    @Override
    public void exitMul(ProjektParser.MulContext ctx) {
        Value b = valuesStack.pop();
        Value a = valuesStack.pop();
        valuesStack.push(LLVMGenerator.mul(a, b));
    }

    @Override
    public void exitDiv(ProjektParser.DivContext ctx) {
        Value b = valuesStack.pop();
        Value a = valuesStack.pop();
        valuesStack.push(LLVMGenerator.div(a, b));
    }

    @Override
    public void exitNeg(ProjektParser.NegContext ctx) {
        valuesStack.push(LLVMGenerator.neg(valuesStack.pop()));
    }

    @Override
    public void exitString(ProjektParser.StringContext ctx) {
        String str = ctx.STRING().getText();
        str = str.substring(1, str.length() - 1);
        String name = LLVMGenerator.createString(str);
        valuesStack.push(new Value(VarType.STRING, name, ValueKind.REGISTER));
    }
}
