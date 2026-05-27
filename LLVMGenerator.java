import java.util.List;

class LLVMGenerator {
    static String main = "";
    static int tmp = 1;
    static String header = "";
    static int strId = 0;
    static String functions = "";
    static int funTmp = 0;
    static boolean inFunction = false;

    static void emit(String s) {
        if (inFunction) {
            functions += s + "\n";
        } else {
            main += s + "\n";
        }
    }

    static int getTmp() {
        return inFunction ? funTmp : tmp;
    }

    static int reg() {
        return inFunction ? funTmp++ : tmp++;
    }

    static boolean isInFunction() {
        return inFunction;
    }
    
    static void declare(String id, VarType type) {
        if (type == VarType.STRING) {
            emit(String.format("%%%s = alloca i8*\n", id));
        } else {
            emit(String.format("%%%s = alloca %s\n", id, type));
        }
    }

    static void declareArray(String id, VarType type, int size) {
        emit(String.format("%%%s = alloca [%d x %s]\n", id, size, type));
    }

    static void checkBounds(Value index, int size) {
        String idxVal = loadIfNeeded(index);
        String cmpLess = "%" + reg();
        String cmpGreater = "%" + reg();
        String result = "%" + reg();

        emit(String.format("%s = icmp slt i32 %s, 0\n", cmpLess, idxVal));
        emit(String.format("%s = icmp sge i32 %s, %d\n", cmpGreater, idxVal, size));
        emit(String.format("%s = or i1 %s, %s\n", result, cmpLess, cmpGreater));
        emit(String.format("br i1 %s, label @bounds_error, label %s\n", result, "continue_" + getTmp()));

        // This is a simplified representation. In a real compiler, we'd need to manage basic blocks.
        // Given the current linear output of LLVMGenerator, we will use a conditional jump
        // if we had blocks, but since we are just appending to 'main', we need to handle this carefully.
        // For this specific architecture, I'll implement bounds checking by emitting a
        // call to a runtime failure function if the condition is met.
    }

    static Value loadArrayElement(String id, Value index, int size, VarType elementType) {
        String idxVal = loadIfNeeded(index);
        String ptr = "%" + reg();
        String result = "%" + reg();

        // Bounds check: if (idx <<  0 || idx >= size) { abort(); }
        // Since we have no basic blocks in the current Generator, we'll use a 'branch' pattern
        // or just assume the user understands we'll use a helper function.
        // For now, let's implement the GEP and Load.

        emit(String.format("%s = getelementptr inbounds [%d x %s], [%d x %s]* %%%s, i32 0, i32 %s\n",
                ptr, size, elementType, size, elementType, id, idxVal));

        emit(String.format("%s = load %s, %s* %s\n",
                result, elementType, elementType, ptr));

        return new Value(elementType, result, ValueKind.REGISTER);
    }

    static void storeArrayElement(String id, Value index, Value val, int size) {
        String idxVal = loadIfNeeded(index);
        String valStr = loadIfNeeded(val);
        String ptr = "%" + reg();

        emit(String.format("%s = getelementptr inbounds [%d x %s], [%d x %s]* %%%s, i32 0, i32 %s\n",
                ptr, size, val.type(), size, val.type(), id, idxVal));

        emit(String.format("store %s %s, %s* %s\n",
                val.type(), valStr, val.type(), ptr));
    }

    static void write(Value v) {
        if (v.kind() == ValueKind.ARRAY_VARIABLE || v.kind() == ValueKind.ARRAY_LITERAL) {
             throw new RuntimeException("Please use writeArray(Value v, int size) instead");
        }

        String value;
        String format;
        String llvmType;
        int fmLength;

        switch (v.type()) {
            case STRING -> {
                if (v.kind() == ValueKind.VARIABLE) {
                    String ptr = "%" + reg();
                    emit(String.format(
                        "%s = load i8*, i8** %%%s\n",
                        ptr, v.value()
                    ));
                    value = ptr;
                } else {
                    value = v.value();
                }
                format = "@strps";
                llvmType = "i8*";
                fmLength = 4;
            }
            case INT -> {
                value = loadIfNeeded(v);
                format = "@strp";
                llvmType = "i32";
                fmLength = 4;
            }
            case REAL -> {
                String val = loadIfNeeded(v);

                value = "%" + reg();
                emit(String.format(
                    "%s = fpext float %s to double\n",
                    value, val
                ));
                format = "@strp_float";
                llvmType = "double";
                fmLength = 7;
            }
            case REALD -> {
                value = loadIfNeeded(v);
                format = "@strp_double";
                llvmType = "double";
                fmLength = 8;
            }
            default -> {
                throw new RuntimeException("Unsupported type: " + v.type());
            }
        }

        emit(String.format(
            "%%%d = call i32 (i8*, ...) @printf(" +
            "i8* getelementptr inbounds ([%d x i8], [%d x i8]* %s, i32 0, i32 0), " +
            "%s %s)\n",
            reg(),
            fmLength,
            fmLength,
            format,
            llvmType,
            value
        ));
    }

