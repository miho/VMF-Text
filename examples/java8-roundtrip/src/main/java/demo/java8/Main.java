/*
 * Copyright 2017-2026 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */
package demo.java8;

import demo.java8.parser.Java8ModelParser;
import demo.java8.unparser.Java8ModelUnparser;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Mid-complexity round-trip showcase: parse a small Java 8 source file into a
 * typed model, prove byte-identical unparse, then replace a string literal and
 * a method name on the model while preserving every other byte.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        Path input = Path.of(args.length > 0 ? args[0] : "sample/Greeter.java");
        String source = new String(Files.readAllBytes(input), StandardCharsets.UTF_8);

        // 1) parse into a typed VMF model
        Java8ModelParser parser = new Java8ModelParser();
        List<String> syntaxErrors = new ArrayList<>();
        parser.getErrorListeners().add(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                    int line, int charPositionInLine, String msg,
                                    RecognitionException e) {
                syntaxErrors.add(line + ":" + charPositionInLine + " " + msg);
            }
        });
        Java8Model model = parser.parse(source);
        require(syntaxErrors.isEmpty(), "syntax errors: " + syntaxErrors);

        // 2) unparse without edits: byte-identical
        String roundTrip = new Java8ModelUnparser().unparse(model);
        require(source.equals(roundTrip), "round trip is not byte-identical");
        System.out.println("[1] " + input + " (" + source.length()
                + " chars) round-tripped byte-identically");

        // 3) replace values on the model: method name + string literal
        model.vmf().content().stream(MethodDeclaration.class)
                .filter(m -> "greet".equals(m.getMethodName()))
                .forEach(m -> m.setMethodName("sayHello"));

        model.vmf().content().stream(StringLiteral.class)
                .filter(lit -> "\"hello\"".equals(lit.getStringValue()))
                .forEach(lit -> lit.setStringValue("\"hello, world\""));

        // 4) unparse: exactly the expected lines differ
        String edited = new Java8ModelUnparser().unparse(model);
        List<String> diff = diffLines(source, edited);
        diff.forEach(System.out::println);
        require(diff.size() == 4, "expected exactly two changed lines, got "
                + (diff.size() / 2));
        require(edited.contains("sayHello"), "expected renamed method");
        require(edited.contains("\"hello, world\""), "expected replaced string");
        System.out.println("[2] two model value edits -> two changed lines;"
                + " every other byte is untouched");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            System.err.println("FAILED: " + message);
            System.exit(1);
        }
    }

    private static List<String> diffLines(String before, String after) {
        String[] a = before.split("\n", -1);
        String[] b = after.split("\n", -1);
        require(a.length == b.length, "line count changed: "
                + a.length + " -> " + b.length);
        List<String> out = new ArrayList<>();
        for (int i = 0; i < a.length; i++) {
            if (!a[i].equals(b[i])) {
                out.add("  line " + (i + 1) + "  - " + a[i]);
                out.add("  line " + (i + 1) + "  + " + b[i]);
            }
        }
        return out;
    }
}
