package pt.up.fe.comp.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.Objects;

public class AnalyserUtils {
    public static boolean validType(String name, SymbolTable symbolTable){
        return name.equals("int") || name.equals("boolean") ||
                name.equals("String") || name.equals("void") ||
                existClass(name,symbolTable) || symbolTable.getClassName().equals(name);
    }

    public static Type getIdentifierType(String name,String currFunc,SymbolTable symbolTable){
        var params = symbolTable.getParameters(currFunc);
        if(params != null) {
            for (Symbol symb : params) {
                if (symb.getName().equals(name)) {
                    return symb.getType();
                }
            }
        }

        var local = symbolTable.getLocalVariables(currFunc);
        if(local != null) {
            for (Symbol symb : symbolTable.getLocalVariables(currFunc)) {
                if (symb.getName().equals(name)) {
                    return symb.getType();
                }
            }
        }

        if(!Objects.equals(currFunc, "main")) {
            for (Symbol symb : symbolTable.getFields()) {
                if (symb.getName().equals(name)) {
                    return symb.getType();
                }
            }
        }
        return null;
    }

    public static Type getType(JmmNode node,String currFunc, SymbolTable symbolTable){
        String kind = node.getKind();
        switch (kind) {
            case "Literal":
            case "Binop":
            case "UnaryOp":
            case "PropAccess":
            case "NewStatement":
                if(node.get("type").equals("intArray")){
                    return new Type("int", true);
                }
                else {
                    return new Type(node.get("type"), false);
                }
            case "IdentifierObject":
                return AnalyserUtils.getIdentifierType(node.get("image"),currFunc, symbolTable);
            case "Call":
                return symbolTable.getReturnType(node.get("image"));
            case "Access":
                if(AnalyserUtils.getType(node.getJmmChild(0), currFunc,symbolTable) == null) {
                    return null;
                }
                return new Type(AnalyserUtils.getType(node.getJmmChild(0), currFunc,symbolTable).getName(),false);
            case "ClassObject":
                return new Type(symbolTable.getClassName(),false);
        }
        return null;
    }

    public static Boolean verifyType(JmmNode node, Type type, String currFunc, SymbolTable symb){
        String kind = node.getKind();

        switch (kind) {
            case "Literal":
            case "Binop":
            case "UnaryOp":
            case "PropAccess":
            case "NewStatement":
                if(node.get("type").equals("intArray")){
                    return node.get("type").equals(type.getName()) && type.isArray();
                }
                else {
                    return node.get("type").equals(type.getName());
                }
            case "IdentifierObject":
                Type type1 = AnalyserUtils.getIdentifierType(node.get("image"), currFunc, symb);
                if( type1 != null && AnalyserUtils.existImport(type1.getName(),symb) && AnalyserUtils.existImport(type.getName(),symb)) return true;
                else if ( type1 != null && Objects.equals(type.getName(),symb.getClassName()) && symb.getSuper() != null
                    && AnalyserUtils.existImport(type1.getName(),symb)) return true;
                return Objects.equals(type1, type);
            case "Call":
                Type funcType = symb.getReturnType(node.get("image"));
                if (funcType == null) {
                    return true;
                } else {
                    return funcType.equals(type);
                }
            case "Access":
                return Objects.equals(AnalyserUtils.getType(node, currFunc, symb), type);
            case "ClassObject":
                return symb.getClassName().equals(type.getName());
        }

        return false;
    }

    public static boolean existVariable(String name,String currFunc,SymbolTable symbolTable){
        var params = symbolTable.getParameters(currFunc);
        if(params != null) {
            for (Symbol symb : params) {
                if (symb != null && symb.getName().equals(name)) {
                    return true;
                }
            }
        }

        var local = symbolTable.getLocalVariables(currFunc);
        if(local != null) {
            for (Symbol symb : local) {
                if (symb != null && symb.getName().equals(name)) {
                    return true;
                }
            }
        }

        for(Symbol symb: symbolTable.getFields()){
            if(symb != null && symb.getName().equals(name)){
                return true;
            }
        }
        return existClass(name, symbolTable);
    }

    public static boolean existClass(String name, SymbolTable symbolTable){
        for(String str : symbolTable.getImports()){
            String[] list = str.split("\\.",0);
            if(list[list.length - 1].equals(name)){
                return true;
            }
        }

        return symbolTable.getClassName().equals(name) || name.equals("intArray");
    }

    public static boolean existImport(String name, SymbolTable symbolTable){
        for(String str : symbolTable.getImports()){
            String[] list = str.split("\\.",0);
            if(list[list.length - 1].equals(name)){
                return true;
            }
        }
        return false;
    }

    public static boolean isKnowObject(JmmNode node, String currFunc, SymbolTable symbolTable) {
        if(node.getKind().equals("ClassObject")){
            return true;
        }

        Type type = AnalyserUtils.getType(node, currFunc, symbolTable);
        if(type != null && Objects.equals(type.getName(),
                symbolTable.getClassName()) && symbolTable.getSuper() == null){
            return true;
        }

//        if(node.getKind().equals("IdentifierObject")){
//            return symbolTable.getSuper() == null &&
//                    Objects.equals(AnalyserUtils.getIdentifierType(node.get("image"), currFunc, symbolTable).getName(), symbolTable.getClassName());
//        }

        Type retType = symbolTable.getReturnType(String.valueOf(node.getOptional("image")));
        // return type is not null, is equal to class current class, curr class don't extend nothing
        return retType != null && retType.getName().equals(symbolTable.getClassName()) && symbolTable.getSuper() == null;
    }
}
