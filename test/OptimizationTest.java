import org.junit.Test;
import pt.up.fe.comp.TestUtils;

public class OptimizationTest {
    @Test
    public void test(){
        var ollirResult = TestUtils.optimize("import Node.comp.Bar;\nimport Edge.comp.O; class Foo extends Bar{int a; int[] b; String s; boolean d; public static void main(String[] args) {} public int foo(int anInt, int[] anArray, boolean aBool, String aString) {int mati; O bibs; mati = anArray[1] * a; mati=anArray.length; !(mati + a < mati); bibs = new O(); bibs.go(a,3);  return a;} }");
        TestUtils.noErrors(ollirResult.getReports());
    }
}
