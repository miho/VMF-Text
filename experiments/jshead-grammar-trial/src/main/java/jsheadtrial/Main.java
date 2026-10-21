package jsheadtrial;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import jsheadtrial.parser.JavaScriptModelParser;
import jsheadtrial.unparser.JavaScriptModelUnparser;

/**
 * Trial driver: every sample is a test case. A sample passes when it parses and
 * unparses byte-exact — or, for a negative test (a sample with a
 * {@code <name>.errors} file next to it, the grammars-v4 convention), when
 * parsing fails with exactly the syntax errors listed in that file. Prints one
 * line per sample, then {@code passed x/n} and the reasons of the failures, so
 * {@code x < n} always means something is wrong.
 */
public class Main {
    static final String EXT = ".js";

    public static void main(String[] args) throws Exception {
        Path dir = Paths.get(args.length > 0 ? args[0] : "samples");
        List<Path> files;
        try (Stream<Path> s = Files.list(dir)) {
            files = s.filter(p -> p.toString().endsWith(EXT)).sorted().collect(Collectors.toList());
        }
        int exact = 0, expectedErrors = 0, mismatch = 0, parseFail = 0, unparseFail = 0, wrongErrors = 0, missingErrors = 0;
        long parseNs = 0, unparseNs = 0, bytes = 0;
        List<String> details = new ArrayList<>();
        for (Path f : files) {
            String src = new String(Files.readAllBytes(f), StandardCharsets.UTF_8);
            bytes += src.length();
            Path errorsFile = f.resolveSibling(f.getFileName() + ".errors");
            List<String> expected = Files.exists(errorsFile)
                    ? lines(new String(Files.readAllBytes(errorsFile), StandardCharsets.UTF_8)) : null;
            JavaScriptModel model;
            long t0 = System.nanoTime();
            try {
                model = new JavaScriptModelParser().parse(src);
            } catch (JavaScriptModelParser.SyntaxErrorException e) {
                List<String> actual = trimmed(e.getErrors());
                if (expected != null && expected.equals(actual)) {
                    expectedErrors++;
                    details.add(String.format("PASS expected error  %-32s %s", f.getFileName(), String.join(" | ", actual)));
                } else if (expected != null) {
                    wrongErrors++;
                    details.add(String.format("FAIL wrong errors    %-32s expected %s got %s", f.getFileName(), expected, actual));
                } else {
                    parseFail++;
                    details.add(String.format("FAIL parse           %-32s %d syntax error(s), first: %s", f.getFileName(), actual.size(),
                            actual.isEmpty() ? "" : actual.get(0)));
                }
                continue;
            } catch (Throwable t) {
                parseFail++;
                details.add(String.format("FAIL parse           %-32s %s: %s", f.getFileName(), t.getClass().getSimpleName(), firstLine(t.getMessage())));
                continue;
            }
            long t1 = System.nanoTime();
            parseNs += (t1 - t0);
            if (expected != null) {
                missingErrors++;
                details.add(String.format("FAIL no syntax error %-32s %s expects %s", f.getFileName(), errorsFile.getFileName(), expected));
                continue;
            }
            String out;
            try {
                out = new JavaScriptModelUnparser().unparse(model);
            } catch (Throwable t) {
                unparseFail++;
                details.add(String.format("FAIL unparse         %-32s %s: %s", f.getFileName(), t.getClass().getSimpleName(), firstLine(t.getMessage())));
                continue;
            }
            long t2 = System.nanoTime();
            unparseNs += (t2 - t1);
            if (out.equals(src)) {
                exact++;
                details.add(String.format("PASS exact           %-32s %6d chars  parse %5d ms  unparse %5d ms", f.getFileName(), src.length(), (t1 - t0) / 1_000_000, (t2 - t1) / 1_000_000));
            } else {
                mismatch++;
                int i = firstDiff(src, out);
                details.add(String.format("FAIL mismatch        %-32s first diff @%d: expected %s got %s", f.getFileName(), i, ctx(src, i), ctx(out, i)));
                Files.write(Paths.get("build", "mismatch-" + f.getFileName() + ".out"), out.getBytes(StandardCharsets.UTF_8));
            }
        }
        details.forEach(System.out::println);
        int passed = exact + expectedErrors;
        System.out.printf("%nSUMMARY passed %d/%d (exact round trips %d, expected syntax errors %d)"
                        + " | failed %d (mismatch %d, parse %d, unparse %d, wrong errors %d, no error %d)"
                        + " | %d chars | parse %d ms | unparse %d ms%n",
                passed, files.size(), exact, expectedErrors,
                files.size() - passed, mismatch, parseFail, unparseFail, wrongErrors, missingErrors,
                bytes, parseNs / 1_000_000, unparseNs / 1_000_000);
    }

    /** The non-empty lines of an .errors file, trailing white-space removed. */
    static List<String> lines(String text) {
        List<String> result = new ArrayList<>();
        for (String line : text.split(String.valueOf((char) 10))) {
            String l = line.stripTrailing();
            if (!l.isEmpty()) result.add(l);
        }
        return result;
    }

    static List<String> trimmed(List<String> errors) {
        return errors.stream().map(String::stripTrailing).collect(Collectors.toList());
    }

    static int firstDiff(String a, String b) { int n = Math.min(a.length(), b.length()); for (int i = 0; i < n; i++) if (a.charAt(i) != b.charAt(i)) return i; return n; }
    static String ctx(String s, int i) { int a = Math.max(0, i - 15), b = Math.min(s.length(), i + 25); return "\"" + s.substring(a, b).replace(String.valueOf((char) 10), "<NL>").replace(String.valueOf((char) 13), "<CR>").replace(String.valueOf((char) 9), "<TAB>") + "\""; }
    static String firstLine(String m) { return m == null ? "" : m.split(String.valueOf((char) 10))[0]; }
}
