import org.junit.Test;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

public class ExampleTest {

    @Test
    public void testStatement() {
        //var parserResult = TestUtils.parse("b=2+3+4+5*4*5*1/3/1&&123<3 && a[0];", "Statement");
        var parserResult = TestUtils.parse("{};", "Statement");
        TestUtils.noErrors(parserResult.getReports());
        //System.out.println(parserResult);
        //parserResult.getReports().get(0).getException().get().printStackTrace(); //passes when tests fails
        //System.out.println();
        //var analysisResult = TestUtils.analyse(parserResult);
    }

    @Test
    public void testExpression() {
        //var parserResult = TestUtils.parse("foo && 1 && 1 + (3 + 4);", "AndExpression");
        // var parserResult = TestUtils.parse("foo.bar().fu(a,b,1)", "AndExpression");
        //var parserResult = TestUtils.parse("new int[38]", "AndExpression");
        //var parserResult = TestUtils.parse("new som()", "AndExpression");
        var parserResult = TestUtils.parse("a[23 + 2]", "AndExpression");
        TestUtils.noErrors(parserResult.getReports());
        //System.out.println(parserResult);
        //parserResult.getReports().get(0).getException().get().printStackTrace(); //passes when tests fails
        //System.out.println();
        //var analysisResult = TestUtils.analyse(parserResult);
    }

    @Test
    public void testType() {
        var parserResult = TestUtils.parse("a[2] = 3;");
        TestUtils.noErrors(parserResult.getReports());
        //System.out.println(parserResult);
        //parserResult.getReports().get(0).getException().get().printStackTrace(); //passes when tests fails
        //System.out.println();
        //var analysisResult = TestUtils.analyse(parserResult);
    }

    @Test
    public void testImport() {
        var parserResult = TestUtils.parse("import Node.comp.feup;\nimport Edge.comp.feup;\nimport Math.utils;");
        TestUtils.noErrors(parserResult.getReports());
        //System.out.println(parserResult);
        //parserResult.getReports().get(0).getException().get().printStackTrace(); //passes when tests fails
        //System.out.println();
        //var analysisResult = TestUtils.analyse(parserResult);
    }


    @Test
    public void testImportDeclaration() {
        var result = TestUtils.parse("import bar;", "ImportDec");
        TestUtils.noErrors(result.getReports());
        //TestUtils.parse("2+3\n10+20\n");
        // var parserResult = TestUtils.parse("2+3\n10+20\n");
        // parserResult.getReports().get(0).getException().get().printStackTrace();
        // // System.out.println();
        // var analysisResult = TestUtils.analyse(parserResult);
    }

