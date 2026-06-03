import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Stack;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LLVMActions extends ProjektBaseListener {
    HashMap<String, VariableInfo> variables = new HashMap<>();
    HashMap<String, StructInfo> structs = new HashMap<>();
    Map<String, VariableInfo> locals = new HashMap<>();

    Stack<Value> valuesStack = new Stack<>();

    // To store elements of array literals temporarily
    HashMap<String, List<Value>> arrayLiterals = new HashMap<>();
    int literalId = 0;

    String currentForVar;

    String currentStructName;
    LinkedHashMap<String, VarType> currentStructFields;
    LinkedHashMap<String, MethodInfo> currentStructMethods;

    HashMap<String, VarType> functions = new HashMap<>();

    private VariableInfo getVariableInfo(String id) {
        boolean isInFunction = LLVMGenerator.isInFunction();
        if (isInFunction && locals.containsKey(id)) {
            return locals.get(id);
        } else if (!isInFunction && variables.containsKey(id)) {
            return variables.get(id);
        } else {
            return null;
        }
    }

    @Override
    public void exitCode(ProjektParser.CodeContext ctx) {
        System.out.println(LLVMGenerator.generate());
    }

    @Override
    public void enterStructDeclaration(ProjektParser.StructDeclarationContext ctx) {
        String structName = ctx.ID().getText();

        if (structs.containsKey(structName)) {
            System.err.printf(
                "Error: struct '%s' is already defined\n",
                structName
            );
            System.exit(1);
        }

        if (variables.containsKey(structName)) {
            System.err.printf(
                "Error: '%s' is already defined as a variable; cannot use it as a struct type name\n",
                structName
            );
            System.exit(1);
        }

        currentStructName = structName;
        currentStructFields = new LinkedHashMap<>();
        currentStructMethods = new LinkedHashMap<>();
    }

    @Override
    public void exitField(ProjektParser.FieldContext ctx) {
        String fieldName = ctx.ID().getText();
        VarType fieldType = VarType.fromString(ctx.type().getText());

        if (currentStructFields.containsKey(fieldName)) {
            System.err.printf(
                "Error: duplicate field '%s' in struct '%s'\n",
                fieldName, currentStructName
            );
            System.exit(1);
        }

        currentStructFields.put(fieldName, fieldType);
    }

    @Override
    public void exitStructDeclaration(ProjektParser.StructDeclarationContext ctx) {
        StructInfo info = new StructInfo(
            currentStructName,
            currentStructFields,
            currentStructMethods
        );
        structs.put(currentStructName, info);

        LLVMGenerator.declareStruct(
            currentStructName,
            new ArrayList<>(currentStructFields.values())
        );

        currentStructName = null;
        currentStructFields = null;
        currentStructMethods = null;
    }

    private VarType parseFunType(ProjektParser.FunTypeContext ctx) {
        if (ctx instanceof ProjektParser.VoidTypeContext) {
            return VarType.VOID;
        }
        return VarType.fromString(ctx.getText());
    }

    @Override
    public void enterStructMethod(ProjektParser.StructMethodContext ctx) {
        String methodName = ctx.ID().getText();
        VarType returnType = parseFunType(ctx.funType());

        if (currentStructMethods.containsKey(methodName)) {
            System.err.printf(
                "Error: duplicate method '%s' in struct '%s'\n",
                methodName, currentStructName
            );
            System.exit(1);
        }

        List<String> names = new ArrayList<>();
        List<VarType> types = new ArrayList<>();

        if (ctx.paramList() != null) {
            for (ProjektParser.ParamContext p : ctx.paramList().param()) {
                names.add(p.ID().getText());
                types.add(VarType.fromString(p.type().getText()));
            }
        }

        currentStructMethods.put(methodName, new MethodInfo(returnType, names, types));

        locals = new HashMap<>();
        for (int i = 0; i < names.size(); i++) {
            locals.put(names.get(i), new VariableInfo(types.get(i), 1, false, true));
        }

        LLVMGenerator.startMethod(currentStructName, methodName, returnType, names, types);
    }

    @Override
    public void exitStructMethod(ProjektParser.StructMethodContext ctx) {
        LLVMGenerator.endFunction();
        locals.clear();
    }

    private StructInfo currentStructInfo() {
        return new StructInfo(
            currentStructName,
            currentStructFields,
            currentStructMethods
        );
    }

    private void handleThisFieldAssign(String fieldName, Value val) {
        if (currentStructName == null) {
            System.err.println("Error: 'this' field assignment outside struct method");
            System.exit(1);
        }

        StructInfo structInfo = currentStructInfo();
        int fieldIdx = structInfo.fieldIndex(fieldName);
        if (fieldIdx < 0) {
            System.err.printf(
                "Error: struct '%s' has no field '%s'\n",
                currentStructName, fieldName
            );
            System.exit(1);
        }

        VarType fieldType = structInfo.fieldType(fieldName);
        val = coerce(val, fieldType, "this." + fieldName);
        LLVMGenerator.storeFieldThis(currentStructName, fieldIdx, val, fieldType);
    }

    private void handleFieldAssign(String varName, String fieldName, Value val) {
        VariableInfo varInfo = getVariableInfo(varName);

        if (varInfo == null) {
            System.err.printf(
                "Error: unknown variable '%s'\n",
                varName
            );
            System.exit(1);
        }

        if (!varInfo.isStruct()) {
            System.err.printf(
                "Error: '%s' is not a struct\n",
                varName
            );
            System.exit(1);
        }

        StructInfo structInfo = structs.get(varInfo.structName());

        if (structInfo == null) {
            System.err.printf(
                "Error: unknown struct type '%s'\n",
                varInfo.structName()
            );
            System.exit(1);
        }

        int fieldIdx = structInfo.fieldIndex(fieldName);

        if (fieldIdx < 0) {
            System.err.printf("Error: struct '%s' has no field '%s'\n", varInfo.structName(), fieldName);
            System.exit(1);
        }

        VarType fieldType = structInfo.fieldType(fieldName);

        val = coerce(val, fieldType, varName + "." + fieldName);

        LLVMGenerator.storeField(varName, varInfo.structName(), fieldIdx, val, fieldType);
    }

    private Value coerce(Value val, VarType target, String context) {
        if (val.type() == target)
            return val;

        if (val.type() == VarType.INT && (target == VarType.REAL || target == VarType.REALD)) {
            return LLVMGenerator.castValue(val, target);
        }

        System.err.printf(
            "Error: type mismatch at '%s': expected %s, got %s\n",
            context, target, val.type()
        );
        System.exit(1);

        return null;
    }

    @Override
    public void enterIf(ProjektParser.IfContext ctx) {
        LLVMGenerator.ifBegin();
    }

    @Override
    public void exitIf(ProjektParser.IfContext ctx) {
        LLVMGenerator.ifEnd(ctx.elseClause() != null);
    }

    @Override
    public void enterBlockIf(ProjektParser.BlockIfContext ctx) {
        if (!(ctx.getParent() instanceof ProjektParser.ElseClauseContext)) {
            LLVMGenerator.ifBranch();
        }
    }

    @Override
    public void exitBlockIf(ProjektParser.BlockIfContext ctx) {
        LLVMGenerator.blockIfEnd();
    }

    @Override
    public void enterElseIfClause(ProjektParser.ElseIfClauseContext ctx) {
        LLVMGenerator.elseIfNext();
    }

    @Override
    public void enterElseClause(ProjektParser.ElseClauseContext ctx) {
        LLVMGenerator.elseBegin();
    }

    @Override
    public void exitCond(ProjektParser.CondContext ctx) {
        Value right = valuesStack.pop();
        Value left = valuesStack.pop();

        String op = switch (ctx.condOp().getText()) {
            case "==" -> "eq";
            case "!=" -> "ne";
            case "<" -> "slt";
            case ">" -> "sgt";
            case "<=" -> "sle";
            case ">=" -> "sge";
            default -> "eq";
        };


        Value result = LLVMGenerator.icmp(left, right, op);

        if (ctx.getParent() instanceof ProjektParser.WhileLoopContext) {
            LLVMGenerator.whileCond(result);
        }
    }

    @Override
    public void enterWhileLoop(ProjektParser.WhileLoopContext ctx) {
        LLVMGenerator.whileBegin();
    }

    @Override
    public void exitWhileLoop(ProjektParser.WhileLoopContext ctx) {
        LLVMGenerator.whileEnd();
    }

    @Override
    public void exitForHeader(ProjektParser.ForHeaderContext ctx) {
        String var = ctx.ID().getText();

        Value end = valuesStack.pop();
        Value start = valuesStack.pop();

        variables.put(var, new VariableInfo(VarType.INT, 1, false));
        LLVMGenerator.declare(var, VarType.INT);
        LLVMGenerator.forBegin(var, start, end);

        currentForVar = var;
    }

    @Override
    public void exitForLoop(ProjektParser.ForLoopContext ctx) {
        LLVMGenerator.forInc(currentForVar);
        LLVMGenerator.forEnd();

        currentForVar = null;
    }

    @Override
    public void exitInt(ProjektParser.IntContext ctx) {
        valuesStack.push(new Value(VarType.INT, ctx.INT().getText()));
    }

    @Override
    public void exitReal(ProjektParser.RealContext ctx) {
        String text = ctx.REAL().getText();
        text = text.substring(0, text.length() - 1);

        float value = Float.parseFloat(text);

        if (Float.isInfinite(value) || Float.isNaN(value)) {
            System.err.printf(
                "Error line %d: invalid float literal '%s'\n",
                ctx.getStart().getLine(), text
            );
            System.exit(1);
        }

        valuesStack.push(
            new Value(VarType.REAL, LLVMGenerator.llvmFloatConstant(value))
        );
    }

    @Override
    public void exitReald(ProjektParser.RealdContext ctx) {
        String text = ctx.REALD().getText();

        double value = Double.parseDouble(text);

        if (Double.isInfinite(value) || Double.isNaN(value)) {
            System.err.printf("Error line %d: invalid double literal '%s'\n", ctx.getStart().getLine(), text);
            System.exit(1);
        }

        valuesStack.push(new Value(VarType.REALD, text));
    }

    @Override
    public void exitId(ProjektParser.IdContext ctx) {
        String id = ctx.ID().getText();

        if (structs.containsKey(id)) {
            valuesStack.push(new Value(VarType.STRUCT, "STRUCT_" + id, ValueKind.REGISTER));
            return;
        }

        VariableInfo info = getVariableInfo(id);

        if (info != null) {
            if (info.isArray()) {
                valuesStack.push(new Value(info.elementType(), id, ValueKind.ARRAY_VARIABLE));
            } else if (info.isStruct()) {
                valuesStack.push(new Value(VarType.STRUCT, id, ValueKind.VARIABLE));
            } else if (info.isParameter()) {
                valuesStack.push(new Value(info.elementType(), id, ValueKind.PARAMETER));
            } else {
                valuesStack.push(new Value(info.elementType(), id, ValueKind.VARIABLE));
            }
        } else {
            System.err.printf("Error: unknown variable %s\n", id);
            System.exit(1);
        }
    }

    @Override
    public void exitAssign(ProjektParser.AssignContext ctx) {
        Value val = valuesStack.pop();
        boolean isInFunction = LLVMGenerator.isInFunction();

        if (ctx.lvalue() instanceof ProjektParser.FieldLvalContext fieldLval) {
            String varName = fieldLval.ID(0).getText();
            String fieldName = fieldLval.ID(1).getText();

            handleFieldAssign(varName, fieldName, val);

            return;
        }

        if (ctx.lvalue() instanceof ProjektParser.ThisFieldLvalContext thisFieldLval) {
            handleThisFieldAssign(thisFieldLval.ID().getText(), val);
            return;
        }

        if (ctx.lvalue() instanceof ProjektParser.IdLvalContext idLval) {
            String id = idLval.ID().getText();
            VariableInfo info = getVariableInfo(id);

            if (info != null) {
                if (info.isStruct()) {
                    System.err.printf(
                        "Error: cannot reassign struct variable '%s' as a whole; use field assignments\n",
                        id
                    );
                    System.exit(1);
                }

                if (info.isArray()) {
                    System.err.println("Error: cannot assign scalar to array variable " + id);
                    System.exit(1);
                }

                if (info.elementType() != val.type()) {
                    System.err.println("Type mismatch for variable " + id);
                    System.exit(1);
                }

                LLVMGenerator.assign(id, val);
            } else {
                if (structs.containsKey(id)) {
                    System.err.printf(
                        "Error: '%s' is already defined as a struct type; cannot use it as a variable name\n",
                        id
                    );
                    System.exit(1);
                }

                if (val.kind() == ValueKind.ARRAY_LITERAL) {
                    String litId = val.value();
                    List<Value> elements = arrayLiterals.get(litId);
                    VarType type = elements.isEmpty() ? VarType.INT : elements.get(0).type();
                    int size = elements.size();
                    
                    if (isInFunction) {
                        locals.put(id, new VariableInfo(type, size, true));
                    } else {
                        variables.put(id, new VariableInfo(type, size, true));
                    }
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
                    if (isInFunction) {
                        locals.put(id, new VariableInfo(type, size, true));
                    } else {
                        variables.put(id, new VariableInfo(type, size, true));
                    }
                    LLVMGenerator.declareArray(id, type, size);

                    for (int i = 0; i < size; i++) {
                       Value defVal = new Value(type, "0");
                       LLVMGenerator.storeArrayElement(id, new Value(VarType.INT, String.valueOf(i)), defVal, size);
                    }
                } else if (val.kind() == ValueKind.REGISTER && val.value().startsWith("STRUCT_")) {
                    String structName = val.value().substring(7);

                    if (!structs.containsKey(structName)) {
                        System.err.printf(
                            "Error: unknown struct type '%s'\n",
                            structName
                        );
                        System.exit(1);
                    }

                    variables.put(id, new VariableInfo(structName, VarType.STRUCT, 1, false));

                    LLVMGenerator.allocStruct(id, structName);
                } else {
                    if (isInFunction && !locals.containsKey(id)) {
                        locals.put(id, new VariableInfo(val.type(), 1, false));
                    } else if (!isInFunction && !variables.containsKey(id)) {
                        variables.put(id, new VariableInfo(val.type(), 1, false));
                    } else {
                        System.err.println("Error: variable " + id + " already declared");
                        System.exit(1);
                    }
                    // variables.put(id, new VariableInfo(val.type(), 1, false));
                    LLVMGenerator.declare(id, val.type());
                    LLVMGenerator.assign(id, val);
                }
            }
        } else if (ctx.lvalue() instanceof ProjektParser.IndexLvalContext indexLval) {
            String id = indexLval.ID().getText();
            Value index = valuesStack.pop();
            VariableInfo info = getVariableInfo(id);

            if (info == null) {
                System.err.println("Error: variable " + id + " not found in current scope");
                System.exit(1);
            }
            if (!info.isArray()) {
                System.err.println("Error: " + id + " is not an array");
                System.exit(1);
            }

            LLVMGenerator.storeArrayElement(id, index, val, info.size());
        } else {
            System.err.println("Error: invalid lvalue");
            System.exit(1);
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

        VariableInfo info = getVariableInfo(id);
        if (info == null) {
            System.err.println("Error: unknown array " + id);
            System.exit(1);
        }
        if (!info.isArray()) {
            System.err.println("Error: " + id + " is not an array");
            System.exit(1);
        }

        valuesStack.push(LLVMGenerator.loadArrayElement(id, index, info.size(), info.elementType()));
    }

    @Override
    public void exitThisFieldRval(ProjektParser.ThisFieldRvalContext ctx) {
        if (currentStructName == null) {
            System.err.println("Error: 'this' field access outside struct method");
            System.exit(1);
        }

        String fieldName = ctx.ID().getText();
        StructInfo structInfo = currentStructInfo();

        int fieldIdx = structInfo.fieldIndex(fieldName);
        if (fieldIdx < 0) {
            System.err.printf(
                "Error: struct '%s' has no field '%s'\n",
                currentStructName, fieldName
            );
            System.exit(1);
        }

        VarType fieldType = structInfo.fieldType(fieldName);
        valuesStack.push(
            LLVMGenerator.loadFieldThis(currentStructName, fieldIdx, fieldType)
        );
    }

    @Override
    public void exitFieldRval(ProjektParser.FieldRvalContext ctx) {
        String varName = ctx.ID(0).getText();
        String fieldName = ctx.ID(1).getText();

        VariableInfo varInfo = getVariableInfo(varName);

        if (varInfo == null) {
            System.err.printf("Error: unknown variable '%s'\n", varName);
            System.exit(1);
        }

        if (!varInfo.isStruct()) {
            System.err.printf("Error: '%s' is not a struct\n", varName);
            System.exit(1);
        }

        StructInfo structInfo = structs.get(varInfo.structName());

        if (structInfo == null) {
            System.err.printf("Error: unknown struct type '%s'\n", varInfo.structName());
            System.exit(1);
        }

        int fieldIdx = structInfo.fieldIndex(fieldName);

        if (fieldIdx < 0) {
            System.err.printf("Error: struct '%s' has no field '%s'\n", varInfo.structName(), fieldName);
            System.exit(1);
        }

        VarType fieldType = structInfo.fieldType(fieldName);

        valuesStack.push(LLVMGenerator.loadField(varName, varInfo.structName(), fieldIdx, fieldType));
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
            VariableInfo info = getVariableInfo(id);
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

    @Override
    public void enterFunctionDef(ProjektParser.FunctionDefContext ctx) {
        String funcName = ctx.ID().getText();
        VarType returnType = parseFunType(ctx.funType());

        locals = new HashMap<>();
        functions.put(funcName, returnType);

        List<String> names = new ArrayList<>();
        List<VarType> types = new ArrayList<>();

        if (ctx.paramList() != null && ctx.paramList().param() != null) {
            for (ProjektParser.ParamContext p : ctx.paramList().param()) {
                names.add(p.ID().getText());
                types.add(VarType.fromString(p.type().getText()));
                locals.put(p.ID().getText(), new VariableInfo(
                    VarType.fromString(p.type().getText()),
                    1,
                    false,
                    true
                ));
            }
        }

        LLVMGenerator.startFunction(funcName, returnType, names, types);
    }

    @Override
    public void exitReturnStat(ProjektParser.ReturnStatContext ctx) {
        Value retVal;

        if (ctx.expr() != null) {
            retVal = valuesStack.pop();
        } else {
            retVal = new Value(VarType.VOID, "", ValueKind.REGISTER);
        }

        LLVMGenerator.returnValue(retVal);
    }

    @Override
    public void exitFunctionDef(ProjektParser.FunctionDefContext ctx) {
        LLVMGenerator.endFunction();
        locals.clear();
    }

    @Override
    public void exitFunctionCall(ProjektParser.FunctionCallContext ctx) {
        String funcName = ctx.ID().getText();
        VarType funType = functions.get(funcName);
        if (funType == null) {
            System.err.printf("Error: unknown function '%s'\n", funcName);
            System.exit(1);
        }
        List<Value> args = collectCallArgs(ctx.argList());
        valuesStack.push(LLVMGenerator.call(funcName, args, funType));
    }

    @Override
    public void exitMethodCall(ProjektParser.MethodCallContext ctx) {
        String receiverName = ctx.ID(0).getText();
        String methodName = ctx.ID(1).getText();

        VariableInfo receiver = getVariableInfo(receiverName);
        if (receiver == null || !receiver.isStruct()) {
            System.err.printf(
                "Error: '%s' is not a struct variable\n",
                receiverName
            );
            System.exit(1);
        }

        StructInfo structInfo = structs.get(receiver.structName());
        MethodInfo method = structInfo.method(methodName);
        if (method == null) {
            System.err.printf(
                "Error: struct '%s' has no method '%s'\n",
                receiver.structName(), methodName
            );
            System.exit(1);
        }

        List<Value> args = collectCallArgs(ctx.argList());
        valuesStack.push(LLVMGenerator.callMethod(
            receiver.structName(),
            methodName,
            receiverName,
            args,
            method.returnType()
        ));
    }

    private List<Value> collectCallArgs(ProjektParser.ArgListContext argList) {
        List<Value> args = new ArrayList<>();
        if (argList != null) {
            for (ProjektParser.ExprContext exprCtx : argList.expr()) {
                args.add(0, valuesStack.pop());
            }
        }
        return args;
    }


}
