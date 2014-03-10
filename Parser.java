package cop5555fa13;

import java.util.ArrayList;
import java.util.List;

import cop5555fa13.TokenStream;
import cop5555fa13.TokenStream.LexicalException;
import cop5555fa13.TokenStream.Token;
import cop5555fa13.TokenStream.Kind;
import cop5555fa13.ast.AlternativeStmt;
import cop5555fa13.ast.AssignExprStmt;
import cop5555fa13.ast.AssignPixelStmt;
import cop5555fa13.ast.AssignStmt;
import cop5555fa13.ast.BinaryExpr;
import cop5555fa13.ast.BooleanLitExpr;
import cop5555fa13.ast.ConditionalExpr;
import cop5555fa13.ast.PreDefExpr;
import cop5555fa13.ast.Dec;
import cop5555fa13.ast.Expr;
import cop5555fa13.ast.FileAssignStmt;
import cop5555fa13.ast.IdentExpr;
import cop5555fa13.ast.ImageAttributeExpr;
import cop5555fa13.ast.IntLitExpr;
import cop5555fa13.ast.IterationStmt;
import cop5555fa13.ast.PauseStmt;
import cop5555fa13.ast.Pixel;
import cop5555fa13.ast.Program;
import cop5555fa13.ast.SampleExpr;
import cop5555fa13.ast.ScreenLocationAssignmentStmt;
import cop5555fa13.ast.SetVisibleAssignmentStmt;
import cop5555fa13.ast.ShapeAssignmentStmt;
import cop5555fa13.ast.SinglePixelAssignmentStmt;
import cop5555fa13.ast.SingleSampleAssignmentStmt;
import cop5555fa13.ast.Stmt;
import static cop5555fa13.TokenStream.Kind.*;

public class Parser {

	@SuppressWarnings("serial")
	public class SyntaxException extends Exception {
		Token t;

		public SyntaxException(Token t, String msg) {
			super(msg);
			this.t = t;
		}

		public String toString() {
			return super.toString() + "\n" + t.toString();
		}

		public Kind getKind() {
			return t.kind;
		}
	}

	// TokenStream stream;
	private int i = 0;
	private Token t;
	private final TokenStream stream;
//	private int RBRACEcounter;

	Token progName;  //keep the program name in case you don't generate an AST
	List<SyntaxException> errorList;  //save the error for grading purposes
	// private boolean pauseInd = false;

	/* You will need additional fields */

	/**
	 * creates a simple parser.
	 * 
	 * @param initialized_stream
	 *            a TokenStream that has already been initialized by the Scanner
	 * @throws LexicalException
	 */
	public Parser(TokenStream initialized_stream) throws LexicalException {
		this.stream = initialized_stream;
		errorList = new ArrayList<SyntaxException>();
		/* You probably want to do more here */
		Scanner s = new Scanner(stream);
		s.scan();
		consume();
	}

	/* just localize dealing with IOException from next method */
	void consume() {
		t = stream.getToken(i++);
	}

	/*
	 * This method parses the input from the given token stream. If the input is
	 * correct according to the phrase structure of the language, it returns
	 * normally. Otherwise it throws a SyntaxException containing the Token
	 * where the error was detected and an appropriate error message. The
	 * contents of your error message will not be graded, but the "kind" of the
	 * token will be.
	 */
	public Program parse(){
		/* You definitely need to do more here */
		Program p = null;
		try {
			p = parseProgram();
			match(EOF);
		} catch (SyntaxException e) {
			System.out.println("adding exception: actual:" + t.kind + " " + t.getText() + " " + e.getMessage());
			errorList.add(e);
		}
		if (errorList.isEmpty()) {
			return p;
		} else
			return null;
	}
	
	public List<SyntaxException> getErrorList(){
//		System.out.println("error list length: " + errorList.size());
		return errorList;
	}
	
	public String getProgName(){
		return (progName != null ?  progName.getText() : "no program name");
	}
	
