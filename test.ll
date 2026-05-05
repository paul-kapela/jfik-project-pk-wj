declare i32 @printf(i8*, ...)
declare i32 @__isoc99_scanf(i8*, ...)
@strp = constant [4 x i8] c"%d\0A\00"
@strs = constant [3 x i8] c"%d\00"
@strp_float = constant [4 x i8] c"%f\0A\00"
@strp_double = constant [5 x i8] c"%lf\0A\00"
@strps = constant [4 x i8] c"%s\0A\00"
@strss = constant [6 x i8] c"%255s\00"
@.str.0 = constant [4 x i8] c"INT\00"
@.str.1 = constant [6 x i8] c"a + b\00"
@.str.2 = constant [6 x i8] c"a - b\00"
@.str.3 = constant [6 x i8] c"a * b\00"
@.str.4 = constant [6 x i8] c"a / b\00"
@.str.5 = constant [5 x i8] c"REAL\00"
@.str.6 = constant [6 x i8] c"a + b\00"
@.str.7 = constant [6 x i8] c"a - b\00"
@.str.8 = constant [6 x i8] c"a * b\00"
@.str.9 = constant [6 x i8] c"a / b\00"
@.str.10 = constant [11 x i8] c"test_text1\00"
@.str.11 = constant [257 x i8] c"Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Donec quam felis, ultricies nec, pellentesque eu, pretium quis,.\00"
@.str.12 = constant [1 x i8] c"\00"
@.str.13 = constant [513 x i8] c"Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Donec quam felis, ultricies nec, pellentesque eu, pretium quis, sem. Nulla consequat massa quis enim. Donec pede justo, fringilla vel, aliquet nec, vulputate eget, arcu. In enim justo, rhoncus ut, imperdiet a, venenatis vitae, justo. Nullam dictum felis eu pede mollis pretium. Integer tincidunt. Cras dapibus. Vivamus e\00"
define i32 @main() nounwind {
%x = alloca i32
store i32 5, i32* %x
%1 = load i32, i32* %x
%2 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strp, i32 0, i32 0), i32 %1)
%3 = call i32 (i8*, ...) @__isoc99_scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* @strs, i32 0, i32 0), i32* %x)
%4 = load i32, i32* %x
%5 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strp, i32 0, i32 0), i32 %4)
%6 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strps, i32 0, i32 0), i8* getelementptr inbounds ([4 x i8], [4 x i8]* @.str.0, i32 0, i32 0))
%a = alloca i32
store i32 5, i32* %a
%b = alloca i32
store i32 3, i32* %b
%7 = load i32, i32* %a
%8 = load i32, i32* %b
%9 = add i32 %7, %8
%c = alloca i32
store i32 %9, i32* %c
%10 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strps, i32 0, i32 0), i8* getelementptr inbounds ([6 x i8], [6 x i8]* @.str.1, i32 0, i32 0))
%11 = load i32, i32* %c
%12 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strp, i32 0, i32 0), i32 %11)
%13 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strps, i32 0, i32 0), i8* getelementptr inbounds ([6 x i8], [6 x i8]* @.str.2, i32 0, i32 0))
%14 = load i32, i32* %a
%15 = load i32, i32* %b
%16 = sub i32 %14, %15
%17 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strp, i32 0, i32 0), i32 %16)
%18 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strps, i32 0, i32 0), i8* getelementptr inbounds ([6 x i8], [6 x i8]* @.str.3, i32 0, i32 0))
%19 = load i32, i32* %a
%20 = load i32, i32* %b
%21 = mul i32 %19, %20
%22 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strp, i32 0, i32 0), i32 %21)
%23 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strps, i32 0, i32 0), i8* getelementptr inbounds ([6 x i8], [6 x i8]* @.str.4, i32 0, i32 0))
%24 = load i32, i32* %a
%25 = load i32, i32* %b
%26 = sdiv i32 %24, %25
%27 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strp, i32 0, i32 0), i32 %26)
%28 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strps, i32 0, i32 0), i8* getelementptr inbounds ([5 x i8], [5 x i8]* @.str.5, i32 0, i32 0))
%d = alloca float
store float 5.0, float* %d
%e = alloca float
store float 3.0, float* %e
%29 = load float, float* %d
%30 = load float, float* %e
%31 = fadd float %29, %30
%f = alloca float
store float %31, float* %f
%32 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strps, i32 0, i32 0), i8* getelementptr inbounds ([6 x i8], [6 x i8]* @.str.6, i32 0, i32 0))
%33 = load float, float* %f
%34 = fpext float %33 to double
%35 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strp_float, i32 0, i32 0), double %34)
%36 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strps, i32 0, i32 0), i8* getelementptr inbounds ([6 x i8], [6 x i8]* @.str.7, i32 0, i32 0))
%37 = load float, float* %d
%38 = load float, float* %e
%39 = fsub float %37, %38
%40 = fpext float %39 to double
%41 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strp_float, i32 0, i32 0), double %40)
%42 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strps, i32 0, i32 0), i8* getelementptr inbounds ([6 x i8], [6 x i8]* @.str.8, i32 0, i32 0))
%43 = load float, float* %d
%44 = load float, float* %e
%45 = fmul float %43, %44
%46 = fpext float %45 to double
%47 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strp_float, i32 0, i32 0), double %46)
%48 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strps, i32 0, i32 0), i8* getelementptr inbounds ([6 x i8], [6 x i8]* @.str.9, i32 0, i32 0))
%49 = load float, float* %d
%50 = load float, float* %e
%51 = fdiv float %49, %50
%52 = fpext float %51 to double
%53 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strp_float, i32 0, i32 0), double %52)
%text1 = alloca i8*
store i8* getelementptr inbounds ([11 x i8], [11 x i8]* @.str.10, i32 0, i32 0), i8** %text1
%54 = load i8*, i8** %text1
%55 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strps, i32 0, i32 0), i8* %54)
%56 = alloca [256 x i8]
%57 = getelementptr inbounds [256 x i8], [256 x i8]* %56, i32 0, i32 0
store i8* %57, i8** %text1
%58 = call i32 (i8*, ...) @__isoc99_scanf(i8* getelementptr inbounds ([6 x i8], [6 x i8]* @strss, i32 0, i32 0), i8* %57)
%59 = load i8*, i8** %text1
%60 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strps, i32 0, i32 0), i8* %59)
%text256 = alloca i8*
store i8* getelementptr inbounds ([257 x i8], [257 x i8]* @.str.11, i32 0, i32 0), i8** %text256
%61 = load i8*, i8** %text256
%62 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strps, i32 0, i32 0), i8* %61)
%63 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strps, i32 0, i32 0), i8* getelementptr inbounds ([1 x i8], [1 x i8]* @.str.12, i32 0, i32 0))
%text512 = alloca i8*
store i8* getelementptr inbounds ([513 x i8], [513 x i8]* @.str.13, i32 0, i32 0), i8** %text512
%64 = load i8*, i8** %text512
%65 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strps, i32 0, i32 0), i8* %64)
ret i32 0 }

