package pt.up.fe.comp.optimizations.utils;

import org.specs.comp.ollir.Descriptor;
import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.Node;
import org.specs.comp.ollir.VarScope;

import java.util.*;

public class IntGraph {
    private HashMap<NodeIntGraph, HashSet<NodeIntGraph>> adjMatrix = new HashMap<>();
    private HashMap<NodeIntGraph, Integer> coloring = new HashMap<>();


    public IntGraph(ArrayList<HashMap<Node, HashSet<String>>> liveRanges, Method method){

        for(String name: method.getVarTable().keySet()) {
            Descriptor d = method.getVarTable().get(name);
            if (!(d.getScope() == VarScope.PARAMETER) && !(d.getScope() == VarScope.FIELD))
                adjMatrix.put(new NodeIntGraph(name), new HashSet<>());
        }

        for (HashMap<Node, HashSet<String>> range: liveRanges) {
            for (Node node : range.keySet()) {
                HashSet<String> vars = range.get(node);
                for (String varFrom : vars) {
                    NodeIntGraph from = new NodeIntGraph(varFrom);
                    for (String varTo : vars) {
                        if(varFrom.equals(varTo)) continue;
                        NodeIntGraph to = new NodeIntGraph(varTo);
                        adjMatrix.get(from).add(to);
                    }
                }
            }
        }
    }

    public boolean colorGraph(int numColors){
        Stack<NodeIntGraph> stack = new Stack<>();
        HashMap<NodeIntGraph, HashSet<NodeIntGraph>> adjMatrixCpy = getadjMatrixCpy();
        while (stack.size() != adjMatrix.size()) {
            boolean removedNode = false;
            for (var node : adjMatrix.keySet()) {
                if(adjMatrixCpy.containsKey(node)){
                    if (adjMatrixCpy.get(node).size() < numColors) {
                        stack.add(node);
                        for(var neigh : adjMatrixCpy.entrySet()){
                            neigh.getValue().remove(node);
                        }
                        adjMatrixCpy.remove(node);
                        removedNode = true;
                    }
                }
            }
            if (!removedNode) {
                System.out.println(stack.size());
                return false;
            }
        }
        System.out.println("BUILDS STACK");
        while(!stack.empty()){
            NodeIntGraph node = stack.pop();
            int color = minColorAvailable(node);
            if(color > numColors){
                return false;
            }
            coloring.put(node, color);
        }

        return true;
    }

    private HashMap<NodeIntGraph, HashSet<NodeIntGraph>> getadjMatrixCpy() {
        HashMap<NodeIntGraph, HashSet<NodeIntGraph>> adjMatrixCpy = new HashMap<>();
        for(var node : adjMatrix.entrySet()){
            adjMatrixCpy.put(node.getKey(), new HashSet<>(node.getValue()));
        }
        return adjMatrixCpy;
    }

    private int minColorAvailable(NodeIntGraph node) {
        HashSet<Integer> colorSet = new HashSet<>();
        for(NodeIntGraph neigh: adjMatrix.get(node)){
            if(coloring.containsKey(neigh)){
                colorSet.add(coloring.get(neigh));
            }
        }
        for(int i = 1; i <= colorSet.size(); i++){
            if(!colorSet.contains(i)){
                return i;
            }
        }
        return colorSet.size()+1;
    }

    public HashMap<NodeIntGraph, HashSet<NodeIntGraph>> getAdjMatrix() {
        return adjMatrix;
    }

    public void applyColoring(HashMap<String, Descriptor> varTable, int size) {
        for(var color : coloring.entrySet()){
            varTable.get(color.getKey().getVarName()).setVirtualReg(color.getValue() + size);
        }
    }
}
