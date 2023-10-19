package pt.up.fe.comp.analysis;

import pt.up.fe.comp.analysis.table.SymbTable;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

public class VisitorSymbolTable extends AJmmVisitor<Integer, Integer> {

    final private SymbTable table = new SymbTable();
    final List<Report> reports = new ArrayList<>();

    public VisitorSymbolTable() {
        addVisit("Start", this::visitStart);
        addVisit("ImportDec", this::visitImport);
        addVisit("ClassDeclaration", this::visitClass);
        addVisit("MethodDeclaration", this::visitMethod);
    }

    private Integer visitMethod(JmmNode node , Integer dummy){
        String name = node.get("image");

        if(table.getMethods().contains(name) || AnalyserUtils.existClass(name,table)){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("column")),
                    "Already declared " + name ));
        }

        for(int i = 0; i < node.getChildren().size(); i++){
            JmmNode elem = node.getChildren().get(i);
            if(elem.getKind().equals("Type")){
                Type typeClass = this.getType(elem);
                if(typeClass == null){
                    return dummy;
                }
                else{
                    table.addReturnType(name,typeClass);
                }
            }
            else if(elem.getKind().equals("Param")){
                Type typeClass = this.getType(elem.getJmmChild(0));
                if(typeClass != null){
                    table.addParameter(name,new Symbol(typeClass,elem.get("image")));
                }
            }
            else if(elem.getKind().equals("VarDeclaration")){
                Type typeClass = this.getType(elem.getJmmChild(0));
                if(typeClass != null){
                    table.addLocalVariable(name, new Symbol(typeClass,elem.get("image")));
                }
            }
        }
        return dummy;
    }

    private Integer visitClass(JmmNode node, Integer dummy){
        table.setClass(node.get("image"));
        for (int i = 0; i < node.getChildren().size(); i++){
            JmmNode elem = node.getChildren().get(i);
            if(elem.getKind().equals("ClassInheritance")){
                table.setClassSuper(elem.get("image"));
            }
            else if(elem.getKind().equals("VarDeclaration")){
                JmmNode type = elem.getChildren().get(0);
                String name = elem.get("image");
                Type typeClass = this.getType(type);
                if(typeClass != null){
                    table.addField(new Symbol(typeClass,name));
                }
            }
            else if(elem.getKind().equals("MethodDeclaration")){
                visit(elem, 1);
            }
        }
        return dummy;
    }

    private Integer visitImport(JmmNode node, Integer dummy) {
        StringBuilder importName = new StringBuilder();
        importName.append(node.get("image"));
        for (int i = 0; i < node.getChildren().size(); i++){
            importName.append(".");
            importName.append(node.getChildren().get(i).get("image"));
        }
        table.addImport(importName.toString());
        return dummy;
    }

    private Integer visitStart(JmmNode node, Integer dummy) {
        for(var n : node.getChildren()){
            visit(n, 1);
        }
        return null;
    }

    public SymbTable getTable() {
        return table;
    }

    private Type getType(JmmNode type){
        String isArray = type.get("isArray");
        if(AnalyserUtils.validType(type.get("type"), table)){
            return new Type(type.get("type"), isArray.equals("true"));
        }
        else{
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(type.get("line")), Integer.parseInt(type.get("column")),
                    "Invalid type " + type.get("type") ));
            return null;
        }
    }

    public List<Report> getReports() {
        return reports;
    }
}

