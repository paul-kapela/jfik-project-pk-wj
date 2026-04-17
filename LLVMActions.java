import java.util.HashSet;

public class LLVMActions extends ProjektBaseListener {
    HashSet<String> variables = new HashSet<String>();
    
    @Override
    public void exitCode(ProjektParser.CodeContext ctx) {
        System.out.println(LLVMGenerator.generate());
    }
}