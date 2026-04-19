import org.antlr.v4.runtime.*;

public class TokenTest {
    public static void main(String[] args) throws Exception {
        CharStream input = CharStreams.fromFileName(args[0]);
        ProjektLexer lexer = new ProjektLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();
        
        for (Token t : tokens.getTokens()) {
            System.out.println(t.getType() + " " + lexer.getVocabulary().getSymbolicName(t.getType()) + " = '" + t.getText() + "'");
        }
    }
}
