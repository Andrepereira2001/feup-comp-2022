package pt.up.fe.comp.optimizations;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.optimizations.utils.IntGraph;

import java.util.*;

public class Interference {
    private HashMap<Node, HashSet<String>> def = new HashMap<>();
    private HashMap<Node , HashSet<String>> use = new HashMap<>();

    private HashMap<Node, HashSet<String>> in = new HashMap<>();
    private HashMap<Node , HashSet<String>> out = new HashMap<>();

    private Method method;
    public Interference(){

    }

    public IntGraph getInterferenceGraph(Method method) {
        method.buildCFG();
        method.buildVarTable();
        def.clear();
        use.clear();
        in.clear();
        out.clear();
        this.method = method;
        for(Instruction instruction : method.getInstructions()){
            def.put(instruction, getDefinitions(instruction));
            use.put(instruction, getUsages(instruction));
            in.put(instruction, new HashSet<>());
            out.put(instruction, new HashSet<>());
        }

        processDataflow();
        ArrayList<HashMap<Node, HashSet<String>>> liveRanges = new ArrayList<>();
        liveRanges.add(in); liveRanges.add(out);
        return new IntGraph( liveRanges, method);
    }


    public void processDataflow() {
        HashMap<Node,HashSet<String>> previousOut;
        HashMap<Node,HashSet<String>> previousIn;

        boolean done = false;

        while (!done){
            previousOut = deepCopyMatrix(out);
            previousIn = deepCopyMatrix(in);
            for (Instruction inst : method.getInstructions()) {
                out.put(inst, getOut(inst));
                in.put(inst,  getIn(inst));
            }

            done = true;
            for(Instruction inst: method.getInstructions()){
                done &= (out.get(inst).equals(previousOut.get(inst)) && in.get(inst).equals(previousIn.get(inst)));
            }
        }
    }

    private HashMap<Node, HashSet<String>> deepCopyMatrix(HashMap<Node, HashSet<String>> matrix) {
        HashMap<Node, HashSet<String>> cpy = new HashMap<>();
        for(var node : matrix.entrySet()){
            cpy.put(node.getKey(), new HashSet<>(node.getValue()));
        }
        return cpy;
    }


    public HashSet<String> getOut(Node node) {
        HashSet<String> out = new HashSet<>();

        for (Node successor : node.getSuccessors()) {
            if (!in.containsKey(successor)) continue;
            out.addAll(in.get(successor));
        }
        return out;
    }

    public HashSet<String> getIn(Node node) {
        HashSet<String> in = new HashSet<>(this.out.get(node));
        in.removeAll(this.def.get(node));
        in.addAll(this.use.get(node));
        return in;
    }

    private HashSet<String> getUsages(Instruction instruction) {
        switch (instruction.getInstType()) {
            case ASSIGN:
                return getUsedAssign((AssignInstruction) instruction);
            case CALL:
                return getUsedCall((CallInstruction) instruction);
            case PUTFIELD:
                return getUsedPutField((PutFieldInstruction) instruction);
            case BINARYOPER:
                return getUsedBinaryOperator((BinaryOpInstruction) instruction);
            case UNARYOPER:
                return getUsedUnaryOperator((UnaryOpInstruction) instruction);
            case NOPER:
                return getUsedNoper((SingleOpInstruction) instruction);
            case BRANCH:
                return getUsedBranch((CondBranchInstruction) instruction);
            case RETURN:
                return getUsedReturn((ReturnInstruction) instruction);
        }
        return new HashSet<>();
    }

    private HashSet<String> getUsedUnaryOperator(UnaryOpInstruction instruction) {
        HashSet<String> usedVars = new HashSet<>();
        usedVars.addAll(operandNames(instruction.getOperand()));
        return usedVars;
    }

    private HashSet<String> getUsedReturn(ReturnInstruction instruction) {
        HashSet<String> usedVars = new HashSet<>();
        if(instruction.hasReturnValue()){
            usedVars.addAll(operandNames(instruction.getOperand()));
        }
        return usedVars;
    }

    private HashSet<String> getUsedBranch(CondBranchInstruction instruction) {
        HashSet<String> usedVars = new HashSet<>();
        Instruction condition = instruction.getCondition();
        switch(condition.getInstType()){
            case UNARYOPER:
                UnaryOpInstruction unaryCondition = (UnaryOpInstruction) condition;
                usedVars.addAll(operandNames(unaryCondition.getOperand()));
                break;
            case BINARYOPER:
                BinaryOpInstruction binaryCondition = (BinaryOpInstruction) condition;
                usedVars.addAll(operandNames(binaryCondition.getLeftOperand()));
                usedVars.addAll(operandNames(binaryCondition.getRightOperand()));
                break;
            case NOPER:
                SingleOpInstruction noOpCondition = (SingleOpInstruction) condition;
                usedVars.addAll(operandNames(noOpCondition.getSingleOperand()));
                break;
        }
        return usedVars;
    }

    private HashSet<String> getUsedNoper(SingleOpInstruction instruction) {
        HashSet<String> usedVars = new HashSet<>();
        usedVars.addAll(operandNames(instruction.getSingleOperand()));
        return usedVars;
    }

    private HashSet<String> getUsedBinaryOperator(BinaryOpInstruction instruction) {
        HashSet<String> usedVars = new HashSet<>();
        usedVars.addAll(operandNames(instruction.getLeftOperand()));
        usedVars.addAll(operandNames(instruction.getRightOperand()));
        return usedVars;
    }

    private HashSet<String> getUsedPutField(PutFieldInstruction instruction) {
        HashSet<String> usedVars = new HashSet<>();
        usedVars.addAll(operandNames(instruction.getThirdOperand()));
        return usedVars;
    }

    private HashSet<String> getUsedCall(CallInstruction instruction) {
        HashSet<String> usedVars = new HashSet<>();
        if(instruction.getNumOperands() > 1){
            if (instruction.getInvocationType() != CallType.NEW)
                usedVars.addAll(operandNames(instruction.getSecondArg()));

            for(Element arg : instruction.getListOfOperands()){
                usedVars.addAll(operandNames(arg));
            }
        }
        return usedVars;
    }

    private HashSet<String> getUsedAssign(AssignInstruction instruction) {
        HashSet<String> usedVars = new HashSet<>();
        if(instruction.getDest() instanceof ArrayOperand){
            usedVars.addAll(operandNames(instruction.getDest()));
        }

        usedVars.addAll(getUsages(instruction.getRhs()));
        return usedVars;
    }

    private HashSet<String> operandNames(Element element){
        HashSet<String> usedVars = new HashSet<>();
        if(element.isLiteral()){
            return usedVars;
        }
        if(element instanceof ArrayOperand){
            ArrayOperand arrayOperand = (ArrayOperand) element;
            ArrayList<Element> indexOperand = arrayOperand.getIndexOperands();
            usedVars.add(((Operand) indexOperand.get(0)).getName());
        }

        String varName = ((Operand) element).getName();
        Descriptor d = method.getVarTable().get(varName);
        if (d.getScope() == VarScope.PARAMETER || d.getScope() == VarScope.FIELD)
            return usedVars;

        usedVars.add(varName);
        return usedVars;
    }

    private HashSet<String> getDefinitions(Instruction instruction) {
        HashSet<String> definedVars = new HashSet<>();
        if(instruction.getInstType() == InstructionType.ASSIGN){
            AssignInstruction assignInstruction = (AssignInstruction) instruction;
            Element definition = assignInstruction.getDest();
            if(!(definition instanceof ArrayOperand)){
                definedVars.add(((Operand) definition).getName());
            }
        }
        return definedVars;
    }
}
