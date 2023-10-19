package pt.up.fe.comp.analysis;

import pt.up.fe.comp.IfStatement;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

public class VisitorTypeVerifier extends AJmmVisitor<SymbolTable, Integer> {
    List<Report> reports = new ArrayList<>();
    String currFunc;

    public VisitorTypeVerifier(){
        addVisit("Start", this::visitDefault);
        addVisit("ClassDeclaration", this::visitDefault);
        addVisit("MethodDeclaration", this::visitMethod);
        addVisit("Call", this::visitCall);
        addVisit("Args", this::visitDefault);
        addVisit("PropAccess", this::visitDefault);
        addVisit("Assign", this::visitAssign);
        addVisit("Binop", this::visitOp);
        addVisit("UnaryOp", this::visitOp);
        addVisit("Access", this::visitAccess);
        addVisit("IfStatement", this::visitDefault);
        addVisit("Condition", this::visitOp);
        addVisit("Then", this::visitDefault);
        addVisit("ElseStatement", this::visitDefault);
        addVisit("Block", this::visitDefault);
        addVisit("WhileStatement", this::visitDefault);
        addVisit("ReturnStatement", this::visitOp);
        addVisit("Body", this::visitDefault);
    }

    private Integer visitMethod(JmmNode node , SymbolTable symbolTable){

        this.currFunc = node.get("image");
        for(var n : node.getChildren()){
            visit(n, symbolTable);
        }
        this.currFunc = null;
        return 0;
    }

    private Integer visitAssign(JmmNode node, SymbolTable symbolTable){
        JmmNode child1 = node.getJmmChild(0);
        JmmNode child2 = node.getJmmChild(1);

        Type type2 = AnalyserUtils.getType(child2,currFunc,symbolTable);

        if (type2 != null && !AnalyserUtils.verifyType(child1,type2,currFunc,symbolTable)){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("column")),
                    "Invalid assignment for " + child1.getOptional("image").orElse(child1.getKind()) ));
        }

        visit(child1,symbolTable);
        visit(child2,symbolTable);

        return 0;
    }

    private Integer visitOp(JmmNode node, SymbolTable symbolTable){

        // NEG -> <op> bool
        // AND -> bool <op> bool
        // LT ADD SUB MUL DIV -> int <op> int
        for(JmmNode child : node.getChildren()){
            switch (node.get("op")) {
                case "LT": {
                    if (!AnalyserUtils.verifyType(child, new Type("int", false), currFunc, symbolTable)) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("column")),
                                "Invalid operand type " + child.getOptional("image").orElse(child.getKind()) + " in less then operation"));
                    }
                    break;
                }
                case "ADD": {
                    if (!AnalyserUtils.verifyType(child, new Type("int", false), currFunc, symbolTable)) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("column")),
                                "Invalid operand type " + child.getOptional("image").orElse(child.getKind()) + " in addiction operation"));
                    }
                    break;
                }
                case "SUB": {
                    if (!AnalyserUtils.verifyType(child, new Type("int", false), currFunc, symbolTable)) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("column")),
                                "Invalid operand type " + child.getOptional("image").orElse(child.getKind()) + " in subtraction operation"));
                    }
                }
                case "MUL": {
                    if (!AnalyserUtils.verifyType(child, new Type("int", false), currFunc, symbolTable)) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("column")),
                                "Invalid operand type " + child.getOptional("image").orElse(child.getKind()) + " in multiplication operation"));
                    }
                    break;
                }
                case "DIV": {
                    if (!AnalyserUtils.verifyType(child, new Type("int", false), currFunc, symbolTable)) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("column")),
                                "Invalid operand type " + child.getOptional("image").orElse(child.getKind()) + " in division operation"));
                    }
                    break;
                }
                case "AND": {
                    if (!AnalyserUtils.verifyType(child, new Type("boolean", false), currFunc, symbolTable)) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("column")),
                                "Invalid operand type " + child.getOptional("image").orElse(child.getKind()) + " in and operation"));
                    }
                    break;
                }
                case "NEG": {
                    if (!AnalyserUtils.verifyType(child, new Type("boolean", false), currFunc, symbolTable)) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("column")),
                                "Invalid operand type " + child.getOptional("image").orElse(child.getKind()) + " in negation operation"));
                    }
                    break;
                }
                case "COND": {
                    if (!AnalyserUtils.verifyType(child, new Type("boolean", false), currFunc, symbolTable)) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("column")),
                                "Invalid operand type " + child.getOptional("image").orElse(child.getKind()) + " in conditional operation"));
                    }
                    break;
                }
                case "RET": {
                    Type retType = symbolTable.getReturnType(currFunc);
                    if (retType != null && !AnalyserUtils.verifyType(child, retType, currFunc, symbolTable)) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("column")),
                                "Invalid return type " + child.getOptional("image").orElse(child.getKind())));
                    }
                    break;
                }
            }

            visit(child,symbolTable);
        }
        return 0;
    }

    private Integer visitCall(JmmNode node, SymbolTable symbolTable) {
        //function call parameters type verification
        JmmNode args = node.getJmmChild(1);
        List<Symbol> symbolParams = symbolTable.getParameters(node.get("image"));

        if (symbolParams != null && AnalyserUtils.isKnowObject(node.getJmmChild(0),currFunc, symbolTable)) {

            //verify number of parameters
            if (args.getChildren().size() != symbolParams.size()) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("column")),
                        "Invalid number of arguments in function call " + node.get("image")));
            } else {
                for (int j = 0; j < symbolParams.size(); j++) {
                    if (!AnalyserUtils.verifyType(args.getJmmChild(j), symbolParams.get(j).getType(), currFunc, symbolTable)) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("column")),
                                "Invalid argument type " + args.getJmmChild(j).get("image") + " in function call " + node.get("image")));
                    }
                }
            }
            // verify if function is called over known class && class don't extends
        }else if(AnalyserUtils.verifyType(node.getJmmChild(0),new Type(symbolTable.getClassName(),false),currFunc,symbolTable)
                && symbolTable.getSuper() == null && !symbolTable.getMethods().contains(node.get("image"))){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("column")),
                    "Invalid function call " + node.get("image")));
        }

        visit(node.getJmmChild(0),symbolTable);
        visit(node.getJmmChild(1),symbolTable);

        return 0;
    }

    private Integer visitAccess(JmmNode node, SymbolTable symbolTable){
        JmmNode child1 = node.getJmmChild(0), child2 = node.getJmmChild(1);

        if(AnalyserUtils.getType(child1,currFunc,symbolTable) == null || !AnalyserUtils.getType(child1,currFunc,symbolTable).isArray()){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("column")),
                    "Invalid array variable " + child1.getOptional("image").orElse(child1.getKind())));
        }
        if(! AnalyserUtils.verifyType(child2,new Type("int",false),currFunc,symbolTable)){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("column")),
                    "Invalid array index value " + child2.getOptional("image").orElse(child2.getKind())));
        }

        visit(child1,symbolTable);
        visit(child2,symbolTable);

        return 0;
    }

    private Integer visitDefault(JmmNode node, SymbolTable symbolTable) {
        for(var n : node.getChildren()){
            visit(n, symbolTable);
        }
        return 0;
    }

    public List<Report> getReports() {
        return reports;
    }
}
