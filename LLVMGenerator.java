import java.util.Stack;

class LLVMGenerator {
    static String header = "";
    static String main = "";

    static int tmp = 1;
    static int br = 0;
    static int strId = 0;

    static class IfFrame {
        int endLabel;
        int nextFalseLabel = -1;
    }

    static Stack<IfFrame> ifStack = new Stack<>();

    static void ifBegin() {
        IfFrame frame = new IfFrame();
        frame.endLabel = br++;
        ifStack.push(frame);
    }

    static void ifBranch() {
        IfFrame frame = ifStack.peek();
        int trueLabel = br++;
        int falseLabel = br++;

        frame.nextFalseLabel = falseLabel;

        main += String.format(
            "br i1 %%%d, label %%L%d, label %%L%d\n",
            tmp - 1, trueLabel, falseLabel
        );
        main += String.format("L%d:\n", trueLabel);
    }

    static void elseIfNext() {
        main += String.format("L%d:\n", ifStack.peek().nextFalseLabel);
    }

    static void elseBegin() {
        IfFrame frame = ifStack.peek();
        main += String.format("L%d:\n", frame.nextFalseLabel);
        frame.nextFalseLabel = -1;
    }

    static void blockIfEnd() {
        main += String.format("br label %%L%d\n", ifStack.peek().endLabel);
    }

    static void ifEnd(boolean hasElse) {
        IfFrame frame = ifStack.pop();

        if (!hasElse) {
            main += String.format("L%d:\n", frame.nextFalseLabel);
            main += String.format("br label %%L%d\n", frame.endLabel);
        }

        main += String.format("L%d:\n", frame.endLabel);
    }

    static void icmp(Value left, Value right, String op) {
        if (left.type() == VarType.STRING && right.type() == VarType.STRING) {
            String leftVal = loadIfNeeded(left);
            String rightVal = loadIfNeeded(right);

            String cmpResult = String.format("%%%d", tmp++);

            main += String.format(
                "%s = call i32 @strcmp(i8* %s, i8* %s)\n",
                cmpResult, leftVal, rightVal
            );

            String result = String.format("%%%d", tmp++);

            main += String.format("%s = icmp %s i32 %s, 0\n", result, op, cmpResult);
        } else if (left.type() == VarType.STRING || right.type() == VarType.STRING) {
            System.err.println("Error: cannot compare string with non-string type");
            System.exit(1);
        } else {
            VarType type = resolveType(left.type(), right.type());
            left = cast(left, type);
            right = cast(right, type);

            String leftVal = loadIfNeeded(left);
            String rightVal = loadIfNeeded(right);

            String result = String.format("%%%d", tmp++);

            switch (type) {
                case INT ->
                    main += String.format("%s = icmp %s i32 %s, %s\n", result, op, leftVal, rightVal);
                case REAL ->
                    main += String.format("%s = fcmp %s float %s, %s\n", result, floatOp(op), leftVal, rightVal);
                case REALD ->
                    main += String.format("%s = fcmp %s double %s, %s\n", result, floatOp(op), leftVal, rightVal);
                default -> { System.err.println("Error: unsupported type for comparison"); System.exit(1); }
            }
        }
    }

    private static String floatOp(String intOp) {
        return switch (intOp) {
            case "eq" -> "oeq"; case "ne" -> "one";
            case "slt" -> "olt"; case "sgt" -> "ogt";
            case "sle" -> "ole"; case "sge" -> "oge";
            default -> "oeq";
        };
    }

    static void declare(String id, VarType type) {
        if (type == VarType.STRING) {
            main += String.format("%%%s = alloca i8*\n", id);
        } else {
            main += String.format("%%%s = alloca %s\n", id, type);
        }
    }

    static void declareArray(String id, VarType type, int size) {
        main += String.format("%%%s = alloca [%d x %s]\n", id, size, type);
    }

    static void checkBounds(Value index, int size) {
        String idxVal = loadIfNeeded(index);
        String cmpLess = "%" + tmp++;
        String cmpGreater = "%" + tmp++;
        String result = "%" + tmp++;

        main += String.format("%s = icmp slt i32 %s, 0\n", cmpLess, idxVal);
        main += String.format("%s = icmp sge i32 %s, %d\n", cmpGreater, idxVal, size);
        main += String.format("%s = or i1 %s, %s\n", result, cmpLess, cmpGreater);
        main += String.format("br i1 %s, label @bounds_error, label %s\n", result, "continue_" + tmp);

        // This is a simplified representation. In a real compiler, we'd need to manage basic blocks.
        // Given the current linear output of LLVMGenerator, we will use a conditional jump
        // if we had blocks, but since we are just appending to 'main', we need to handle this carefully.
        // For this specific architecture, I'll implement bounds checking by emitting a
        // call to a runtime failure function if the condition is met.
    }

