package pt.up.fe.comp.optimizations;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import java.util.ArrayList;
import java.util.List;

public class DeadCodeOptimizer extends AJmmVisitor<String, Integer> {
    boolean changed = false;
    List<JmmNode> assigns = new ArrayList<>();
    List<JmmNode> varDecs = new ArrayList<>();

    public DeadCodeOptimizer(){

        addVisit("Start", this::startVisit);
        addVisit("Assign", this::assignVisit);
        addVisit("ImportDec", this::defaultVisit);
        addVisit("ImportPackage", this::defaultVisit);
        addVisit("ClassDeclaration", this::defaultVisit);
        addVisit("ClassInheritance", this::defaultVisit);
        addVisit("VarDeclaration", this::varDecVisit);
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

    private int startVisit(JmmNode node, String dummy) {
        for(var n : node.getChildren()){
            visit(n);
        }

        return 0;
    }

    private int defaultVisit(JmmNode node, String dummy) {
        for(var n : node.getChildren()){
            visit(n);
        }
        return 0;
    }

    private int assignVisit(JmmNode node, String dummy){
        assigns.add(node);
        visit(node.getJmmChild(1));
        return 0;
    }

    private int varDecVisit(JmmNode node, String dummy){
        if(node.getJmmParent().getKind().equals("MethodDeclaration")){
            varDecs.add(node);
        }
        for(var n : node.getChildren()){
            visit(n);
        }
        return 0;
    }

    private int methodVisit(JmmNode node, String dummy){
        varDecs.clear();
        assigns.clear();
        for(var n : node.getChildren()){
            visit(n);
        }

        for(var assign: assigns){
            int index = assign.getIndexOfSelf();
            List<JmmNode> calls = findCall(assign);
            for(var call: calls){
                node.add(call, index);
                index++;
            }
            changed = true;
            assign.delete();
        }

        for(var varDec: varDecs){
            changed = true;
            varDec.delete();
        }
        return 0;
    }

    private int identifierVisit(JmmNode node, String dummy){
        for (int i = varDecs.size()-1; i >=0; i--) {
            if(varDecs.get(i).get("image").equals(node.get("image"))){
                varDecs.remove(varDecs.get(i));
            }
        }

        for (int i = assigns.size()-1; i >=0; i--) {
            if(assigns.get(i).getJmmChild(0).get("image").equals(node.get("image"))){
                assigns.remove(i);
                return 0;
            }
        }

        return 0;
    }

    private int whileVisit(JmmNode node, String dummy){

        if(node.getJmmChild(0).getKind().equals("Condition")){
            if(node.getJmmChild(0).getJmmChild(0).getKind().equals("Literal")){
                if(node.getJmmChild(0).getJmmChild(0).get("image").equals("false")){
                    node.delete();
                    changed = true;
                    return 0;
                }
            }
        }

        visit(node.getJmmChild(1));
        visit(node.getJmmChild(0));


        if(node.getJmmChild(1).getKind().equals("Body")){
            if(node.getJmmChild(1).getJmmChild(0).getKind().equals("Block")){
                if(node.getJmmChild(1).getJmmChild(0).getChildren().isEmpty()){
                    int index = node.getIndexOfSelf();
                    List<JmmNode> calls = findCall(node.getJmmChild(0));
                    for(var call: calls){
                        node.getJmmParent().add(call, index);
                        index++;
                    }
                    node.delete();
                    changed = true;
                }
            }
        }
        return 0;
    }

    private int ifVisit(JmmNode node, String dummy){
        if(node.getJmmChild(0).getKind().equals("Condition")){
            if(node.getJmmChild(0).getJmmChild(0).getKind().equals("Literal")){
                if(node.getJmmChild(0).getJmmChild(0).get("image").equals("false")){
                    int index = node.getIndexOfSelf();
                    for(var n: node.getJmmChild(2).getJmmChild(0).getChildren()){
                        visit(n);
                        node.getJmmParent().add(n, index);
                        index+=1;
                    }
                    node.delete();
                    changed = true;
                    return 0;
                } else if(node.getJmmChild(0).getJmmChild(0).get("image").equals("true")) {
                    int index = node.getIndexOfSelf();
                    for(var n: node.getJmmChild(1).getJmmChild(0).getChildren()){
                        visit(n);
                        node.getJmmParent().add(n, index);
                        index+=1;
                    }
                    node.delete();
                    changed = true;
                    return 0;
                }
            }
        }

        visit(node.getJmmChild(0));

        List<JmmNode> currAssigns = new ArrayList<>(assigns);
        List<JmmNode> currVarDecs = new ArrayList<>(varDecs);

        visit(node.getJmmChild(1));

        List<JmmNode> assigns1 = new ArrayList<>(assigns);
        List<JmmNode> varDecs1 = new ArrayList<>(varDecs);

        assigns = currAssigns;
        varDecs = currVarDecs;

        visit(node.getJmmChild(2));

        for (JmmNode n : assigns1){
            if(!assigns.contains(n)){
                assigns.add(n);
            }
        }

        for (JmmNode n : varDecs1){
            if(!varDecs.contains(n)){
                varDecs.add(n);
            }
        }

        if(node.getJmmChild(1).getJmmChild(0).getChildren().isEmpty() &&
                node.getJmmChild(2).getJmmChild(0).getChildren().isEmpty()){
            int index = node.getIndexOfSelf();
            List<JmmNode> calls = findCall(node.getJmmChild(0));
            for(var call: calls){
                node.getJmmParent().add(call, index);
                index++;
            }
            node.delete();
            changed = true;
        }
        return 0;
    }

    private List<JmmNode> findCall(JmmNode node){
        List<JmmNode> calls = new ArrayList<>();
        for(JmmNode n: node.getChildren()){
            if(n.getKind().equals("Call")){
                calls.add(n);
            } else {
                calls.addAll(findCall(n));
            }
        }
        return calls;
    }

    public boolean isChanged() {
        return changed;
    }

}
