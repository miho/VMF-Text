package demo.optionals;

import demo.optionals.parser.OptDemoModelParser;
import demo.optionals.unparser.OptDemoModelUnparser;
import java.nio.file.*;

/** Optional null↔value: setName on an absent group emits (name); clear drops it. */
public class Main {
  public static void main(String[] args) throws Exception {
    String source = Files.readString(Path.of("sample/items.txt"));
    OptDemoModelParser parser = new OptDemoModelParser();
    OptDemoModelUnparser unparser = new OptDemoModelUnparser();
    OptDemoModel model = parser.parse(source);
    require(source.equals(unparser.unparse(model)), "round-trip");
    System.out.println("[1] " + source);

    Item first = model.getRoot().getItems().get(0);
    first.setName("x");
    require("r (x), r (keep)".equals(unparser.unparse(model)), "null→value");
    System.out.println("[2] setName(\"x\") on absent optional → " + unparser.unparse(model));

    Item second = model.getRoot().getItems().get(1);
    second.setName(null);
    require("r (x), r".equals(unparser.unparse(model)), "value→null");
    System.out.println("[3] setName(null) drops the group, keeps leading space before r");
    System.out.println("[4] optional null↔value presence update (0.2.1+)");
  }
  static void require(boolean c, String m) { if (!c) { System.err.println("FAILED: "+m+" got?"); System.exit(1);} }
}
