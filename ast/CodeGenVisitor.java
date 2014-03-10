package cop5555fa13.ast;

import static cop5555fa13.TokenStream.Kind.*;
import static cop5555fa13.TokenStream.Kind;

import java.util.HashMap;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import cop5555fa13.runtime.*;

public class CodeGenVisitor implements ASTVisitor, Opcodes {

	private ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
	private String progName;
	
	private int slot = 0;
	private int getSlot(String name){
		Integer s = slotMap.get(name);
		if (s != null) return s;
		else{
			slotMap.put(name, slot);
			return slot++;
		}		
	}

	HashMap<String,Integer> slotMap = new HashMap<String,Integer>();
	private HashMap<String, SymbolTableEntry> symbolTable = new HashMap<String, SymbolTableEntry>();
	
	// map to look up JVM types correspondingHashMap<K, V> language
	static final HashMap<Kind, String> typeMap = new HashMap<Kind, String>();
	static {
		typeMap.put(_int, "I");
		typeMap.put(pixel, "I");
		typeMap.put(_boolean, "Z");
		typeMap.put(image, "Lcop5555fa13/runtime/PLPImage;");
	}

	@Override
	public Object visitDec(Dec dec, Object arg) throws Exception {
		SymbolTableEntry decEntry = new SymbolTableEntry(dec.type);
		symbolTable.put(dec.ident.getText(), decEntry);
		MethodVisitor mv = (MethodVisitor)arg;
		//insert source line number info into classfile
		Label l = new Label();
		mv.visitLabel(l);
		mv.visitLineNumber(dec.ident.getLineNumber(),l);
		//get name and type
		String varName = dec.ident.getText();
		Kind t = dec.type;
		String jvmType = typeMap.get(t);
		Object initialValue = (t == _int || t==pixel || t== _boolean) ? Integer.valueOf(0) : null;
		//add static field to class file for this variable
		FieldVisitor fv = cw.visitField(ACC_STATIC, varName, jvmType, null,
				initialValue);
		fv.visitEnd();
		//if this is an image, generate code to create an empty image
		if (t == image){
			mv.visitTypeInsn(NEW, PLPImage.className);
			mv.visitInsn(DUP);
			mv.visitMethodInsn(INVOKESPECIAL, PLPImage.className, "<init>", "()V");
			mv.visitFieldInsn(PUTSTATIC, progName, varName, typeMap.get(image));
		}
		return null;
	}

	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		MethodVisitor mv = (MethodVisitor)arg;
		String sourceFileName = (String) arg;
		progName = program.getProgName();
		String superClassName = "java/lang/Object";

