/*
 * Copyright 2017-2026 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */
package demo.java24;

import demo.java24.parser.Java24ModelParser;
import demo.java24.unparser.Java24ModelUnparser;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Round-trip showcase: parse a real Java 24 source file into a typed model,
 * prove that unparsing reproduces it byte-identically, then apply one
 * surgical model edit and show that exactly one line changes.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        Path input = Path.of(args.length > 0 ? args[0] : "sample/Shapes.java");
        String source = new String(Files.readAllBytes(input), StandardCharsets.UTF_8);

        // 1) parse the source into a typed VMF model
        Java24ModelParser parser = new Java24ModelParser();
        List<String> syntaxErrors = new ArrayList<>();
        parser.getErrorListeners().add(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                    int line, int charPositionInLine, String msg,
                                    RecognitionException e) {
                syntaxErrors.add(line + ":" + charPositionInLine + " " + msg);
            }
        });
        Java24Model model = parser.parse(source);
        require(syntaxErrors.isEmpty(), "syntax errors: " + syntaxErrors);

        // 2) unparse without any edit: the output is byte-identical,
        //    including all comments, blank lines and irregular spacing
        String roundTrip = new Java24ModelUnparser().unparse(model);
        require(source.equals(roundTrip), "round trip is not byte-identical");
        System.out.println("[1] " + input + " (" + source.length()
                + " chars) round-tripped byte-identically");

        // 3) one surgical edit on the model: rename method describe -> render
        model.vmf().content().stream(MethodDeclaration.class)
                .filter(m -> "describe".equals(m.getMethodName().getText()))
                .forEach(m -> m.getMethodName().setText("render"));

        // 4) unparse the edited model: exactly one line differs
        String edited = new Java24ModelUnparser().unparse(model);
        List<String> diff = diffLines(source, edited);
        diff.forEach(System.out::println);
        require(diff.size() == 2, "expected exactly one changed line, got "
                + (diff.size() / 2));
        System.out.println("[2] one model edit -> one changed line; every"
                + " other byte is untouched");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            System.err.println("FAILED: " + message);
            System.exit(1);
        }
    }

    /**
     * Line-based diff. The showcase edit replaces text in place, so the
     * line structure must not change.
     */
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
