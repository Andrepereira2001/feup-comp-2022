package pt.up.fe.comp.optimizations;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import java.util.HashMap;
import java.util.Map;

public class ConstantPropagationOptimizer extends AJmmVisitor<String, Integer> {
    Map<String, JmmNode> constants = new HashMap<>();
    boolean changed = false;

    public ConstantPropagationOptimizer(){

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
        constants.clear();
        for(var child: node.getChildren()){
            visit(child);

        }
        return 0;
    }

    private int assignVisit(JmmNode node, String dummy) {
        visit(node.getJmmChild(1));
        if(node.getJmmChild(1).getKind().equals("Literal")){
            JmmNode child = node.getChildren().get(1);
            constants.put(node.getJmmChild(0).get("image"), child);
        }
        else {
            constants.put(node.getJmmChild(0).get("image"), null);
        }
        return 0;
    }

    private int identifierVisit(JmmNode node, String s) {
        String image = node.get("image");
        if(constants.containsKey(image) && constants.get(image) != null){
            copyNodeTo(constants.get(image), node);
        }

        return 0;
    }

    private int whileVisit(JmmNode node, String s) {
        Map<String, JmmNode> actualMap = new HashMap<>(constants);
        constants.clear();

        //visit Body
        visit(node.getJmmChild(1));
        Map<String, JmmNode> bodyConstants = new HashMap<>(constants);
        constants.clear();

        for(String cons: actualMap.keySet()){
            if(bodyConstants.containsKey(cons)){
                if(bodyConstants.get(cons) == null || actualMap.get(cons) == null){
                    constants.put(cons, null);
                }
                else if(bodyConstants.get(cons).get("image").equals(actualMap.get(cons).get("image"))){
                    constants.put(cons, actualMap.get(cons));
                }
            } else {
                constants.put(cons, actualMap.get(cons));
            }
        }

        Map<String, JmmNode> finalConstants = new HashMap<>(constants);
        visit(node.getJmmChild(1));
        constants = finalConstants;

        //visit Condition
        visit(node.getJmmChild(0));
        return 0;
    }

    private int ifVisit(JmmNode node, String s) {
        //visit Condition
        visit(node.getJmmChild(0));

        Map<String, JmmNode> actualMap = new HashMap<>(constants);

        //visit Then
        visit(node.getJmmChild(1));
        Map<String, JmmNode> map1 = new HashMap<>(constants);

        constants = new HashMap<>(actualMap);
        //visit Else
        visit(node.getJmmChild(2));

        for(String cons: map1.keySet()){
            if(constants.containsKey(cons)){
                if(map1.get(cons) == null || constants.get(cons) == null){
                    constants.put(cons, null);
                }
                else if(!constants.get(cons).get("image").equals(map1.get(cons).get("image"))){
                    constants.remove(cons);
                }
            } else {
                constants.remove(cons);
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
