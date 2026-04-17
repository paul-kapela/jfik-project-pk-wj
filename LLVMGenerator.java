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
}