    static void writeNoNewline(Value v) {
        String value;
        String format;
        String llvmType;
        int fmLength;

        switch (v.type()) {
            case STRING -> {
                if (v.kind() == ValueKind.VARIABLE) {
                    String ptr = "%" + reg();
                    emit(String.format(
                        "%s = load i8*, i8** %%%s\n",
                        ptr, v.value()
                    ));
                    value = ptr;
                } else {
                    value = v.value();
                }
                format = "@strps_no_nl";
                llvmType = "i8*";
                fmLength = 3;
            }
            case INT -> {
                value = loadIfNeeded(v);
                format = "@strp_no_nl";
                llvmType = "i32";
                fmLength = 3;
            }
            case REAL -> {
                String val = loadIfNeeded(v);
                value = "%" + reg();
                emit(String.format(
                    "%s = fpext float %s to double\n",
                    value, val
                ));
                format = "@strp_float_no_nl";
                llvmType = "double";
                fmLength = 6;
            }
            case REALD -> {
                value = loadIfNeeded(v);
                format = "@strp_double_no_nl";
                llvmType = "double";
                fmLength = 7;
            }
            default -> {
                throw new RuntimeException("Unsupported type: " + v.type());
            }
        }

        emit(String.format(
            "%%%d = call i32 (i8*, ...) @printf(" +
            "i8* getelementptr inbounds ([%d x i8], [%d x i8]* %s, i32 0, i32 0), " +
            "%s %s)\n",
            reg(),
            fmLength,
            fmLength,
            format,
            llvmType,
            value
        ));
    }

    static void writeArray(Value v, int size, VarType type) {
        emit(String.format("%%%d = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([2 x i8], [2 x i8]* @str_bracket_open, i32 0, i32 0), i8* null)\n", reg()));

        for (int i = 0; i < size; i++) {
            Value element = loadArrayElement(v.value(), new Value(VarType.INT, String.valueOf(i)), size, type);
            writeNoNewline(element);

            if (i < size - 1) {
                emit(String.format("%%%d = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([2 x i8], [2 x i8]* @str_comma, i32 0, i32 0), i8* null)\n", reg()));
            }
        }

        emit(String.format("%%%d = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([2 x i8], [2 x i8]* @str_bracket_close, i32 0, i32 0), i8* null)\n", reg()));
        emit(String.format("%%%d = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([2 x i8], [2 x i8]* @str_nl, i32 0, i32 0), i8* null)\n", reg()));
    }

    static void read(Value v) {
        if (v.kind() != ValueKind.VARIABLE) {
            throw new RuntimeException("read requires a variable");
        }

        String arg;
        String format;
        String llvmType;
        int fmtLen;

        switch (v.type()) {
            case STRING -> {
                int buf = reg();
                emit(String.format(
                    "%%%d = alloca [256 x i8]\n",
                    buf
                ));

                int ptr = reg();
                emit(String.format(
                    "%%%d = getelementptr inbounds [256 x i8], [256 x i8]* %%%d, i32 0, i32 0\n",
                    ptr, buf
                ));

                emit(String.format(
                    "store i8* %%%d, i8** %%%s\n",
                    ptr, v.value()
                ));

                arg = "%" + ptr;
                format = "@strss";
                llvmType = "i8*";
                fmtLen = 6;
            }

            case INT -> {
                arg = "%" + v.value();
                format = "@strs";
                llvmType = "i32*";
                fmtLen = 3;
            }

            case REAL -> {
                arg = "%" + v.value();
                format = "@strs_float";
                llvmType = "float*";
                fmtLen = 3;
            }

            case REALD -> {
                arg = "%" + v.value();
                format = "@strs_double";
                llvmType = "double*";
                fmtLen = 4;
            }

            default -> {
                throw new RuntimeException("Unsupported type for read: " + v.type());
            }
        }

        emit(String.format(
            "%%%d = call i32 (i8*, ...) @__isoc99_scanf(" +
            "i8* getelementptr inbounds ([%d x i8], [%d x i8]* %s, i32 0, i32 0), " +
            "%s %s)\n",
            reg(),
            fmtLen,
            fmtLen,
            format,
            llvmType,
            arg
        ));
    }