		// visit the ClassWriter to set version, attributes, class name and superclass name
		cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, progName, null, superClassName, null);
		//Optionally, indicate the name of the source file
		cw.visitSource(sourceFileName, null);
		// initialize creation of main method
		String mainDesc = "([Ljava/lang/String;)V";
		mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", mainDesc, null, null);
		mv.visitCode();
		Label start = new Label();
		mv.visitLabel(start);
		mv.visitLineNumber(program.ident.getLineNumber(), start);		
		
		getSlot("args");
		getSlot("x");
		getSlot("y");
		
		//visit children
		for(Dec dec : program.decList){
			dec.visit(this,mv);
		}
		for (Stmt stmt : program.stmtList){
			stmt.visit(this, mv);
		}
		
		
		//add a return statement to the main method
		mv.visitInsn(RETURN);
		
		//finish up
		Label end = new Label();
		mv.visitLabel(end);
		//visit local variables. The one is slot 0 is the formal parameter of the main method.
		mv.visitLocalVariable("args","[Ljava/lang/String;",null, start, end, getSlot("args"));
		//if there are any more local variables, visit them now.
		mv.visitLocalVariable("x","I",null,start,end,getSlot("x"));
		mv.visitLocalVariable("y","I",null,start,end,getSlot("y"));
		
		//finish up method
		mv.visitMaxs(1,1);
		mv.visitEnd();
		//convert to bytearray and return 
		return cw.toByteArray();
	}

	@Override
	public Object visitAlternativeStmt(AlternativeStmt alternativeStmt,
			Object arg) throws Exception {
		MethodVisitor mv = (MethodVisitor)arg;
		alternativeStmt.expr.visit(this, mv);
		Label elseLabel = new Label();
		mv.visitJumpInsn(IFEQ, elseLabel);
		for (Stmt stmt : alternativeStmt.ifStmtList){
			stmt.visit(this, mv);
		}
		Label end = new Label();
		mv.visitJumpInsn(GOTO, end);
		mv.visitLabel(elseLabel);
		for (Stmt stmt : alternativeStmt.elseStmtList){
			stmt.visit(this, mv);
		}
		mv.visitLabel(end);
		return null;
	}

	@Override
	public Object visitPauseStmt(PauseStmt pauseStmt, Object arg)
			throws Exception {
		MethodVisitor mv = (MethodVisitor)arg;
		//generate code to leave image on top of stack
		pauseStmt.expr.visit(this, mv);
	    //generate code to update frame, consuming the second image address.
	    mv.visitMethodInsn(INVOKESTATIC, PLPImage.className, "pause", "(I)V");
		return null;
	}

	@Override
	public Object visitIterationStmt(IterationStmt iterationStmt, Object arg)
			throws Exception {
		MethodVisitor mv = (MethodVisitor)arg;
		Label guardLabel = new Label();
		mv.visitJumpInsn(GOTO, guardLabel);
		Label bodyLabel = new Label();
		mv.visitLabel(bodyLabel);
		for (Stmt stmt : iterationStmt.stmtList){
			stmt.visit(this, mv);
		}
		mv.visitLabel(guardLabel);
		iterationStmt.expr.visit(this, mv);
		mv.visitJumpInsn(IFNE, bodyLabel);
		return null;
	}

	@Override
	public Object visitAssignPixelStmt(AssignPixelStmt assignPixelStmt,
			Object arg) throws Exception {
		MethodVisitor mv = (MethodVisitor)arg;
		String identName = assignPixelStmt.lhsIdent.getText();
		if(Kind.pixel == symbolTable.get(identName).getaKind()){
			assignPixelStmt.pixel.visit(this, mv);
			mv.visitFieldInsn(PUTSTATIC, progName, identName, typeMap.get(symbolTable.get(identName).getaKind())); 
		}
		else if(Kind.image == symbolTable.get(identName).getaKind()){
//			System.out.println("Starting assign image...");
			// set local variable x to 0
			mv.visitLdcInsn(new Integer(0));
			mv.visitVarInsn(ISTORE, getSlot("x"));
			Label outerLabel = new Label();
			mv.visitJumpInsn(GOTO, outerLabel);
			
			// outer loop
//			System.out.println("In outer loop");
			mv.visitLabel(outerLabel);
			// get local variable x
			mv.visitVarInsn(ILOAD, getSlot("x"));
			// get the width of image
			mv.visitFieldInsn(GETSTATIC, progName, identName, PLPImage.classDesc);
			mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, "getWidth", "()I");
			Label end = new Label();
			// end if x is greater or equal to width
			mv.visitJumpInsn(IF_ICMPGE, end);
			
			// inner loop
			// set local variable y to 0
			mv.visitLdcInsn(new Integer(0));
			mv.visitVarInsn(ISTORE, getSlot("y"));
//			System.out.println("In inner loop");
			Label innerLabel = new Label();
			mv.visitLabel(innerLabel);
			// get local variable y
			mv.visitVarInsn(ILOAD, getSlot("y"));
			// get the height of image
			mv.visitFieldInsn(GETSTATIC, progName, identName, PLPImage.classDesc);
			mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, "getHeight", "()I");
			Label incX = new Label();
			// increase x by 1 and return to outer loop when y is greater or equal to height
			mv.visitJumpInsn(IF_ICMPGE, incX);
			
			// set the pixel
//			System.out.println("setting the pixel");
			mv.visitFieldInsn(GETSTATIC, progName, identName, PLPImage.classDesc);
			mv.visitVarInsn(ILOAD, getSlot("x"));
			mv.visitVarInsn(ILOAD, getSlot("y"));
			assignPixelStmt.pixel.visit(this, mv);
			mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, "setPixel", "(III)V");
			
			// increase y by 1
//			System.out.println("increase y by 1");
			mv.visitVarInsn(ILOAD, getSlot("y"));
			mv.visitLdcInsn(new Integer(1));
			mv.visitInsn(IADD);
			mv.visitVarInsn(ISTORE, getSlot("y"));
			mv.visitJumpInsn(GOTO, innerLabel);
			
			// increase x by 1 and return to outer loop
