package demo.multilist;

import demo.multilist.parser.MultiListModelParser;
import demo.multilist.unparser.MultiListModelUnparser;
import java.nio.file.*;

/** Two lists on one rule: ListShapeHint lets each splice without clearing the other. */
public class Main {
  public static void main(String[] args) throws Exception {
    String source = Files.readString(Path.of("sample/data.txt"));
    MultiListModelParser parser = new MultiListModelParser();
    MultiListModelUnparser unparser = new MultiListModelUnparser();
    MultiListModel model = parser.parse(source);
    require(source.equals(unparser.unparse(model)), "round-trip");
    var hints = model.getRoot().getLexicalInfo().getListShapeHints();
    require(hints.size() == 2, "expected 2 list-shape hints");
    System.out.println("[1] hints: " + hints.get(0).getPropertyName()
        + " + " + hints.get(1).getPropertyName());

    model.getRoot().getIds().remove(0);
    require("  b; 1,\n 2".equals(unparser.unparse(model)), "ids remove");
    System.out.println("[2] ids.remove(0) keeps nums trivia → " + visible(unparser.unparse(model)));

    model.getRoot().getNums().add(9);
    System.out.println("[3] nums.add(9) keeps ids → " + visible(unparser.unparse(model)));
    System.out.println("[4] codegen ListShapeHint (0.2.1+)");
  }
  static String visible(String s) { return s.replace("\n","\\n"); }
  static void require(boolean c, String m) { if (!c) { System.err.println("FAILED: "+m); System.exit(1);} }
}
