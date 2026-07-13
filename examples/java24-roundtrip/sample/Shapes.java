/*
 * Sample input for the VMF-Text round-trip showcase.
 *
 * Every comment, blank line and odd spacing in this file survives
 * parse -> unparse byte-for-byte.
 */
package demo.shapes;

import java.util.List;

/** A sealed shape hierarchy. */
sealed interface Shape permits Circle, Box { }

/** Circles have a radius. */
record Circle(double radius) implements Shape { }

non-sealed class Box implements Shape {
    // intentionally odd formatting:   the extra spaces below stay put
    double   width  = 1.0,   height = 2.0;   // trailing comment
}

class Report {

    /**
     * Describes a shape, using pattern matching and a text block.
     */
    static String describe(Shape shape) {
        return switch (shape) {
            case Circle(double radius) when radius > 0 -> """
                    a circle
                    """;
            case Circle circle -> "a degenerate circle";
            case Box box -> "a box";
        };
    }
}