	/**
	 * Program ::= ident { Dec* Stmt* }
	 * 
	 * @return Program
	 * @throws SyntaxException
	 */
	private Program parseProgram() throws SyntaxException {
		progName = match(IDENT);
		match(LBRACE);
		List<Dec> decList = new ArrayList<Dec>();
		while (inFirstDec()) {
			//YOU PROBABLY HAVE SOMETHING LIKE JUST A CALL TO parseDec
			//PUT IT IN A TRY-CATCH BLOCK, and IF AN EXCEPTION IS THROWN,
			//TRY TO SKIP TOKENS UNTIL YOU GET TO EITHER A SEMI, OR
			//THE BEGINNING OF A NEW DEC. 
			try{
				decList.add(parseDec());
			}
			catch(SyntaxException e){
				System.out.println("Dec adding exception: actual:" + t.kind + " " + t.getText() + " " + e.getMessage());
				errorList.add(e);
				//skip tokens until next semi, consume it, then continue parsing
				while (!isKind(SEMI, image, _int, _boolean, pixel, RBRACE, EOF)){
					consume(); 
				}
				if (isKind(SEMI)){consume();}  //IF A SEMI, CONSUME IT BEFORE CONTINUING
				else if (isKind(RBRACE)){break;}
			}
		}

		List<Stmt> stmtList = new ArrayList<Stmt>();
		//ADD YOUR OWN CODE TO DEAL WITH STATEMENTS
		while (inFirstStmt()){
			try{
				Stmt currStmt = parseStmt();
				if(currStmt != null){
					stmtList.add(currStmt);
				}
			}
			catch(SyntaxException e){
				System.out.println("Stmt adding exception: actual:" + t.kind + " " + t.getText() + " " + e.getMessage());
				errorList.add(e);
				int bracecounter = 0;
				while (!isKind(SEMI, pause, _while, _if, EOF)){
					if(isKind(LBRACE)) {
						bracecounter += 1;
//						System.out.println("statement bracecounter is: " + bracecounter);
					}
					if(isKind(RBRACE)){
//						System.out.println("statement bracecounter is: " + bracecounter);
						if(bracecounter == 0) break;
						else bracecounter -= 1;
					}
					consume(); 
				}
				if (isKind(SEMI)){consume();}  //IF A SEMI, CONSUME IT BEFORE CONTINUING
			}
		}
		match(RBRACE);

		//After parsing has finished- if there were no errors, create and return a Program node, which is the AST of the program
		if (errorList.isEmpty()) return new Program(progName, decList, stmtList);
		//otherwise print out the errors and return null
		//the test program will also look at the error list
		System.out.println("error" + (errorList.size()>1?"s parsing program ":" parsing program ") + getProgName());
		for(SyntaxException e: errorList){		
			System.out.println(e.getMessage() + " at line" + e.t.getLineNumber());
		}
		
		System.out.println("error list length: " + errorList.size());
		for (int i = 0 ; i < errorList.size(); i++){
			System.out.println(errorList.get(i));
		}
		return null;
	}
	
	/**
	 * Dec ::= Type IDENT ;
	 * 
	 * @return Dec
	 * @throws SyntaxException
	 */
	private Dec parseDec() throws SyntaxException {
		try {
			Token aToken = stream.new Token(t.kind, t.beg, t.end);
			consume();
			Token ident = match(IDENT);
			match(SEMI);
			return new Dec(aToken.kind, ident);
		} catch (SyntaxException se){
			throw se;
		}
	}
	
	/**
	 * Stmt ::= ; | AssignStmt | PauseStmt | IterationStmt | AlternativeStmt
	 * 
	 * @return Stmt
	 * @throws SyntaxException
	 */
	private Stmt parseStmt() throws SyntaxException {
		Stmt currStmt = null;
		try{
			if (isKind(SEMI)) {
				consume();
				return null;
			} else if (isKind(IDENT)) {
				currStmt = parseAssignStmt();
			} else if (isKind(pause)) {
				currStmt = parsePauseStmt();
			} else if (isKind(_while)) {
				currStmt = parseIterationStmt();
			} else if (isKind(_if)) {
				currStmt = parseAlternativeStmt();
			} else {
				error("Invalid statement");
			}
			return currStmt;
		} catch (SyntaxException se){
			throw se;
		}
	}
	
