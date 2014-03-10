package cop5555fa13;

import static cop5555fa13.TokenStream.Kind.AND;
import static cop5555fa13.TokenStream.Kind.ASSIGN;
import static cop5555fa13.TokenStream.Kind.COLON;
import static cop5555fa13.TokenStream.Kind.COMMA;
import static cop5555fa13.TokenStream.Kind.COMMENT;
import static cop5555fa13.TokenStream.Kind.DIV;
import static cop5555fa13.TokenStream.Kind.DOT;
import static cop5555fa13.TokenStream.Kind.EOF;
import static cop5555fa13.TokenStream.Kind.EQ;
import static cop5555fa13.TokenStream.Kind.GEQ;
import static cop5555fa13.TokenStream.Kind.GT;
import static cop5555fa13.TokenStream.Kind.IDENT;
import static cop5555fa13.TokenStream.Kind.INT_LIT;
import static cop5555fa13.TokenStream.Kind.LEQ;
import static cop5555fa13.TokenStream.Kind.LPAREN;
import static cop5555fa13.TokenStream.Kind.LSHIFT;
import static cop5555fa13.TokenStream.Kind.LSQUARE;
import static cop5555fa13.TokenStream.Kind.LT;
import static cop5555fa13.TokenStream.Kind.MINUS;
import static cop5555fa13.TokenStream.Kind.MOD;
import static cop5555fa13.TokenStream.Kind.NEQ;
import static cop5555fa13.TokenStream.Kind.NOT;
import static cop5555fa13.TokenStream.Kind.OR;
import static cop5555fa13.TokenStream.Kind.PLUS;
import static cop5555fa13.TokenStream.Kind.QUESTION;
import static cop5555fa13.TokenStream.Kind.RPAREN;
import static cop5555fa13.TokenStream.Kind.RSHIFT;
import static cop5555fa13.TokenStream.Kind.RSQUARE;
import static cop5555fa13.TokenStream.Kind.SEMI;
import static cop5555fa13.TokenStream.Kind.TIMES;
import static cop5555fa13.TokenStream.Kind.Z;
import static cop5555fa13.TokenStream.Kind._else;
import static cop5555fa13.TokenStream.Kind._if;
import static cop5555fa13.TokenStream.Kind._int;
import static cop5555fa13.TokenStream.Kind.image;
import static cop5555fa13.TokenStream.Kind.red;
import static cop5555fa13.TokenStream.Kind.x;
import static cop5555fa13.TokenStream.Kind.y;
import static cop5555fa13.TokenStream.Kind.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import cop5555fa13.TokenStream.Kind;
import cop5555fa13.TokenStream.LexicalException;
import cop5555fa13.TokenStream.Token;

public class Scanner {

	//ADD METHODS AND FIELDS
	private enum State {
		START, GOT_EQUALS, GOT_EXCLAM, GOT_LESSTHAN, GOT_GREATERTHAN, GOT_SLASH, IDENT_PART, DIGITS, STRING, EOF, PAUSE, COMMENT
	}
	private State state;
	
	//private final String keywordStr[] = {"image", "int", "boolean", "pixel", "pixels", "blue", "red", "green", "Z", "shape", "width", "height", 
	//		"location", "x_loc", "y_loc", "SCREEN_SIZE", "visible", "x", "y", "pause", "while", "if", "else"};
	private Map<String, Kind> KWMap;

//	private final String separatorStr[] = {".", ";", ",", "(", ")", "[", "]", "{", "}", ":", "?"};
	private Map<String, Kind> SPMap;
	
//	private final String operatorStr[] = {"=", "|", "&", "!", "<", ">", "+", "-", "*", "/", "%"};
	private Map<String, Kind> OPMap;
	
//	private final String boolStr[] = {"true", "false"};
	
	// local references to TokenStream objects for convenience
	final TokenStream stream; // set in constructor
	
	final char cEOF = (char) -1;

	private int index; // points to the next char to process during scanning, or if none, past the end of the array
	
	private char ch; // the character that is handling with
	
//	private char prev; // the character that is previously handled
	
	private int buffer = 0;
    
	public Scanner(TokenStream stream) {
		//IMPLEMENT THE CONSTRUCTOR
		this.stream = stream;
		initialSPMap();
		initialOPMap();
		initialKWMap();
	}


