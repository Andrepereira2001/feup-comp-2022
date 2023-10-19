package pt.up.fe.comp.analysis;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

public class VisitorVariableVerifier extends AJmmVisitor<SymbolTable, Integer> {
    List<Report> reports = new ArrayList<>();
    String currFunc;
    String latestCall = null;

    public VisitorVariableVerifier(){
        addVisit("Start", this::visitStart);
        addVisit("ClassDeclaration", this::visitClass);
        addVisit("MethodDeclaration", this::visitMethod);
    }

    private Integer visitMethod(JmmNode node , SymbolTable symbolTable){
        currFunc = node.get("image");
        visitNode(node, symbolTable);
        return 0;
    }



    private void visitNode(JmmNode node, SymbolTable symbolTable){
        for (int i = 0; i < node.getChildren().size(); i++){
            JmmNode elem = node.getChildren().get(i);
            // Variable is declared
            if(elem.getKind().equals("IdentifierObject")){
                if (!AnalyserUtils.existVariable(elem.get("image"),currFunc, symbolTable)) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("column")),
                            "Variable not declared " + elem.get("image") + "."));
                }
            }
            // Class is imported
            else if (elem.getKind().equals("NewStatement")){
                if (!AnalyserUtils.existClass(elem.get("type"), symbolTable)) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("column")),
                            "Not imported " + elem.get("type") + "."));
                }
            }
            // Method exists in class
            else if (elem.getKind().equals("ClassObject")){
                if(currFunc.equals("main")){
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("column")),
                            "Invalid access to this while in static function."));
                }
                // method don't exist and class do not extend other class -> throw report
                if (latestCall != null && !symbolTable.getMethods().contains(latestCall) && symbolTable.getSuper() == null){
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("column")),
                            "Not declared function " + latestCall + "."));
                }
                latestCall = null;
            }
            // Save function called name
            else if (elem.getKind().equals("Call")){
                if(latestCall != null){
                    Type retType = symbolTable.getReturnType(elem.get("image"));
                    //Throw report if function not declared
                    if(retType != null && retType.getName().equals(symbolTable.getClassName()) &&
                            symbolTable.getSuper() == null && !symbolTable.getMethods().contains(latestCall)){
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("column")),
                                "Not declared function " + latestCall + "."));
                    }
                }
                latestCall = elem.get("image");
            }
            visitNode(elem, symbolTable);

        }
    }

    private Integer visitClass(JmmNode node, SymbolTable symbolTable){
        for (int i = 0; i < node.getChildren().size(); i++){
            JmmNode elem = node.getChildren().get(i);
            // Class inherited not imported
            if(elem.getKind().equals("ClassInheritance")){
                if (!AnalyserUtils.existClass(elem.get("image"), symbolTable)) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("column")),
                            "Not imported " + elem.get("image") + "."));
                }
            }
            // Iterate over all nodes
            else if(elem.getKind().equals("MethodDeclaration")){
                visit(elem, symbolTable);
            }
        }
        return 0;
    }

    private Integer visitStart(JmmNode node, SymbolTable symbolTable) {
        for(var n : node.getChildren()){
            visit(n, symbolTable);
        }
        return 0;
    }

    public List<Report> getReports() {
        return reports;
    }
}