//			System.out.println("increase x by 1");
			mv.visitLabel(incX);
			mv.visitVarInsn(ILOAD, getSlot("x"));
			mv.visitLdcInsn(new Integer(1));
			mv.visitInsn(IADD);
			mv.visitVarInsn(ISTORE, getSlot("x"));
			mv.visitJumpInsn(GOTO, outerLabel);
			
			// the label of the end
//			System.out.println("At the end of assignment");
			mv.visitLabel(end);
			mv.visitFieldInsn(GETSTATIC, progName, identName, PLPImage.classDesc);
		    mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, 
		    		"updateFrame", PLPImage.updateFrameDesc);
		}
		return null;
	}

	@Override
	public Object visitPixel(Pixel pixel, Object arg) throws Exception {
		MethodVisitor mv = (MethodVisitor)arg;
		//generate code to leave image on top of stack
		pixel.redExpr.visit(this, mv);
		pixel.greenExpr.visit(this, mv);
		pixel.blueExpr.visit(this, mv);
		mv.visitMethodInsn(INVOKESTATIC, "cop5555fa13/runtime/Pixel", "makePixel", "(III)I");
		return null;
	}

	@Override
	public Object visitSinglePixelAssignmentStmt(
			SinglePixelAssignmentStmt singlePixelAssignmentStmt, Object arg)
			throws Exception {
		MethodVisitor mv = (MethodVisitor)arg;
		//generate code to leave image on top of stack
		String imageName = singlePixelAssignmentStmt.lhsIdent.getText();
		mv.visitFieldInsn(GETSTATIC, progName, imageName, PLPImage.classDesc);
		//duplicate address. Will consume one for updating xLocation field and one for invoking updateFrame.
		mv.visitInsn(DUP);
		//visit xExpr on rhs to leave its value on top of the stack
		singlePixelAssignmentStmt.xExpr.visit(this,mv);
		//visit yExpr on rhs to leave its value on top of the stack
		singlePixelAssignmentStmt.yExpr.visit(this,mv);
		//visit pixel on rhs to leave its value on top of the stack
		singlePixelAssignmentStmt.pixel.visit(this,mv);
		//generate code to update frame, consuming the second image address.
	    mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, "setPixel", "(III)V");
	    //generate code to update frame, consuming the second image address.
	    mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, 
	    		"updateFrame", PLPImage.updateFrameDesc);
		return null;
	}

	@Override
	public Object visitSingleSampleAssignmentStmt(
			SingleSampleAssignmentStmt singleSampleAssignmentStmt, Object arg)
			throws Exception {
		MethodVisitor mv = (MethodVisitor)arg;
		//generate code to leave image on top of stack
		String imageName = singleSampleAssignmentStmt.lhsIdent.getText();
		mv.visitFieldInsn(GETSTATIC, progName, imageName, PLPImage.classDesc);
		//duplicate address. Will consume one for updating xLocation field and one for invoking updateFrame.
		mv.visitInsn(DUP);
		//visit xExpr on rhs to leave its value on top of the stack
		singleSampleAssignmentStmt.xExpr.visit(this,mv);
		//visit yExpr on rhs to leave its value on top of the stack
		singleSampleAssignmentStmt.yExpr.visit(this,mv);
		//get the color code red:0, green:1, blue:2
		int colorCode = 0;
		String colorStr = singleSampleAssignmentStmt.color.getText();
		if("red".equals(colorStr)){colorCode = ImageConstants.RED;}
		else if("green".equals(colorStr)){colorCode = ImageConstants.GRN;}
		else if("blue".equals(colorStr)){colorCode = ImageConstants.BLU;}
		mv.visitLdcInsn(new Integer(colorCode));
		//visit pixel on rhs to leave its value on top of the stack
		singleSampleAssignmentStmt.rhsExpr.visit(this, mv);
		//generate code to update frame, consuming the second image address.
	    mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, "setSample", "(IIII)V");
	    //generate code to update frame, consuming the second image address.
	    mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, 
	    		"updateFrame", PLPImage.updateFrameDesc);
		return null;
	}

	@Override
	public Object visitScreenLocationAssignmentStmt(
			ScreenLocationAssignmentStmt screenLocationAssignmentStmt,
			Object arg) throws Exception {
		MethodVisitor mv = (MethodVisitor)arg;
		//generate code to leave image on top of stack
		String imageName = screenLocationAssignmentStmt.lhsIdent.getText();
		mv.visitFieldInsn(GETSTATIC, progName, imageName, PLPImage.classDesc);
		//duplicate address. Will consume one for updating xLocation field and one for invoking updateFrame.
		mv.visitInsn(DUP);
		//visit xScreenExpr on rhs to leave its value on top of the stack
		screenLocationAssignmentStmt.xScreenExpr.visit(this,mv);
		//set visible field
		mv.visitFieldInsn(PUTFIELD, PLPImage.className, "x_loc", "I");	
		//duplicate address. Will consume one for updating yLocation field and one for invoking updateFrame.
		mv.visitInsn(DUP);
		//visit yScreenExpr on rhs to leave its value on top of the stack
		screenLocationAssignmentStmt.yScreenExpr.visit(this,mv);
		//set visible field
		mv.visitFieldInsn(PUTFIELD, PLPImage.className, "y_loc", "I");	
	    //generate code to update frame, consuming the third image address.
	    mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, 
	    		"updateFrame", PLPImage.updateFrameDesc);
		return null;
	}

	@Override
	public Object visitShapeAssignmentStmt(
			ShapeAssignmentStmt shapeAssignmentStmt, Object arg)
			throws Exception {
		MethodVisitor mv = (MethodVisitor)arg;
		//generate code to leave image on top of stack
		String imageName = shapeAssignmentStmt.lhsIdent.getText();
		mv.visitFieldInsn(GETSTATIC, progName, imageName, PLPImage.classDesc);
		//duplicate address. Will consume one for updating xLocation field and one for invoking updateFrame.
		mv.visitInsn(DUP);
		//visit width on rhs to leave its value on top of the stack
		shapeAssignmentStmt.width.visit(this,mv);
		//set visible field
		mv.visitFieldInsn(PUTFIELD, PLPImage.className, "width", "I");	
		//duplicate address. Will consume one for updating yLocation field and one for invoking updateFrame.
		mv.visitInsn(DUP);
		//visit height on rhs to leave its value on top of the stack
		shapeAssignmentStmt.height.visit(this,mv);
		//set visible field
		mv.visitFieldInsn(PUTFIELD, PLPImage.className, "height", "I");
		mv.visitInsn(DUP);
		mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, "updateImageSize", "()V");
	    //generate code to update frame, consuming the third image address.
	    mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, 
	    		"updateFrame", PLPImage.updateFrameDesc);
		return null;
	}

	@Override
	public Object visitSetVisibleAssignmentStmt(
			SetVisibleAssignmentStmt setVisibleAssignmentStmt, Object arg)
			throws Exception {
		MethodVisitor mv = (MethodVisitor)arg;
		//generate code to leave image on top of stack
		String imageName = setVisibleAssignmentStmt.lhsIdent.getText();
		mv.visitFieldInsn(GETSTATIC, progName,imageName,PLPImage.classDesc);
		//duplicate address. Will consume one for updating setVisible field
		//and one for invoking updateFrame.
		mv.visitInsn(DUP);
		//visit expr on rhs to leave its value on top of the stack
		setVisibleAssignmentStmt.expr.visit(this,mv);
		//set visible field
		mv.visitFieldInsn(PUTFIELD, PLPImage.className, "isVisible", "Z");	
	    //generate code to update frame, consuming the second image address.
	    mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, 
	    		"updateFrame", PLPImage.updateFrameDesc);
		return null;
	}

	@Override
	public Object FileAssignStmt(cop5555fa13.ast.FileAssignStmt fileAssignStmt,
			Object arg) throws Exception {
		MethodVisitor mv = (MethodVisitor)arg;
		//generate code to leave address of target image on top of stack
	    String image_name = fileAssignStmt.lhsIdent.getText();
	    mv.visitFieldInsn(GETSTATIC, progName, image_name, typeMap.get(image));
	    //generate code to duplicate this address.  We'll need it for loading
	    //the image and again for updating the frame.
	    mv.visitInsn(DUP);
		//generate code to leave address of String containing a filename or url
//	    mv.visitLdcInsn(fileAssignStmt.fileName.getText());
	    mv.visitLdcInsn(fileAssignStmt.fileName.getText().replace("\"", ""));
		//generate code to get the image by calling the loadImage method
	    mv.visitMethodInsn(INVOKEVIRTUAL, 
	    		PLPImage.className, "loadImage", PLPImage.loadImageDesc);
	    //generate code to update frame
	    mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, 
	    		"updateFrame", PLPImage.updateFrameDesc);
		return null;
	}

	@Override
	public Object visitConditionalExpr(ConditionalExpr conditionalExpr,
			Object arg) throws Exception {
		MethodVisitor mv = (MethodVisitor)arg;
		conditionalExpr.condition.visit(this, mv);
		Label falseConditionLabel = new Label();
		mv.visitJumpInsn(IFEQ, falseConditionLabel);
		conditionalExpr.trueValue.visit(this, mv);
		Label end = new Label();
		mv.visitJumpInsn(GOTO, end);
		mv.visitLabel(falseConditionLabel);
		conditionalExpr.falseValue.visit(this, mv);
		mv.visitLabel(end);
		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg)
			throws Exception {
		MethodVisitor mv = (MethodVisitor)arg;
		//visit e0 on rhs to leave its value on top of the stack
		binaryExpr.e0.visit(this, mv);
		//visit e1 on rhs to leave its value on top of the stack
		binaryExpr.e1.visit(this, mv);
		//the operator is +, add two exprs and leave its value on top of the stack
		if(Kind.PLUS == binaryExpr.op.kind){
			mv.visitInsn(IADD);
		}
		//the operator is -, subtract two exprs and leave its value on top of the stack
		else if(Kind.MINUS == binaryExpr.op.kind){
			mv.visitInsn(ISUB);
		}
		//the operator is *, times two exprs and leave its value on top of the stack
		else if(Kind.TIMES == binaryExpr.op.kind){
			mv.visitInsn(IMUL);
		}
		//the operator is /, divide two exprs and leave its value on top of the stack
		else if(Kind.DIV == binaryExpr.op.kind){
			mv.visitInsn(IDIV);
		}
		//the operator is %, mod from two exprs and leave its value on top of the stack
		else if(Kind.MOD == binaryExpr.op.kind){
			mv.visitInsn(IREM);
		}
		//the operator is <<, left shift from two exprs and leave its value on top of the stack
		else if(Kind.LSHIFT == binaryExpr.op.kind){
			mv.visitInsn(ISHL);
		}
		//the operator is >>, right shift from two exprs and leave its value on top of the stack
		else if(Kind.RSHIFT == binaryExpr.op.kind){
			mv.visitInsn(ISHR);
		}
		//the operator is |, or operation on two exprs and leave its value on top of the stack
		else if(Kind.OR == binaryExpr.op.kind){
			mv.visitInsn(IOR);
			System.out.println("Visit IOR");
		}
		//the operator is &, and operation on two exprs and leave its value on top of the stack
		else if(Kind.AND == binaryExpr.op.kind){
			mv.visitInsn(IAND);
			System.out.println("Visit IAND");
		}
		//the operator is ==, eq operation on two exprs and leave its value on top of the stack
		else if(Kind.EQ == binaryExpr.op.kind || Kind.NEQ == binaryExpr.op.kind || Kind.LT == binaryExpr.op.kind
				|| Kind.GT == binaryExpr.op.kind || Kind.LEQ == binaryExpr.op.kind || Kind.GEQ == binaryExpr.op.kind){
			Label label = new Label();
			if(Kind.EQ == binaryExpr.op.kind)
				mv.visitJumpInsn(IF_ICMPEQ, label);
			else if(Kind.NEQ == binaryExpr.op.kind)
				mv.visitJumpInsn(IF_ICMPNE, label);
			else if(Kind.LT == binaryExpr.op.kind)
				mv.visitJumpInsn(IF_ICMPLT, label);
			else if(Kind.GT == binaryExpr.op.kind)
				mv.visitJumpInsn(IF_ICMPGT, label);
			else if(Kind.LEQ == binaryExpr.op.kind)
				mv.visitJumpInsn(IF_ICMPLE, label);
			else if(Kind.GEQ == binaryExpr.op.kind)
				mv.visitJumpInsn(IF_ICMPGE, label);
			mv.visitLdcInsn(new Integer(0));
			Label end = new Label();
			mv.visitJumpInsn(GOTO, end);
			mv.visitLabel(label);
			mv.visitLdcInsn(new Integer(1));
			mv.visitLabel(end);
		}
		return null;
	}

	@Override
	public Object visitSampleExpr(SampleExpr sampleExpr, Object arg)
			throws Exception {
		MethodVisitor mv = (MethodVisitor)arg;
		String imageName = sampleExpr.ident.getText();
		mv.visitFieldInsn(GETSTATIC, progName, imageName, PLPImage.classDesc);
		sampleExpr.xLoc.visit(this, mv);
		sampleExpr.yLoc.visit(this, mv);
		int colorCode = 0;
		String colorStr = sampleExpr.color.getText();
		if("red".equals(colorStr)){colorCode = ImageConstants.RED;}
		else if("green".equals(colorStr)){colorCode = ImageConstants.GRN;}
		else if("blue".equals(colorStr)){colorCode = ImageConstants.BLU;}
		mv.visitLdcInsn(new Integer(colorCode));
		mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, "getSample", "(III)I");
		return null;
	}

	@Override
	public Object visitImageAttributeExpr(
			ImageAttributeExpr imageAttributeExpr, Object arg) throws Exception {
		MethodVisitor mv = (MethodVisitor)arg;
		//generate code to leave address of target image on top of stack
	    String image_name = imageAttributeExpr.ident.getText();
	    mv.visitFieldInsn(GETSTATIC, progName, image_name, typeMap.get(image));
//	    mv.visitVarInsn(ALOAD, 0);
	    //generate code to get the selector
	    String selector = imageAttributeExpr.selector.getText();
	    mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, "get" + Character.toUpperCase(selector.charAt(0)) + selector.substring(1), "()I");
	    return null;
	}

	@Override
	public Object visitIdentExpr(IdentExpr identExpr, Object arg)
			throws Exception {
		MethodVisitor mv = (MethodVisitor)arg;
		String ident = identExpr.ident.getText();
		mv.visitFieldInsn(GETSTATIC, progName, identExpr.ident.getText(), typeMap.get(symbolTable.get(ident).getaKind()));
//		System.out.println("Get " + identExpr.ident.getText() + " type is: " + symbolTable.get(ident).getaKind());

		return null;
	}

	@Override
	public Object visitIntLitExpr(IntLitExpr intLitExpr, Object arg)
			throws Exception {
		MethodVisitor mv = (MethodVisitor)arg;
		int lit = intLitExpr.intLit.getIntVal();
		mv.visitLdcInsn(lit);
		return null;
	}

	@Override
	public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg)
			throws Exception {
		MethodVisitor mv = (MethodVisitor)arg;
		String lit = booleanLitExpr.booleanLit.getText();
		int val = lit.equals("true")? 1 : 0;
		mv.visitLdcInsn(val);
		return null;
	}

	@Override
	public Object visitPreDefExpr(PreDefExpr preDefExpr, Object arg)
			throws Exception {
		MethodVisitor mv = (MethodVisitor)arg;
		if(Kind.Z == preDefExpr.constantLit.kind){
			mv.visitLdcInsn(ImageConstants.Z);
		}
		else if(Kind.SCREEN_SIZE == preDefExpr.constantLit.kind){
			mv.visitLdcInsn(PLPImage.SCREENSIZE);
		}
		else if(Kind.x == preDefExpr.constantLit.kind){
			mv.visitVarInsn(ILOAD, getSlot("x"));
		}
		else if(Kind.y == preDefExpr.constantLit.kind){
			mv.visitVarInsn(ILOAD, getSlot("y"));
		}
		return null;
	}

	@Override
	public Object visitAssignExprStmt(AssignExprStmt assignExprStmt, Object arg)
			throws Exception {
		MethodVisitor mv = (MethodVisitor)arg;
		//get the ident name
		String identName = assignExprStmt.lhsIdent.getText();
		//visit expr on rhs to leave its value on top of the stack
		assignExprStmt.expr.visit(this, mv);
		//set ident field
		mv.visitFieldInsn(PUTSTATIC, progName, identName, typeMap.get(symbolTable.get(identName).getaKind())); 
		return null;
	}

}
