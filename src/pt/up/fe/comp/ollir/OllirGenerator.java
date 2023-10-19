package pt.up.fe.comp.ollir;

import pt.up.fe.comp.analysis.AnalyserUtils;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.sql.SQLOutput;
import java.util.*;
import java.util.stream.Collectors;

public class OllirGenerator extends AJmmVisitor<String,String> {
    private final StringBuilder code;
    private final SymbolTable symbolTable;
    private String currFunc = null;
    private String expectedType = null;
    private int tNum = 0;
    private int loopNum = 0;
    private int ifNum = 0;
    private final Map<String,String> config;

    public OllirGenerator(SymbolTable symbolTable, Map<String,String> config){
        this.code = new StringBuilder();
        this.symbolTable = symbolTable;
        this.config = config;

        addVisit("Start", this::startVisit);
        addVisit("ImportDec", this::emptyVisit);
        addVisit("ClassDeclaration", this::classVisit);
        addVisit("ClassInheritance", this::emptyVisit);
        addVisit("VarDeclaration", this::emptyVisit);
        addVisit("MethodDeclaration", this::methodVisit);
        addVisit("Call", this::callVisit);
        addVisit("ClassObject", this::classObjectVisit);
        addVisit("Args", this::argsVisit);
        addVisit("ReturnStatement", this::returnVisit);
        addVisit("IdentifierObject", this::identifierObjectVisit);
        addVisit("Binop", this::emptyVisit);
        addVisit("Access", this::accessVisit);
        addVisit("Assign", this::assignVisit);
        addVisit("Literal", this::literalVisit);
        addVisit("IfStatement", this::ifVisit);
        addVisit("Block", this::blockVisit);
        addVisit("WhileStatement", this::whileVisit);
    }

    public String getCode(){
        return code.toString();
    }

    //same as programVisit
    private String startVisit(JmmNode node, String dummy){
        for (var importString : symbolTable.getImports()){
            code.append("import ").append(importString).append(";\n");
        }

        for (var child : node.getChildren()){
            code.append(visit(child));
        }
        return code.toString();
    }

    private String emptyVisit(JmmNode node, String dummy) {
        return "";
    }

    //same as classDeclVisit
    private String classVisit(JmmNode node, String dummy){

        code.append(symbolTable.getClassName());
        String superClass = symbolTable.getSuper();
        if(superClass != null){
            code.append(" extends ").append(superClass);
        }
        code.append(" {\n");

        for(Symbol symbol: symbolTable.getFields()){
            code.append(".field private ")
                    .append(OllirUtils.getCode(symbol))
                    .append(";\n");
        }

        for (var child : node.getChildren()){
            code.append(visit(child));
        }

        code.append("}\n");
        return "";
    }

    private String methodVisit(JmmNode node, String dummy){
        StringBuilder aux = new StringBuilder();

        currFunc = node.get("image");
        var methodSignature = node.get("image");
        var isStatic =  Boolean.valueOf(node.get("isStatic"));

        aux.append(".method public ");

        if(isStatic){
            aux.append("static ");
        }

        aux.append(methodSignature).append("(");

        var paramCode = symbolTable.getParameters(methodSignature).stream()
                .map(symbol -> OllirUtils.getCode(symbol))
                .collect(Collectors.joining(", "));

        aux.append(paramCode);
        aux.append(")");

        aux.append(OllirUtils.getCode(symbolTable.getReturnType(methodSignature)));

        aux.append(" {\n");

        for (var child : node.getChildren()){
            if(!(child.getKind().equals("Type") || child.getKind().equals("Param"))){
                aux.append(visit(child));
            }
        }

        if(currFunc.equals("main")){
            aux.append("ret.V;\n");
        }

        aux.append("}\n");

        currFunc = null;

        return aux.toString();
    }

    private String callVisit(JmmNode node, String extra){
        StringBuilder aux = new StringBuilder();

        //String object = visit(node.getJmmChild(0));
        List<String> object  = multipleFuncParser(node.getJmmChild(0), true);

        StringBuilder codeArgsBefore = new StringBuilder();
        StringBuilder codeArgsAfter = new StringBuilder();

        var args = node.getJmmChild(1).getChildren();

        for (int i = 0; i < args.size(); i++) {
            List<String> codeArgs = this.multipleFuncParser(args.get(i), true);
            codeArgsBefore.append(codeArgs.get(0));
            codeArgsAfter.append(", ").append(codeArgs.get(1));
        }


        aux.append(object.get(0));
        aux.append(codeArgsBefore);

        if(extra != null){
            aux.append(extra);
        }
        if(node.getJmmChild(0).getKind().equals("IdentifierObject") &&
                !OllirUtils.existVariable(node.getJmmChild(0).get("image"),currFunc , symbolTable)){
            aux.append("invokestatic(");
        } else {
            aux.append("invokevirtual(");
        }
        aux.append(object.get(1)).append(", \"").append(node.get("image")).append("\"").append(codeArgsAfter.toString());

        aux.append(")");
        Type type = symbolTable.getReturnType(node.get("image"));
        if (type != null && (node.getJmmChild(0).getKind().equals("ClassObject") || node.getJmmChild(0).getKind().equals("IdentifierObject"))) {
            aux.append(OllirUtils.getCode(type));
        }
        else if(expectedType == null) {
            aux.append(".V");
        } else {
            aux.append(expectedType);
        }
        aux.append(";\n");
        return aux.toString();
    }