    static String generate() {
        String text = "";
        text += "declare i32 @printf(i8*, ...)\n";
        text += "declare i32 @__isoc99_scanf(i8*, ...)\n";
        // INT
        text += "@strp = constant [4 x i8] c\"%d\\0A\\00\"\n";
        text += "@strs = constant [3 x i8] c\"%d\\00\"\n";
        text += "@strp_no_nl = constant [3 x i8] c\"%d\\00\"\n";
        // REAL
        text += "@strp_float = constant [7 x i8] c\"%.7lf\\0A\\00\"\n";
        text += "@strp_float_no_nl = constant [6 x i8] c\"%.7lf\\00\"\n";
        text += "@strs_float = constant [3 x i8] c\"%f\\00\"\n";
        // REALD
        text += "@strp_double = constant [8 x i8] c\"%.15lf\\0A\\00\"\n";
        text += "@strp_double_no_nl = constant [7 x i8] c\"%.15lf\\00\"\n";
        text += "@strs_double = constant [4 x i8] c\"%lf\\00\"\n";
        // STRING
        text += "@strps = constant [4 x i8] c\"%s\\0A\\00\"\n";
        text += "@strps_no_nl = constant [3 x i8] c\"%s\\00\"\n";
        text += "@strss = constant [6 x i8] c\"%255s\\00\"\n";
        // Arrays
        text += "@str_bracket_open = constant [2 x i8] c\"[\\00\"\n";
        text += "@str_bracket_close = constant [2 x i8] c\"]\\00\"\n";
        text += "@str_comma = constant [2 x i8] c\",\\00\"\n";
        text += "@str_nl = constant [2 x i8] c\"\\0A\\00\"\n";
        text += header;
        text += functions;
        text += "define i32 @main() nounwind {\n";
        text += main;
        text += "ret i32 0 }\n";
        return text;
    }



    private static VarType resolveType(VarType a, VarType b) {
        if (a == VarType.REALD || b == VarType.REALD) {
            return VarType.REALD;
        } else if (a == VarType.REAL || b == VarType.REAL) {
            return VarType.REAL;
        } else {
            return VarType.INT;
        }
    }

    private static Value cast(Value v, VarType targetType) {
        if (v.type() == targetType) {
            return v;
        }

        String val = loadIfNeeded(v);
        String result = "%" + reg();

        if (targetType == VarType.REAL && v.type() == VarType.INT) {
            emit(result + " = sitofp i32 " + val + " to float\n");
            return new Value(VarType.REAL, result, ValueKind.REGISTER);
        } else if (targetType == VarType.REALD) {
            if (v.type() == VarType.INT) {
                emit(result + " = sitofp i32 " + val + " to double\n");
            } else if (v.type() == VarType.REAL) {
                emit(result + " = fpext float " + val + " to double\n");
            } else {
                return v;
            }
            return new Value(VarType.REALD, result, ValueKind.REGISTER);
        }
        return v;
    }

    private static Value aritmeticOperation(Value a, Value b, String intOp, String realOp, String realdOp) {
        VarType type = resolveType(a.type(), b.type());

        a = cast(a, type);
        b = cast(b, type);

        String aVal = loadIfNeeded(a);
        String bVal = loadIfNeeded(b);

        String result = "%" + reg();

        switch (type) {
            case INT -> emit(result + " = " + intOp + " i32 " + aVal + ", " + bVal + "\n");
            case REAL -> emit(result + " = " + realOp + " float " + aVal + ", " + bVal + "\n");
            case REALD -> emit(result + " = " + realdOp + " double " + aVal + ", " + bVal + "\n");
            default -> {
                System.err.println("Unsupported type for arithmetic operation: " + type);
                System.exit(1);
            }
        }

        return new Value(type, result, ValueKind.REGISTER);
    }

    private static void checkNumeric(Value a, Value b) {
        if (a.type() == VarType.STRING || b.type() == VarType.STRING) {
            throw new RuntimeException("Operation not supported for string type");
        }
    }

    static Value add(Value a, Value b) {
        checkNumeric(a, b);
        return aritmeticOperation(a, b, "add", "fadd", "fadd");
    }

    static Value sub(Value a, Value b) {
        checkNumeric(a, b);
        return aritmeticOperation(a, b, "sub", "fsub", "fsub");
    }

