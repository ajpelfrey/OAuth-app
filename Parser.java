package edu.ufl.cise.cop4020fa23;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.SyntaxException;
import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;
import static edu.ufl.cise.cop4020fa23.Kind.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Parser implements IParser {
	final ILexer lexer;
	private IToken t;
	List < IToken > tokens = new ArrayList < > ();
	private int counter = 0;

	public Parser(ILexer lexer) throws LexicalException {
		super();

		this.lexer = lexer;

		t = lexer.next();

		while (t.kind() != Kind.EOF) {
			tokens.add(t);
			t = lexer.next();
		}
		if (t.kind() == Kind.EOF) {
			tokens.add(t);
		}

		t = tokens.get(0);
	}
	@Override

	public AST parse() throws PLCCompilerException {

		AST e = program();

		return e;

	}

	private AST program() throws SyntaxException, LexicalException {

		IToken type = null;

		IToken identName;
		if (isKind(RES_image, RES_pixel, RES_int, RES_string, RES_void, RES_boolean)) {
			type = type();
		}

		if (isKind(Kind.IDENT)) {

			identName = t;

			consume();

		} else {

			throw new SyntaxException(t.sourceLocation() + "Ident missing");

		}

		matches(Kind.LPAREN);

		List < NameDef > paramList = paramList();

		matches(Kind.RPAREN);

		Block block = block();

		if (t.kind() != Kind.EOF) {

			throw new SyntaxException(t.sourceLocation() + "Block parse at end, but not EOF token: " + t.kind());

		}
		return new Program(t, type, identName, paramList, block);

	}
	private Block block() throws SyntaxException, LexicalException {

		IToken firstToken = t;

		List < Block.BlockElem > blockElements = new ArrayList < > ();

		match(BLOCK_OPEN);
		while (!isKind(BLOCK_CLOSE)) {

			if (isKind(RES_image, RES_pixel, RES_int, RES_string, RES_void, RES_boolean)) {
				blockElements.add(declaration());

			} else if (isKind(IDENT, RES_write, RES_do, RES_if, RETURN, BLOCK_OPEN)) {
				blockElements.add(statement());

			}
			if (t.kind()==RPAREN)
			{
				consume();
			}

			match(Kind.SEMI);

		}

		match(Kind.BLOCK_CLOSE);

		return new Block(firstToken, blockElements);

	}


	private List < NameDef > paramList() throws SyntaxException, LexicalException {
		List < NameDef > paramsList = new ArrayList < > ();

		if (isKind(RES_image, RES_pixel, RES_int, RES_string, RES_void, RES_boolean)) {

			paramsList.add(nameDef());

			while (matches(Kind.COMMA)) {

				paramsList.add(nameDef());

			}

		}

		return paramsList;

	}
	private NameDef nameDef() throws LexicalException, SyntaxException {

		IToken firstToken = t;
		IToken type;
		type = type();
		Dimension dimension=null;

		if (isKind(Kind.LSQUARE)) {

			dimension = dimension();

		}

		IToken identName = t;

		match(IDENT);

		return new NameDef(firstToken, type, dimension, identName);

	}
	private IToken type() throws SyntaxException, LexicalException {

		IToken type;

		if (isKind(RES_image, RES_pixel, RES_int, RES_string, RES_void, RES_boolean)) {

			type = t;

			consume();

			return type;

		}

		throw new SyntaxException(t.sourceLocation(), "Type missing");

	}

	private Declaration declaration() throws SyntaxException, LexicalException {

		IToken firstToken = t;
		if (isKind(RES_image, RES_pixel, RES_int, RES_string, RES_void, RES_boolean)) {
			NameDef nameDef = nameDef();

			Expr initializer = null;

			if (matches(Kind.ASSIGN)) {

				initializer = expr();

			}


			return new Declaration(firstToken, nameDef, initializer);

		} else
			throw new SyntaxException("Decleration error" + t.text());
	}

	private boolean isExprStart() throws LexicalException {
		return isKind(Kind.QUESTION) || isKind(Kind.BANG) || isKind(Kind.MINUS) || isKind(Kind.RES_width) || isKind(Kind.RES_height) || isKind(Kind.STRING_LIT) || isKind(Kind.NUM_LIT) || isKind(Kind.BOOLEAN_LIT) || isKind(Kind.IDENT) || isKind(Kind.LPAREN) || isKind(Kind.CONST) || isKind(Kind.LSQUARE);
	}

	private Expr expr() throws SyntaxException, LexicalException {
		if (isExprStart()) {
			return logicalOrExpr();
		} else if (isKind(Kind.QUESTION))
			return conditionalExpr();

		else {
			throw new SyntaxException(t.sourceLocation() + "not Expr");
		}
	}

	private Expr conditionalExpr() throws LexicalException, SyntaxException {
		IToken firstToken = t;

		match(QUESTION);
		Expr guard = expr();
		if (t.kind()==RPAREN)
		{
			consume();
		}

		Expr gg = null;
		match(RARROW);
		Expr trueExpr = expr();
		match(COMMA);
		Expr falseExpr = expr();
		return new ConditionalExpr(firstToken, guard, trueExpr, falseExpr);
	}

	private Expr logicalOrExpr() throws LexicalException, SyntaxException {
		IToken firstToken = t;
		Expr logicalOrExpr = logicalAndExpr();
		Expr right;
		IToken operator;

		while (isKind(Kind.OR) || isKind(Kind.BITOR)) {
			operator = t;
			consume();
			right = logicalAndExpr();
			logicalOrExpr = new BinaryExpr(firstToken, logicalOrExpr, operator, right);

		}

		return logicalOrExpr;

	}

	private Expr logicalAndExpr() throws SyntaxException, LexicalException {

		IToken firstToken = t;

		Expr logicalAndExpr = comparisonExpr();

		Expr rightExpr;

		IToken op;

		while (isKind(Kind.AND, Kind.BITAND)) {

			op = t;

			consume();

			rightExpr = logicalAndExpr();

			logicalAndExpr = new BinaryExpr(firstToken, logicalAndExpr, op, rightExpr);

		}

		return logicalAndExpr;

	}

	private Expr comparisonExpr() throws SyntaxException, LexicalException {

		IToken firstToken = t;

		Expr rightExpr;

		IToken op;
		Expr comparisonExpr = powExpr();

		while (isKind(LT) || isKind(GT) || isKind(EQ) || isKind(LE) || isKind(GE)) {

			op = t;

			consume();

			rightExpr = powExpr();

			comparisonExpr = new BinaryExpr(firstToken, comparisonExpr, op, rightExpr);

		}

		return comparisonExpr;

	}

	private Expr powExpr() throws SyntaxException, LexicalException {

		IToken firstToken = t;
		Expr rightExpr;
		Expr leftExpr;
		IToken op;

		leftExpr = additiveExpr();

		while (isKind(Kind.EXP)) {

			op = t;

			consume();

			rightExpr = powExpr();

			leftExpr = new BinaryExpr(firstToken, leftExpr, op, rightExpr);

		}

		return leftExpr;

	}

	private Expr additiveExpr() throws SyntaxException, LexicalException {

		IToken firstToken = t;
		Expr rightExpr;
		IToken op;
		Expr leftExpr = multiplicativeExpr();

		while (isKind(Kind.PLUS) || isKind(Kind.MINUS)) {

			op = t;
			consume();
			rightExpr = multiplicativeExpr();
			if (t.kind()==RPAREN){
			consume();}

			leftExpr = new BinaryExpr(firstToken, leftExpr, op, rightExpr);

		}

		return leftExpr;

	}

	private Expr multiplicativeExpr() throws SyntaxException, LexicalException {

		IToken firstToken = t;

		Expr leftExpr = unaryExpr();

		Expr rightExpr;

		IToken op;

		while (isKind(Kind.TIMES) || isKind(Kind.DIV) || isKind(Kind.MOD)) { //should be

			op = t;
			consume();

			rightExpr = unaryExpr();

			leftExpr = new BinaryExpr(firstToken, leftExpr, op, rightExpr);

		}
		//consume();

		return leftExpr;

	}

	private Expr unaryExpr() throws SyntaxException, LexicalException {

		IToken firstToken = t;

		Expr e;

		if (matches(Kind.BANG) || matches(Kind.MINUS) || matches(Kind.RES_width) || matches(Kind.RES_height)) {

			e = expr();

			return new UnaryExpr(firstToken, firstToken, e);

		} else

			return postfixExpr();

	}

	private Expr postfixExpr() throws SyntaxException, LexicalException {

		IToken firstToken = t;
		PixelSelector pixel = null;
		ChannelSelector channel = null;
		Expr primaryExpr = primaryExpr();

		if (isKind(Kind.LSQUARE)) {

			pixel = pixelSelector();

			if (isKind(Kind.COLON)) {

				channel = channelSelector();
			}
			return new PostfixExpr(firstToken, primaryExpr, pixel, channel);

		} else if (isKind(COLON)) {
			channel = channelSelector();

			return new PostfixExpr(firstToken, primaryExpr, pixel, channel);

		} else {
			return primaryExpr;
		}

	}

	private Expr primaryExpr() throws SyntaxException, LexicalException {

		IToken firstToken = t;

		Expr expr;

		if (matches(Kind.STRING_LIT)) {

			expr = new StringLitExpr(firstToken);

		} else if (matches(Kind.NUM_LIT)) {

			expr = new NumLitExpr(firstToken);

		} else if (matches(Kind.BOOLEAN_LIT)) {

			expr = new BooleanLitExpr(firstToken);

		} else if (matches(Kind.IDENT)) {

			expr = new IdentExpr(firstToken);

		} else if (matches(Kind.CONST)) {

			expr = new ConstExpr(firstToken);

		} else if (isKind(Kind.LSQUARE)) {

			expr = expandedPixelExpr();

		} else if (matches(Kind.LPAREN)) {

			expr = expr();

		}
		else if (firstToken.kind() == QUESTION)
		{
			expr = conditionalExpr();
		}

		else {

			throw new SyntaxException(t.sourceLocation() + t.text() +t.kind()+ "Not right Token");

		}

		return expr;

	}

	private ChannelSelector channelSelector() throws SyntaxException, LexicalException {

		IToken firstToken = t;

		match(COLON);

		IToken color;

		if (matches(Kind.RES_red, Kind.RES_green, Kind.RES_blue)) {

			if (isKind(RES_blue)) {
				consume();
			}
			if (isKind(RES_green)) {
				consume();
			}
			if (isKind(RES_red)) {
				consume();
			}
			color = prev();
		} else

			throw new SyntaxException(t.sourceLocation(), "color token err");

		return new ChannelSelector(firstToken, color);

	}

	private PixelSelector pixelSelector() throws SyntaxException, LexicalException {

		IToken firstToken = t;

		consume();

		Expr xExpr = expr();

		match(COMMA);

		Expr yExpr = expr();

		match(RSQUARE);

		return new PixelSelector(firstToken, xExpr, yExpr);

	}

	private Expr expandedPixelExpr() throws SyntaxException, LexicalException {

		IToken firstToken = t;

		consume();

		Expr red = expr();

		match(COMMA);

		Expr grn = expr();

		match(COMMA);

		Expr blu = expr();

		match(RSQUARE);

		return new ExpandedPixelExpr(firstToken, red, grn, blu);

	}
	public IToken prev() throws LexicalException {
		IToken prev;
		try {
			prev = tokens.get(counter - 1);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new LexicalException(t.sourceLocation(), "invalid index for t:" + t.toString());
		}
		// Return the previous token.
		return prev;
	}






	private Dimension dimension() throws SyntaxException, LexicalException {

		IToken firstToken = t;

		match(Kind.LSQUARE);

		Expr width;
		width = expr();

		match(Kind.COMMA);

		Expr height;
		height = expr();

		match(Kind.RSQUARE);

		return new Dimension(firstToken, width, height);

	}

	private Statement statement() throws SyntaxException, LexicalException {

		IToken firstToken = t;
		LValue lvalue;

		if (isKind(Kind.IDENT)) { //no consume consume in lval
			lvalue = lvalue();

			match(Kind.ASSIGN);

			Expr expr = expr();

			return new AssignmentStatement(firstToken, lvalue, expr);

		} else if (matches(Kind.RES_write)) { //write Expr

			Expr expr;
			expr = expr();

			return new WriteStatement(firstToken, expr);

		} else if (matches(Kind.RES_do)) {

			List < GuardedBlock > guardedBlockList = new ArrayList < > ();
			matches(RES_do);
			GuardedBlock guardB = guardedBlock();
			guardedBlockList.add(guardB);

			while (matches(Kind.BOX)) {

				GuardedBlock gB = guardedBlock();

				guardedBlockList.add(gB);

			}

			match(Kind.RES_od);

			return new DoStatement(firstToken, guardedBlockList);

		} else if (matches(Kind.RES_if)) {
			List < GuardedBlock > guardBlockList = new ArrayList < > ();

			GuardedBlock guardBlock = guardedBlock();

			guardBlockList.add(guardBlock);

			while (matches(Kind.BOX)) {

				GuardedBlock guardedBlock = guardedBlock();

				guardBlockList.add(guardedBlock);

			}

			match(Kind.RES_fi);
			return new IfStatement(firstToken, guardBlockList);
		} else if (matches(Kind.RETURN)) {
			Expr expr = expr();
			return new ReturnStatement(firstToken, expr);
		} else if (isKind(Kind.BLOCK_OPEN)) {
			Block block = block();
			return new StatementBlock(firstToken, block);

		}
		throw new SyntaxException(t.sourceLocation(), "Statement issue");

	}

	private LValue lvalue() throws LexicalException, SyntaxException {

		IToken firstToken = t;
		IToken name = firstToken;
		match(IDENT);
		PixelSelector pixelSelector = null;

		ChannelSelector channelSelector = null;

		if (isKind(Kind.LSQUARE))

			pixelSelector = pixelSelector();

		if (isKind(Kind.COLON))

			channelSelector = channelSelector();

		return new LValue(firstToken, name, pixelSelector, channelSelector);

	}

	private GuardedBlock guardedBlock() throws SyntaxException, LexicalException {

		IToken firstToken = t;

		Expr guard;

		guard = expr();

		match(Kind.RARROW);

		Block block;
		block = block();

		return new GuardedBlock(firstToken, guard, block);

	}
	public boolean matches(Kind...kind) throws LexicalException { //mult parameter and consumes
		if (kind.length == 0) {
			return false;
		}
		if (isKind(kind)) {
			consume();
			return true;
		}
		return false;
	}
	public boolean match(Kind kind) throws SyntaxException, LexicalException { //single parameter doesn't consume

		if (!matches(kind)) {

			throw new SyntaxException("not right kind for AST"+"expected type "+kind+"actual"+t.kind());

		}

		return true;

	}

	public boolean matches(Kind kind) throws LexicalException { //single parameter and consumes

		if (isKind(kind)) {
			consume();
			return true;
		}
		return false;
	}

	public boolean isKind(Kind...kinds) throws LexicalException { //mult param doesn't consume

		if (kinds.length == 0) {
			return false;
		}
		if (t.kind() != EOF) {
			return Arrays.stream(kinds).anyMatch(kind -> t.kind() == kind);
		}

		if (t.kind() == EOF) { // if EOF, throw a LexicalException.
			throw new LexicalException(t.sourceLocation() + "EOF");
		}
		return false;
	}
	public boolean isKind(Kind kinds) throws LexicalException { //single parameter doesn't consume

		if (t.kind() == kinds) {
			return true;
		}

		if (t.kind() == Kind.EOF) {
			throw new LexicalException(t.sourceLocation() + "EOF");
		}
		return false;
	}

	public IToken consume() {
		if (t.kind() == Kind.EOF) {
			return t;
		} else
			t = tokens.get(++counter);
		return t;
	}

}