package py3trial;

import py3trial.parser.Python3ModelParser;
import py3trial.unparser.Python3ModelUnparser;
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
                Python3Model m = new Python3ModelParser().parse(src);
                out = new Python3ModelUnparser().unparse(m);
                tag = out.equals(src) ? "EXACT   " : "MISMATCH";
            } catch (Throwable t) {
                tag = "FAIL    ";
                out = t.getClass().getSimpleName() + ": " + (t.getMessage() == null ? "" : t.getMessage().split("\n")[0]);
            }
            System.out.println(tag + " in : " + show(src));
            if (!tag.startsWith("EXACT")) System.out.println("         out: " + show(out));
        }
    }
    static String show(String s) { return "\"" + s.replace(String.valueOf((char) 10), "<NL>").replace(String.valueOf((char) 9), "<TAB>") + "\""; }
}
