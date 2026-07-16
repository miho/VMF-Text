package demo.lexemes;

import demo.lexemes.parser.LexemeLangModelParser;
import demo.lexemes.unparser.LexemeLangModelUnparser;
import java.nio.file.*;

/** Original type-mapped spellings: 1 stays 1, not 1.0, until you change the value. */
public class Main {
  public static void main(String[] args) throws Exception {
    Path input = Path.of(args.length > 0 ? args[0] : "sample/numbers.txt");
    String source = Files.readString(input);
    LexemeLangModelParser parser = new LexemeLangModelParser();
    LexemeLangModelUnparser unparser = new LexemeLangModelUnparser();
    LexemeLangModel model = parser.parse(source);
    require(source.equals(unparser.unparse(model)), "round-trip must keep lexemes");
    System.out.println("[1] mixed spellings round-tripped: " + source);

    model.getRoot().getValues().set(1, 9.0);
    require("(1, 9.0, .5, 3.)".equals(unparser.unparse(model)), "siblings keep lexemes");
    System.out.println("[2] set(1, 9.0) → " + unparser.unparse(model));

    model.getRoot().getValues().set(0, 1.5);
    require("(1.5, 9.0, .5, 3.)".equals(unparser.unparse(model)), "edited uses converter");
    System.out.println("[3] set(0, 1.5) uses converter text for the edited slot");
    System.out.println("[4] OriginalLexeme on LexicalInfo (0.2.1+)");
  }
  static void require(boolean c, String m) { if (!c) { System.err.println("FAILED: "+m); System.exit(1);} }
}