    @Test
    public void testImportDeclaration2() {
        var result = TestUtils.analyse("import bar; class A{}");
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testImport2() {
        var parserResult = TestUtils.analyse("import Node.comp.feup;\nimport Edge.comp.feup;\nimport Math.utils; class A{}");
        TestUtils.noErrors(parserResult.getReports());
    }

    @Test
    public void testClassSuper() {
        var parserResult = TestUtils.analyse("import Node.comp.feup;\nimport Edge.comp.feup;\nimport Math.utils; class A extends B{}");
        TestUtils.noErrors(parserResult.getReports());
    }

    @Test
    public void testVarDeclaration() { // TODO Check error integerArray identifier
        var parserResult = TestUtils.analyse("import Node.comp.feup;\nimport Edge.comp.feup;\nimport Math.utils; class Foo extends Bar{int a; int[] b; integerArray c; String s; boolean d;}");
        TestUtils.noErrors(parserResult.getReports());
    }

    @Test
    public void testMethodDeclaration() {
        var parserResult = TestUtils.analyse("import Node.comp.Bar;\nimport Edge.comp.feup;\nimport Math.utils; class Foo extends Bar{int a; int[] b; String s; boolean d; public static void main(String[] args) {} public int foo(int anInt, int[] anArray, boolean aBool, String aString) {int mati; int bibs; 2*3*a; bibs=anArray.length; aBool + aString < mati; joao = new O(); return a;}}");
        TestUtils.noErrors(parserResult.getReports());
    }

    @Test
    public void testVariableUsage(){
        var parserResult = TestUtils.analyse("import Node.comp.Bar;\nimport Edge.comp.O; class Foo {int a; int[] b; String s; boolean d; public static void main(String[] args) {} public int foo(int anInt, int[] anArray, boolean aBool, String aString) {int mati; int bibs; 2*3*a; bibs=anArray.length; aBool + aString < mati; bibs = new O(); bibs.go(a,3); this.foo().level2(false, 10).level3(true) ; return a;}}");
        TestUtils.noErrors(parserResult.getReports());
    }

    @Test
    public void testVariableUsage2(){
        var parserResult = TestUtils.analyse("import Node.comp.Bar;\nimport Edge.comp.O; class Foo extends Bar{int a; int[] b; String s; boolean d; public static void main(String[] args) {} public int foo(int anInt, int[] anArray, boolean aBool, String aString) {int mati; int bibs; 2*3*a; bibs=anArray.length; aBool + aString < mati; bibs = new O(); bibs.go(a,3); this.level1().level2(false, 10).level3(true) ; return a;}}");
        TestUtils.noErrors(parserResult.getReports());
    }

    @Test
    public void testTypeUsage(){
        var parserResult = TestUtils.analyse("import Node.comp.Bar;\nimport Edge.comp.O; class Foo extends Bar{int a; int[] b; String s; boolean d; public static void main(String[] args) {} public int foo(int anInt, int[] anArray, boolean aBool, String aString) {int mati; int bibs; 2*3*a; bibs=anArray.length; aBool + aString < mati; bibs = new O(); bibs.go(a,3); this.foo(this.boo(),b,d,s).level2(false, 10).level3(true) ; this.foo(this.test() + 1,b,!d,s); return a;} public int test() {return 2;} }");
        TestUtils.noErrors(parserResult.getReports());
    }

    @Test
    public void testTypeUsage2(){
        var parserResult = TestUtils.analyse("import Node.comp.Bar;\nimport Edge.comp.O; class Foo extends Bar{int a; int[] b; String s; boolean d; public static void main(String[] args) {} public int foo(int anInt, int[] anArray, boolean aBool, String aString) {int mati; int bibs; 2*3*a; bibs=anArray.length; !(mati + a < mati); bibs = new O(); bibs.go(a,3);  return a;} }");
        TestUtils.noErrors(parserResult.getReports());
    }

    @Test
    public void testAssign(){
        var parserResult = TestUtils.analyse("import Node.comp.Bar;\nimport Edge.comp.O; class Foo extends Bar{int a; int[] b; String s; boolean d; public static void main(String[] args) {} public int foo(int anInt, int[] anArray, boolean aBool, String aString) {int mati; O bibs; 2*3*a; mati=anArray.length; !(mati + a < mati); bibs = new O(); bibs.go(a,3);  return a;} }");
        TestUtils.noErrors(parserResult.getReports());
    }

    @Test
    public void testArray(){
        var parserResult = TestUtils.analyse("import Node.comp.Bar;\nimport Edge.comp.O; class Foo extends Bar{int a; int[] b; String s; boolean d; public static void main(String[] args) {} public int foo(int anInt, int[] anArray, boolean aBool, String aString) {int mati; O bibs; mati = anArray[1*23*4*1*3] * a; mati=anArray.length; O.boas(); !(mati + a < mati); bibs = new O(); bibs.go(a,3);  return a;} }");
        TestUtils.noErrors(parserResult.getReports());
    }

    @Test
    public void testFuncCall(){
        var parserResult = TestUtils.analyse(SpecsIo.getResource("fixtures/public/HelloWorld.jmm"));
        TestUtils.noErrors(parserResult.getReports());
    }

}
