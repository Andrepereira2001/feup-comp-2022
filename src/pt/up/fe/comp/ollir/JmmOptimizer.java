package pt.up.fe.comp.ollir;

import jdk.swing.interop.SwingInterOpUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.optimizations.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

public class JmmOptimizer implements JmmOptimization {
    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        System.out.println("Optimize!");

        if(Objects.equals(semanticsResult.getConfig().get("optimize"), "true")) {
            boolean changed = false;
             do {
                // Constant Propagation
                ConstantPropagationOptimizer constantPropagationOptimizer = new ConstantPropagationOptimizer();
                constantPropagationOptimizer.visit(semanticsResult.getRootNode());

                // Dead Code Elimination
                DeadCodeOptimizer deadCodeOptimizer = new DeadCodeOptimizer();
                deadCodeOptimizer.visit(semanticsResult.getRootNode());

                 // Copy Propagation
                CopyPropagationOptimizer copyPropagationOptimizer = new CopyPropagationOptimizer();
                copyPropagationOptimizer.visit(semanticsResult.getRootNode());

                 // Constant Folding
                ConstantFoldingOptimizer constantFoldingOptimizer = new ConstantFoldingOptimizer();
                constantFoldingOptimizer.visit(semanticsResult.getRootNode());

                changed = constantPropagationOptimizer.isChanged() ||
                        deadCodeOptimizer.isChanged() ||
                        copyPropagationOptimizer.isChanged() ||
                        constantFoldingOptimizer.isChanged();

             } while(changed);

//            System.out.println("Optimized ast:");
//            System.out.println(semanticsResult.getRootNode().toTree());
            //verify if the semanstics result is being changed
        }

        semanticsResult.getRootNode().toTree();
        return semanticsResult;
    }

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {
        var ollirGenerator = new OllirGenerator(semanticsResult.getSymbolTable(), semanticsResult.getConfig());
        ollirGenerator.visit(semanticsResult.getRootNode());
        String ollirCode = ollirGenerator.getCode();

        System.out.println("\n\nOllir code: ");
        System.out.println(ollirCode);

        return new OllirResult(semanticsResult,ollirCode, Collections.emptyList());
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        if(ollirResult.getConfig().containsKey("registerAllocation")) {
            if(!Objects.equals(ollirResult.getConfig().get("registerAllocation"), "-1")){
               RegisterAllocation registerAllocation = new RegisterAllocation(ollirResult.getOllirClass(),
                       Integer.parseInt(ollirResult.getConfig().get("registerAllocation")));

               if(Objects.equals(ollirResult.getConfig().get("registerAllocation"), "0")){
                   registerAllocation.allocateMinRegisters();
               }else{
                   registerAllocation.allocateRegisters();
               }
            }
        }
        return ollirResult;
    }
}
