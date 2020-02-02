package io.github.graphqly.reflector.utils.printer;

import java.io.StringWriter;

public class CodePrinterTest {
  public static void testCodePrinter() {
    StringWriter out = new StringWriter();
    CodePrinter printer = new CodePrinter(out);
    printer.println("{");
    printer.printlnForward("a: b,");
    printer.println("c: {");
    printer.printlnForward("age: 7");
    printer.printlnBackward("}");
    printer.printlnBackward("}");
    printer.flush();

    String expectedOutput = "{\n" + "\ta: b,\n" + "\tc: {\n" + "\t\tage: 7\n" + "\t}\n" + "}";
    assert expectedOutput.equals(out.toString());
  }

  public static void testCodePrinter2() {
    String input =
        "input DeploymentRequest {\n"
            + "\textra: Json\n"
            + "host: String = \"0.0.0.0\"\n"
            + "port: Int = 9630\n"
            + "}";
    String expected =
        "input DeploymentRequest {\n"
            + "\textra: Json\n"
            + "\thost: String = \"0.0.0.0\"\n"
            + "\tport: Int = 9630\n"
            + "}";

    assert expected.equals(CodePrinter.prettyPrint(input));
  }

  public static void main(String[] args) {
    testCodePrinter2();
  }
}