    private String identifierObjectVisit(JmmNode node, String integer) {
        StringBuilder str = new StringBuilder();

        if(OllirUtils.isLocalVariable(node.get("image"),currFunc,symbolTable)){
            str.append(node.get("image"));
            return str.toString();
        }
        int index = OllirUtils.getParameterIndex(node.get("image"),currFunc,symbolTable);
        if(index != -1){
            // If function is static index from 0..N / else index from 1..N
            if(!currFunc.equals("main")) {
                index++;
            }
            str.append("$").append(index).append(".").append(node.get("image"));
            return str.toString();
        }


        return node.get("image");
    }

    private String classObjectVisit(JmmNode node, String integer) {
        return node.get("image");
    }

    private String argsVisit(JmmNode node, String integer) {

        return "";
    }

    private String returnVisit(JmmNode node, String extra) {
        StringBuilder aux = new StringBuilder();

        if(extra != null){
            aux.append(extra);
        }

        String lastExpectedType = expectedType;
        expectedType = OllirUtils.getCode(symbolTable.getReturnType(currFunc));
        List<String> codeArgs = this.multipleFuncParser(node.getJmmChild(0), true);

        aux.append(codeArgs.get(0));

        aux.append("ret").append(OllirUtils.getCode(symbolTable.getReturnType(currFunc))).append(" ")
                .append(codeArgs.get(1)).append(";\n");

        expectedType = lastExpectedType;
        return aux.toString();
    }

    private String literalVisit(JmmNode node, String dummy) {
        return node.get("image") + OllirUtils.getOllirType(node.get("type"));
    }

