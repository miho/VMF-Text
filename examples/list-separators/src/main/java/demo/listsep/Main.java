package demo.listsep;

import demo.listsep.parser.ListSepModelParser;
import demo.listsep.unparser.ListSepModelUnparser;
import java.nio.file.*;

/**
 * Demonstrates ListShapeHint.separatorCount for multi-token separators
 * ({@code ',' 'and'}) and separator-less {@code ID (ID)*} lists.
 */
public class Main {
  public static void main(String[] args) throws Exception {
    String source = Files.readString(Path.of("sample/data.txt"));
    ListSepModelParser parser = new ListSepModelParser();
    ListSepModelUnparser unparser = new ListSepModelUnparser();
    ListSepModel model = parser.parse(source);
    require(source.equals(unparser.unparse(model)), "round-trip");

    var multiHints = model.getRoot().getMulti().getLexicalInfo().getListShapeHints();
    var noSepHints = model.getRoot().getNosep().getLexicalInfo().getListShapeHints();
    require(multiHints.get(0).getSeparatorCount() == 2, "multi sepCount");
    require(noSepHints.get(0).getSeparatorCount() == 0, "nosep sepCount");
    System.out.println("[1] separatorCount: multi=2, nosep=0");

    model.getRoot().getMulti().getItems().set(1, "z");
    require("a , and  z,\nand c; x  y\n z\n".equals(unparser.unparse(model)), "multi set");
    System.out.println("[2] multi set keeps ',' 'and' trivia");

    model.getRoot().getNosep().getItems().remove(1);
    require("a , and  z,\nand c; x z\n".equals(unparser.unparse(model)), "nosep remove");
    System.out.println("[3] nosep remove keeps remaining items");
    System.out.println("[4] ListShapeHint.separatorCount (0.2.1+)");
  }

  static void require(boolean c, String m) {
    if (!c) {
      System.err.println("FAILED: " + m);
      System.exit(1);
    }
  }
}
