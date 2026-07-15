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
 * Parse an irregularly spaced {@code (1,2,3)} list, prove byte-identical
 * unparse, then replace one value on the model and show that surrounding
 * whitespace is preserved.
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

        // 3) replace one value on the model (index 1: 2 -> 99)
        Array array = model.getRoot();
        require(array.getValues().size() >= 2, "expected at least two values");
        require(Integer.valueOf(2).equals(array.getValues().get(1)),
                "expected second value to be 2");
        array.getValues().set(1, 99);

        // 4) unparse: the edited token changes; surrounding whitespace stays
        String edited = new ArrayLangModelUnparser().unparse(model);
        require(!source.equals(edited), "edited output should differ");
        require(edited.contains("99"), "expected replaced value 99 in output");
        System.out.println("[2] replaced values[1]: 2 -> 99");
        System.out.println("  before: " + visible(source));
        System.out.println("  after:  " + visible(edited));
        System.out.println("[3] one value edit; surrounding whitespace preserved");
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
