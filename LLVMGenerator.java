class LLVMGenerator {
    static String main = "";
    static int tmp = 1;
    static String header = "";
    static int strId = 0;

    static void declare(String id, VarType type) {
        if (type == VarType.STRING) {
            main += String.format("%%%s = alloca i8*\n", id);
        } else {
            main += String.format("%%%s = alloca %s\n", id, type);
        }
    }

    static void write(Value v) {
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
                fmLength = 4;
            }
            case REALD -> {
                value = loadIfNeeded(v);
                format = "@strp_double";
                llvmType = "double";
                fmLength = 5;
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
        // INT
        text += "@strp = constant [4 x i8] c\"%d\\0A\\00\"\n";
        text += "@strs = constant [3 x i8] c\"%d\\00\"\n";
        // REAL
        text += "@strp_float = constant [4 x i8] c\"%f\\0A\\00\"\n";
        // REALD
        text += "@strp_double = constant [5 x i8] c\"%lf\\0A\\00\"\n";
        // STRING
        text += "@strps = constant [4 x i8] c\"%s\\0A\\00\"\n";
        text += "@strss = constant [6 x i8] c\"%255s\\00\"\n";
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
        main += String.format(
            "%s = sub %s 0, %s\n",
            result, v.type(), valStr);
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