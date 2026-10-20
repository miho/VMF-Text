package py3trial;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import py3trial.parser.Python3ModelParser;
import py3trial.unparser.Python3ModelUnparser;

/** Python 3 trial driver: parse + unparse every sample, report byte-exactness and timings. */
public class Main {
    public static void main(String[] args) throws Exception {
        Path dir = Paths.get(args.length > 0 ? args[0] : "samples");
        List<Path> files;
        try (Stream<Path> s = Files.list(dir)) {
            files = s.filter(p -> p.toString().endsWith(".py")).sorted().collect(Collectors.toList());
        }
        int exact = 0, parseFail = 0, unparseFail = 0, mismatch = 0;
        long parseNs = 0, unparseNs = 0, bytes = 0;
        List<String> details = new ArrayList<>();
        for (Path f : files) {
            String src = new String(Files.readAllBytes(f), StandardCharsets.UTF_8);
            bytes += src.length();
            Python3Model model;
            long t0 = System.nanoTime();
            try {
                model = new Python3ModelParser().parse(src);
            } catch (Throwable t) {
                parseFail++;
                details.add("PARSE-FAIL  " + f.getFileName() + " : " + t.getClass().getSimpleName() + ": " + firstLine(t.getMessage()));
                continue;
            }
            long t1 = System.nanoTime();
            parseNs += (t1 - t0);
            String out;
            try {
                out = new Python3ModelUnparser().unparse(model);
            } catch (Throwable t) {
                unparseFail++;
                details.add("UNPARSE-FAIL " + f.getFileName() + " : " + t.getClass().getSimpleName() + ": " + firstLine(t.getMessage()));
                continue;
            }
            long t2 = System.nanoTime();
            unparseNs += (t2 - t1);
            if (out.equals(src)) {
                exact++;
                details.add(String.format("EXACT       %-32s %6d chars  parse %5d ms  unparse %5d ms", f.getFileName(), src.length(), (t1 - t0) / 1_000_000, (t2 - t1) / 1_000_000));
            } else {
                mismatch++;
                int i = firstDiff(src, out);
                details.add(String.format("MISMATCH    %-32s first diff @%d: expected %s got %s", f.getFileName(), i, ctx(src, i), ctx(out, i)));
                Files.write(Paths.get("build", "mismatch-" + f.getFileName() + ".out"), out.getBytes(StandardCharsets.UTF_8));
            }
        }
        details.forEach(System.out::println);
        System.out.printf("%nSUMMARY files=%d exact=%d mismatch=%d parseFail=%d unparseFail=%d | %d chars | parse %d ms | unparse %d ms%n",
                files.size(), exact, mismatch, parseFail, unparseFail, bytes, parseNs / 1_000_000, unparseNs / 1_000_000);
    }
    static int firstDiff(String a, String b) { int n = Math.min(a.length(), b.length()); for (int i = 0; i < n; i++) if (a.charAt(i) != b.charAt(i)) return i; return n; }
    static String ctx(String s, int i) { int a = Math.max(0, i - 15), b = Math.min(s.length(), i + 25); return "\"" + s.substring(a, b).replace(String.valueOf((char) 10), "<NL>").replace(String.valueOf((char) 13), "<CR>").replace(String.valueOf((char) 9), "<TAB>") + "\""; }
    static String firstLine(String m) { return m == null ? "" : m.split("\n")[0]; }
}