    static Value mul(Value a, Value b) {
        checkNumeric(a, b);
        return aritmeticOperation(a, b, "mul", "fmul", "fmul");
    }

    static Value div(Value a, Value b) {
        checkNumeric(a, b);
        if ("0".equals(b.value()) || "0.0".equals(b.value())) {
            throw new RuntimeException("Operation not supported for division by zero");
        }
        return aritmeticOperation(a, b, "sdiv", "fdiv", "fdiv");
    }

    static Value neg(Value v) {
        if (v.type() == VarType.STRING) {
            throw new RuntimeException("Operation not supported for string type");
        }
        String result = "%" + reg();
        String valStr = loadIfNeeded(v);

        String op = "sub";
        String zero = "0";
        if (v.type() == VarType.REAL || v.type() == VarType.REALD) {
            op = "fsub";
            zero = "0.0";
        }

        emit(String.format(
            "%s = %s %s %s, %s\n",
            result, op, v.type(), zero, valStr));
        return new Value(v.type(), result, ValueKind.REGISTER);
    }

    static void assign(String id, Value v) {
        String llvmType = v.type().toString();
        String valueToStore = loadIfNeeded(v);

        if (v.type() == VarType.STRING) {
            emit(String.format(
                "store i8* %s, i8** %%%s\n",
                v.value(), id
            ));
        } else {
            emit(String.format(
                "store %s %s, %s* %%%s\n",
                llvmType, valueToStore, llvmType, id));
        }
    }

    private static String loadIfNeeded(Value v) {
        if (v.kind() == ValueKind.PARAMETER) {
            return "%" + v.value();
        }

        if (v.type() == VarType.STRING) {
            if (v.kind() == ValueKind.VARIABLE) {
                emit(String.format(
                    "%%%d = load i8*, i8** %%%s\n",
                    getTmp(), v.value()
                ));
                return "%" + reg();
            }
            return v.value();
        } else if (v.kind() == ValueKind.VARIABLE) {
            String llvmType = v.type().toString();
            emit(String.format(
                "%%%d = load %s, %s* %%%s\n",
                getTmp(), llvmType, llvmType, v.value()));
            return "%" + reg();
        }
        return v.value();
    }

    static String createString(String text) {
        String name = "@.str." + strId++;
        header += String.format(
            "%s = constant [%d x i8] c\"%s\\00\"\n",
            name, text.length() + 1, text
        );
        return String.format(
            "getelementptr inbounds ([%d x i8], [%d x i8]* %s, i32 0, i32 0)",
            text.length() + 1,
            text.length() + 1,
            name
        );
    }

    static void startFunction(
            String name,
            VarType returnType,
            List<String> paramNames,
            List<VarType> paramTypes
    ) {
        inFunction = true;
        funTmp = 0;

        StringBuilder sb = new StringBuilder();

        sb.append(String.format("define %s @%s(", returnType, name));

        for (int i = 0; i < paramTypes.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(String.format("%s %%%s", paramTypes.get(i), paramNames.get(i)));
        }

        sb.append(") {\n");
        sb.append("entry:\n");

        for (int i = 0; i < paramTypes.size(); i++) {
            String paramName = paramNames.get(i);
            VarType type = paramTypes.get(i);

            sb.append(String.format("%%%s.addr = alloca %s\n", paramName, type));
            sb.append(String.format(
                    "store %s %%%s, %s* %%%s.addr\n",
                    type, paramName, type, paramName
            ));
        }

        functions += sb.toString();
    }

    static void endFunction() {
        inFunction = false;
        functions += "}\n";
    }

    static void returnValue(Value v) {
        if (v.type() == VarType.VOID) {
            functions += "ret void\n";
            return;
        }
        
        String val = loadIfNeeded(v);
        functions += String.format("ret %s %s\n", v.type(), val);
    }

    static Value call(String funcName, List<Value> args, VarType returnType) {
        StringBuilder argList = new StringBuilder();

        for (int i = 0; i < args.size(); i++) {
            if (i > 0) argList.append(", ");
            argList.append(args.get(i).type())
                    .append(" ")
                    .append(loadIfNeeded(args.get(i)));
        }

        if (returnType == VarType.VOID) {
            emit(String.format("call void @%s(%s)\n", funcName, argList));
            return new Value(VarType.VOID, "", ValueKind.REGISTER);
        }

        String result = "%" + reg();
        emit(String.format("%s = call %s @%s(%s)\n",
            result, returnType, funcName, argList));

        return new Value(returnType, result, ValueKind.REGISTER);
    }
}
