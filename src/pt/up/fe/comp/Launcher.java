package pt.up.fe.comp;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.up.fe.comp.analysis.JmmAnalyser;
import pt.up.fe.comp.jasmin.Jasmin;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.ollir.JmmOptimizer;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;

public class Launcher {

    public static void main(String[] args) {
        SpecsSystem.programStandardInit();

        SpecsLogs.info("Executing with args: " + Arrays.toString(args));

        // read the input code
//        if (args.length != 1) {
//            throw new RuntimeException("Expected a single argument, a path to an existing input file.");
//        }

        // Create config
        Map<String, String> config = new HashMap<>();
        config.put("inputFile", args[0]);
        config.put("optimize", "false");
        config.put("debug", "false");
        config.put("registerAllocation", "-1");

        for(var arg: args){
            if(arg.contains("-r")){
                String num = List.of(arg.split("=")).get(1);
                config.put("registerAllocation", num);
            } else if(arg.contains("-o")){
                config.put("optimize", "true");
            } else if(arg.contains("-d")){
                config.put("debug", "true");
            } else if(arg.contains("-i")){
                String file = List.of(arg.split("=")).get(1);
                config.put("inputFile",file);
            }
        }

        File inputFile = new File(config.get("inputFile"));
        if (!inputFile.isFile()) {
            throw new RuntimeException("Expected a path to an existing input file, got '" + config.get("inputFile") + "'.");
        }
        String input = SpecsIo.read(inputFile);

        // Instantiate JmmParser
        SimpleParser parser = new SimpleParser();

        // Parse stage
        JmmParserResult parserResult = parser.parse(input, config);

        // Check if there are parsing errors
        TestUtils.noErrors(parserResult.getReports());

        // Instantiate JmmAnalysis
        JmmAnalyser analyser = new JmmAnalyser();

        // Analysis stage
        JmmSemanticsResult analysisResult = analyser.semanticAnalysis(parserResult);

        // Check if there are no errors
        TestUtils.noErrors(analysisResult.getReports());

        // Instantiate JmmOptimizer
        var optimizer = new JmmOptimizer();

        // Optimization stage for AST
        JmmSemanticsResult semanticsResult = optimizer.optimize(analysisResult);

        // Check if there are no errors
        TestUtils.noErrors(semanticsResult.getReports());

        OllirResult ollirResult = optimizer.toOllir(semanticsResult);

        // Check if there are no errors
        TestUtils.noErrors(ollirResult.getReports());

        // Optimization stage for Ollir
        OllirResult optimizationResult = optimizer.optimize(ollirResult);

        // Check if there are no errors
        TestUtils.noErrors(optimizationResult.getReports());

        var jasmin = new Jasmin();

        // Optimization stage for Ollir
        JasminResult jasminResult = jasmin.toJasmin(optimizationResult);

        System.out.println(jasminResult.getJasminCode());

        // Check if there are no errors
        TestUtils.noErrors(jasminResult.getReports());

        File file = jasminResult.compile();

        SpecsIo.copy(file,new File("result.class"));

        // ... add remaining stages

    }

}
