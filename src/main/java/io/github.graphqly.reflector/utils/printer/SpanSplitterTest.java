package io.github.graphqly.reflector.utils.printer;

import java.util.List;

public class SpanSplitterTest {
  static void test() {
    String input = "  'Can you see it' function(){ return \" Is big \\\"problem\\\", \\no? \"; }";
    List<SpanSplitter.Span> spans = SpanSplitter.split(input);
    assert spans.size() == 2;
    for (SpanSplitter.Span span : spans) {
      System.out.printf("%s -> [%s]\n", span, input.substring(span.begin, span.end));
    }
  }

  public static void main(String[] args) {
    test();
  }
}
