package edu.ufl.cise.cop4020fa23;

public class StringAppender {

    public static String append(String ident, String type, String nameDef) {
        StringBuilder sb = new StringBuilder();
        sb.append("package edu.ufl.cise.cop4020fa23;");
        sb.append("public class ").append(ident).append("{");
        sb.append("public static ").append(type).append(" apply(");
        sb.append(")");
        sb.append("{");

    sb.append(nameDef);
    sb.append("}");


    sb.append("}");





        return sb.toString();
    }
}