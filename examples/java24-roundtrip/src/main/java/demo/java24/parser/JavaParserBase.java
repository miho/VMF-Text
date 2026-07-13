/*
 * Copyright 2017-2026 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */
package demo.java24.parser;

import org.antlr.v4.runtime.*;

public abstract class JavaParserBase extends demo.java24.unparser.antlr4.JavaParserBase {
    public JavaParserBase(TokenStream input) {
        super(input);
    }
}