	public void scan() throws LexicalException {
		//THIS IS PROBABLY COMPLETE
		if(stream.inputChars.length < 0){
			throw stream.new LexicalException(0, "inputChars is empty");
		}
		Token t;
		index = 0;
		state = State.START;
		try {
			do {
				t = next();
//				System.out.println("t kind is:<" + t.kind + "> and t token is:<" + t.toString() + ">");
				if (t.kind.equals(COMMENT)) {
					stream.comments.add((Token) t);
				} else
					stream.tokens.add(t);
			} while (!t.kind.equals(EOF));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Token next() throws LexicalException, IOException{
		//COMPLETE THIS METHOD.  THIS IS THE FUN PART!
		Token t = null;
		int begOffset = index - buffer;
		do {
        	if(state != State.EOF && buffer == 0){
        		getch();
        	}
    		buffer = 0;
			switch (state) { 
		    	/*in each state, check the next character. either create a token or change state*/
		        case START:
					switch (ch) {
						case (char) cEOF:
							state = State.EOF;
							break; // end of file
						case '=':
							state = State.GOT_EQUALS;
							break;
						case '!':
							state = State.GOT_EXCLAM;
							break;
						case '<':
							state = State.GOT_LESSTHAN;
							break;
						case '>':
							state = State.GOT_GREATERTHAN;
							break;
						case '0':
							t = stream.new Token(INT_LIT, begOffset, index);
							break;
						case '/':
							state = State.GOT_SLASH;
							break;
						case '"':
							state = State.STRING;
							break;
						default:
							if (Character.isDigit(ch)) {
								state = State.DIGITS;
							} else if (Character.isJavaIdentifierStart(ch)) {
								state = State.IDENT_PART;
							} else if (isWhiteSpace(ch)) {
								begOffset++;
							} else if (isSeparator(ch)) {
								t = stream.new Token(SPMap.get(String.valueOf(ch)), begOffset, index);
							} else if (isOperators(ch)) {
								t = stream.new Token(OPMap.get(String.valueOf(ch)), begOffset, index);
							} else {
								// handle error
								throw stream.new LexicalException(index, "Invalid character:" + ch);
							}
					}
		  			break; // end of state START
		        case DIGITS:
		        	switch (ch) {
					case (char) cEOF:
						t = stream.new Token(INT_LIT, begOffset, index);
						state = State.EOF;
						break; // end of file
					default:
						if (Character.isDigit(ch)) {
//							state = State.DIGITS;
						} else {
							t = stream.new Token(INT_LIT, begOffset, index-1);
							buffer = 1;
							state = State.START;
						}
		        	}
		        	break; // end of state DIGIT
		        case STRING:
		        	switch (ch) {
					case (char) cEOF:
						throw stream.new LexicalException(index, "Quotes dismatched");
					case '"':
						t = stream.new Token(STRING_LIT, begOffset, index);
						state = State.START;
						break;
		        	}
		        	break; // end of state STRING
		        case IDENT_PART:
		        	String currStr;
		        	switch (ch) {
					case (char) cEOF:
						currStr = String.valueOf(stream.inputChars, begOffset, index - begOffset);
						if(isKeyword(currStr)){
							t = stream.new Token(KWMap.get(currStr), begOffset, index);
						}
						else if(isBoolean(currStr)){
							t = stream.new Token(BOOLEAN_LIT, begOffset, index);
						}
						else {
							t = stream.new Token(IDENT, begOffset, index);
						}
						state = State.EOF;
						break; // end of file
					default:
						if (!Character.isJavaIdentifierPart(ch)) {
							currStr = String.valueOf(stream.inputChars, begOffset, index - begOffset - 1);
							if(isKeyword(currStr)){
								t = stream.new Token(KWMap.get(currStr), begOffset, index - 1);
							}
							else if(isBoolean(currStr)){
								t = stream.new Token(BOOLEAN_LIT, begOffset, index - 1);
							}
							else {
								t = stream.new Token(IDENT, begOffset, index - 1);
							}
							buffer = 1;
							state = State.START;
						} 
		        	}
		        	break; // end of state IDENT
		        case GOT_EQUALS:
		        	switch (ch) {
					case (char) cEOF:
						t = stream.new Token(ASSIGN, begOffset, index);
						state = State.EOF;
						break; // end of file
					case '=':
						t = stream.new Token(EQ, begOffset, index);
						state = State.START;
						break;
					default:
						t = stream.new Token(ASSIGN, begOffset, index-1);
						state = State.START;
						buffer = 1;
		        	}
		        	break; // end of state EQUAL
		        case GOT_EXCLAM:
		        	switch (ch) {
					case (char) cEOF:
						t = stream.new Token(NOT, begOffset, index);
						state = State.EOF;
						break; // end of file
					case '=':
						t = stream.new Token(NEQ, begOffset, index);
						state = State.START;
						break;
					default:
						t = stream.new Token(NOT, begOffset, index-1);
						state = State.START;
						buffer = 1;
		        	}
		        	break; // end of state EXCLAM
		        case GOT_LESSTHAN:
					switch (ch) {
						case (char) cEOF:
							t = stream.new Token(LT, begOffset, index);
							state = State.EOF;
							break; // end of file
						case '=':
							t = stream.new Token(LEQ, begOffset, index);
							state = State.START;
							break;
						case '<':
							t = stream.new Token(LSHIFT, begOffset, index);
							state = State.START;
							break;
						default:
							t = stream.new Token(LT, begOffset, index-1);
							state = State.START;
							buffer = 1;
					}
		  			break; // end of state LESSTHAN
		        case GOT_GREATERTHAN:
		        	switch (ch) {
					case (char) cEOF:
						t = stream.new Token(GT, begOffset, index);
						state = State.EOF;
						break; // end of file
					case '=':
						t = stream.new Token(GEQ, begOffset, index);
						state = State.START;
						break;
					case '>':
						t = stream.new Token(RSHIFT, begOffset, index);
						state = State.START;
						break;
					default:
						t = stream.new Token(GT, begOffset, index-1);
						state = State.START;
						buffer = 1;
		        	}
		        	break; // end of state GREATERTHAN
		        case GOT_SLASH:
		        	switch (ch) {
					case (char) cEOF:
						t = stream.new Token(DIV, begOffset, index);
						state = State.EOF;
						break; // end of file
					case '/':
						state = State.COMMENT;
						break;
					default:
						t = stream.new Token(DIV, begOffset, index-1);
						state = State.START;
						buffer = 1;
		        	}
		        	break; // end of state SLASH
		        case COMMENT:
		        	switch (ch) {
					case (char) cEOF:
						t = stream.new Token(COMMENT, begOffset, index);
						state = State.EOF;
						break; // end of file
					default:
			        	if((ch == '\r' && (index < stream.inputChars.length - 1 && stream.inputChars[index+1] != '\n')) || ch == '\n'){
			        		t = stream.new Token(COMMENT, begOffset, index);
			        		state = State.START;
			        	}
		        	}
		        	break;
		        case EOF:
		        	t = stream.new Token(EOF, begOffset, index+1);
		        	break;

		        default:
		        	assert false : "should not reach here";
			}// end of switch(state)
		}   while (t == null); // loop terminates when a token is created
		return t;
	}
	
	private void getch() throws IOException {
	    //get the next char from the token stream and update index
//		System.out.println("index is: " + index + " and input length is: " + stream.inputChars.length);
		if(index == stream.inputChars.length){
			ch = (char) -1;
//			System.out.println("Getting the end of the character and the char:<" + ch + ">");
		}
		else{
			ch = stream.inputChars[index];
//			System.out.println("Getting the index:<" + index + "> and the char:<" + ch + ">");
			index++;
		}

    }
	
	private boolean isWhiteSpace(char ch){
		if (Character.isWhitespace(ch)){
			return true;
		}
		return false;
	}
	
	// 
	private boolean isKeyword(String str){
//		Set<String> kwHS = new HashSet<String>(Arrays.asList(keywordStr));
		if (KWMap.containsKey(str)){
			return true;
		}
		return false;
	}
	
	private boolean isSeparator(char ch){
//		Set<String> spHS = new HashSet<String>(Arrays.asList(separatorStr));
		if (SPMap.containsKey(String.valueOf(ch))){
			return true;
		}
		return false;
	}
	
	private boolean isOperators(char ch){
//		Set<String> opHS = new HashSet<String>(Arrays.asList(operatorStr));
		if (OPMap.containsKey(String.valueOf(ch))){
			return true;
		}
		return false;
	}
	
	private boolean isBoolean(String str){
		if ("true".equals(str) || "false".equals(str)){
			return true;
		}
		return false;
	}
	
	private void initialSPMap(){
		SPMap = new HashMap<String, Kind>();
		SPMap.put(".", DOT);
		SPMap.put(";", SEMI);
		SPMap.put(",", COMMA);
		SPMap.put("(", LPAREN);
		SPMap.put(")", RPAREN);
		SPMap.put("[", LSQUARE);
		SPMap.put("]", RSQUARE);
		SPMap.put("{", LBRACE);
		SPMap.put("}", RBRACE);
		SPMap.put(":", COLON);
		SPMap.put("?", QUESTION);
	}
	
	private void initialOPMap(){
		OPMap = new HashMap<String, Kind>();
		OPMap.put("=", ASSIGN);
		OPMap.put("|", OR);
		OPMap.put("&", AND);
		OPMap.put("==", EQ);
		OPMap.put("!=", NEQ);
		OPMap.put("<", LT);
		OPMap.put(">", GT);
		OPMap.put("<=", LEQ);
		OPMap.put(">=", GEQ);
		OPMap.put("+", PLUS);
		OPMap.put("-", MINUS);
		OPMap.put("*", TIMES);
		OPMap.put("/", DIV);
		OPMap.put("%", MOD);
		OPMap.put("!", NOT);
		OPMap.put("<<", LSHIFT);
		OPMap.put(">>", RSHIFT);
	}
	
	private void initialKWMap(){
		KWMap = new HashMap<String, Kind>();
		KWMap.put("image", image);
		KWMap.put("int", _int);
		KWMap.put("boolean", _boolean);
		KWMap.put("pixel", pixel);
		KWMap.put("pixels", pixels);
		KWMap.put("blue", blue);
		KWMap.put("red", red);
		KWMap.put("green", green);
		KWMap.put("Z", Z);
		KWMap.put("shape", shape);
		KWMap.put("width", width);
		KWMap.put("height", height);
		KWMap.put("location", location);
		KWMap.put("x_loc", x_loc);
		KWMap.put("y_loc", y_loc);
		KWMap.put("SCREEN_SIZE", SCREEN_SIZE);
		KWMap.put("visible", visible);
		KWMap.put("x", x);
		KWMap.put("y", y);
		KWMap.put("pause", pause);
		KWMap.put("while", _while);
		KWMap.put("if", _if);
		KWMap.put("else", _else);
	}
}
