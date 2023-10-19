package pt.up.fe.comp.optimizations;

import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.Method;
import pt.up.fe.comp.optimizations.utils.IntGraph;

public class RegisterAllocation {
    final private Interference interference;
    final private ClassUnit classUnit;
    final private int maxNumberRegisters;

    public RegisterAllocation(ClassUnit classUnit, int maxNumberRegisters){
        this.interference = new Interference();
        this.classUnit = classUnit;
        this.maxNumberRegisters = maxNumberRegisters;
    }

    public boolean allocateRegisters(){
        for(Method method : classUnit.getMethods()){
            if(!allocateRegisters(method)){
                return false;
            }
        }
        return true;
    }

    public void allocateMinRegisters(){
        for(Method method : classUnit.getMethods()){
            allocateMinRegisters(method);
        }
    }

    public void allocateMinRegisters(Method method){
        IntGraph interferenceGraph = interference.getInterferenceGraph(method);
        for(int i = 0; i <= interferenceGraph.getAdjMatrix().size(); i++){
            if(interferenceGraph.colorGraph(i)){
                interferenceGraph.applyColoring(method.getVarTable(), method.getParams().size());
                return;
            }
        }

    }

    private boolean allocateRegisters(Method method) {
        IntGraph interferenceGraph = interference.getInterferenceGraph(method);
        if(interferenceGraph.colorGraph(maxNumberRegisters - method.getParams().size())){
            interferenceGraph.applyColoring(method.getVarTable(), method.getParams().size());
            return true;
        }
        return false;
    }
}
