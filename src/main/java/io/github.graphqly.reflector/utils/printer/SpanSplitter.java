package io.github.graphqly.reflector.utils.printer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Class to split input strings into a list of spans: string & non_string */
public class SpanSplitter {
  public enum SpanKind {
    STRING,
    NON_STRING
  }

  private static Pattern SINGLE_QUOTED_STRING_PATTERN = Pattern.compile("'(\\\\.|[^'])*'");
  private static Pattern DOUBLE_QUOTED_STRING_PATTERN = Pattern.compile("\"(\\\\.|[^\"])*\"");

  private static void matchString(String input, Pattern pattern, List<Span> spans) {
    Matcher matcher = pattern.matcher(input);
    while (matcher.find()) {
      int begin = matcher.start();
      int end = matcher.end();
      spans.add(Span.ofString(begin, end));
    }
  }

  public static List<Span> split(String input) {
    List<Span> stringSpans = getStringSpans(input);

    List<Span> spans = new ArrayList<>();
    int begin = 0, end = 0, stringSpanIndex = 0;
    boolean lastStringSpan = false;
    int stringSpanCount = stringSpans.size();
    Span stringSpan;

    while (true) {
      stringSpan = null;
      if (stringSpanCount != 0 && !lastStringSpan) {
        end = stringSpans.get(stringSpanIndex).begin;
        stringSpan = stringSpans.get(stringSpanIndex);
      } else {
        end = input.length();
      }

      if (end > begin) spans.add(Span.ofNonString(begin, end));
      if (stringSpan != null) spans.add(stringSpan);

      if (lastStringSpan || stringSpanCount == 0) {
        break;
      }

      begin = stringSpans.get(stringSpanIndex).end;
      if (++stringSpanIndex >= stringSpanCount) {
        lastStringSpan = true; // gonna parse the last span
      }
    }

    return spans;
  }

  private static List<Span> getStringSpans(String input) {
    List<Span> stringSpans = new ArrayList<>();
    matchString(input, DOUBLE_QUOTED_STRING_PATTERN, stringSpans);

    int begin = 0;
    int maxSpanIndex = stringSpans.size();
    for (int index = 0; index < maxSpanIndex; index++) {
      Span span = stringSpans.get(index);
      matchString(input.substring(begin, span.begin), SINGLE_QUOTED_STRING_PATTERN, stringSpans);
      begin = span.end;
    }

    // may be there's only single quoted strings
    if (stringSpans.size() == 0) {
      matchString(input, SINGLE_QUOTED_STRING_PATTERN, stringSpans);
    }

    Collections.sort(stringSpans, Comparator.comparingInt(span -> span.begin));
    return stringSpans;
  }

  static class Span {
    public int begin = 0;
    public int end = 0;
    public SpanKind kind = SpanKind.STRING;

    public static Span ofNonString(int begin, int end) {
      return createSpan(begin, end, SpanKind.NON_STRING);
    }

    public static Span ofString(int begin, int end) {
      return createSpan(begin, end, SpanKind.STRING);
    }

    private static Span createSpan(int begin, int end, SpanKind kind) {
      Span span = new Span();
      span.begin = begin;
      span.end = end;
      span.kind = kind;
      return span;
    }

    @Override
    public String toString() {
      return "Span{" + "begin=" + begin + ", end=" + end + ", kind=" + kind + '}';
    }
  }
}
