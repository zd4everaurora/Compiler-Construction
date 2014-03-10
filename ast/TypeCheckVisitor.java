package cop5555fa13.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cop5555fa13.TokenStream.Kind;

public class TypeCheckVisitor implements ASTVisitor {

	private HashMap<String, SymbolTableEntry> symbolTable = null;
	private List<ASTNode> errorNodeList = null;
	private StringBuilder errorLog = null;
	private String programName = "";
//	StringBuilder sb;

/*	public String getString() {
		return sb.toString();
	}*/
	
	public TypeCheckVisitor(){
		symbolTable = new HashMap<String, SymbolTableEntry>();
		errorNodeList = new ArrayList<ASTNode>();
		errorLog = new StringBuilder();
//		sb = new StringBuilder();
	}
	
	public List<ASTNode> getErrorNodeList() {
		return errorNodeList;
	}

	public String getLog() {
		return errorLog.toString();
	}
	
	public boolean isCorrect(){
		return errorNodeList.size() == 0;
	}
	
	private void check(boolean correct, ASTNode aNode, String errMsg) throws Exception{
		if(correct){}
		else{
			errorNodeList.add(aNode);
			errorLog.append(errMsg);
		}
	}
	
	private Kind lookupType(String aIdent) throws Exception{
		if(symbolTable.containsKey(aIdent)){
			return symbolTable.get(aIdent).getaKind();
		}
		return null;
	}

