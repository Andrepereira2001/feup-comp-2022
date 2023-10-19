package pt.up.fe.comp.optimizations;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import java.util.HashMap;
import java.util.Map;

public class CopyPropagationOptimizer extends AJmmVisitor<String, Integer>{

    Map<String, JmmNode> copyVariables = new HashMap<>();
    boolean changed = false;

    public CopyPropagationOptimizer(){

        addVisit("Start", this::defaultVisit);
        addVisit("Assign", this::assignVisit);
        addVisit("ImportDec", this::defaultVisit);
        addVisit("ImportPackage", this::defaultVisit);
        addVisit("ClassDeclaration", this::defaultVisit);
        addVisit("ClassInheritance", this::defaultVisit);
        addVisit("VarDeclaration", this::defaultVisit);
        addVisit("MethodDeclaration", this::methodVisit);
        addVisit("Call", this::defaultVisit);
        addVisit("ClassObject", this::defaultVisit);
        addVisit("Args", this::defaultVisit);
        addVisit("ReturnStatement", this::defaultVisit);
        addVisit("IdentifierObject", this::identifierVisit);
        addVisit("Binop", this::defaultVisit);
        addVisit("Access", this::defaultVisit);
        addVisit("Literal", this::defaultVisit);
        addVisit("IfStatement", this::ifVisit);
        addVisit("Block", this::defaultVisit);
        addVisit("WhileStatement", this::whileVisit);
        addVisit("Type", this::defaultVisit);
        addVisit("Param", this::defaultVisit);
        addVisit("Condition", this::defaultVisit);
        addVisit("Body", this::defaultVisit);
        addVisit("Then", this::defaultVisit);
        addVisit("ElseStatement", this::defaultVisit);
        addVisit("NewStatement", this::defaultVisit);
        addVisit("UnaryOp", this::defaultVisit);
        addVisit("PropAccess", this::defaultVisit);
    }

    private int defaultVisit(JmmNode node, String dummy) {
        for(var child: node.getChildren()){
            visit(child);
        }
        return 0;
    }

    private int methodVisit(JmmNode node, String dummy) {
        copyVariables.clear();
        for(var child: node.getChildren()){
            visit(child);

        }
        return 0;
    }

    private int assignVisit(JmmNode node, String dummy) {
        visit(node.getJmmChild(1));
        if(node.getJmmChild(1).getKind().equals("IdentifierObject")){
            JmmNode child = node.getChildren().get(1);
            copyVariables.put(node.getJmmChild(0).get("image"), child);
        }
        else {
            copyVariables.put(node.getJmmChild(0).get("image"), null);
        }
        return 0;
    }

    private int identifierVisit(JmmNode node, String s) {
        String image = node.get("image");
        if(copyVariables.containsKey(image) && copyVariables.get(image) != null){
            copyNodeTo(copyVariables.get(image), node);
        }
        return 0;
    }

    private int whileVisit(JmmNode node, String s) {
        Map<String, JmmNode> actualMap = new HashMap<>(copyVariables);
        copyVariables.clear();

        //visit Body
        visit(node.getJmmChild(1));
        Map<String, JmmNode> bodyConstants = new HashMap<>(copyVariables);
        copyVariables.clear();

        for(String cons: actualMap.keySet()){
            if(bodyConstants.containsKey(cons)){
                if(bodyConstants.get(cons) == null || actualMap.get(cons) == null){
                    copyVariables.put(cons, null);
                }
                else if(bodyConstants.get(cons).get("image").equals(actualMap.get(cons).get("image"))){
                    copyVariables.put(cons, actualMap.get(cons));
                }
            } else {
                copyVariables.put(cons, actualMap.get(cons));
            }
        }

        Map<String, JmmNode> finalConstants = new HashMap<>(copyVariables);
        visit(node.getJmmChild(1));
        copyVariables = finalConstants;

        //visit Condition
        visit(node.getJmmChild(0));
        return 0;
    }

    private int ifVisit(JmmNode node, String s) {
        //visit Condition
        visit(node.getJmmChild(0));

        Map<String, JmmNode> actualMap = new HashMap<>(copyVariables);


        //visit Then
        visit(node.getJmmChild(1));
        Map<String, JmmNode> map1 = new HashMap<>(copyVariables);

        copyVariables = new HashMap<>(actualMap);
        //visit Else
        visit(node.getJmmChild(2));

        for(String cons: map1.keySet()){
            if(copyVariables.containsKey(cons)){
                if(map1.get(cons) == null || copyVariables.get(cons) == null){
                    copyVariables.put(cons, null);
                }
                else if(!copyVariables.get(cons).get("image").equals(map1.get(cons).get("image"))){
                    copyVariables.remove(cons);
                }
            } else {
                copyVariables.remove(cons);
            }
        }

        return 0;
    }

    public boolean isChanged() {
        return changed;
    }

    private void copyNodeTo(JmmNode copyNode, JmmNode destNode){
        changed = true;
        JmmNode newNode = new JmmNodeImpl(copyNode.getKind());
        newNode.put("image", copyNode.get("image"));
        newNode.put("type", copyNode.get("type"));
        newNode.put("line", destNode.get("line"));
        newNode.put("column", destNode.get("column"));
        destNode.replace(newNode);
    }

}
