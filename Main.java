import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

public class Main {
    public static void main(String[] args) throws Exception {
        CharStream input = CharStreams.fromFileName(args[0]);

        ProjektLexer lexer = new ProjektLexer(input);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ProjektParser parser = new ProjektParser(tokens);

        ParseTree tree = parser.code();

        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new LLVMActions(), tree);
    }
}