	private AssignStmt parseAssignStmt() throws SyntaxException{
		AssignStmt currAssignStmt = null;
		Token ident = null;
		try{
			ident = stream.new Token(t.kind, t.beg, t.end);
			consume();	//IDENT
			if (isKind(ASSIGN)) {
				consume();
				if (inFirstExpr()) {
					Expr expr = parseExpr();
					match(SEMI);
					currAssignStmt = new AssignExprStmt(ident, expr);
				} else if (inFirstPixel()) {
					Pixel aPixel = parsePixel();
					match(SEMI);
					currAssignStmt = new AssignPixelStmt(ident, aPixel);
				} else if (isKind(STRING_LIT)) {
					Token fileName = stream.new Token(t.kind, t.beg, t.end);
					consume();
					match(SEMI);
					currAssignStmt = new FileAssignStmt(ident, fileName);
				} else {
					error("Invalid assignment for IDENT");
				}
			} else if (isKind(DOT)) {
				consume();
				if (isKind(pixels)) {
					consume();
					match(LSQUARE);
					Expr xExpr = parseExpr();
					match(COMMA);
					Expr yExpr = parseExpr();
					match(RSQUARE);
					if (isKind(ASSIGN)) {
						consume();
						Pixel aPixel = parsePixel();
						match(SEMI);
						currAssignStmt = new SinglePixelAssignmentStmt(ident, xExpr, yExpr, aPixel);
					} else if (isKind(red, green, blue)) {
						Token color = stream.new Token(t.kind, t.beg, t.end);
						consume();
						match(ASSIGN);
						Expr rhsExpr = parseExpr();
						match(SEMI);
						currAssignStmt = new SingleSampleAssignmentStmt(ident, xExpr, yExpr, color, rhsExpr);
					} else {
						error("Invalid format for pixels assignment");
					}
				}
				else if(isKind(shape)){
					consume();
					match(ASSIGN);
					match(LSQUARE);
					Expr width = parseExpr();
					match(COMMA);
					Expr height = parseExpr();
					match(RSQUARE);
					match(SEMI);
					currAssignStmt = new ShapeAssignmentStmt(ident, width, height);
				}
				else if(isKind(location)){
					consume();
					match(ASSIGN);
					match(LSQUARE);
					Expr xScreenExpr = parseExpr();
					match(COMMA);
					Expr yScreenExpr = parseExpr();
					match(RSQUARE);
					match(SEMI);
					currAssignStmt = new ScreenLocationAssignmentStmt(ident, xScreenExpr, yScreenExpr);
				}
				else if(isKind(visible)){
					consume();
					match(ASSIGN);
					Expr expr = parseExpr();
					match(SEMI);
					currAssignStmt = new SetVisibleAssignmentStmt(ident, expr);
				}
				else {
					error("Invalid value after 'IDENT.', expected: pixels, shape, location visible");
				}
			} else {
				error("AssignStmt expected with ident followed by '=' or ','");
			}
		} catch(SyntaxException se){
			throw se;
		}
		return currAssignStmt;
	}
	
	/**
	 * Expr ::= OrExpr ( nil | ? Expr : Expr )
	 * 
	 * @return Expr
	 * @throws SyntaxException
	 */
	private Expr parseExpr() throws SyntaxException{
		Expr currExpr = null;
		try{
			currExpr = parseOrExpr();
			if(isKind(QUESTION)){
				consume();
				Expr currExprT = parseExpr();
				match(COLON);
				Expr currExprF = parseExpr();
				currExpr = new ConditionalExpr(currExpr, currExprT, currExprF);
			}
		} catch(SyntaxException se){
			throw se;
		}
		return currExpr;
	}
	