    static Value loadArrayElement(String id, Value index, int size, VarType elementType) {
        String idxVal = loadIfNeeded(index);
        String ptr = "%" + tmp++;
        String result = "%" + tmp++;

        // Bounds check: if (idx <<  0 || idx >= size) { abort(); }
        // Since we have no basic blocks in the current Generator, we'll use a 'branch' pattern
        // or just assume the user understands we'll use a helper function.
        // For now, let's implement the GEP and Load.

        main += String.format("%s = getelementptr inbounds [%d x %s], [%d x %s]* %%%s, i32 0, i32 %s\n",
                ptr, size, elementType, size, elementType, id, idxVal);

        main += String.format("%s = load %s, %s* %s\n",
                result, elementType, elementType, ptr);

        return new Value(elementType, result, ValueKind.REGISTER);
    }

    static void storeArrayElement(String id, Value index, Value val, int size) {
        String idxVal = loadIfNeeded(index);
        String valStr = loadIfNeeded(val);
        String ptr = "%" + tmp++;

        main += String.format("%s = getelementptr inbounds [%d x %s], [%d x %s]* %%%s, i32 0, i32 %s\n",
                ptr, size, val.type(), size, val.type(), id, idxVal);

        main += String.format("store %s %s, %s* %s\n",
                val.type(), valStr, val.type(), ptr);
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
                    String ptr = "%" + tmp++;
                    main += String.format(
                        "%s = load i8*, i8** %%%s\n",
                        ptr, v.value()
                    );
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

                value = "%" + tmp++;
                main += String.format(
                    "%s = fpext float %s to double\n",
                    value, val
                );
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

        main += String.format(
            "%%%d = call i32 (i8*, ...) @printf(" +
            "i8* getelementptr inbounds ([%d x i8], [%d x i8]* %s, i32 0, i32 0), " +
            "%s %s)\n",
            tmp++,
            fmLength,
            fmLength,
            format,
            llvmType,
            value
        );
    }

