package edu.ufl.cise.cop4020fa23;
import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.ast.Dimension;
import edu.ufl.cise.cop4020fa23.exceptions.CodeGenException;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.TypeCheckException;
import edu.ufl.cise.cop4020fa23.runtime.ConsoleIO;
import edu.ufl.cise.cop4020fa23.runtime.ImageOps;
import edu.ufl.cise.cop4020fa23.runtime.PixelOps;

import java.awt.*;

import java.awt.image.BufferedImage;
import java.util.*;
import java.lang.*;
import java.util.List;

public class CodeGeneratorVisitor implements ASTVisitor {

    private Map < String, Integer > declaredVariableCounts = new HashMap < > ();
    private String writeString;
    String toPrint = null;
    Boolean inBinaryOP = false;

    private int counter = 1;
    //  private int n = counter;
    private SymbolTable symbolTable;
    private String packageName;
    String nameDef = "";

    void setPackageName(String packageName)

    {
        this.packageName = packageName;
    }
    StringBuilder sb = new StringBuilder();
    String block;

    // Program program= ;

    @Override
    public String visitProgram(Program program, Object arg) throws PLCCompilerException {
        boolean isLast = true;

        sb.append("package edu.ufl.cise.cop4020fa23;");
        sb.append("import edu.ufl.cise.cop4020fa23.runtime.ConsoleIO;\n");
        sb.append("import edu.ufl.cise.cop4020fa23.runtime.PixelOps;\n");
        sb.append("import java.awt.image.BufferedImage;\n");
        sb.append("import edu.ufl.cise.cop4020fa23.runtime.ImageOps;");
        sb.append("import edu.ufl.cise.cop4020fa23.runtime.FileURLIO;");

        if (program.getType() == Type.STRING) {
            sb.append("import java.lang.*;\n");
        }
        //get and append class name
        String ident = program.getName();
        sb.append("public class ").append(ident).append("{");
        //get and append type name
        String type = program.getTypeToken().text();
        if (program.getType() == Type.STRING) {
            type = "String";
        }
        if (program.getType() == Type.PIXEL) {
            type = "int";
        }
        if (program.getType() == Type.IMAGE) {
            type = "BufferedImage";
        }

        sb.append("public static ").append(type).append(" apply(");

        List < NameDef > params = program.getParams();

        if (!params.isEmpty()) {
            for (NameDef param: params) {

                Type nametype = param.getType();
                if (nametype == Type.BOOLEAN) {
                    sb.append("boolean ");
                    String e = param.getName();
                    if (Objects.equals(e, "false")) {
                        sb.append(" isfalse");
                        if (params.indexOf(param) != params.size() - 1) {

                            sb.append(", ");
                        }
                    }
                    if (Objects.equals(e, "true")) {
                        sb.append(" istrue");
                        if (params.indexOf(param) != params.size() - 1) {

                            sb.append(", ");
                        }

                    } else {
                        if (params.indexOf(param) != params.size() - 1) {
                            sb.append(param.getName());
                            sb.append(", ");
                        } else {
                            sb.append(param.getName());
                        }

                    }

                }
                if (nametype == Type.INT) {
                    sb.append("int ");
                    sb.append(param.getName());
                    if (params.indexOf(param) != params.size() - 1) {

                        sb.append(", ");
                    }

                }
                if (nametype == Type.STRING) {
                    sb.append("String ");
                    sb.append(param.getName());
                    if (params.indexOf(param) != params.size() - 1) {

                        sb.append(", ");
                    }

                }
                if (nametype == Type.PIXEL) {
                    sb.append("int ");
                    sb.append(param.getName());
                    if (params.indexOf(param) != params.size() - 1) {

                        sb.append(", ");
                    }

                }
            }
        }
        sb.append(")");
        sb.append("{");

        Block b = program.getBlock();
        String block1 = block;
        String namedefs = "";
        if (!program.getBlock().getElems().isEmpty()) {
            b.visit(this, arg);
        }
        sb.append("}");
        sb.append("}");

        return sb.toString();
    }
    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCCompilerException {
        // Expr height =  assignmentStatement.getlValue().getNameDef().getDimension().getHeight();

        if (assignmentStatement.getlValue().getType() == Type.IMAGE && assignmentStatement.getE().getType() == Type.PIXEL) {

            sb.append("for (int x=0; x < ");
            sb.append(assignmentStatement.getlValue().getName());
            sb.append(".getWidth(); x++) {");
            sb.append("for (int y=0; y < ");
            sb.append(assignmentStatement.getlValue().getName());
            sb.append(".getHeight(); y++) {");

            sb.append("ImageOps.setRGB(");
            assignmentStatement.getlValue().visit(this, arg);
            //   sb.append(assignmentStatement.getlValue().getName());
            sb.append(",");
            sb.append("x");
            sb.append(",");
            sb.append("y");
            sb.append(",");
            sb.append("(");

            assignmentStatement.getE().visit(this, arg);
            sb.append(")");
            sb.append(")");

            sb.append(";");
            sb.append("}}");

            return null;
        }
        if (assignmentStatement.getlValue().getChannelSelector() == null && assignmentStatement.getlValue().getPixelSelector() == null && assignmentStatement.getE().getType() == Type.IMAGE)
        {
            sb.append("ImageOps.copyInto(");
            assignmentStatement.getE().visit(this, arg); //should be expanded pixel for num 6
            sb.append(",");
            assignmentStatement.getlValue().visit(this, arg);
            sb.append(");");
            return null;
            //sb.append()
        }
        if (assignmentStatement.getE().getType()==Type.STRING&&assignmentStatement.getlValue().getChannelSelector()==null&&assignmentStatement.getlValue().getPixelSelector()==null)
        {
            sb.append("ImageOps.copyInto(");
            sb.append("FileURLIO.readImage(");
            assignmentStatement.getE().visit(this,arg);
            sb.append(")");
            sb.append(",");
            sb.append(assignmentStatement.getlValue().firstToken.text());
            sb.append(");");

            return null;


        }

        assignmentStatement.getlValue().visit(this, arg);

        if (assignmentStatement.getlValue().getType() == Type.PIXEL && assignmentStatement.getlValue().getChannelSelector() != null) {
            sb.append(" = ");
            assignmentStatement.getlValue().getChannelSelector().visit(this, arg);
            sb.append(assignmentStatement.getlValue().firstToken.text());
            sb.append(",");
            assignmentStatement.getE().visit(this, arg);

            sb.append(");");
            return null;
        }


        if (assignmentStatement.getlValue().getType() == Type.PIXEL && assignmentStatement.getE().getType() == Type.INT) {
            // assignmentStatement.getlValue().visit(this,arg);
            sb.append("=");
            sb.append("PixelOps.pack(");
            assignmentStatement.getE().visit(this, arg);
            sb.append(",");
            assignmentStatement.getE().visit(this, arg);
            sb.append(",");
            assignmentStatement.getE().visit(this, arg);
            sb.append(");");
            return null;
        }

        sb.append("=");
        assignmentStatement.getE().visit(this, arg); //should be expanded pixel for num 6
        sb.append(";");
        return null;
        //  throw new TypeCheckException("visitAssignmentStatement");
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        if (inBinaryOP == false) {



                //  PLUS, MINUS, TIMES, DIV, MOD;

                if ((binaryExpr.getLeftExpr().getType() == Type.PIXEL && binaryExpr.getRightExpr().getType() == Type.PIXEL)) {
                    sb.append("(");
                    if (binaryExpr.getOp().kind() == Kind.PLUS) {
                        sb.append("ImageOps.binaryPackedPixelPixelOp(ImageOps.OP.PLUS,");
                    } else if (binaryExpr.getOp().kind() == Kind.MOD) {
                        sb.append("ImageOps.binaryPackedPixelPixelOp(ImageOps.OP.MOD,");
                    } else if (binaryExpr.getOp().kind() == Kind.DIV) {
                        sb.append("ImageOps.binaryPackedPixelPixelOp(ImageOps.OP.DIV,");
                    } else if (binaryExpr.getOp().kind() == Kind.TIMES) {
                        sb.append("ImageOps.binaryPackedPixelPixelOp(ImageOps.OP.TIMES,");
                    } else if (binaryExpr.getOp().kind() == Kind.MINUS) {
                        sb.append("ImageOps.binaryPackedPixelPixelOp(ImageOps.OP.MINUS,");
                    }else if (binaryExpr.getOp().kind() == Kind.EQ) {
                        sb.append("ImageOps.binaryPackedPixelBooleanOp(ImageOps.BoolOP.EQUALS,");
                    }

                    binaryExpr.getLeftExpr().visit(this, arg);

                    sb.append(",");
                    binaryExpr.getRightExpr().visit(this, arg);

                    sb.append(")");
                    sb.append(")");

                    return null;

                }   if ((binaryExpr.getLeftExpr().getType() == Type.PIXEL && binaryExpr.getRightExpr().getType() == Type.INT)) {
                    sb.append("(");
                    if (binaryExpr.getOp().kind() == Kind.PLUS) {
                        sb.append("ImageOps.binaryPackedPixelScalarOp(ImageOps.OP.PLUS,");
                    } else if (binaryExpr.getOp().kind() == Kind.MOD) {
                        sb.append("ImageOps.binaryPackedPixelScalarOp(ImageOps.OP.MOD,");
                    } else if (binaryExpr.getOp().kind() == Kind.DIV) {
                        sb.append("ImageOps.binaryPackedPixelScalarOp(ImageOps.OP.DIV,");
                    } else if (binaryExpr.getOp().kind() == Kind.TIMES) {
                        sb.append("ImageOps.binaryPackedPixelScalarOp(ImageOps.OP.TIMES,");
                    } else if (binaryExpr.getOp().kind() == Kind.MINUS) {
                        sb.append("ImageOps.binaryPackedPixelScalarOp(ImageOps.OP.MINUS,");
                    }

                    binaryExpr.getLeftExpr().visit(this, arg);

                    sb.append(",");
                    binaryExpr.getRightExpr().visit(this, arg);

                    sb.append(")");
                    sb.append(")");

                    return null;

                }


        }

        if (binaryExpr.getLeftExpr().getType() == Type.STRING && binaryExpr.getOp().kind() == Kind.EQ) {
            binaryExpr.getLeftExpr().visit(this, arg);
            sb.append(".equals(");
            binaryExpr.getRightExpr().visit(this, arg);
            sb.append(")");
            return null;
        }

        if (binaryExpr.getOp().kind() == Kind.EXP) {
            sb.append("((int)Math.round(Math.pow(");
            binaryExpr.getLeftExpr().visit(this, arg);
            sb.append(",");
            binaryExpr.getRightExpr().visit(this, arg);
            sb.append(")))");
            return null;
        } else {
            if ((binaryExpr.getOp().kind() == Kind.MINUS)) {
                binaryExpr.getLeftExpr().visit(this, arg);

                sb.append("-");
                binaryExpr.getRightExpr().visit(this, arg);
                return null;
            }
            if ((binaryExpr.getOp().kind() == Kind.GT)) {
                // binaryExpr.getLeftExpr().visit(this, arg);
                sb.append(binaryExpr.getLeftExpr().firstToken().text());
                sb.append(">");
                binaryExpr.getRightExpr().visit(this, arg);
                return null;
            }
            if (binaryExpr.getOp().kind() == Kind.PLUS && binaryExpr.getLeftExpr().getType() == Type.IMAGE && binaryExpr.getRightExpr().getType() == Type.IMAGE) {
                sb.append("ImageOps.OP.PLUS,");
                sb.append(binaryExpr.getLeftExpr().firstToken.text());
                sb.append(",");
                sb.append(binaryExpr.getRightExpr().firstToken.text());
                sb.append("))");
                return null;

            }
            if (binaryExpr.getOp().kind() == Kind.DIV && binaryExpr.getLeftExpr().getType() == Type.IMAGE && binaryExpr.getRightExpr().getType() == Type.INT) {
                sb.append("ImageOps.OP.DIV,");
                sb.append(binaryExpr.getLeftExpr().firstToken.text());
                sb.append(",");
                sb.append(binaryExpr.getRightExpr().firstToken.text());

                return null;
            }
            if (binaryExpr.getOp().kind() == Kind.TIMES && binaryExpr.getLeftExpr().getType() == Type.IMAGE && binaryExpr.getRightExpr().getType() == Type.INT) {
                sb.append("ImageOps.OP.TIMES,");
                sb.append(binaryExpr.getLeftExpr().firstToken.text());
                sb.append(",");
                sb.append(binaryExpr.getRightExpr().firstToken.text());

                return null;
            } else {
                sb.append("(");
                binaryExpr.getLeftExpr().visit(this, arg);

                if (binaryExpr.getOp().kind() == Kind.RETURN) //TODO add all cases
                {
                    sb.append("^");
                }
                if (binaryExpr.getOp().kind() == Kind.TIMES) //TODO add all cases
                {
                    sb.append("*");
                }
                if (binaryExpr.getOp().kind() == Kind.EXP) //TODO add all cases
                {
                    sb.append("**");
                }

                if (binaryExpr.getOp().kind() == Kind.BLOCK_OPEN) //TODO add all cases
                {
                    sb.append("<:");
                }
                if (binaryExpr.getOp().kind() == Kind.BLOCK_CLOSE) //TODO add all cases
                {
                    sb.append(":>");
                }
                if (binaryExpr.getOp().kind() == Kind.RARROW) //TODO add all cases
                {
                    sb.append("->");
                }
                if (binaryExpr.getOp().kind() == Kind.BOX) //TODO add all cases
                {
                    sb.append("[]");
                }
                if (binaryExpr.getOp().kind() == Kind.GT) //TODO add all cases
                {
                    sb.append(">");
                }
                if (binaryExpr.getOp().kind() == Kind.LT) //TODO add all cases
                {
                    sb.append("<");
                }

                if (binaryExpr.getOp().kind() == Kind.ASSIGN) //TODO add all cases
                {
                    sb.append("=");
                }

                if (binaryExpr.getOp().kind() == Kind.PLUS) {
                    sb.append("+");
                }
                if (binaryExpr.getOp().kind() == Kind.GE) {
                    sb.append(">=");
                }
                if (binaryExpr.getOp().kind() == Kind.LE) {
                    sb.append("<=");
                }
                if (binaryExpr.getOp().kind() == Kind.DIV) {
                    sb.append("/");
                }
                if (binaryExpr.getOp().kind() == Kind.MOD) {
                    sb.append("%");
                }
                if (binaryExpr.getOp().kind() == Kind.EQ) {
                    sb.append("==");
                }
                if (binaryExpr.getOp().kind() == Kind.AND) {
                    sb.append("&&");
                }
                if (binaryExpr.getOp().kind() == Kind.OR) {
                    sb.append("||");
                }
            }
            //  if (binaryExpr.getOp().text()=="+")
            // {
            //.    sb.append("+");
            // }
            binaryExpr.getRightExpr().visit(this, arg);
            sb.append(")");
        }
        return null;
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws PLCCompilerException {

        //  StringBuilder sb = new StringBuilder();
        String blockName;

        List < Block.BlockElem > blockElems = block.getElems();
        for (Block.BlockElem elem: blockElems) {
            elem.visit(this, arg);
        }

        return null;
    }

    @Override
    public Object visitBlockStatement(StatementBlock statementBlock, Object arg) throws PLCCompilerException {

        statementBlock.getBlock().visit(this, arg);
        return null;

        //  throw new TypeCheckException("visitBlockStatement");
    }

    @Override
    public Object visitChannelSelector(ChannelSelector channelSelector, Object arg) throws PLCCompilerException {
        //if (channelSelector.)
        // if (channelSelector.)
        // sb.append("PixelOps.setRed(")
        if (channelSelector.color() == Kind.RES_green) {
            sb.append("PixelOps.setGreen(");

        }
        if (channelSelector.color() == Kind.RES_red) {
            sb.append("PixelOps.setRed(");
            return null;

        }
        if (channelSelector.color() == Kind.RES_blue) {
            sb.append("PixelOps.setBlue(");
            return null;

        }
        return null;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCCompilerException {

        sb.append("(");
        sb.append("(");

        conditionalExpr.getGuardExpr().visit(this, arg);
        sb.append(")");

        sb.append("?");
        conditionalExpr.getTrueExpr().visit(this, arg);
        sb.append(":");
        conditionalExpr.getFalseExpr().visit(this, arg);
        sb.append(")");
        return null;
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {
        // ImageOps image = new ImageOps();

        declaration.getNameDef().visit(this, arg);
        if (declaration.getNameDef().getType() != Type.IMAGE && declaration.getInitializer() != null) {
            sb.append("=");
            declaration.getInitializer().visit(this, arg);
            sb.append(";");
            return null;

        }
        if (declaration.getInitializer() != null && declaration.getInitializer().getType() == Type.IMAGE && declaration.getNameDef().getDimension() == null) {
            sb.append("= ImageOps.cloneImage(");
            declaration.getInitializer().visit(this, arg);
            sb.append(");");
            return null;
        }

        if (declaration.getInitializer() != null && declaration.getInitializer().getType() == Type.IMAGE && declaration.getNameDef().getDimension() != null) {
            sb.append("= ImageOps.copyAndResize(");
            declaration.getInitializer().visit(this, arg);
            sb.append(",");
            declaration.getNameDef().getDimension().visit(this, arg);
            sb.append(");");
            return null;
        }

        if (declaration.getInitializer() != null && declaration.getInitializer().getType() == Type.INT && declaration.getNameDef().getDimension() == null) {
            sb.append("= ImageOps.cloneImage((");
            //if ( declaration.getInitializer().)
            if (declaration.firstToken().kind() == Kind.RES_image) {
                sb.append("ImageOps.binaryImageImageOp(");
                // inBinaryOP=true;
                // String init = declaration.getInitializer().toString();
            }

            declaration.getInitializer().visit(this, arg);
            sb.append(");;");
            return null;
        }

        if (declaration.getNameDef().getType() == Type.IMAGE && declaration.getInitializer() == null) {

            if (declaration.getNameDef().getDimension() == null) {
                throw new CodeGenException("SHOULD HAVE DIMMENSION");
            }

            sb.append("= ImageOps.makeImage(");
            declaration.getNameDef().getDimension().visit(this, arg);
            sb.append(");");
            return null;

        }

        if (declaration.getInitializer() != null && declaration.getInitializer().getType() == Type.STRING) {
            sb.append("= FileURLIO.readImage(");
            declaration.getInitializer().visit(this, arg);
            sb.append(");");
            return null;
        }

        if (declaration.getInitializer() == null) {

            sb.append(";");
            return null;
        }
        if (declaration.getNameDef().getDimension() == null && declaration.firstToken().kind() == Kind.RES_image && declaration.getNameDef().getTypeToken().kind() == Kind.RES_image) {
            sb.append("= ImageOps.cloneImage((");
            sb.append("ImageOps.binaryImageScalarOp(");
            declaration.getInitializer().visit(this, arg);
            sb.append(")));");
            return null;

        }
        if (declaration.getNameDef().getDimension() != null && declaration.firstToken().kind() == Kind.RES_image && declaration.getNameDef().getTypeToken().kind() == Kind.RES_image) {
            sb.append("=ImageOps.copyAndResize((");
            sb.append("ImageOps.binaryImageScalarOp(");
            declaration.getInitializer().visit(this, arg);
            sb.append(")),");
            declaration.getNameDef().getDimension().visit(this, arg);
            sb.append(");");
            return null;

        } else {
            sb.append("=");
            declaration.getInitializer().visit(this, arg);
            sb.append(";");
        }

        return null;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
        dimension.getWidth().visit(this, arg);
        sb.append(",");

        dimension.getHeight().visit(this, arg);

        return null;
        //throw new TypeCheckException("visitDimension");
    }

    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {

        if (doStatement.getGuardedBlocks() != null) {
            for (int i = 0; i < doStatement.getGuardedBlocks().size(); i++) {

                doStatement.getGuardedBlocks().get(i).visit(this, arg);

            }
        }
        return null;

        // throw new TypeCheckException("visitDoStatement");
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        sb.append("PixelOps.pack(");
        expandedPixelExpr.getRed().visit(this, arg);
        sb.append(",");
        expandedPixelExpr.getGreen().visit(this, arg);

        sb.append(",");
        expandedPixelExpr.getBlue().visit(this, arg);
        sb.append(")");

        //   throw new TypeCheckException("visitExpandedPixelExpr");
        return null;
    }

    @Override
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException {
        if (guardedBlock.getGuard() != null) {
            sb.append("if");
            sb.append("(");

            //  sb.append(guardedBlock.g],)
            guardedBlock.getGuard().visit(this, arg);
            sb.append(")");

        }
        if (guardedBlock.getBlock() != null) {
            sb.append("{ ");
            guardedBlock.getBlock().visit(this, arg);
            sb.append("}");

        }
        return null;
        //        throw new TypeCheckException("visitGuardedBlock");
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        if (identExpr.getName() != null)
            if (identExpr.getNameDef() == null) {
                sb.append(identExpr.getName());
                return null;
            } {
            if (Objects.equals(identExpr.getNameDef().getName(), "false") || Objects.equals(identExpr.getNameDef().getName(), "isfalse")) {
                sb.append("isfalse");
                return null;

            }
            if (Objects.equals(identExpr.getNameDef().getName(), "true") || Objects.equals(identExpr.getNameDef().getName(), "istrue")) {
                sb.append("istrue");
                return null;

            }
            if (Objects.equals(identExpr.getNameDef().getName(), "INT")) {
                sb.append("int ");
                return null;
            }
            if (Objects.equals(identExpr.getNameDef().getName(), "string")) {
                sb.append("String ");
                return null;
            }
            if (Objects.equals(identExpr.getNameDef().getName(), "Z")) {
                sb.append("Z");
                return null;
            }
        }
        if (Objects.equals(identExpr.getNameDef().getName(), "x")) {
            sb.append("x");
            return null;
        }
        if (Objects.equals(identExpr.getNameDef().getName(), "y")) {
            sb.append("y");
            return null;
        }
        if (Objects.equals(identExpr.getNameDef().getName(), "im")) {
            sb.append("im");
            return null;
        }

        sb.append(identExpr.getNameDef().getName());
        return null;
    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {

        if (ifStatement.getGuardedBlocks() != null)

        {
            for (int i = 0; i < ifStatement.getGuardedBlocks().size(); i++) {

                if (i != ifStatement.getGuardedBlocks().size() && i != 0) {
                    sb.append("else ");
                }
                ifStatement.getGuardedBlocks().get(i).visit(this, arg);
            }
        }
        return null;
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {

        if (lValue.firstToken.kind() == Kind.IDENT) {

            sb.append(lValue.firstToken.text());
            //   lValue.g
        }

        return null;
    }
    private int identifierCount = 1;
    List < String > declaredVariables = new ArrayList < > ();

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {

        if (nameDef.getType() == Type.IMAGE) {
            sb.append("BufferedImage");
            String varName = nameDef.getJavaName();

            sb.append(" ");

            sb.append(varName);
            return null;

        }
        if (nameDef.getType() == Type.STRING) {
            sb.append("String");

        }
        if (nameDef.getType() == Type.PIXEL) {
            sb.append("int");

        } else {
            sb.append(nameDef.getType().name().toLowerCase());
        }
        String varName = nameDef.getJavaName();

        sb.append(" ");

        sb.append(varName);
        return null;
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCCompilerException {
        sb.append(numLitExpr.firstToken.text());
        return null;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException {

        sb.append("[");
        if ((pixelSelector.xExpr() != null)) {
            pixelSelector.xExpr().visit(this, arg);
        }
        sb.append(",");
        if ((pixelSelector.yExpr() != null)) {
            pixelSelector.yExpr().visit(this, arg);
        }
        sb.append("]");

        //  sb.append(pixelSelector.toString());
        return null;
        //     throw new TypeCheckException("visitPixelSelector");
    }

    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {
        if (postfixExpr.primary().getType() == Type.PIXEL && (postfixExpr.channel().color() == Kind.RES_red)) {

            sb.append("PixelOps.red(");
            sb.append(postfixExpr.primary().firstToken().text());
            sb.append(")");
            return null;
        }
        if (postfixExpr.primary().getType() == Type.PIXEL && (postfixExpr.channel().color() == Kind.RES_green)) {
            sb.append("PixelOps.green(");
            sb.append(postfixExpr.primary().firstToken().text());
            sb.append(")");
            return null;
        }
        if (postfixExpr.primary().getType() == Type.PIXEL && (postfixExpr.channel().color() == Kind.RES_blue)) {
            sb.append("PixelOps.blue(");
            sb.append(postfixExpr.primary().firstToken().text());
            sb.append(")");
            return null;
        }
        if (postfixExpr.primary() != null && postfixExpr.channel() == null && postfixExpr.primary().firstToken.kind() == Kind.IDENT) {
            sb.append("ImageOps.getRGB(");
            sb.append(postfixExpr.primary().firstToken().text());
            sb.append(",");

            sb.append(postfixExpr.pixel().xExpr().firstToken().text());
            sb.append(",");
            sb.append(postfixExpr.pixel().yExpr().firstToken().text());
            sb.append(")");
            return null;

        }
        if (postfixExpr.primary()!=null &&postfixExpr.channel()!=null && postfixExpr.pixel()!=null)
        {
            if (postfixExpr.channel().color()==Kind.RES_red) {
            sb.append("PixelOps.red(");
                sb.append("ImageOps.getRGB(");
                    sb.append(postfixExpr.primary().firstToken.text());
                    sb.append(",");
                    sb.append(postfixExpr.pixel().xExpr().firstToken.text());sb.append(",");
                //    sb.append(",");

                sb.append(postfixExpr.pixel().yExpr().firstToken.text());
                sb.append(")");
                sb.append(")");

            }if (postfixExpr.channel().color()==Kind.RES_green) {
            sb.append("PixelOps.green(");
                sb.append("ImageOps.getRGB(");
                    sb.append(postfixExpr.primary().firstToken.text());
                    sb.append(",");
                    sb.append(postfixExpr.pixel().xExpr().firstToken.text());sb.append(",");
                //    sb.append(",");

                sb.append(postfixExpr.pixel().yExpr().firstToken.text());
                sb.append(")");
                sb.append(")");

            }if (postfixExpr.channel().color()==Kind.RES_blue) {
            sb.append("PixelOps.blue(");
                sb.append("ImageOps.getRGB(");
                    sb.append(postfixExpr.primary().firstToken.text());
                    sb.append(",");
                    sb.append(postfixExpr.pixel().xExpr().firstToken.text());sb.append(",");
                //    sb.append(",");

                sb.append(postfixExpr.pixel().yExpr().firstToken.text());
                sb.append(")");
                sb.append(")");
            }
        return null;
        }
        if (postfixExpr.channel()!=null&&postfixExpr.pixel()==null)
        {
            if (postfixExpr.channel().color()==Kind.RES_red)
            {
                sb.append("ImageOps.extractRed(");
                postfixExpr.primary().visit(this,arg);
                sb.append(")");
            }if (postfixExpr.channel().color()==Kind.RES_blue)
            {
                sb.append("ImageOps.extractBlue(");
                postfixExpr.primary().visit(this,arg);
                sb.append(")");


            }if (postfixExpr.channel().color()==Kind.RES_green)
            {
                sb.append("ImageOps.extractGreen(");
                postfixExpr.primary().visit(this,arg);
                sb.append(")");


            }
            return null;
        }


        if (postfixExpr.primary() != null) {

            postfixExpr.primary().visit(this, arg);

        }
        if (postfixExpr.pixel() != null) {
            postfixExpr.pixel().visit(this, arg);
        }
        if (postfixExpr.channel()!=null)
        {
            postfixExpr.channel().visit(this,arg);
        }
        return null;
        // sb.append("")
        //        throw new TypeCheckException("visitPostfixExpr");
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {
        sb.append("return ");

        Expr e = returnStatement.getE();
        e.visit(this, arg);
        sb.append(";");
        return null;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCCompilerException {
        sb.append(stringLitExpr.getText());
        toPrint = stringLitExpr.getText();
        //  sb.append(";");
        return stringLitExpr;
        //        throw new TypeCheckException("visitStringLitExpr");
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCCompilerException {

        Kind opKind = unaryExpr.getOp();

        sb.append("(");

        if (Objects.equals(opKind.toString(), "RES_height")) {
            sb.append("(");
            unaryExpr.getExpr().visit(this, arg);
            sb.append(".getHeight()))");
            return null;
        }
        if (Objects.equals(opKind.toString(), "RES_width")) {
            sb.append("(");
            unaryExpr.getExpr().visit(this, arg);
            sb.append(".getWidth()))");
            return null;
        }
        if (Objects.equals(opKind.toString(), "SEMI"))

            sb.append(";");
        if (Objects.equals(opKind.toString(), "COMMA"))
            sb.append(",");
        if (Objects.equals(opKind.toString(), "QUESTION"))
            sb.append("?");
        if (Objects.equals(opKind.toString(), "RPAREN"))
            sb.append(")");
        if (Objects.equals(opKind.toString(), "LPAREN"))
            sb.append("(");
        if (Objects.equals(opKind.toString(), "LT"))
            sb.append("<");
        if (Objects.equals(opKind.toString(), "GT"))
            sb.append(">");
        if (Objects.equals(opKind.toString(), "LSQUARE"))
            sb.append("[");
        if (Objects.equals(opKind.toString(), "RSQUARE"))
            sb.append("]");
        if (Objects.equals(opKind.toString(), "ASSIGN"))
            sb.append("=");
        if (Objects.equals(opKind.toString(), "EQ"))
            sb.append("==");
        if (Objects.equals(opKind.toString(), "LE"))
            sb.append("<=");
        if (Objects.equals(opKind.toString(), "EQ"))
            sb.append("==");
        if (Objects.equals(opKind.toString(), "LE"))
            sb.append("<=");
        if (Objects.equals(opKind.toString(), "GE"))
            sb.append(">=");
        if (Objects.equals(opKind.toString(), "AND"))
            sb.append("&&");
        if (Objects.equals(opKind.toString(), "BITAND"))
            sb.append("&");
        if (Objects.equals(opKind.toString(), "BITOR"))
            sb.append("|");
        if (Objects.equals(opKind.toString(), "OR"))
            sb.append("||");
        if (Objects.equals(opKind.toString(), "EXP"))
            sb.append("**");
        if (Objects.equals(opKind.toString(), "BLOCK_OPEN"))
            sb.append("<:");
        if (Objects.equals(opKind.toString(), "BLOCK_CLOSE"))
            sb.append(":>");
        if (Objects.equals(opKind.toString(), "RETURN"))
            sb.append("^");
        if (Objects.equals(opKind.toString(), "RARROW"))
            sb.append("->");
        if (Objects.equals(opKind.toString(), "BOX"))
            sb.append("[]");
        if (Objects.equals(opKind.toString(), "MINUS")) {
            sb.append("-");
        }
        if (Objects.equals(opKind.toString(), "BANG")) {
            sb.append("!");
        }
        if (Objects.equals(opKind.toString(), "PLUS")) {
            sb.append("+");
        }
        if (Objects.equals(opKind.toString(), "DIV")) {
            sb.append("/");
        }
        if (Objects.equals(opKind.toString(), "MOD")) {
            sb.append("%");
        }
        if (Objects.equals(opKind.toString(), "TIMES")) {
            sb.append("*");
        }

        Expr e = unaryExpr.getExpr();
        e.visit(this, arg);
        sb.append(")");
        return null;
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        // Object value = writeStatement.getExpr().visit(this, arg);
        sb.append("ConsoleIO.write(");
        Object value;
        value = writeStatement.getExpr().visit(this, arg);
        // Expr e = writeStatement.getExpr();
        //      ConsoleIO.write("\"hello\"");

        // Append the value to the Java code.
        sb.append("); ");
        // String s = ConsoleIO.write();
        //   String tempString = temp.toString();
        //    ConsoleIO.write(tempString);
        return value;
    }

    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
        if (Objects.equals(booleanLitExpr.getText(), "FALSE")) {
            sb.append("false");
        } else {
            sb.append("true");
        }
        return null;
    }

    @Override
    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws PLCCompilerException {
        String text = constExpr.firstToken.text();
        String hex = null;

        if (Objects.equals(constExpr.getName(), "Z")) {
            hex = "255";
        } else {
            if (Objects.equals(constExpr.firstToken.text(), "BLUE")) {
                hex = "0x" + Integer.toHexString(Color.BLUE.getRGB());

            } if (Objects.equals(constExpr.firstToken.text(), "blue")) {
                hex = "0x" + Integer.toHexString(Color.blue.getRGB());

            }
            if (Objects.equals(constExpr.firstToken.text(), "CYAN")) {
                hex = "0x" + Integer.toHexString(Color.CYAN.getRGB());

            }
            if (Objects.equals(constExpr.firstToken.text(), "cyan")) {
                hex = "0x" + Integer.toHexString(Color.cyan.getRGB());

            }
            if (Objects.equals(constExpr.firstToken.text(), "RED")) {
                hex = "0x" + Integer.toHexString(Color.RED.getRGB());
            }if (Objects.equals(constExpr.firstToken.text(), "red")) {
                hex = "0x" + Integer.toHexString(Color.red.getRGB());
            }
            if (Objects.equals(constExpr.firstToken.text(), "GREEN")) {
                hex = "0x" + Integer.toHexString(Color.GREEN.getRGB());
            }if (Objects.equals(constExpr.firstToken.text(), "green")) {
                hex = "0x" + Integer.toHexString(Color.green.getRGB());
            }
            if (Objects.equals(constExpr.firstToken.text(), "PINK")) {
                hex = "0x" + Integer.toHexString(Color.PINK.getRGB());
            }if (Objects.equals(constExpr.firstToken.text(), "pink")) {
                hex = "0x" + Integer.toHexString(Color.pink.getRGB());
            }
            if (Objects.equals(constExpr.firstToken.text(), "MAGENTA")) {
                hex = "0x" + Integer.toHexString(Color.MAGENTA.getRGB());
                //    sb.append(hex);
            }if (Objects.equals(constExpr.firstToken.text(), "magenta")) {
                hex = "0x" + Integer.toHexString(Color.magenta.getRGB());
                //    sb.append(hex);
            }
            if (Objects.equals(constExpr.firstToken.text(), "WHITE")) {
                hex = "0x" + Integer.toHexString(Color.WHITE.getRGB());
                //    sb.append(hex);
            }
            if (Objects.equals(constExpr.firstToken.text(), "white")) {
                hex = "0x" + Integer.toHexString(Color.white.getRGB());
                //    sb.append(hex);
            }
            if (Objects.equals(constExpr.firstToken.text(), "orange")) {
                hex = "0x" + Integer.toHexString(Color.orange.getRGB());
                //    sb.append(hex);
            }
            if (Objects.equals(constExpr.firstToken.text(), "ORANGE")) {
                hex = "0x" + Integer.toHexString(Color.ORANGE.getRGB());
                //    sb.append(hex);
            }
            if (Objects.equals(constExpr.firstToken.text(), "LIGHT_GRAY")) {
                hex = "0x" + Integer.toHexString(Color.LIGHT_GRAY.getRGB());
                //    sb.append(hex);
            }
            if (Objects.equals(constExpr.firstToken.text(), "light_gray")) {
                hex = "0x" + Integer.toHexString(Color.lightGray.getRGB());
                //    sb.append(hex);
            }
            if (Objects.equals(constExpr.firstToken.text(), "YELLOW")) {
                hex = "0x" + Integer.toHexString(Color.YELLOW.getRGB());
                //    sb.append(hex);
            }
            if (Objects.equals(constExpr.firstToken.text(), "yellow")) {
                hex = "0x" + Integer.toHexString(Color.yellow.getRGB());
                //    sb.append(hex);
            }
            if (Objects.equals(constExpr.firstToken.text(), "gray")) {
                hex = "0x" + Integer.toHexString(Color.gray.getRGB());
                //    sb.append(hex);
            }
            if (Objects.equals(constExpr.firstToken.text(), "GRAY")) {
                hex = "0x" + Integer.toHexString(Color.GRAY.getRGB());
                //    sb.append(hex);
            }
            if (Objects.equals(constExpr.firstToken.text(), "DARK_GRAY")) {
                hex = "0x" + Integer.toHexString(Color.DARK_GRAY.getRGB());
            }
            if (Objects.equals(constExpr.firstToken.text(), "dark grey")) {
                hex = "0x" + Integer.toHexString(Color.darkGray.getRGB());
            }
            if (Objects.equals(constExpr.firstToken.text(), "black")) {
                hex = "0x" + Integer.toHexString(Color.black.getRGB());
            }
            if (Objects.equals(constExpr.firstToken.text(), "BLACK")) {
                hex = "0x" + Integer.toHexString(Color.BLACK.getRGB());
            }
        }
        sb.append(hex);

        return null;
    }
}