	/**
	 * OrExpr ::= AndExpr ( | AndExpr )*
	 * 
	 * @return Expr
	 * @throws SyntaxException
	 */
	private Expr parseOrExpr() throws SyntaxException{
		Expr currOrExpr = null;
		try{
			currOrExpr = parseAndExpr();
			while(isKind(OR)){
				Token op = stream.new Token(t.kind, t.beg, t.end);
				consume();
				Expr andExpr = parseAndExpr();
				currOrExpr = new BinaryExpr(currOrExpr, op, andExpr);
			}
		} catch(SyntaxException se){
			throw se;
		}
		return currOrExpr;
	}
	
	/**
	 * AndExpr ::= EqualityExpr ( & EqualityExpr )*
	 * 
	 * @return Expr
	 * @throws SyntaxException
	 */
	private Expr parseAndExpr() throws SyntaxException{
		Expr currAndExpr = null;
		try{
			currAndExpr = parseEqualityExpr();
			while(isKind(AND)){
				Token op = stream.new Token(t.kind, t.beg, t.end);
				consume();
				Expr equalityExpr = parseEqualityExpr();
				currAndExpr = new BinaryExpr(currAndExpr, op, equalityExpr);
			}
		} catch(SyntaxException se){
			throw se;
		}
		return currAndExpr;
	}
	
	/**
	 * EqualityExpr ::= RelExpr ( (==|!=) RelExpr)*
	 * 
	 * @return Expr
	 * @throws SyntaxException
	 */
	private Expr parseEqualityExpr() throws SyntaxException{
		Expr currEqualityExpr = null;
		try{
			currEqualityExpr = parseRelExpr();
			while(isKind(EQ, NEQ)){
				Token op = stream.new Token(t.kind, t.beg, t.end);
				consume();
				Expr relExpr = parseRelExpr();
				currEqualityExpr = new BinaryExpr(currEqualityExpr, op, relExpr);
			}
		} catch(SyntaxException se){
			throw se;
		}
		return currEqualityExpr;
	}
	
	/**
	 * RelExpr ::= ShiftExpr ( (<|>|<=|>=) ShiftExpr )*
	 * 
	 * @return Expr
	 * @throws SyntaxException
	 */
	private Expr parseRelExpr() throws SyntaxException{
		Expr currRelExpr = null;
		try{
			currRelExpr = parseShiftExpr();
			while(isKind(LT, GT, LEQ, GEQ)){
				Token op = stream.new Token(t.kind, t.beg, t.end);
				consume();
				Expr shiftExpr =  parseShiftExpr();
				currRelExpr = new BinaryExpr(currRelExpr, op, shiftExpr);
			}
		} catch(SyntaxException se){
			throw se;
		}
		return currRelExpr;
	}
	
	/**
	 * ShiftExpr ::= AddExpr ( (<<|>>) AddExpr )*
	 * 
	 * @return Expr
	 * @throws SyntaxException
	 */
	private Expr parseShiftExpr() throws SyntaxException{
		Expr currShiftExpr = null;
		try{
			currShiftExpr = parseAddExpr();
			while(isKind(LSHIFT, RSHIFT)){
				Token op = stream.new Token(t.kind, t.beg, t.end);
				consume();
				Expr addExpr = parseAddExpr();
				currShiftExpr = new BinaryExpr(currShiftExpr, op, addExpr);
			}
		} catch(SyntaxException se){
			throw se;
		}
		return currShiftExpr;
	}
	
	/**
	 * AddExpr ::= MultExpr ( (+|-) MultExpr )*
	 * 
	 * @return Expr
	 * @throws SyntaxException
	 */
	private Expr parseAddExpr() throws SyntaxException{
		Expr currAddExpr = null;
		try{
			currAddExpr = parseMultExpr();
			while(isKind(PLUS, MINUS)){
				Token op = stream.new Token(t.kind, t.beg, t.end);
				consume();
				Expr multExpr = parseMultExpr();
				currAddExpr = new BinaryExpr(currAddExpr, op, multExpr);
			}
		} catch(SyntaxException se){
			throw se;
		}
		return currAddExpr;
	}
	