    private List<String> multipleFuncParser(JmmNode node, boolean localCreation) {
        StringBuilder auxBefore = new StringBuilder();
        StringBuilder auxAfter = new StringBuilder();
        String lastExpectedType;

        switch (node.getKind()){
            case "Call":
                tNum++;
                Type type = symbolTable.getReturnType(node.get("image"));
                StringBuilder extra = new StringBuilder();
                if(type == null){
                    extra.append("t").append(tNum).append(expectedType)
                            .append(" :=").append(expectedType).append(" ");
                } else {
                    extra.append("t").append(tNum).append(OllirUtils.getCode(type))
                            .append(" :=").append(OllirUtils.getCode(type)).append(" ");
                }

                int actualTNum = tNum;
                auxBefore.append(visit(node,extra.toString()));

                if(type == null){
                    auxAfter.append("t").append(actualTNum)
                            .append(expectedType);
                } else {
                    auxAfter.append("t").append(actualTNum)
                            .append(OllirUtils.getCode(type));
                }

                break;
            case "UnaryOp":
                JmmNode operand = node.getJmmChild(0);

                // NEG -> <op> bool
                lastExpectedType = expectedType;
                expectedType = OllirUtils.getOllirType("boolean");

                List<String> operandCode = multipleFuncParser(operand, true);

                StringBuilder unOp = new StringBuilder();

                auxBefore.append(operandCode.get(0));

                unOp.append(OllirUtils.getOperationType(node.get("op"))).append(" ")
                        .append(operandCode.get(1));

                if(localCreation){
                    tNum++;
                    auxBefore.append("t").append(tNum)
                            .append(OllirUtils.getOllirType(node.get("type")))
                            .append(" :=").append(OllirUtils.getOllirType(node.get("type")))
                            .append(" ").append(unOp).append(";\n");
                    auxAfter.append("t").append(tNum)
                            .append(OllirUtils.getOllirType(node.get("type")));
                }else{
                    auxAfter.append(unOp);
                }
                expectedType = lastExpectedType;
                break;
            case "Binop":
                JmmNode left = node.getJmmChild(1);
                JmmNode right = node.getJmmChild(0);

                // AND -> bool <op> bool
                // LT ADD SUB MUL DIV -> int <op> int
                lastExpectedType = expectedType;
                switch (node.get("op")){
                    case "LT":
                    case "ADD":
                    case "SUB":
                    case "MUL":
                    case "DIV":
                        expectedType = OllirUtils.getOllirType("int");
                        break;
                    case "AND":
                        expectedType = OllirUtils.getOllirType("boolean");
                        break;
                }


                List<String> leftCode = multipleFuncParser(left, !left.getKind().equals("Literal"));
                List<String> rightCode = multipleFuncParser(right, !right.getKind().equals("Literal"));

                auxBefore.append(leftCode.get(0));
                auxBefore.append(rightCode.get(0));

                StringBuilder op = new StringBuilder();
                op.append(rightCode.get(1)).append(" ")
                        .append(OllirUtils.getOperationType(node.get("op"))).append(" ")
                        .append(leftCode.get(1));
                if(localCreation){
                    tNum++;
                    auxBefore.append("t").append(tNum)
                            .append(OllirUtils.getOllirType(node.get("type")))
                            .append(" :=").append(OllirUtils.getOllirType(node.get("type")))
                            .append(" ").append(op).append(";\n");
                    auxAfter.append("t").append(tNum)
                            .append(OllirUtils.getOllirType(node.get("type")));

                }else {
                    auxAfter.append(op);
                }

                expectedType = lastExpectedType;
                break;
            case "Access":
                List<String> aux = multipleFuncParser(node.getJmmChild(1),true);

                List<String> aux2 = multipleFuncParser(node.getJmmChild(0),true);
                List<String> temp = Arrays.asList(aux2.get(1).split("\\."));
                String arrayAccess = String.join(".",temp.subList(0,temp.size() - 2));

                tNum++;

                auxBefore.append(aux.get(0))
                        .append(aux2.get(0))
                        .append("t").append(tNum).append(".i32")
                        .append(" :=.i32 ")
                        .append(arrayAccess).append("[")
                        .append(aux.get(1))
                                .append("]").append(".i32;\n");

                auxAfter.append("t").append(tNum)
                        .append(".i32");

                break;
            case "PropAccess":
                List<String> arrayCode = multipleFuncParser(node.getJmmChild(0),false);
                tNum++;

                auxBefore.append(arrayCode.get(0))
                        .append("t").append(tNum)
                        .append(".i32")
                        .append(" :=").append(".i32 ")
                        .append("arraylength(").append(arrayCode.get(1)).append(").i32;\n");
                auxAfter.append("t").append(tNum)
                        .append(".i32");
                break;

            case "IdentifierObject":
                Type t = AnalyserUtils.getType(node, currFunc, symbolTable);
                String identifierType = "";
                if(t != null){
                    identifierType = OllirUtils.getCode(t);
                }

                if(OllirUtils.isLocalVariable(node.get("image"),currFunc,symbolTable)){
                    auxAfter.append(node.get("image"))
                            .append(identifierType);
                    break;
                }

                int index = OllirUtils.getParameterIndex(node.get("image"),currFunc,symbolTable);
                if(index != -1){
                    // If function is static index from 0..N / else index from 1..N
                    if(!currFunc.equals("main")) {
                        index++;
                    }
                    tNum++;
                    auxBefore.append("t").append(tNum).append(identifierType).append(" :=").append(identifierType)
                            .append(" $").append(index).append(".").append(node.get("image")).append(identifierType)
                            .append(";\n");

                    auxAfter.append("t").append(tNum)
                            .append(identifierType);

                    break;
                }

                if(OllirUtils.isField(node,currFunc,symbolTable)){
                    tNum++;
                    auxBefore.append("t").append(tNum)
                            .append(identifierType)
                            .append(" :=").append(identifierType)
                            .append(OllirUtils.getOllirGetField(currFunc)).append(node.get("image")).append(identifierType)
                            .append(")").append(identifierType)
                            .append(";\n");
                    auxAfter.append("t").append(tNum)
                            .append(identifierType);
                    break;
                }

                auxAfter.append(node.get("image"))
                        .append(identifierType);

                break;
            case "NewStatement":
                if(node.get("type").equals("intArray")){
                    JmmNode child = node.getJmmChild(0);
                    List<String> accessCode = multipleFuncParser(child, true);
                    auxBefore.append(accessCode.get(0));
                    auxAfter.append("new(array,")
                            .append(accessCode.get(1))
                            .append(").array.i32");
                }else{
                    auxAfter.append("new(").append(node.get("type")).append(")")
                            .append(".").append(node.get("type"));
                }
                break;
            case "ClassObject":
                auxAfter.append(visit(node));
                break;
            case "Literal":
                if(localCreation){
                    if(node.getJmmParent().getKind().equals("Access")){
                        tNum++;
                        int actualtNum = tNum;
                        auxBefore.append("t").append(tNum)
                                .append(OllirUtils.getOllirType(node.get("type")))
                                .append(" :=").append(OllirUtils.getOllirType(node.get("type")))
                                .append(" ").append(visit(node)).append(";\n");

                        auxAfter.append("t").append(actualtNum)
                                .append(OllirUtils.getOllirType(node.get("type")));
                    } else {
                        auxAfter.append(visit(node));
                    }


                    break;
                }
                // dont add new case statement after Literar it uses the default one!!!!!!!!
            default:
                auxAfter.append(node.get("image"))
                        .append(OllirUtils.getCode(Objects.requireNonNull(AnalyserUtils.getType(node, currFunc, symbolTable))));
                break;
        }

        List<String> result = new ArrayList<>();
        result.add(auxBefore.toString());
        result.add(auxAfter.toString());

        return result;
    }

