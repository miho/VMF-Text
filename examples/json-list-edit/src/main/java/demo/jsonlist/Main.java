package demo.jsonlist;

import demo.jsonlist.parser.MiniJsonModelParser;
import demo.jsonlist.unparser.MiniJsonModelUnparser;
import java.nio.file.*;

/** Model-typed JSON array: parent trivia holds brackets/commas; children keep leading WS. */
public class Main {
  public static void main(String[] args) throws Exception {
    String source = Files.readString(Path.of("sample/array.json"));
    MiniJsonModelParser parser = new MiniJsonModelParser();
    MiniJsonModelUnparser unparser = new MiniJsonModelUnparser();
    MiniJsonModel model = parser.parse(source);
    require(source.equals(unparser.unparse(model)), "round-trip");
    System.out.println("[1] JSON array round-trip: " + visible(source));

    Array array = (Array) model.getRoot().getValue();
    array.getValues().remove(1);
    System.out.println("[2] remove(1) → " + visible(unparser.unparse(model)));

    array.getValues().add(Num.newBuilder().withValue(9.0).build());
    System.out.println("[3] add(9) → " + visible(unparser.unparse(model)));
    System.out.println("[4] model-typed delimited list parent-trivia splice (0.2.1+)");
  }
  static String visible(String s) { return s.replace("\n","\\n"); }
  static void require(boolean c, String m) { if (!c) { System.err.println("FAILED: "+m); System.exit(1);} }
}