	/**
	 * MultExpr ::= PrimaryExpr ( (*|/|%) PrimaryExpr )*
	 * 
	 * @return Expr
	 * @throws SyntaxException
	 */
	private Expr parseMultExpr() throws SyntaxException{
		Expr currMultExpr = null;
		try{
			currMultExpr = parsePrimaryExpr();
			while(isKind(TIMES, DIV, MOD)){
				Token op = stream.new Token(t.kind, t.beg, t.end);
				consume();
				Expr primaryExpr = parsePrimaryExpr();
				currMultExpr = new BinaryExpr(currMultExpr, op, primaryExpr);
			}
		} catch(SyntaxException se){
			throw se;
		}
		return currMultExpr;
	}

	/**
	 * PrimaryExpr ::= IDEN | INT_LIT | BOOLEAN_LIT | x | y | z | SCREEN_SIZE |
	 *                 ( Expr ) | IDENT[ Expr , Expr] (red|green|blue) | IDEN.height |
	 *                 IDEN.width | IDENT.x_loc | IDENT.y_loc
	 * 
	 * @return Expr
	 * @throws SyntaxException
	 */
	private Expr parsePrimaryExpr() throws SyntaxException{
		Expr primaryExpr = null;
		try{
			if(isKind(IDENT)){
				Token ident = stream.new Token(t.kind, t.beg, t.end);
				consume();
				if(isKind(LSQUARE)){
					consume();
					Expr xLoc = parseExpr();
					match(COMMA);
					Expr yLoc = parseExpr();
					match(RSQUARE);
					if(isKind(red, green, blue)){
						Token color = stream.new Token(t.kind, t.beg, t.end);
						primaryExpr = new SampleExpr(ident, xLoc, yLoc, color);
						consume();
					}
					else{
						error("red/green/blue expected");
					}
				}
				else if(isKind(DOT)){
					consume();
					if(isKind(height, width, x_loc, y_loc)){
						Token selector = stream.new Token(t.kind, t.beg, t.end);
						primaryExpr = new ImageAttributeExpr(ident, selector);
						consume();
					}
					else{
						error("height/width/x_loc/y_loc expected");
					}
				}
				else{
					primaryExpr = new IdentExpr(ident);
				}
			}
			else if (isKind(INT_LIT)){
				Token intLit = stream.new Token(t.kind, t.beg, t.end);
				primaryExpr = new IntLitExpr(intLit);
				consume();
			}
			else if (isKind(BOOLEAN_LIT)){
				Token booleanLit = stream.new Token(t.kind, t.beg, t.end);
				primaryExpr = new BooleanLitExpr(booleanLit);
				consume();
			}
			else if (isKind(x, y, Z, SCREEN_SIZE)){
				Token preDef = stream.new Token(t.kind, t.beg, t.end);
				primaryExpr = new PreDefExpr(preDef);
				consume();
			}
			else if(isKind(LPAREN)){
				consume();
				primaryExpr = parseExpr();
				match(RPAREN);
			}
			else {
				error("Invalid PrimaryExpr");
			}
		} catch(SyntaxException se){
			throw se;
		}
		return primaryExpr;
	}
	
	private Pixel parsePixel() throws SyntaxException{
		Pixel aPixel = null;
		int bracecounter = 0;
		try{
			consume();
			bracecounter += 1;
//			System.out.println("Pixel bracecounter is: " + bracecounter);
			match(LBRACE);
			bracecounter += 1;
//			System.out.println("Pixel bracecounter is: " + bracecounter);
			Expr redExpr = parseExpr();
			match(COMMA);
			Expr greenExpr = parseExpr();
			match(COMMA);
			Expr blueExpr = parseExpr();
			match(RBRACE);
			bracecounter -= 1;
//			System.out.println("Pixel bracecounter is: " + bracecounter);
			match(RBRACE);
			bracecounter -= 1;
//			System.out.println("Pixel bracecounter is: " + bracecounter);
			aPixel = new Pixel(redExpr, greenExpr, blueExpr);
		} catch(SyntaxException se){
			System.out.println("Pixel adding exception: actual:" + t.kind + " " + t.getText() + " " + se.getMessage());
			errorList.add(se);
			while (!isKind(SEMI, pause, _while, _if, EOF)){
				if(isKind(RBRACE)) {
					consume();
					bracecounter -= 1;
//					System.out.println("Pixel bracecounter is: " + bracecounter);
					if(bracecounter == 0) {break;}
				}
				else {consume();} 
			}
			return null;
		}
		return aPixel;
	}
	
