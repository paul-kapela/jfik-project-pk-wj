// Generated from Projekt.g4 by ANTLR 4.13.2
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link ProjektParser}.
 */
public interface ProjektListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link ProjektParser#code}.
	 * @param ctx the parse tree
	 */
	void enterCode(ProjektParser.CodeContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProjektParser#code}.
	 * @param ctx the parse tree
	 */
	void exitCode(ProjektParser.CodeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProjektParser#primalExpr}.
	 * @param ctx the parse tree
	 */
	void enterPrimalExpr(ProjektParser.PrimalExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProjektParser#primalExpr}.
	 * @param ctx the parse tree
	 */
	void exitPrimalExpr(ProjektParser.PrimalExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProjektParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterExpr(ProjektParser.ExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProjektParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitExpr(ProjektParser.ExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProjektParser#unaryExpr}.
	 * @param ctx the parse tree
	 */
	void enterUnaryExpr(ProjektParser.UnaryExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProjektParser#unaryExpr}.
	 * @param ctx the parse tree
	 */
	void exitUnaryExpr(ProjektParser.UnaryExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link ProjektParser#value}.
	 * @param ctx the parse tree
	 */
	void enterValue(ProjektParser.ValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ProjektParser#value}.
	 * @param ctx the parse tree
	 */
	void exitValue(ProjektParser.ValueContext ctx);
}