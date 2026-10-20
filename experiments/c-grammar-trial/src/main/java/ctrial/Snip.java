package ctrial;

import ctrial.parser.CModelParser;
import ctrial.unparser.CModelUnparser;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Parse+unparse each snippet (separated by a line '----' in the given file) and show the result. */
public class Snip {
    public static void main(String[] args) throws Exception {
        String all = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
        for (String snippet : all.split("(?m)^----\r?\n")) {
            if (snippet.isBlank()) continue;
            String src = snippet;
            String tag;
            String out = "";
            try {
                CModel m = new CModelParser().parse(src);
                out = new CModelUnparser().unparse(m);
                tag = out.equals(src) ? "EXACT   " : "MISMATCH";
            } catch (Throwable t) {
                tag = "FAIL    ";
                out = t.getClass().getSimpleName() + ": " + (t.getMessage() == null ? "" : t.getMessage().split("\n")[0]);
            }
            System.out.println(tag + " in : " + show(src));
            if (!tag.startsWith("EXACT")) System.out.println("         out: " + show(out));
        }
    }
    static String show(String s) { return "\"" + s.replace("\n", "\n").replace("\t", "\t") + "\""; }
}