	private PauseStmt parsePauseStmt() throws SyntaxException{
		PauseStmt pauseStmt = null;
		try{
			consume();	// consume pause
			Expr expr = parseExpr();
			match(SEMI);
			pauseStmt = new PauseStmt(expr);
		} catch(SyntaxException se){
			throw se;
		}
		return pauseStmt;
	}
	
	private IterationStmt parseIterationStmt() throws SyntaxException{
		IterationStmt iterationStmt = null;
		List<Stmt> stmtList = null;
		Expr expr = null;
		try{
			consume();	// consume _while
			match(LPAREN);
			expr = parseExpr();
			match(RPAREN);
			match(LBRACE);
		} catch(SyntaxException se){
			System.out.println("while adding exception: actual:" + t.kind + " " + t.getText() + " " + se.getMessage());
			errorList.add(se);
/*			while (!isKind(LBRACE)) {
				consume();
			}
			consume();*/
		}
		try{
			stmtList = new ArrayList<Stmt>();
			while (inFirstStmt()) {
				try{
					Stmt currStmt = parseStmt();
					if(currStmt != null){
						stmtList.add(currStmt);
					}
				} catch(SyntaxException se){
					System.out.println("while adding exception: actual:" + t.kind + " " + t.getText() + " " + se.getMessage());
					errorList.add(se);
					int bracecounter = 0;
					while (!isKind(SEMI, pause, _while, _if, EOF)) {
						if(isKind(LBRACE)) {
							bracecounter += 1;
							System.out.println("while bracecounter is: " + bracecounter);
						}
						if(isKind(RBRACE)){
							System.out.println("while bracecounter is: " + bracecounter);
							if(bracecounter == 0) break;
							else bracecounter -= 1;
						}
						consume();
					}
					if (isKind(SEMI)){consume();}  //IF A SEMI, CONSUME IT BEFORE CONTINUING
					else if (isKind(RBRACE)){break;}
				}
			}
			match(RBRACE);
		} catch(SyntaxException se){
			System.out.println("while adding exception: actual:" + t.kind + " " + t.getText() + " " + se.getMessage());
			errorList.add(se);
			while (!isKind(RBRACE)) {
				consume();
			}
			consume();
		}
		iterationStmt = new IterationStmt(expr, stmtList); 
		return iterationStmt;
	}
	
