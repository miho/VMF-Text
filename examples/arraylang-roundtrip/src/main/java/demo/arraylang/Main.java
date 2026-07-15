/*
 * Copyright 2017-2026 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */
package demo.arraylang;

import demo.arraylang.parser.ArrayLangModelParser;
import demo.arraylang.unparser.ArrayLangModelUnparser;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Smallest round-trip showcase: the ArrayLang grammar from the VMF-Text README.
 * Parse an irregularly spaced {@code (1,2,3)} list and prove byte-identical
 * unparse. Then replace one list value: because {@code values} is a flat
 * {@code List<Integer>} on one {@code CodeElement}, that edit clears the rule's
 * trivia and the formatter uses conservative separators (not source bundles —
 * those are for persistence/restore). Nested model edits preserve better; see
 * the Java 8 / Java 24 examples and {@code LEXICAL_PRESERVATION_ASSESSMENT.md}.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        Path input = Path.of(args.length > 0 ? args[0] : "sample/numbers.txt");
        String source = new String(Files.readAllBytes(input), StandardCharsets.UTF_8);

        // 1) parse into a typed VMF model
        ArrayLangModelParser parser = new ArrayLangModelParser();
        List<String> syntaxErrors = new ArrayList<>();
        parser.getErrorListeners().add(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                    int line, int charPositionInLine, String msg,
                                    RecognitionException e) {
                syntaxErrors.add(line + ":" + charPositionInLine + " " + msg);
            }
        });
        ArrayLangModel model = parser.parse(source);
        require(syntaxErrors.isEmpty(), "syntax errors: " + syntaxErrors);

        // 2) unparse without edits: byte-identical, including odd spacing
        String roundTrip = new ArrayLangModelUnparser().unparse(model);
        require(source.equals(roundTrip), "round trip is not byte-identical");
        System.out.println("[1] " + input + " (" + source.length()
                + " chars) round-tripped byte-identically");
        System.out.println("  source: " + visible(source));

        // 3) replace one value on the model (index 1: 2 -> 99)
        Array array = model.getRoot();
        require(array.getValues().size() >= 2, "expected at least two values");
        require(Integer.valueOf(2).equals(array.getValues().get(1)),
                "expected second value to be 2");
        array.getValues().set(1, 99);

        // 4) unparse the edited model: value changes; edited primitives use
        //    conservative separators (exact shape is preserved for unedited
        //    parses — see Java 8 / Java 24 examples for in-place token edits)
        String edited = new ArrayLangModelUnparser().unparse(model);
        ArrayLangModel reparsed = parser.parse(edited);
        require(Integer.valueOf(99).equals(reparsed.getRoot().getValues().get(1)),
                "expected replaced value 99 after reparse");
        System.out.println("[2] replaced values[1]: 2 -> 99");
        System.out.println("  after:  " + visible(edited));
        System.out.println("[3] flat primitive list edit -> conservative separators"
                + " for that rule;");
        System.out.println("    unedited round-trips stay byte-identical (step [1]);"
                + " see LEXICAL_PRESERVATION_ASSESSMENT.md");
    }

    private static String visible(String s) {
        return s.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            System.err.println("FAILED: " + message);
            System.exit(1);
        }
    }
}