	@Override
	public Object visitDec(Dec dec, Object arg){
		String ident = dec.ident.getText();
		if(symbolTable.containsKey(ident)){
//			throw new Exception("");
			errorNodeList.add(dec);
			errorLog.append(ident + " is already defined!\n");
		}
		else if(programName.equals(ident)){
			errorNodeList.add(dec);
			errorLog.append(ident + " has the same name as the program!\n");
		}
		SymbolTableEntry decEntry = new SymbolTableEntry(dec.type);
//		System.out.println("Putting: " + ident + " , " + decEntry.getaKind());
		symbolTable.put(ident, decEntry);
		return null;
	}

	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
//		String indent = arg + "  ";
		programName = program.getProgName();
		for (Dec dec: program.decList){dec.visit(this, arg);}
		for (Stmt stmt: program.stmtList){stmt.visit(this, arg);}
		return null;
	}

	@Override
	public Object visitAlternativeStmt(AlternativeStmt alternativeStmt,
			Object arg) throws Exception {
		Kind type = (Kind)alternativeStmt.expr.visit(this,null);
		check(type == Kind._boolean, alternativeStmt, " expr must be boolean!");
		for (Stmt stmt: alternativeStmt.ifStmtList){stmt.visit(this, arg);}
		for (Stmt stmt: alternativeStmt.elseStmtList){stmt.visit(this, arg);}
		return null;
	}

	@Override
	public Object visitPauseStmt(PauseStmt pauseStmt, Object arg)
			throws Exception {
		Kind type = (Kind)pauseStmt.expr.visit(this, arg);
		check(type == Kind._int, pauseStmt, " expr must be int!");
		return null;
	}

	@Override
	public Object visitIterationStmt(IterationStmt iterationStmt, Object arg)
			throws Exception {
		Kind type = (Kind)iterationStmt.expr.visit(this, arg);
		check(type == Kind._boolean, iterationStmt, " expr must be int!");
		for (Stmt stmt: iterationStmt.stmtList){stmt.visit(this, arg);}
		return null;
	}

	@Override
	public Object visitAssignPixelStmt(AssignPixelStmt assignPixelStmt,
			Object arg) throws Exception {
		check(Kind.pixel == lookupType(assignPixelStmt.lhsIdent.getText()) || Kind.image == lookupType(assignPixelStmt.lhsIdent.getText()), 
				assignPixelStmt, assignPixelStmt + " must be pixel or image");
		assignPixelStmt.pixel.visit(this, arg);
		return null;
	}

	@Override
	public Object visitPixel(Pixel pixel, Object arg) throws Exception {
		Kind typeRed = (Kind)pixel.redExpr.visit(this, arg);
		check(typeRed == Kind._int, pixel, " red Pixel must be int!");
		Kind typeGreen = (Kind)pixel.greenExpr.visit(this, arg);
		check(typeGreen == Kind._int, pixel, " green Pixel must be int!");
		Kind typeBlue = (Kind)pixel.blueExpr.visit(this, arg);
		check(typeBlue == Kind._int, pixel, " blue Pixel must be int!");
		return null;
	}

	@Override
	public Object visitSinglePixelAssignmentStmt(
			SinglePixelAssignmentStmt singlePixelAssignmentStmt, Object arg)
			throws Exception {
		check(Kind.image == lookupType(singlePixelAssignmentStmt.lhsIdent.getText()), singlePixelAssignmentStmt, singlePixelAssignmentStmt + " must be image!");
		Kind xType = (Kind)singlePixelAssignmentStmt.xExpr.visit(this, arg);
		check(xType == Kind._int, singlePixelAssignmentStmt, " xExpr must be int!");
		Kind yType = (Kind)singlePixelAssignmentStmt.yExpr.visit(this, arg);
		check(yType == Kind._int, singlePixelAssignmentStmt, " yExpr must be int!");
		singlePixelAssignmentStmt.pixel.visit(this, arg);
		return null;
	}

	@Override
	public Object visitSingleSampleAssignmentStmt(
			SingleSampleAssignmentStmt singleSampleAssignmentStmt, Object arg)
			throws Exception {
		check(Kind.image == lookupType(singleSampleAssignmentStmt.lhsIdent.getText()), singleSampleAssignmentStmt, singleSampleAssignmentStmt + " must be image!");
		Kind xType = (Kind)singleSampleAssignmentStmt.xExpr.visit(this, arg);
		check(xType == Kind._int, singleSampleAssignmentStmt, " xExpr must be int");
		Kind yType = (Kind)singleSampleAssignmentStmt.yExpr.visit(this, arg);
		check(yType == Kind._int, singleSampleAssignmentStmt, " yExpr must be int");
		Kind rhsType = (Kind)singleSampleAssignmentStmt.rhsExpr.visit(this, arg);
		check(rhsType == Kind._int, singleSampleAssignmentStmt, " rhsExpr must be int");
		return null;
	}

	@Override
	public Object visitScreenLocationAssignmentStmt(
			ScreenLocationAssignmentStmt screenLocationAssignmentStmt,
			Object arg) throws Exception {
		check(Kind.image == lookupType(screenLocationAssignmentStmt.lhsIdent.getText()), screenLocationAssignmentStmt, screenLocationAssignmentStmt + " must be image!");
		Kind xType = (Kind)screenLocationAssignmentStmt.xScreenExpr.visit(this, arg);
		check(xType == Kind._int, screenLocationAssignmentStmt, screenLocationAssignmentStmt + " xExpr must be int!");
		Kind yType = (Kind)screenLocationAssignmentStmt.yScreenExpr.visit(this, arg);
		check(yType == Kind._int, screenLocationAssignmentStmt, screenLocationAssignmentStmt + " yExpr must be int!");
		return null;
	}

	@Override
	public Object visitShapeAssignmentStmt(
			ShapeAssignmentStmt shapeAssignmentStmt, Object arg)
			throws Exception {
		check(Kind.image == lookupType(shapeAssignmentStmt.lhsIdent.getText()), shapeAssignmentStmt, shapeAssignmentStmt + " must be image!");
		Kind widthType = (Kind)shapeAssignmentStmt.width.visit(this, arg);
		check(widthType == Kind._int, shapeAssignmentStmt, " width must be int!");
		Kind heightType = (Kind)shapeAssignmentStmt.height.visit(this, arg);
		check(heightType == Kind._int, shapeAssignmentStmt, " height must be int!");
		return null;
	}

	@Override
	public Object visitSetVisibleAssignmentStmt(
			SetVisibleAssignmentStmt setVisibleAssignmentStmt, Object arg)
			throws Exception {
		check(Kind.image == lookupType(setVisibleAssignmentStmt.lhsIdent.getText()), setVisibleAssignmentStmt, setVisibleAssignmentStmt + " must be image!");
		Kind type = (Kind)setVisibleAssignmentStmt.expr.visit(this, arg);
		check(type == Kind._boolean, setVisibleAssignmentStmt, " expr must be boolean!");
		return null;
	}

	@Override
	public Object FileAssignStmt(cop5555fa13.ast.FileAssignStmt fileAssignStmt,
			Object arg) throws Exception {
		check(Kind.image == lookupType(fileAssignStmt.lhsIdent.getText()), fileAssignStmt, fileAssignStmt + " must be image!");
		return null;
	}

	@Override
	public Object visitConditionalExpr(ConditionalExpr conditionalExpr,
			Object arg) throws Exception {
		Kind conditionType = (Kind)conditionalExpr.condition.visit(this, arg);
		check(conditionType == Kind._boolean, conditionalExpr, " condition must be boolean!");
		Kind trueType = (Kind)conditionalExpr.trueValue.visit(this, arg);
		Kind falseType = (Kind)conditionalExpr.falseValue.visit(this, arg);
		check(trueType == falseType, conditionalExpr, " trueValue and falseValue must have the same type!");
		return trueType;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg)
			throws Exception {
		Kind type1 = null;
		Kind type2 = null;
		type1 = (Kind)binaryExpr.e0.visit(this, arg);
		type2 = (Kind)binaryExpr.e1.visit(this, arg);
//		System.out.println("type1 is " + type1.toString());
//		System.out.println("type2 is " + type2.toString());
		if(Kind.OR == binaryExpr.op.kind || Kind.AND == binaryExpr.op.kind){
			check(Kind._boolean == type1, binaryExpr, " the first type must be boolean for op & or |");
			check(Kind._boolean == type2, binaryExpr, " the second type must be boolean for op & or |");
			return Kind._boolean;
		}
		else if(Kind.PLUS == binaryExpr.op.kind || Kind.MINUS == binaryExpr.op.kind || Kind.TIMES == binaryExpr.op.kind 
				|| Kind.DIV == binaryExpr.op.kind || Kind.MOD ==  binaryExpr.op.kind){
			check(Kind._int == type1, binaryExpr, " the first type must be int for op +,_,*,/,%");
			check(Kind._int == type2, binaryExpr, " the second type must be int for op +,_,*,/,%");
			return Kind._int;
		}
		else if(Kind.EQ == binaryExpr.op.kind || Kind.NEQ == binaryExpr.op.kind){
			check(type1 == type2, binaryExpr, " the two types must be the same for op == or !=");
			return Kind._boolean;
		}
		else if(Kind.LSHIFT == binaryExpr.op.kind || Kind.RSHIFT == binaryExpr.op.kind){
			check(Kind._int == type1, binaryExpr, " the first type must be int for op << or >>");
			check(Kind._int == type2, binaryExpr, " the second type must be int for op << or >>");
			return Kind._int;
		}
		else if(Kind.LT == binaryExpr.op.kind || Kind.GT == binaryExpr.op.kind || Kind.LEQ == binaryExpr.op.kind 
				|| Kind.GEQ ==binaryExpr.op.kind){
			check(Kind._int == type1, binaryExpr, " the first type must be int for op <,>,<=,>=");
			check(Kind._int == type2, binaryExpr, " the second type must be int for op <,>,<=,>=");
			return Kind._boolean;
		}
		return null;
	}

	@Override
	public Object visitSampleExpr(SampleExpr sampleExpr, Object arg)
			throws Exception {
		check(Kind.image == lookupType(sampleExpr.ident.getText()), sampleExpr, sampleExpr + " must be image!");
		Kind xLocType = (Kind)sampleExpr.xLoc.visit(this, arg);
		check(xLocType == Kind._int, sampleExpr, " xLoc must be int!");
		Kind yLocType = (Kind)sampleExpr.yLoc.visit(this, arg);
		check(yLocType == Kind._int, sampleExpr, " yLoc must be int!");
		return Kind._int;
	}

	@Override
	public Object visitImageAttributeExpr(
			ImageAttributeExpr imageAttributeExpr, Object arg) throws Exception {
		return  Kind._int;
	}

	@Override
	public Object visitIdentExpr(IdentExpr identExpr, Object arg)
			throws Exception {
		if(symbolTable.containsKey(identExpr.ident.getText())){
//			System.out.println(identExpr.ident.getText() + "   " + symbolTable.get(identExpr.ident.getText()).getaKind());
			return symbolTable.get(identExpr.ident.getText()).getaKind();
		}
		else{
			errorNodeList.add(identExpr);
			errorLog.append(identExpr + " ident is not defined");
		}
		return Kind.IDENT;
	}

	@Override
	public Object visitIntLitExpr(IntLitExpr intLitExpr, Object arg)
			throws Exception { 
		return Kind._int;
	}

	@Override
	public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg)
			throws Exception {
		return Kind._boolean;
	}

	@Override
	public Object visitPreDefExpr(PreDefExpr preDefExpr, Object arg)
			throws Exception {
		return Kind._int;
	}

	@Override
	public Object visitAssignExprStmt(AssignExprStmt assignExprStmt, Object arg)
			throws Exception {
		Kind assignType = null;
		assignType = (Kind)assignExprStmt.expr.visit(this, arg);
		Kind identType = lookupType(assignExprStmt.lhsIdent.getText());
		if(identType == null){
			errorNodeList.add(assignExprStmt);
			errorLog.append(assignExprStmt + " ident is not defined");
		}
		else{
//			System.out.println("assignType: " + assignType + " , identType: " + identType);
			check(assignType == identType, assignExprStmt, " assign type must be the same as ident!");
		}
		return null;
	}

}
