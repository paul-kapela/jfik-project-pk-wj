class LLVMGenerator {
    static String main = "";
    static int tmp = 1;

    static void declare(String id, VarType type) {
        main += String.format("%%%s = alloca %s\n", id, type);
    }

    static void write(Value v) {
        String valueToUse = loadIfNeeded(v);
        main += String.format(
            "%%%d = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strp, i32 0, i32 0), i32 %s)\n",
            tmp, valueToUse
        );
        tmp++;
    }

    static void read(Value v) {
        if (v.kind() == ValueKind.VARIABLE) {
            main += String.format(
                "%%%d = call i32 (i8*, ...) @__isoc99_scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* @strs, i32 0, i32 0), i32* %%%s)\n",
                tmp, v.value()
            );
            tmp++;
        } else {
            System.err.println("Error: read requires a variable");
            System.exit(1);
        }
    }

    static String generate() {
        String text = "";
        text += "declare i32 @printf(i8*, ...)\n";
        text += "declare i32 @__isoc99_scanf(i8*, ...)\n";
        text += "@strp = constant [4 x i8] c\"%d\\0A\\00\"\n";
        text += "@strs = constant [3 x i8] c\"%d\\00\"\n";
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
        String result = "%" + tmp++;
        if (targetType == VarType.REAL) {
            main += result + " = sitofp i32 " + v.value() + " to float\n";
            return new Value(VarType.REAL, result, ValueKind.REGISTER);
        } else if (targetType == VarType.REALD) {
            main += result + " = sitofp i32 " + v.value() + " to double\n";
            return new Value(VarType.REALD, result, ValueKind.REGISTER);
        }
        return v;
    }

    private static Value aritmeticOperation(Value a, Value b, String intOp, String realOp, String realdOp) {
        VarType type = resolveType(a.type(), b.type());
        String result = "%" + tmp++;

        a = cast(a, type);
        b = cast(b, type);

        String aVal = loadIfNeeded(a);
        String bVal = loadIfNeeded(b);

        switch (type) {
            case INT -> main += result + " = " + intOp + " i32 " + aVal + ", " + bVal + "\n";
            case REAL -> main += result + " = " + realOp + " float " + aVal + ", " + bVal + "\n";
            case REALD -> main += result + " = " + realdOp + " double " + aVal + ", " + bVal + "\n";
            default -> {
            }
        }

        return new Value(type, result, ValueKind.REGISTER);
    }

    static Value add(Value a, Value b) {
        return aritmeticOperation(a, b, "add", "fadd", "fadd");
    }

    static Value sub(Value a, Value b) {
        return aritmeticOperation(a, b, "sub", "fsub", "fsub");
    }

    static Value mul(Value a, Value b) {
        return aritmeticOperation(a, b, "mul", "fmul", "fmul");
    }

    static Value div(Value a, Value b) {
        if ("0".equals(b.value()) || "0.0".equals(b.value())) {
            throw new RuntimeException("Dzielenie przez 0");
        }
        return aritmeticOperation(a, b, "sdiv", "fdiv", "fdiv");
    }

    static Value neg(Value v) {
        String result = "%" + tmp++;
        String valStr = loadIfNeeded(v);
        main += String.format(
            "%s = sub %s 0, %s\n",
            result, v.type(), valStr);
        return new Value(v.type(), result, ValueKind.REGISTER);
    }

    static void assign(String id, Value v) {
        String llvmType = v.type().toString();
        String valueToStore = loadIfNeeded(v);

        main += String.format(
            "store %s %s, %s* %%%s\n",
            llvmType, valueToStore, llvmType, id);
    }

    private static String loadIfNeeded(Value v) {
        if (v.kind() == ValueKind.VARIABLE) {
            String llvmType = v.type().toString();
            main += String.format(
                "%%%d = load %s, %s* %%%s\n",
                tmp, llvmType, llvmType, v.value());
            return "%" + tmp++;
        }
        return v.value();
    }
}