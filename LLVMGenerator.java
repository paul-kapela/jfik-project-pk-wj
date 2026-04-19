class LLVMGenerator {
    static String main = "";
    static int tmp = 1;

    static void ifstart() {
        
    }

    static void printf(String id) {
        main += "%" + tmp + " = load i32, i32* %" + id + "\n";
        tmp++;
        main += "%" + tmp + " = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strp, i32 0, i32 0), i32 %" + (tmp - 1) + ")\n"; 
        tmp++;
    }

    static void scanf(String id) {
        main += "%" + tmp + " = call i32 (i8*, ...) @__isoc99_scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* @strs, i32 0, i32 0), i32* %" + id + ")\n";
        tmp++;
    }

    static String generate() {
        String text = "";
        text += "declare i32 @printf(i8*, ...)\n";
        text += "declare i32 @__isoc99_scanf(i8*, ...)\n";
        text += "@strp = constant [4 x i8] c\"%d\\0A\\00\"\n";
        text += "@strs = constant [3 x i8] c\"%d\\00\"\n";
        // text += header;
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
        if (v.type == targetType) {
            return v;
        }
        String result = "%" + tmp++;
        if (targetType == VarType.REAL) {
            main += result + " = sitofp i32 " + v.value + " to float\n";
            return new Value(VarType.REAL, result);
        } else if (targetType == VarType.REALD) {
            main += result + " = sitofp i32 " + v.value + " to double\n";
            return new Value(VarType.REALD, result);
        }
        return v;
    }

    private static Value aritmeticOperation(Value a, Value b, String intOp, String realOp, String realdOp) {
        VarType type = resolveType(a.type, b.type);
        String result = "%" + tmp++;

        a = cast(a, type);
        b = cast(b, type);

        switch (type) {
            case INT -> main += result + " = " + intOp + " i32 " + a.value + ", " + b.value + "\n";
            case REAL -> main += result + " = " + realOp + " float " + a.value + ", " + b.value + "\n";
            case REALD -> main += result + " = " + realdOp + " double " + a.value + ", " + b.value + "\n";
            default -> {
            }
        }
        return new Value(type, result);
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
        if ("0".equals(b.value) || "0.0".equals(b.value)) {
            throw new RuntimeException("Dzielenie przez 0");
        }
        return aritmeticOperation(a, b, "sdiv", "fdiv", "fdiv");
    }
}