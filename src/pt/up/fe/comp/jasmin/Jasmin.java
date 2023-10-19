package pt.up.fe.comp.jasmin;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Jasmin implements JasminBackend {
    private HashMap<String,Descriptor> varTable;
    private ClassUnit classUnit;
    private int stack_size;
    private int max_size;

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        classUnit = ollirResult.getOllirClass();

        String className = classUnit.getClassName();
        StringBuilder jasminCode = new StringBuilder();
        List<Report> reports = new ArrayList<Report>();

        jasminCode.append(getClassDefinition(classUnit));

        for(Field field : classUnit.getFields()){
            jasminCode.append(getField(field));
        }

        StringBuilder methods = new StringBuilder();
        boolean hasConstructor = false;
        for(Method method : classUnit.getMethods()){
            if(method.isConstructMethod()) hasConstructor = true;
            methods.append(getMethod(method));
        }

        if(!hasConstructor)  jasminCode.append(".method public <init>()V\n")
                                        .append("aload_0\n")
                                        .append("invokenonvirtual java/lang/Object/<init>()V\n")
                                        .append("return\n")
                                        .append(".end method\n");

        jasminCode.append(methods);

        return new JasminResult(className, jasminCode.toString(), reports);
    }

    //Method
    private String getMethod(Method method) {
        StringBuilder assignmentCode = new StringBuilder();
        StringBuilder bodyCode = new StringBuilder();

        varTable = method.getVarTable();
        method.buildVarTable();

        stack_size = 0;
        max_size = 0;

        assignmentCode.append(".method ")
                .append(getAccessModifier(method.getMethodAccessModifier()))
                .append(method.isStaticMethod() ? " static " : " ")
                .append(method.isConstructMethod() ? "<init>" : method.getMethodName())
                .append("(")
                .append(getParameters(method.getParams()))
                .append(")")
                .append(getType(method.getReturnType()))
                .append("\n");

        for(Instruction instruction : method.getInstructions()){
            bodyCode.append(getInstruction(instruction, method, true));
        }

        if(method.isConstructMethod()){
            bodyCode.append("return").append("\n");
        }


        //Permitted for checkpoint 2
        assignmentCode.append(".limit stack ").append(max_size).append("\n")
                        .append(".limit locals ").append(getLocals(varTable)).append("\n");

        bodyCode.append(".end method").append("\n");


        return assignmentCode + bodyCode.toString();
    }

    private int getLocals(HashMap<String, Descriptor> varTable) {
        int max = 0;
        for (var x : varTable.entrySet()){
            max = Math.max(x.getValue().getVirtualReg(), max);
        }
        return max + 1;
    }

    private String getInstruction(Instruction instruction, Method method,boolean popReturn) {
        StringBuilder instCode = new StringBuilder();

        List<String> labels = method.getLabels(instruction);

        switch(instruction.getInstType()){
            case ASSIGN:
                AssignInstruction assignInstruction = (AssignInstruction) instruction;

                Instruction rhs = assignInstruction.getRhs();
                Element lhs = assignInstruction.getDest();

                Descriptor var = varTable.get(((Operand) lhs).getName());

                String rhsCode = getInstruction(rhs,method,false);

                switch(lhs.getType().getTypeOfElement()){
                    case BOOLEAN:
                    case INT32:
                        ElementType typeVar = var.getVarType().getTypeOfElement();
                        if(typeVar == ElementType.ARRAYREF){
                            ArrayOperand arrayOperand = (ArrayOperand) lhs;
                            Element indexElement = arrayOperand.getIndexOperands().get(0);

                            instCode.append(getStoreArray(var.getVirtualReg(), varTable.get(((Operand) indexElement).getName()).getVirtualReg(), rhsCode));
                        }else {
                            instCode.append(rhsCode)
                                    .append(istore(var.getVirtualReg()))
                                    .append("\n");
                        }
                        break;

                    case OBJECTREF:
                    case ARRAYREF:
                        instCode.append(rhsCode)
                                .append(astore(var.getVirtualReg()))
                                .append("\n");
                        break;

                }
                break;
            case CALL:
                CallInstruction callInstruction = (CallInstruction) instruction;
                Element caller = callInstruction.getFirstArg();
                Element calledMethod = callInstruction.getSecondArg();
                StringBuilder after = new StringBuilder();

                switch(callInstruction.getInvocationType()){
                    case invokevirtual:
                    case invokespecial:
                        instCode.append(pushToStack(caller));
                        break;

                    case NEW:
                        String callerName = ((Operand) caller).getName();
                        if(callerName.equals("array")){
                            updateStack(1);
                            after.append("newarray int")
                                    .append("\n");
                        }else {
                            updateStack(1);
                            after.append("new ")
                                    .append(callerName)
                                    .append("\n");
                        }
                        break;

                    case arraylength:
                        instCode.append(pushToStack(caller))
                                .append("arraylength\n");
                }

                ArrayList<Element> parameters = callInstruction.getListOfOperands();

                if(calledMethod != null){
                    after.append(callInstruction.getInvocationType()).append(" ")
                            .append(getInvocation(caller, calledMethod, callInstruction.getReturnType(),
                                    getParameters(parameters), callInstruction.getInvocationType()
                            ));
                }
                if(parameters != null){
                    for (var op : parameters){
                        instCode.append(pushToStack(op));
                    }
                    updateStack(-parameters.size());
                }
                if(popReturn && !callInstruction.getReturnType().getTypeOfElement().equals(ElementType.VOID)) {
                    after.append(pop());
                }

                instCode.append(after);
                break;
            case GOTO:
                GotoInstruction gotoInstruction = (GotoInstruction) instruction;
                instCode.append("goto ").append(gotoInstruction.getLabel()).append("\n");
                break;
            case BRANCH:
                CondBranchInstruction branchInstruction = (CondBranchInstruction) instruction;

                Instruction condition = branchInstruction.getCondition();
                InstructionType type = condition.getInstType();
                if(type == InstructionType.UNARYOPER){
                    UnaryOpInstruction unCondition = (UnaryOpInstruction) condition;
                    String operandCode = pushToStack(unCondition.getOperand());
                    updateStack(-1);
                    instCode.append(operandCode)
                            .append("ifeq ").append(branchInstruction.getLabel()).append("\n");
                }else if (type == InstructionType.BINARYOPER){
                    boolean cmpZero = false;
                    BinaryOpInstruction binaryCondition = (BinaryOpInstruction) condition;

                    String leftInst = pushToStack(binaryCondition.getLeftOperand());
                    String rightInst;
                    if(binaryCondition.getRightOperand().isLiteral() &&
                            ((LiteralElement) binaryCondition.getRightOperand()).getLiteral().equals("0")){
                        rightInst = "";
                        cmpZero = true;
                    } else{
                        rightInst = pushToStack(binaryCondition.getRightOperand());
                    }
                    String label = branchInstruction.getLabel();

                    switch(binaryCondition.getOperation().getOpType()){
                        case LTH:
                            updateStack(cmpZero ? -1 : -2);
                            instCode.append(leftInst)
                                    .append(rightInst)
                                    .append(cmpZero ? "iflt" : "if_icmplt ").append(label).append("\n");
                            break;
                        case ANDB:
                            instCode.append(pushToStack(binaryCondition.getLeftOperand()))
                                    .append("ifne ").append(label).append("\n");
                            updateStack(-1);
                            instCode.append(rightInst)
                                    .append("ifne ").append(label).append("\n");
                            updateStack(-1);
                            break;
                        case GTE:
                            updateStack(-2);
                            instCode.append(leftInst)
                                    .append(rightInst)
                                    .append("if_icmpge ").append(label).append("\n");
                            break;
                    }
                } else if (type == InstructionType.NOPER){
                    SingleOpInstruction singCondition = (SingleOpInstruction) condition;
                    String operandCode = pushToStack(singCondition.getSingleOperand());
                    updateStack(-1);
                    instCode.append(operandCode)
                            .append("ifne ").append(branchInstruction.getLabel()).append("\n");
                }

                break;
            case PUTFIELD:
                PutFieldInstruction putFieldInstruction = (PutFieldInstruction) instruction;
                Element secondOperand = putFieldInstruction.getSecondOperand();
                instCode.append(aload(0))
                        .append(pushToStack(putFieldInstruction.getThirdOperand()))
                        .append("putfield ");
                updateStack(-2);
                instCode.append(classUnit.getClassName()).append("/")
                        .append(((Operand) secondOperand).getName()).append(" ")
                        .append(getType(secondOperand.getType())).append("\n");
                break;
            case RETURN:
                ReturnInstruction returnInstruction = (ReturnInstruction) instruction;
                if(returnInstruction.hasReturnValue()){
                    updateStack(-1);
                    instCode.append(pushToStack(returnInstruction.getOperand()))
                            .append((returnInstruction.getOperand().getType().getTypeOfElement() == ElementType.INT32 ||
                                    returnInstruction.getOperand().getType().getTypeOfElement() == ElementType.BOOLEAN) ?
                                    "ireturn" :  "areturn")
                            .append("\n");
                } else {
                    instCode.append("return\n");
                }
                break;
            case GETFIELD:
                GetFieldInstruction getFieldInstruction = (GetFieldInstruction) instruction;
                Element secondOp = getFieldInstruction.getSecondOperand();
                instCode.append(aload(0))
                        .append("getfield ")
                        .append(classUnit.getClassName()).append("/")
                        .append(((Operand) secondOp).getName()).append(" ")
                        .append(getType(secondOp.getType())).append("\n");
                break;
            case UNARYOPER:
                UnaryOpInstruction unaryOpInstruction = (UnaryOpInstruction) instruction;
                Element operand = unaryOpInstruction.getOperand();
                    instCode.append(pushToStack(operand))
                            .append(getOperation(unaryOpInstruction.getOperation().getOpType()));
                break;
            case BINARYOPER:
                BinaryOpInstruction binaryOpInstruction = (BinaryOpInstruction) instruction;
                Element leftEl = binaryOpInstruction.getLeftOperand();
                Element rightEl = binaryOpInstruction.getRightOperand();
                OperationType opType = binaryOpInstruction.getOperation().getOpType();

                instCode.append(pushToStack(leftEl))
                        .append(pushToStack(rightEl))
                        .append(getOperation(opType));
                break;
            case NOPER:
                SingleOpInstruction singleOpInstruction = (SingleOpInstruction) instruction;
                instCode.append(pushToStack(singleOpInstruction.getSingleOperand()));
                break;
            default:
                return instruction.getInstType().toString();

        }

        StringBuilder labelStr = new StringBuilder();

        for(String label : labels){
            labelStr.append(label).append(":\n");
        }

        return labelStr + instCode.toString();
    }

    private String getOperation(OperationType opType){
        switch(opType){
            case ADD:
                updateStack(-1);
                return "iadd\n";
            case SUB:
                updateStack(-1);
                return "isub\n";
            case MUL:
                updateStack(-1);
                return "imul\n";
            case DIV:
                updateStack(-1);
                return "idiv\n";
            case AND:
            case ANDB:
                updateStack(-1);
                return "iand\n";

            case LTH:
                updateStack(-1);
                return "isub\n" + "bipush 31\n" + "iushr\n";

            case NOT:
            case NOTB:
                return "iconst_1\n" + "ixor\n";
        }
        return null;
    }

    private String pop() {
        updateStack(-1);
        return "pop\n";
    }

    private String getInvocation(Element caller, Element calledMethod, Type returnType, String parameters, CallType invocationType) {
        LiteralElement literalMethod = (LiteralElement) calledMethod;
        ClassType callerType = (ClassType) caller.getType();

        if(literalMethod.getLiteral().equals("\"<init>\"")){
            return (caller.getType().getTypeOfElement().equals(ElementType.THIS) ?
                    (classUnit.getSuperClass() != null ?
                            classUnit.getSuperClass() : "java/lang/Object") + "/" : fullClassName(callerType.getName()))
                    + "<init>()V\n";
        }
        String methodName = literalMethod.getLiteral().replace("\"", "");
        return fullClassName(invocationType.equals(CallType.invokestatic) ? ((Operand) caller).getName() : callerType.getName()) + methodName
                + "(" + parameters + ")" + getType(returnType) + "\n";
    }

    private String fullClassName(String className){
        if(className.equals(classUnit.getClassName()) || className.equals(classUnit.getSuperClass()))
            return className + "/";
        else
            return importedClass(className) + "/";
    }

    private String importedClass(String className){
        for (String fullName : classUnit.getImports()){
            if (fullName.substring(fullName.lastIndexOf(".") + 1).trim().equals(className))
                return fullName.replace(".", "/");
        }
        return null;
    }


    private String pushToStack(Element element){
        if(element.isLiteral()){
            LiteralElement literal = (LiteralElement) element;
            return getLiteral(literal);
        }

        Operand operand = (Operand) element;
        Descriptor var = varTable.get(operand.getName());

        switch(element.getType().getTypeOfElement()){
            case INT32:
                if(var.getVarType().getTypeOfElement() == ElementType.ARRAYREF){
                    ArrayOperand arrayOperand = (ArrayOperand) operand;
                    Element indexElement = arrayOperand.getIndexOperands().get(0);
                    String ret =  aload(var.getVirtualReg()) + pushToStack(indexElement) + "iaload\n";
                    updateStack(-1);
                    return ret;
                } else {
                    return iload(var.getVirtualReg());
                }
            case BOOLEAN: ;
                if (operand.getName().equals("false")) {
                    updateStack(1);
                    return "iconst_" + 0 + "\n";
                }
                else if (operand.getName().equals("true")){
                    updateStack(1);
                    return "iconst_" + 1 + "\n";
                }
                else
                    return iload(varTable.get(operand.getName()).getVirtualReg());

            case ARRAYREF:
            case OBJECTREF:
                return aload(var.getVirtualReg());

            case THIS:
                return aload(0);
        }
        return null;
    }

    private String getLiteral(LiteralElement literal) {
        updateStack(1);
        int integerLiteral = Integer.parseInt(literal.getLiteral());
        if(integerLiteral == -1){
            return "iconst_m1\n";
        }
        if(integerLiteral >= -1 && integerLiteral <= 5){
            return "iconst_" + integerLiteral + "\n";
        }
        if(integerLiteral >= -128 && integerLiteral <= 127){
            return "bipush " + integerLiteral + "\n";
        }
        if(integerLiteral >= -32768 && integerLiteral <= 32767){
            return "sipush " + integerLiteral + "\n";
        }
        return "ldc " + integerLiteral + "\n";
    }

    private String getStoreArray(int virtualReg, int virtualRegIndex, String rhsCode) {
        String ret = aload(virtualReg) + iload(virtualRegIndex) + rhsCode + "iastore\n";
        updateStack(-3);
        return ret;
    }

    private String aload(int reg){
        updateStack(1);
        return ((reg >= 0 && reg <=3) ? "aload_" : "aload ") + reg + "\n";
    }

    private String iload(int reg){
        updateStack(1);
        return ((reg >= 0 && reg <=3) ? "iload_" : "iload ") + reg + "\n";
    }

    private String istore(int reg){
        updateStack(-1);
        return ((reg >= 0 && reg <=3) ? "istore_" : "istore ") + reg + "\n";
    }

    private String astore(int reg){
        updateStack(-1);
        return ((reg >= 0 && reg <=3) ? "astore_" : "astore ") + reg + "\n";
    }

    private String getParameters(ArrayList<Element> params){
        StringBuilder paramCode = new StringBuilder();
        for(Element element : params){
            paramCode.append(getType(element.getType()));
        }
        return paramCode.toString();
    }

    //Class Def
    private String getClassDefinition(ClassUnit classUnit){
        StringBuilder jasminCode = new StringBuilder();

        AccessModifiers accessModifier = classUnit.getClassAccessModifier();

        jasminCode.append(".class ")
                .append(getAccessModifier(accessModifier))
                .append(" ")
                .append(classUnit.getClassName())
                .append("\n");

        String superClass = classUnit.getSuperClass();
        jasminCode.append(".super ")
                .append(superClass == null ? "java/lang/Object" : superClass)
                .append("\n");

        return jasminCode.toString();
    }

    //Fields
    private String getField(Field field){
        return ".field " + getAccessModifier(field.getFieldAccessModifier())
                + " " + field.getFieldName() + " "
                + getType(field.getFieldType()) + "\n";
    }


    //Utils
    private String getType(Type type) {
        switch(type.getTypeOfElement()){
            case INT32:
            case BOOLEAN:
            case STRING:
            case VOID:
                return getElementType(type.getTypeOfElement());

            case ARRAYREF:
                ArrayType arrayType = (ArrayType) type;
                return "[" + getElementType(arrayType.getTypeOfElements());

            case OBJECTREF:
            case CLASS:
                assert type instanceof ClassType;
                ClassType classType = (ClassType) type;
                type.show();
                return "L" + classType.getName() + ";";

            case THIS:
                return "";
        }
        return "";
    }

    private String getElementType(ElementType type){
        switch(type) {
            case INT32:
                return "I";

            case BOOLEAN:
                return "Z";

            case STRING:
                return "Ljava/lang/String;";

            case VOID:
                return "V";

            default:
                return "";
        }
    }

    private String getAccessModifier(AccessModifiers accessModifier) {
        return (accessModifier.equals(AccessModifiers.DEFAULT) ? AccessModifiers.PUBLIC : accessModifier).name().toLowerCase();
    }

    private void updateStack(int delta){
        stack_size += delta;
        max_size = Math.max(stack_size, max_size);
    }
}
