package eu.mihosoft.vmftext.tests.java24.parser;

import org.antlr.v4.runtime.*;

import java.util.List;

public abstract class JavaParserBase extends eu.mihosoft.vmftext.tests.java24.unparser.antlr4.JavaParserBase {
    public JavaParserBase(TokenStream input) {
        super(input);
    }
}