    private String whileVisit(JmmNode node, String prefix) {
        StringBuilder code = new StringBuilder();
        int loopNumber = loopNum++;
        JmmNode condition = node.getJmmChild(0).getJmmChild(0);
        JmmNode block = node.getJmmChild(1).getJmmChild(0);

        String lastExpectedType = expectedType;
        expectedType = OllirUtils.getOllirType("boolean");
        List<String> conditionCode = multipleFuncParser(condition, true);
        expectedType = lastExpectedType;

        //Do while
        JmmNode cond = node.getJmmChild(0).getJmmChild(0);
        if(cond.getKind().equals("Literal") && cond.get("image").equals("true") && Objects.equals(config.get("optimize"),"true")) {
            code.append("Loop").append(loopNum).append(":\n");
            code.append(visit(block))
                    .append(conditionCode.get(0))
                    .append("if(").append(conditionCode.get(1))
                    .append(") goto Loop").append(loopNum)
                    .append(";\n");
        }else {
            //Normal while
            code.append("Loop").append(loopNumber).append(":\n");
            code.append(conditionCode.get(0))
                    .append("if( !.bool ").append(conditionCode.get(1))
                    .append(") goto End").append(loopNumber)
                    .append(";\n")
                    .append(visit(block))
                    .append(conditionCode.get(0))
                    .append("goto Loop").append(loopNumber).append(";\n");
            code.append("End").append(loopNumber).append(":\n");
        }


        return code.toString();
    }

    private String assignVisit(JmmNode node, String prefix){
        StringBuilder code = new StringBuilder();

        JmmNode left = node.getJmmChild(0);
        JmmNode right = node.getJmmChild(1);

        String type = OllirUtils.getCode(Objects.requireNonNull(AnalyserUtils.getType(left, currFunc, symbolTable)));
        String lastExpectedType = expectedType;
        expectedType = type;
        List<String> auxCode = multipleFuncParser(right, true);

        //verify if it is field of the class
        if(left.getKind().equals("IdentifierObject") && OllirUtils.isField(left,currFunc,symbolTable)){
            code.append(auxCode.get(0))
                    .append(OllirUtils.getOllirPutField(currFunc))
                    .append(visit(left)).append(type)
                    .append(", ")
                    .append(auxCode.get(1))
                    .append(").V;\n");
        } else{
            code.append(auxCode.get(0))
                .append(visit(left)).append(type)
                .append(" :=").append(type).append(" ")
                .append(auxCode.get(1))
                .append(";\n");
        }

        if(right.getKind().equals("NewStatement") && !right.get("type").equals("intArray")){
            code.append("invokespecial(").append(left.get("image"))
                    .append(".").append(right.get("type")).append(",\"<init>\").V;\n");
        }

        expectedType = lastExpectedType;

        return code.toString();
    }

    private String accessVisit(JmmNode node, String prefix){
        StringBuilder code = new StringBuilder();
        JmmNode index = node.getJmmChild(1);
        JmmNode array = node.getJmmChild(0);


        List<String> auxCode = multipleFuncParser(index, true);
        code.append(auxCode.get(0))
                .append(array.get("image")).append("[")
                .append(auxCode.get(1)).append("]");
        return code.toString();
    }

    private String ifVisit(JmmNode node, String dummy) {
        StringBuilder code = new StringBuilder();
        int ifNumber = ifNum++;

        JmmNode condition = node.getJmmChild(0);
        JmmNode thenStatement = node.getJmmChild(1);
        JmmNode elseStatement = node.getJmmChild(2);

        String lastExpectedType = expectedType;
        expectedType = OllirUtils.getOllirType("boolean");
        List<String> condString = multipleFuncParser(condition.getJmmChild(0),false);
        expectedType = lastExpectedType;
        code.append(condString.get(0));

        code.append("if (").append(condString.get(1)).append(") goto Then").append(ifNumber).append(";\n")
                .append(visit(elseStatement.getJmmChild(0))).append("goto Endif").append(ifNumber).append(";\n")
                .append("Then").append(ifNumber).append(":\n")
                .append(visit(thenStatement.getJmmChild(0)))
                .append("Endif").append(ifNumber).append(":\n");

        return code.toString();
    }

    private String blockVisit(JmmNode node, String dummy) {
        StringBuilder code = new StringBuilder();

        for (var child: node.getChildren()){
            code.append(visit(child));
        }

        return code.toString();
    }
}
