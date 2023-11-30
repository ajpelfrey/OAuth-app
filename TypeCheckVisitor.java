package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.SyntaxException;
import edu.ufl.cise.cop4020fa23.exceptions.TypeCheckException;

import java.util.*;

public class TypeCheckVisitor implements ASTVisitor {


    List<String> list = new ArrayList<>();
    private AST root = null;
    SymbolTable st = new SymbolTable();
    Type programsType = null;
    @Override
    public Object visitProgram(Program program, Object arg) throws PLCCompilerException {
        root = program;
        Type type = Type.kind2type(program.getTypeToken().kind());
        program.setType(type);
        programsType = program.getType();
        //SymbolTable st = new SymbolTable();
        st.enterScope();
        List <NameDef> params = program.getParams();
        for (NameDef param:params)
        {
            param.visit(this, arg);
        }
        program.getBlock().visit(this,arg);
        st.leaveScope();
        return type;
    }
    @Override
    public Object visitBlock(Block block, Object arg) throws PLCCompilerException {
        st.enterScope();
        List<Block.BlockElem> blockElems = block.getElems();
        for (Block.BlockElem elem: blockElems) {
            elem.visit(this, arg);
        }
        st.leaveScope();
        return block;
//block of guard first blocks initializer isn't being set to its type
        //throw new UnsupportedOperationException("visit visitBlock");
    }
    private int identifierCount = 1;
    List<String> declaredVariables = new ArrayList<>();
    private Map<String, Integer> declaredVariableCounts = new HashMap<>();

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {

        Type type=null;
        if (nameDef.getDimension() != null) {
            type = Type.IMAGE;
        }
        else
            if
        ((nameDef.getType() == Type.INT)||
                        (nameDef.getType() == Type.BOOLEAN)||
                        (nameDef.getType() == Type.STRING)
                        || (nameDef.getType() == Type.PIXEL)||
                        (nameDef.getType() == Type.IMAGE))
        {
            type = Type.kind2type(nameDef.getTypeToken().kind());
        }
            else throw new TypeCheckException("wrong type of namedef");

        String varName = nameDef.getName();

            if (declaredVariables.contains(nameDef.getName())) {
                int varCount = declaredVariableCounts.getOrDefault(varName, 0);
                declaredVariableCounts.put(varName, varCount + 1);
                String uniqueVarName = varName + "$" + varCount;

                // Add the unique variable name to declared variables
                declaredVariables.add(uniqueVarName);
                nameDef.setJavaName(uniqueVarName);

                // Variable name already exists, append unique identifier
                varName += "$" + (identifierCount++);
            } else {
                //varName+="$1";
                // New variable name, add to declared variables
                declaredVariables.add(varName);
                nameDef.setJavaName(varName);
            }

        st.insert(nameDef);
        return type;
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {
        Expr d = declaration.getInitializer();

        if (d != null) {
            d.visit(this, arg);
        }


        NameDef nameDef = declaration.getNameDef();


            if (nameDef != null) {
            nameDef.visit(this,arg);
        }
        if (nameDef.getDimension() != null)
        {
            Dimension dimension = (Dimension) nameDef.getDimension().visit(this, arg);
            return nameDef.getType();
        }
        //if (!((d==null)||(d.getType()==nameDef.getType())||(d.getType()==Type.STRING&&nameDef.getType()==Type.IMAGE))){
         //   throw new TypeCheckException("name");
       // }
        else {
            return nameDef.getType();
        }
    }
    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCCompilerException {
      //  SymbolTable symbolTable = st;
        st.enterScope();

        Boolean b=false;


        assignmentStatement.getlValue().visit(this, arg);
        assignmentStatement.getE().visit(this, arg);
//assignmentStatement.getE().setType();
       b = assignmentCompatible(assignmentStatement.getlValue().getType(), assignmentStatement.getE().getType());
        st.leaveScope();

       if (!b)
        {
           throw new TypeCheckException("not copatible "+assignmentStatement.getE().getType()+ "<-e"+assignmentStatement.getlValue().getType());
        }
       // symbolTable.leaveScope();

        return null;
    }

    private boolean assignmentCompatible(Type lValueType, Type exprType) {
// Check if the types are the same.
        if (lValueType == exprType) {
            return true;
        }
else if  (lValueType ==Type.PIXEL&&exprType==Type.INT)
{
    return true;
}
        else if  (lValueType ==Type.IMAGE&&exprType==Type.INT||lValueType ==Type.IMAGE&&exprType==Type.PIXEL
        ||lValueType ==Type.IMAGE&&exprType==Type.STRING)
        {
            return true;
        }
        else
// Otherwise, the types are not assignment compatible.
        return false;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        Type leftType=null;
        Expr left = binaryExpr.getLeftExpr();
        Kind op1 = binaryExpr.getOp().kind();
         Kind op  = op1;
        //System.out.println(op.name());
        Expr right = binaryExpr.getRightExpr();
        Type inferBinaryType = null;
        if (left!=null){

       left.visit(this, arg);
            leftType=left.getType();
            if (left.getType()==null)
            {
                throw new TypeCheckException("left type null");
            }

        }
        else throw new TypeCheckException("left is null");
        if (right!=null){
        right.visit(this, arg);}
        else throw new TypeCheckException("r is null");

        if (left.getType()==right.getType())
        {
            if (op==(Kind.PLUS))
            {
                inferBinaryType= Type.INT;
                binaryExpr.setType(inferBinaryType);
                return null;
            }
        }


        switch (left.getType()) {
            case PIXEL:
                switch (op) {
                    case MINUS,TIMES,DIV,MOD:
                        if (leftType==right.getType()) {
                            inferBinaryType = leftType;
                        }
                        break;
                    case BITAND, BITOR:
                        if (Objects.requireNonNull(right.getType()) == Type.PIXEL) {
                            inferBinaryType = Type.PIXEL;
                        }
                        break;
                    case EXP:
                        switch (right.getType()) {
                            case INT:
                                inferBinaryType = Type.PIXEL;
                                break;
                        }
                        break;
                }
                break;
            case BOOLEAN:
                switch (op) {
                    case AND, OR, PLUS:
                        switch (right.getType()) {
                            case BOOLEAN:
                                inferBinaryType = Type.BOOLEAN;
                                break;
                        }
                        break;
                }
                break;
            case INT:
                switch (op) {
                    case LT, GT, LE, GE:
                        if (Objects.requireNonNull(right.getType()) == Type.INT) {
                            inferBinaryType = Type.BOOLEAN;
                        }
                        break;
                    case MINUS,TIMES,DIV,MOD:
                        if (leftType==right.getType()) {
                            inferBinaryType = leftType;
                        }
                        break;

                }
                break;
            case IMAGE:
                switch (op) {

                    case EQ:
                        switch (right.getType()) {
                            case IMAGE:
                                inferBinaryType = Type.BOOLEAN;
                                break;
                        }
                        break;

                    case MINUS,TIMES,DIV,MOD:
                        if (leftType==right.getType()) {
                            inferBinaryType = leftType;
                        }
                        break;
                    default:

                        throw new TypeCheckException("Unsupported binary operation for IMAGE type");
                }
                break;
        }

        if (inferBinaryType == null) {
            return null;
        }
binaryExpr.setType(inferBinaryType);
        return inferBinaryType;
    }


    @Override
    public Object visitBlockStatement(StatementBlock statementBlock, Object arg) throws PLCCompilerException {
// Check if the StatementBlock has a Block.
        if (statementBlock.getBlock() != null) {
// Visit the Block.
            statementBlock.getBlock().visit(this, arg);
        }

        return null;
    }

    @Override
    public Object visitChannelSelector(ChannelSelector channelSelector, Object arg) throws PLCCompilerException {
        if (channelSelector.color()==null){
 throw new UnsupportedOperationException("visitChannelSelector"+channelSelector.color());}
        return arg;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCCompilerException {

         conditionalExpr.getGuardExpr().visit(this, arg);
       // Type guardExprType =.getType();
        //Type trueExprType = conditionalExpr.getTrueExpr().getType();
        conditionalExpr.getTrueExpr().visit(this,arg);
       conditionalExpr.getFalseExpr().visit(this,arg);
       // Type falseExprType = conditionalExpr.getFalseExpr().getType();

        if (conditionalExpr.getGuardExpr().getType() != Type.BOOLEAN) {
            throw new TypeCheckException("Guard expression must be of type BOOLEAN."+ conditionalExpr.getGuardExpr().toString());
        }

        if (conditionalExpr.getTrueExpr().getType() != conditionalExpr.getFalseExpr().getType()) {
            throw new TypeCheckException("True and false expressions must have the same type.");
        }

        conditionalExpr.setType(conditionalExpr.getTrueExpr().getType());
       // conditionalExpr.visit(this, arg);

        return null;
    }


    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
    Type typeW = (Type) dimension.getWidth().visit(this, arg);
      check(typeW==Type.INT, dimension, "image width must be INT");
        Type typeH = dimension.getHeight() != null ? (Type) dimension.getHeight().visit(this, arg) : Type.INT;
        check(typeH==Type.INT, dimension, "image height must be INT");
        return dimension;

    }
    private void check(boolean condition, AST ast, String errorMessage) throws PLCCompilerException {
        if (!condition) {
            throw new PLCCompilerException(errorMessage);
        }
    }

    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
        // Check if the DoStatement has a single GuardedBlock.
        if (doStatement.getGuardedBlocks().size() == 1) {
            // Get the GuardedBlock node.
            GuardedBlock guardedBlock = doStatement.getGuardedBlocks().get(0);
            // Visit the GuardedBlock node.
            guardedBlock.visit(this, arg);

            return null;
        }
        else if (doStatement.getGuardedBlocks().size() > 1) {
            for (GuardedBlock guardedBlock : doStatement.getGuardedBlocks()) {
                // Generate code for the GuardedBlock's body.
                guardedBlock.visit(this, arg);
                return null;
        }
        }

        else {
            // Throw an exception if the DoStatement has more than one GuardedBlock.
            throw new PLCCompilerException("DoStatements must have  GuardedBlock.");
        }
        return null;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        // Check the types of the Exprred, Exprgreen, and Exprblue expressions.

        //if (expandedPixelExpr.getRed().getType()==Type.I);

     //   Type red = (Type) expandedPixelExpr.getRed().visit(this, arg);
expandedPixelExpr.getRed().visit(this,arg);
//if (expandedPixelExpr.get)
        expandedPixelExpr.getRed().setType(expandedPixelExpr.getRed().getType());
        if (expandedPixelExpr.getRed().getType() != Type.INT&&expandedPixelExpr.getRed().getType() != Type.PIXEL) {
            throw new TypeCheckException(("Exprred expression must be of type INT.")+expandedPixelExpr.getRed().getType());
        }

        Type green = (Type) expandedPixelExpr.getGreen().visit(this, arg);


       /* if (green != Type.INT) {
            throw new TypeCheckException("Exprgreen expression must be of type INT.");
        }*/
        Type blue = (Type) expandedPixelExpr.getBlue().visit(this, arg);

       /* if (blue != Type.INT) {
            throw new TypeCheckException("Exprblue expression must be of type INT.");
        }*/

        // Set the type of the ExpandedPixelExpr node to PIXEL.
        expandedPixelExpr.setType(Type.PIXEL);

        // Return null.
        return null;
    }

    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException {
        // Check if the Expr node in the ExprGuard node evaluates to true. If it does not, then skip the Block node in the GuardedBlock node.
        Expr expr = guardedBlock.getGuard();
       expr.setType(Type.BOOLEAN);

        // Visit the Block node in the GuardedBlock node.
        guardedBlock.getGuard().visit(this,arg);
        guardedBlock.getBlock().visit(this, arg);
        return null;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        // Get the symbol table.
        SymbolTable symbolTable = st;
String uniqueVarName=null;

        // Check if the IdentExpr name is defined in the symbol table.
        NameDef n = symbolTable.lookup(identExpr.getName());

        // If the IdentExpr name is not defined in the symbol table, throw an exception.
        if (n == null) {
            throw new TypeCheckException("Undeclared identifier"+identExpr.getName()+identExpr.getType());
        }


        // Assign the IdentExpr name definition and type to the IdentExpr object.
        identExpr.setNameDef(n);
        identExpr.setType(n.getType());

        // Return the type of the IdentExpr object.
        return n.getType();
    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {

        // Check if the DoStatement has a single GuardedBlock.
        if (ifStatement.getGuardedBlocks().size() == 1) {
            // Get the GuardedBlock node.
            GuardedBlock guardedBlock = ifStatement.getGuardedBlocks().get(0);

            // Visit the GuardedBlock node.
            guardedBlock.visit(this, arg);

            return null;
        }
        else if (ifStatement.getGuardedBlocks().size() > 1) {
            for (GuardedBlock guardedBlock : ifStatement.getGuardedBlocks()) {
                // Generate code for the GuardedBlock's body.
                guardedBlock.visit(this, arg);
                return null;
            }
        }

        else {
            // Throw an exception if the DoStatement has more than one GuardedBlock.
            throw new PLCCompilerException("DoStatements must have exactly one GuardedBlock.");
        }
        return null;

    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {

        String name = lValue.getName();
        NameDef symbolDef = st.lookup(name);
       
        if (symbolDef == null) {
            throw new PLCCompilerException("LValue name not found in symbol table: " + name);
        }
        Type type = symbolDef.getType();
        lValue.setType(type);


        if (lValue.getType()==Type.PIXEL)
        {
            if (lValue.getPixelSelector()==null&&lValue.getChannelSelector()!=null)
            {
                return Type.PIXEL;
            }
        }
        if (lValue.getType()==Type.IMAGE)
        {
            if (lValue.getPixelSelector()==null&&lValue.getChannelSelector()!=null)
            {
                return Type.IMAGE;
            }
            if (lValue.getPixelSelector()==null&&lValue.getChannelSelector()==null)
            {
                return Type.IMAGE;
            }
            if (lValue.getPixelSelector()!=null&&lValue.getChannelSelector()!=null)
            {
                return Type.INT;
            }

        }
        if (lValue.getPixelSelector() != null) {
           lValue.getPixelSelector().visit(this, arg);
            lValue.setType(Type.IMAGE);
        }

        return null;
    }



    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCCompilerException {
      Type type = Type.INT;
        numLitExpr.setType(Type.INT);
        return type;
}

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException {


        if (pixelSelector.xExpr()!=null&&pixelSelector.xExpr().firstToken.kind()==Kind.NUM_LIT||pixelSelector.xExpr().firstToken.kind()==Kind.IDENT)
        {
            if (pixelSelector.xExpr().firstToken.kind()==Kind.IDENT)
            {
                if (st.lookup(pixelSelector.xExpr().toString())==null)
                {
                    NameDef x = new SyntheticNameDef(pixelSelector.xExpr().firstToken().text());

                    st.insert(x);
                }
            }
            pixelSelector.xExpr().setType(Type.INT);
        }
        if (pixelSelector.yExpr()!=null&&pixelSelector.yExpr().firstToken.kind()==Kind.NUM_LIT||pixelSelector.yExpr().firstToken.kind()==Kind.IDENT)
        {
            if (pixelSelector.yExpr().firstToken.kind()==Kind.IDENT)
            {
                if (st.lookup(pixelSelector.yExpr().toString())==null)
                {
                    Expr temp = pixelSelector.yExpr();


                    st.insert(new SyntheticNameDef(pixelSelector.yExpr().firstToken().text()));

                }
            }
            pixelSelector.yExpr().setType(Type.INT);
        }
        else throw new TypeCheckException("pixe");
        if (pixelSelector.xExpr().getType()!=Type.INT||pixelSelector.yExpr().getType()!=Type.INT)
        {
            throw new TypeCheckException("must be int pixelselect");
        }
return null;
    }

    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {
        PixelSelector pixelSelector = postfixExpr.pixel();

        postfixExpr.primary().visit(this,arg);
        postfixExpr.primary().setType(postfixExpr.primary().getType());
        //visitPixelSelector(postfixExpr.pixel(), arg);
if (pixelSelector!=null){

        postfixExpr.pixel().visit(this,arg);

}
    Type type;
    type = inferPostFixExprType(postfixExpr);
    if (type!=null)
    {
    postfixExpr.setType(type);
    return type;
}

    return null;}

    private Type inferPostFixExprType(PostfixExpr postfixExpr) throws PLCCompilerException {

        IdentExpr i = (IdentExpr) postfixExpr.primary();
        Expr primary = postfixExpr.primary();
        NameDef n = st.lookup(i.getName());
        if (n!=null)
        {
            i.setType(n.getType());
        }


        PixelSelector pixel = postfixExpr.pixel();
        ChannelSelector Channel = postfixExpr.channel();
        PostfixExpr infer = null;

        if ((n.getType() == Type.IMAGE) && (pixel != null) && (Channel != null)) {
            return Type.INT;
        }
        if (n.getType() == Type.PIXEL && pixel != null && Channel != null)
        {
            return Type.INT;
        }
        if(pixel==null&&Channel.color()==null)
        {
            return primary.getType();
        }
        else if (pixel!=null &&Channel==null)
        {
            return Type.PIXEL;
        }
        return primary.getType();
    }


    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {
        Expr expr = returnStatement.getE();

        if (expr == null ) {

            //throw new TypeCheckException("here");
            return null;
        }
        expr.visit(this,arg);
        Type type;
        type = programsType;
        //st.lookup(returnStatement.getE().toString());

        expr.setType(type);
       // expr.visit(this,arg);

        return null;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCCompilerException {


       stringLitExpr.setType(Type.STRING);
        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCCompilerException {

        Kind o = unaryExpr.getOp();
        Expr e = unaryExpr.getExpr();
;        e.visit(this ,arg);
        Type tt = unaryExpr.getExpr().getType();
        Type t=  inferUnaryExpr(tt, o);
        unaryExpr.setType(t);
       if (t==null){
           throw new SyntaxException("Unary expr issue");
       }
       else
        return null;
    }

    private Type inferUnaryExpr(Type type, Kind op) throws TypeCheckException {
        if (type==Type.BOOLEAN&&op==Kind.BANG)
        {
            return Type.BOOLEAN;
        }
        if (type==Type.INT&&op==Kind.MINUS)
        {
            return Type.INT;
        }
        if (type==Type.IMAGE&&op==Kind.RES_width)
        {
            return Type.INT;
        }
        if (type==Type.IMAGE&&op==Kind.RES_height)
        {
            return Type.INT;
        }
return null;
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
       Expr e= writeStatement.getExpr();

       if (e!=null) {
           e.visit(this, arg);

           return null;
       }
       else throw new TypeCheckException("write");
    }

    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
booleanLitExpr.setType(Type.BOOLEAN);
return Type.BOOLEAN;
    }

    @Override
    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws PLCCompilerException {
        if (Objects.equals(constExpr.getName(), "Z"))
        {
            constExpr.setType(Type.INT);
        } else
        {
            constExpr.setType(Type.PIXEL);
        }

        if (constExpr.getName()==null)
        {
            throw new TypeCheckException("name is null");
        }
    return null;
}

}
