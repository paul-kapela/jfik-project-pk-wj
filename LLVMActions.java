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
        valuesStack.push(new Value(VarType.REAL, ctx.REAL().getText()));
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
        LLVMGenerator.write(valuesStack.pop());
    }

    @Override
    public void exitWrite(ProjektParser.WriteContext ctx) {
        LLVMGenerator.read(valuesStack.pop());
    }

    @Override
    public void exitAdd(ProjektParser.AddContext ctx) {
        valuesStack.push(LLVMGenerator.add(valuesStack.pop(), valuesStack.pop()));
    }

    @Override
    public void exitSub(ProjektParser.SubContext ctx) {
        valuesStack.push(LLVMGenerator.sub(valuesStack.pop(), valuesStack.pop()));
    }

    @Override
    public void exitMul(ProjektParser.MulContext ctx) {
        valuesStack.push(LLVMGenerator.mul(valuesStack.pop(), valuesStack.pop()));
    }

    @Override
    public void exitDiv(ProjektParser.DivContext ctx) {
        valuesStack.push(LLVMGenerator.div(valuesStack.pop(), valuesStack.pop()));
    }

    @Override
    public void exitNeg(ProjektParser.NegContext ctx) {
        valuesStack.push(LLVMGenerator.neg(valuesStack.pop()));
    }
}
