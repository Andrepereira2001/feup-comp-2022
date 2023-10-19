package pt.up.fe.comp.analysis;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JmmAnalyser implements JmmAnalysis {
    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {
        List<Report> reports = new ArrayList<>();
        //The interface contains a single method, semanticAnalysis,
        // that receives the JmmParserResult from the previous stage, JmmParser, and returns a JmmSemanticsResult.
        // The JmmSemanticsResult receives the JmmParserResult,
        // an instance of SymbolTable that you will have to create,
        System.out.println("\n\nAnnotated AST:"); // TODO REMOVE
        System.out.println(parserResult.getRootNode().toTree());

        var visitor = new VisitorSymbolTable();
        visitor.visit(parserResult.getRootNode(), 0);

        // generated symbol table
        SymbolTable symbolTable = visitor.getTable();
        reports.addAll(visitor.getReports());

        System.out.println("\n\nSymbol table:"); // TODO REMOVE
        System.out.println(symbolTable.print());

        var variableVerification = new VisitorVariableVerifier();
        variableVerification.visit(parserResult.getRootNode(), symbolTable);

        var typeVerification = new VisitorTypeVerifier();
        typeVerification.visit(parserResult.getRootNode(), symbolTable);


        reports.addAll(variableVerification.getReports());
        reports.addAll(typeVerification.getReports());

        // and a list of new reports originated in this stage.
        return new JmmSemanticsResult(parserResult, symbolTable, reports);
    }
}


