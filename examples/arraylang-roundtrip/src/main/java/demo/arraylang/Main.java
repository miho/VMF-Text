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
 * Exact unedited round-trip, then in-place set / structural add / remove while
 * preserving sibling whitespace (VMF-Text 0.2.1+).
 */
public class Main {

    public static void main(String[] args) throws Exception {
        Path input = Path.of(args.length > 0 ? args[0] : "sample/numbers.txt");
        String source = new String(Files.readAllBytes(input), StandardCharsets.UTF_8);

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

        String roundTrip = new ArrayLangModelUnparser().unparse(model);
        require(source.equals(roundTrip), "round trip is not byte-identical");
        System.out.println("[1] " + input + " (" + source.length()
                + " chars) round-tripped byte-identically");
        System.out.println("  source: " + visible(source));

        Array array = model.getRoot();
        require(array.getValues().size() >= 2, "expected at least two values");
        require(Integer.valueOf(2).equals(array.getValues().get(1)),
                "expected second value to be 2");

        // in-place set
        array.getValues().set(1, 99);
        String afterSet = new ArrayLangModelUnparser().unparse(model);
        String expectedSet = source.replaceFirst("2", "99");
        require(expectedSet.equals(afterSet),
                "set should preserve whitespace, got: " + visible(afterSet));
        System.out.println("[2] values.set(1, 99)");
        System.out.println("  after:  " + visible(afterSet));

        // structural remove of the edited value
        array.getValues().remove(1);
        String afterRemove = new ArrayLangModelUnparser().unparse(model);
        require(!afterRemove.contains("99"), "expected 99 removed");
        require(afterRemove.contains("1") && afterRemove.contains("3"),
                "expected remaining values kept");
        System.out.println("[3] values.remove(1)");
        System.out.println("  after:  " + visible(afterRemove));

        // structural append
        array.getValues().add(42);
        String afterAdd = new ArrayLangModelUnparser().unparse(model);
        require(afterAdd.contains("42"), "expected appended 42");
        System.out.println("[4] values.add(42)");
        System.out.println("  after:  " + visible(afterAdd));
        System.out.println("[5] set/add/remove keep sibling whitespace (0.2.1+)");
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
