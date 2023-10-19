package pt.up.fe.comp.optimizations;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import java.util.HashMap;
import java.util.Map;

public class ConstantFoldingOptimizer extends AJmmVisitor<String, Integer> {
    boolean changed = false;

    public ConstantFoldingOptimizer(){

        addVisit("Start", this::defaultVisit);
        addVisit("Assign", this::defaultVisit);
        addVisit("ImportDec", this::defaultVisit);
        addVisit("ImportPackage", this::defaultVisit);
        addVisit("ClassDeclaration", this::defaultVisit);
        addVisit("ClassInheritance", this::defaultVisit);
        addVisit("VarDeclaration", this::defaultVisit);
        addVisit("MethodDeclaration", this::defaultVisit);
        addVisit("Call", this::defaultVisit);
        addVisit("ClassObject", this::defaultVisit);
        addVisit("Args", this::defaultVisit);
        addVisit("ReturnStatement", this::defaultVisit);
        addVisit("IdentifierObject", this::defaultVisit);
        addVisit("Binop", this::binopVisit);
        addVisit("Access", this::defaultVisit);
        addVisit("Literal", this::defaultVisit);
        addVisit("IfStatement", this::defaultVisit);
        addVisit("Block", this::defaultVisit);
        addVisit("WhileStatement", this::defaultVisit);
        addVisit("Type", this::defaultVisit);
        addVisit("Param", this::defaultVisit);
        addVisit("Condition", this::defaultVisit);
        addVisit("Body", this::defaultVisit);
        addVisit("Then", this::defaultVisit);
        addVisit("ElseStatement", this::defaultVisit);
        addVisit("NewStatement", this::defaultVisit);
        addVisit("UnaryOp", this::unaryVisit);
        addVisit("PropAccess", this::defaultVisit);
    }

    private int defaultVisit(JmmNode node, String dummy) {
        for(var child: node.getChildren()){
            visit(child);
        }
        return 0;
    }

    private int binopVisit(JmmNode node, String dummy){
        if(node.getJmmChild(0).getKind().equals("Literal") && node.getJmmChild(1).getKind().equals("Literal")){
            simplify(node);
        }
        else {
            for (var child : node.getChildren()) {
                visit(child);
            }
        }
        return 0;
    }

    private int unaryVisit(JmmNode node, String dummy){
        if(node.getJmmChild(0).getKind().equals("Literal")){
            simplify(node);
        } else {
            for (var child : node.getChildren()) {
                visit(child);
            }
        }
        return 0;
    }

    public boolean isChanged() {
        return changed;
    }

    private void simplify(JmmNode node) {
        JmmNode newNode = null;
        int int0, int1;
        boolean bool0, bool1;

        // NEG -> <op> bool
        // AND -> bool <op> bool
        // LT ADD SUB MUL DIV -> int <op> int
        switch (node.get("op")) {
            case "ADD":
                newNode = new JmmNodeImpl("Literal");
                int0 = Integer.parseInt(node.getJmmChild(0).get("image"));
                int1 = Integer.parseInt(node.getJmmChild(1).get("image"));
                newNode.put("image", String.valueOf(int0 + int1));
                newNode.put("type", "int");
                break;

            case "SUB":
                newNode = new JmmNodeImpl("Literal");
                int0 = Integer.parseInt(node.getJmmChild(0).get("image"));
                int1 = Integer.parseInt(node.getJmmChild(1).get("image"));
                newNode.put("image", String.valueOf(int0 - int1));
                newNode.put("type", "int");
                break;

            case "MUL":
                newNode = new JmmNodeImpl("Literal");
                int0 = Integer.parseInt(node.getJmmChild(0).get("image"));
                int1 = Integer.parseInt(node.getJmmChild(1).get("image"));
                newNode.put("image", String.valueOf(int0 * int1));
                newNode.put("type", "int");
                break;

            case "DIV":
                newNode = new JmmNodeImpl("Literal");
                newNode.put("type", "int");
                int0 = Integer.parseInt(node.getJmmChild(0).get("image"));
                int1 = Integer.parseInt(node.getJmmChild(1).get("image"));
                // TODO verify the need to throw executin / generate report
                if (int1 != 0) {
                    newNode.put("image", String.valueOf(int0 / int1));
                }
                else {
                    newNode = null;
                }
                break;

            case "LT":
                newNode = new JmmNodeImpl("Literal");
                int0 = Integer.parseInt(node.getJmmChild(0).get("image"));
                int1 = Integer.parseInt(node.getJmmChild(1).get("image"));
                newNode.put("image", String.valueOf(int0 < int1));
                newNode.put("type", "boolean");
                break;

            case "AND":
                newNode = new JmmNodeImpl("Literal");
                bool0 = Boolean.parseBoolean(node.getJmmChild(0).get("image"));
                bool1 = Boolean.parseBoolean(node.getJmmChild(1).get("image"));
                newNode.put("image", String.valueOf(bool0 && bool1));
                newNode.put("type", "boolean");
                break;

            //Unary Case
            case "NEG":
                newNode = new JmmNodeImpl("Literal");
                bool0 = Boolean.parseBoolean(node.getJmmChild(0).get("image"));
                newNode.put("image", String.valueOf(!bool0));
                newNode.put("type", "boolean");
                break;
        }

        if (newNode != null) {
            changed = true;
            newNode.put("line", node.get("line"));
            newNode.put("column", node.get("column"));
            node.replace(newNode);
        }
    }

}
