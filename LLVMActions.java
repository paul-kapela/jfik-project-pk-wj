import java.util.HashMap;
import java.util.Stack;

public class LLVMActions extends ProjektBaseListener {
    HashMap<String, VarType> variables = new HashMap<String, VarType>();
    Stack<Value> valuesStack = new Stack<Value>();

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
        String text = ctx.REAL().getText();
        text = text.substring(0, text.length() - 1);

        float value = Float.parseFloat(text);

        if (Float.isInfinite(value) || Float.isNaN(value)) {
            System.err.println(String.format(
                "Error line %d: invalid float literal '%s'",
                ctx.getStart().getLine(),
                text
            ));
            System.exit(1);
        }

        String llvmFloat = String.format(
            "%.15e",
            (double)value
        );

        valuesStack.push(
            new Value(VarType.REAL, llvmFloat)
        );
    }

    @Override
    public void exitReald(ProjektParser.RealdContext ctx) {
        String text = ctx.REALD().getText();

        double value = Double.parseDouble(text);

        if (Double.isInfinite(value) || Double.isNaN(value)) {
            System.err.println(String.format("Error line %d: invalid double literal '%s'", ctx.getStart().getLine(), text));
            System.exit(1);
        }

        valuesStack.push(new Value(VarType.REALD, text));
    }

    @Override
    public void exitId(ProjektParser.IdContext ctx) {
        String id = ctx.ID().getText();

        if (variables.containsKey(id)) {
            VarType type = variables.get(id);
            valuesStack.push(new Value(type, id, ValueKind.VARIABLE));
        } else {
            System.err.println(String.format("Error: unknown variable %s", id));
            System.exit(1);
        }
    }

    @Override
    public void exitAssign(ProjektParser.AssignContext ctx) {
        String id = ctx.ID().getText();
        Value v = valuesStack.pop();

        if (variables.containsKey(id)) {
            VarType existingType = variables.get(id);

            if (existingType != v.type()) {
                System.err.println(String.format(
                    "Error line %d: cannot assign %s to variable '%s' of type %s",
                    ctx.getStart().getLine(), v.type(), id, existingType
                ));
                System.exit(1);
            }
        } else {
            variables.put(id, v.type());
            LLVMGenerator.declare(id, v.type());
        }

        LLVMGenerator.assign(id, v);
    }

    @Override
    public void exitRead(ProjektParser.ReadContext ctx) {
        LLVMGenerator.read(valuesStack.pop());
    }

    @Override
    public void exitWrite(ProjektParser.WriteContext ctx) {
        LLVMGenerator.write(valuesStack.pop());
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
