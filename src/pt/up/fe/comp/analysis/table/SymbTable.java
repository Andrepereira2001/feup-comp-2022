package pt.up.fe.comp.analysis.table;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbTable implements SymbolTable {
    // lista de imports
    private final List<String> imports = new ArrayList<String>();
    // class name String
    private String className = null;
    // class que extende
    private String classSuper = null;
    // lista das variáveis locais da class
    private final List<Symbol> fields = new ArrayList<Symbol>();
    // hash map do return type dos metodos da class
    private final Map<String, Type> returnTypes = new HashMap<String,Type>();
    // hash map dos parametros de cada metodo da class
    private final Map<String, List<Symbol>> parameters = new HashMap<String,List<Symbol>>();
    // hash map das variaveis locais de cada metodo da class
    private final Map<String, List<Symbol>> localVariables = new HashMap<String,List<Symbol>>();

    @Override
    public List<String> getImports() {
        return this.imports;
    }

    @Override
    public String getClassName() {
        return this.className;
    }

    @Override
    public String getSuper() {
        return this.classSuper;
    }

    @Override
    public List<Symbol> getFields() {
        return this.fields;
    }

    public List<String> getFieldsToString() {
        List<String> fieldNames = new ArrayList<>();
        for(Symbol field: this.fields){
            fieldNames.add(field.getName());
        }
        return fieldNames;
    }

    @Override
    public List<String> getMethods() {
        return new ArrayList<>(this.returnTypes.keySet());
    }

    @Override
    public Type getReturnType(String methodSignature) {
        return this.returnTypes.get(methodSignature);
    }

    @Override
    public List<Symbol> getParameters(String methodSignature) {
        return this.parameters.get(methodSignature);
    }

    @Override
    public List<Symbol> getLocalVariables(String methodSignature) {
        return this.localVariables.get(methodSignature);
    }

    public void addLocalVariable(String name, Symbol symb){
        localVariables.get(name).add(symb);
    }

    public void addParameter(String name, Symbol symb){
        parameters.get(name).add(symb);
    }

    public void addReturnType(String name,Type type){
        returnTypes.put(name,type);
        parameters.put(name, new ArrayList<Symbol>());
        localVariables.put(name, new ArrayList<Symbol>());
    }

    public void addImport(String impt){
        this.imports.add(impt);
    }

    public void setClass(String className){
        this.className = className;
    }

    public void setClassSuper(String superClass){
        this.classSuper = superClass;
    }

    public void addField(Symbol field){
        this.fields.add(field);
    }

    public void printAll(){
        System.out.println("Imports  " + imports);
        System.out.println("Class  " + className);
        System.out.println("Class Super  " + classSuper);
        System.out.println("Fields  " + fields);
        System.out.println("Return types  " + returnTypes);
        System.out.println("Parameters  " + parameters);
        System.out.println("Local Variables  " + localVariables);
    }
}
