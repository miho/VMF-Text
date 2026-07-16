package demo.barelist;

import demo.barelist.parser.BareListModelParser;
import demo.barelist.unparser.BareListModelUnparser;
import java.nio.file.*;

/** Bare delimited list: item (',' item)* — structural edits keep sibling whitespace. */
public class Main {
  public static void main(String[] args) throws Exception {
    Path input = Path.of(args.length > 0 ? args[0] : "sample/items.txt");
    String source = Files.readString(input);
    BareListModelParser parser = new BareListModelParser();
    BareListModelUnparser unparser = new BareListModelUnparser();
    BareListModel model = parser.parse(source);
    require(source.equals(unparser.unparse(model)), "round-trip");
    System.out.println("[1] bare list round-tripped byte-identically");
    System.out.println("  source: " + visible(source));

    model.getRoot().getItems().set(1, 99);
    require("1 ,  99,\n 3".equals(unparser.unparse(model)), "set");
    System.out.println("[2] items.set(1, 99) keeps sibling whitespace");

    model.getRoot().getItems().remove(1);
    require("1,\n 3".equals(unparser.unparse(model)), "remove");
    System.out.println("[3] items.remove(1) splices trivia (no brackets)");

    model.getRoot().getItems().add(0, 0);
    String after = unparser.unparse(model);
    require(after.startsWith("0,"), "insert");
    System.out.println("[4] items.add(0, 0) → " + visible(after));
    System.out.println("[5] bare T (',' T)* list splice (0.2.1+)");
  }
  static String visible(String s) { return s.replace("\n", "\\n"); }
  static void require(boolean c, String m) { if (!c) { System.err.println("FAILED: "+m); System.exit(1);} }
}
