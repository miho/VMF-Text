package demo.optocc;

import demo.optocc.parser.OptOccurrenceModelParser;
import demo.optocc.unparser.OptOccurrenceModelUnparser;
import java.nio.file.*;

/** occurrenceIndex pins repeated optional paths so mixed presence cannot drift. */
public class Main {
  public static void main(String[] args) throws Exception {
    String source = Files.readString(Path.of("sample/items.txt"));
    OptOccurrenceModelParser parser = new OptOccurrenceModelParser();
    OptOccurrenceModelUnparser unparser = new OptOccurrenceModelUnparser();
    OptOccurrenceModel model = parser.parse(source);
    require(source.equals(unparser.unparse(model)), "round-trip");
    Item first = model.getRoot().getItems().get(0);
    require(first.getLexicalInfo().getOptionalStates().stream()
        .allMatch(s -> s.getOccurrenceIndex() >= 0), "indexed");
    System.out.println("[1] optional states carry occurrenceIndex >= 0");

    model.getRoot().getItems().get(1).setName("new");
    require("x (keep), x (new)".equals(unparser.unparse(model)), "no steal");
    System.out.println("[2] setName on 2nd item keeps first group: "
        + unparser.unparse(model));
    System.out.println("[3] OptionalState.occurrenceIndex (0.2.1+)");
  }
  static void require(boolean c, String m) { if (!c) { System.err.println("FAILED: "+m); System.exit(1);} }
}
