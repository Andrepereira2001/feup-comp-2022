package pt.up.fe.comp.optimizations.utils;

import org.specs.comp.ollir.Node;

import java.util.Objects;

public class NodeIntGraph {
    private final String varName;

    public NodeIntGraph(String varName){
        this.varName = varName;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){
            return true;
        }

        if(!(obj instanceof NodeIntGraph)){
            return false;
        }
        NodeIntGraph nodeIntGraph = (NodeIntGraph) obj;
        return this.varName.equals(nodeIntGraph.varName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(varName);
    }

    public String getVarName() {
        return varName;
    }
}