	private AlternativeStmt parseAlternativeStmt() throws SyntaxException{
		AlternativeStmt alternativeStmt = null;
		Expr expr = null;
		List<Stmt> ifStmtList = null;
		List<Stmt> elseStmtList = null;
		try{
			consume();	// consume _if
			match(LPAREN);
			expr = parseExpr();
			match(RPAREN);
			match(LBRACE);
		} catch(SyntaxException se){
			System.out.println("if adding exception: actual:" + t.kind + " " + t.getText() + " " + se.getMessage());
			errorList.add(se);
/*			while (!isKind(LBRACE)) {
				consume();
			}
			consume();*/
		}
		try{
			ifStmtList = new ArrayList<Stmt>();
			elseStmtList = new ArrayList<Stmt>();
			while (inFirstStmt()) {
				try{
					Stmt currIfStmt = parseStmt();
					if(currIfStmt != null){
						ifStmtList.add(currIfStmt);
					}
				} catch(SyntaxException se){
					System.out.println("if adding exception: actual:" + t.kind + " " + t.getText() + " " + se.getMessage());
					errorList.add(se);
	//					System.out.println("current token: " + t.kind + " " + t.getText());
					int bracecounter = 0;
					while (!isKind(SEMI, pause, _while, _if, EOF)){
						if(isKind(LBRACE)) {
							bracecounter += 1;
							System.out.println("if bracecounter is: " + bracecounter);
						}
						if(isKind(RBRACE)){
							System.out.println("if bracecounter is: " + bracecounter);
							if(bracecounter == 0) break;
							else bracecounter -= 1;
						}
						consume(); 
					}
					if (isKind(SEMI)){consume();}  //IF A SEMI, CONSUME IT BEFORE CONTINUING
					else if (isKind(RBRACE)){break;}
				}
			}
			match(RBRACE);
		} catch(SyntaxException se){
			System.out.println("if adding exception: actual:" + t.kind + " " + t.getText() + " " + se.getMessage());
			errorList.add(se);
			while (!isKind(RBRACE)) {
				consume();
			}
			consume();
		}

		try{
			if (isKind(_else)) {
				consume();
				match(LBRACE);
				while (inFirstStmt()) {
					try{
						Stmt currElseStmt = parseStmt();
						if(currElseStmt != null){
							elseStmtList.add(currElseStmt);
						}
					} catch(SyntaxException se){
						System.out.println("else adding exception: actual:" + t.kind + " " + t.getText() + " " + se.getMessage());
						errorList.add(se);
						int bracecounter = 0;
						while (!isKind(SEMI, pause, _while, _if, EOF)){ 
							if(isKind(LBRACE)) {
								bracecounter += 1;
								System.out.println("else bracecounter is: " + bracecounter);
							}
							if(isKind(RBRACE)){
								System.out.println("else bracecounter is: " + bracecounter);
								if(bracecounter == 0) break;
								else bracecounter -= 1;
							}
							consume(); 
						}
						if (isKind(SEMI)){consume();}  //IF A SEMI, CONSUME IT BEFORE CONTINUING
						else if (isKind(RBRACE)){break;}
					}
				}
				match(RBRACE);
			} 
		}catch(SyntaxException se){
			System.out.println("else adding exception: actual:" + t.kind + " " + t.getText() + " " + se.getMessage());
			errorList.add(se);
			while (!isKind(RBRACE)) {
				consume();
			}
			consume();
		}
		alternativeStmt = new AlternativeStmt(expr, ifStmtList, elseStmtList);
		return alternativeStmt;
	}
		
	private boolean inFirstDec() {
		if (isKind(image) || isKind(pixel) || isKind(_int) || isKind(_boolean)) {
			return true;
		}
		return false;
	}
	
	private boolean inFirstStmt() {
		if (isKind(SEMI) || isKind(IDENT) || isKind(pause) || isKind(_while)
				|| isKind(_if)) {
			return true;
		}
		return false;
	}
	
	private boolean inFirstExpr() {
		if (isKind(IDENT) || isKind(INT_LIT) || isKind(BOOLEAN_LIT)
				|| isKind(x) || isKind(y) || isKind(Z) || isKind(SCREEN_SIZE)
				|| isKind(LPAREN)) {
			return true;
		}
		return false;
	}

	private boolean inFirstPixel() {
		return isKind(LBRACE);
	}

	/*
	 * Java hint -- Methods with a variable number of parameters may be useful.
	 * For example, this method takes a token and variable number of "kinds",
	 * and indicates whether the kind of the given token is among them. The Java
	 * compiler creates an array holding the given parameters.
	 */
	private boolean isKind(Kind... kinds) {
		Kind k = t.kind;
		for (int i = 0; i != kinds.length; ++i) {
			if (k == kinds[i])
				return true;
		}
		return false;
	}

	Token match(Kind kind) throws SyntaxException {
		Token orinToken = null;
		if (isKind(kind)) {
			orinToken = t;
			consume();
		} else{
			if(SEMI.equals(kind)){
				System.out.println("Match adding exception: actual:" + t.kind + " " + t.getText() + " expected SEMI");
				errorList.add(new SyntaxException(t, "expected SEMI"));
			}
			else{
				error("expected " + kind);
			}
		}
		return orinToken;
	}

	void error(String msg) throws SyntaxException {
		// deal with error
		throw new SyntaxException(t, msg);
	}

}
