package pt.up.fe.comp.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;

public class OllirUtils {
    public static String getCode(Symbol symbol){
        return symbol.getName() + getCode(symbol.getType());
    }

    public static String getCode(Type type){
        StringBuilder code = new StringBuilder();

        if(type.isArray()){
            code.append(".array");
        }

        code.append(getOllirType(type.getName()));

        return code.toString();
    }

    public static String getOllirType(String jmmType){
        switch(jmmType){
            case "int":
                return ".i32";
            case "boolean":
                return ".bool";
            case "void":
                return ".V";
            default:
                return "." + jmmType;

        }
    }

    public static boolean isLocalVariable(String name, String currFunc, SymbolTable symbolTable){
        for(Symbol symb: symbolTable.getLocalVariables(currFunc)){
            if(symb.getName().equals(name)){
                return true;
            }
        }
        return false;
    }

    public static int getParameterIndex(String name, String currFunc, SymbolTable symbolTable){
        if(OllirUtils.isLocalVariable(name, currFunc, symbolTable)) return -1;

        List<Symbol> params = symbolTable.getParameters(currFunc);

        for(int i = 0; i < params.size(); i++){
            if(params.get(i).getName().equals(name)){
                return i;
            }
        }
        return -1;
    }

    public static boolean isField(JmmNode node, String currFunc, SymbolTable symbolTable){
        String name = (node.getKind().equals("IdentifierObject") ?
                node.get("image")
                : node.getJmmChild(0).get("image"));
        if(OllirUtils.isLocalVariable(name,currFunc, symbolTable)) return false;
        if(OllirUtils.getParameterIndex(name,currFunc, symbolTable) != -1) return false;


        for(Symbol symb: symbolTable.getFields()){
            if(symb.getName().equals(name)){
                return true;
            }
        }
        return false;
    }

    public static boolean existVariable(String name, String currFunc, SymbolTable symbolTable){

        for(Symbol symb : symbolTable.getParameters(currFunc)){
            if(symb.getName().equals(name)){
                return true;
            }
        }
        for(Symbol symb: symbolTable.getLocalVariables(currFunc)){
            if(symb.getName().equals(name)){
                return true;
            }
        }
        for(Symbol symb: symbolTable.getFields()){
            if(symb.getName().equals(name)){
                return true;
            }
        }
        return false;
    }

    public static String getOperationType(String op) {
        switch(op){
            case "ADD":
                return "+.i32";
            case "AND":
                return "&&.bool";
            case "LT":
                return "<.bool";
            case "SUB":
                return "-.i32";
            case "MUL":
                return "*.i32";
            case "DIV":
                return "/.i32";
            case "NEG":
                return "!.bool"; // i'm guessing;
            default:
                return "";
        }
    }

    public static String getOllirPutField(String currFunc){
        String ret = "putfield(this, ";

//        if (currFunc.equals("main")){
//            ret = "putstatic( ";
//        }

        return ret;
    }

    public static String getOllirGetField(String currFunc){
        String ret = " getfield(this, ";

//        if (currFunc.equals("main")){
//            ret = " getstatic(this, ";
//        }

        return ret;
    }
}