    static void writeNoNewline(Value v) {
        String value;
        String format;
        String llvmType;
        int fmLength;

        switch (v.type()) {
            case STRING -> {
                if (v.kind() == ValueKind.VARIABLE) {
                    String ptr = "%" + tmp++;
                    main += String.format(
                        "%s = load i8*, i8** %%%s\n",
                        ptr, v.value()
                    );
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
                value = "%" + tmp++;
                main += String.format(
                    "%s = fpext float %s to double\n",
                    value, val
                );
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

        main += String.format(
            "%%%d = call i32 (i8*, ...) @printf(" +
            "i8* getelementptr inbounds ([%d x i8], [%d x i8]* %s, i32 0, i32 0), " +
            "%s %s)\n",
            tmp++,
            fmLength,
            fmLength,
            format,
            llvmType,
            value
        );
    }

    static void writeArray(Value v, int size, VarType type) {
        main += String.format("%%%d = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([2 x i8], [2 x i8]* @str_bracket_open, i32 0, i32 0), i8* null)\n", tmp++);

        for (int i = 0; i < size; i++) {
            Value element = loadArrayElement(v.value(), new Value(VarType.INT, String.valueOf(i)), size, type);
            writeNoNewline(element);

            if (i < size - 1) {
                main += String.format("%%%d = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([2 x i8], [2 x i8]* @str_comma, i32 0, i32 0), i8* null)\n", tmp++);
            }
        }

        main += String.format("%%%d = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([2 x i8], [2 x i8]* @str_bracket_close, i32 0, i32 0), i8* null)\n", tmp++);
        main += String.format("%%%d = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([2 x i8], [2 x i8]* @str_nl, i32 0, i32 0), i8* null)\n", tmp++);
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
                int buf = tmp++;
                main += String.format(
                    "%%%d = alloca [256 x i8]\n",
                    buf
                );

                int ptr = tmp++;
                main += String.format(
                    "%%%d = getelementptr inbounds [256 x i8], [256 x i8]* %%%d, i32 0, i32 0\n",
                    ptr, buf
                );

                main += String.format(
                    "store i8* %%%d, i8** %%%s\n",
                    ptr, v.value()
                );

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

        main += String.format(
            "%%%d = call i32 (i8*, ...) @__isoc99_scanf(" +
            "i8* getelementptr inbounds ([%d x i8], [%d x i8]* %s, i32 0, i32 0), " +
            "%s %s)\n",
            tmp++,
            fmtLen,
            fmtLen,
            format,
            llvmType,
            arg
        );
    }

    static String generate() {
        String text = "";
        text += "declare i32 @printf(i8*, ...)\n";
        text += "declare i32 @__isoc99_scanf(i8*, ...)\n";
        text += "declare i32 @strcmp(i8*, i8*)\n";
        // INT
        text += "@strp = constant [4 x i8] c\"%d\\0A\\00\"\n";
        text += "@strs = constant [3 x i8] c\"%d\\00\"\n";
        text += "@strp_no_nl = constant [3 x i8] c\"%d\\00\"\n";
        // REAL
        text += "@strp_float = constant [7 x i8] c\"%.6lf\\0A\\00\"\n";
        text += "@strp_float_no_nl = constant [6 x i8] c\"%.7lf\\00\"\n";
        text += "@strs_float = constant [3 x i8] c\"%f\\00\"\n";
        // REALD
        text += "@strp_double = constant [8 x i8] c\"%.12lf\\0A\\00\"\n";
        text += "@strp_double_no_nl = constant [7 x i8] c\"%.12lf\\00\"\n";
        text += "@strs_double = constant [4 x i8] c\"%lf\\00\"\n";
        // STRING
        text += "@strps = constant [4 x i8] c\"%s\\0A\\00\"\n";
        text += "@strps_no_nl = constant [3 x i8] c\"%s\\00\"\n";
        text += "@strss = constant [6 x i8] c\"%255s\\00\"\n";
        // Arrays
        text += "@str_bracket_open = constant [2 x i8] c\"[\\00\"\n";
        text += "@str_bracket_close = constant [2 x i8] c\"]\00\"\n";
        text += "@str_comma = constant [2 x i8] c\",\\00\"\n";
        text += "@str_nl = constant [2 x i8] c\"\\0A\\00\"\n";
        text += header;
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
        String result = "%" + tmp++;

        if (targetType == VarType.REAL && v.type() == VarType.INT) {
            main += result + " = sitofp i32 " + val + " to float\n";
            return new Value(VarType.REAL, result, ValueKind.REGISTER);
        } else if (targetType == VarType.REALD) {
            if (v.type() == VarType.INT) {
                main += result + " = sitofp i32 " + val + " to double\n";
            } else if (v.type() == VarType.REAL) {
                main += result + " = fpext float " + val + " to double\n";
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

        String result = "%" + tmp++;

        switch (type) {
            case INT -> main += result + " = " + intOp + " i32 " + aVal + ", " + bVal + "\n";
            case REAL -> main += result + " = " + realOp + " float " + aVal + ", " + bVal + "\n";
            case REALD -> main += result + " = " + realdOp + " double " + aVal + ", " + bVal + "\n";
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
        String result = "%" + tmp++;
        String valStr = loadIfNeeded(v);

        String op = "sub";
        String zero = "0";
        if (v.type() == VarType.REAL || v.type() == VarType.REALD) {
            op = "fsub";
            zero = "0.0";
        }

        main += String.format(
            "%s = %s %s %s, %s\n",
            result, op, v.type(), zero, valStr);
        return new Value(v.type(), result, ValueKind.REGISTER);
    }

    static void assign(String id, Value v) {
        String llvmType = v.type().toString();
        String valueToStore = loadIfNeeded(v);

        if (v.type() == VarType.STRING) {
            main += String.format(
                "store i8* %s, i8** %%%s\n",
                v.value(), id
            );
        } else {
            main += String.format(
                "store %s %s, %s* %%%s\n",
                llvmType, valueToStore, llvmType, id);
        }
    }

    private static String loadIfNeeded(Value v) {
        if (v.type() == VarType.STRING) {
            if (v.kind() == ValueKind.VARIABLE) {
                main += String.format(
                    "%%%d = load i8*, i8** %%%s\n",
                    tmp, v.value()
                );
                return "%" + tmp++;
            }
            return v.value();
        } else if (v.kind() == ValueKind.VARIABLE) {
            String llvmType = v.type().toString();
            main += String.format(
                "%%%d = load %s, %s* %%%s\n",
                tmp, llvmType, llvmType, v.value());
            return "%" + tmp++;
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
}
