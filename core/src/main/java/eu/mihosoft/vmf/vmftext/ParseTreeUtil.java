/*
 * Copyright 2017-2018 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 * Copyright 2017-2018 Goethe Center for Scientific Computing, University Frankfurt. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * If you use this software for scientific research then please cite the following publication(s):
 *
 * M. Hoffer, C. Poliwoda, & G. Wittum. (2013). Visual reflection library:
 * a framework for declarative GUI programming on the Java platform.
 * Computing and Visualization in Science, 2013, 16(4),
 * 181–192. http://doi.org/10.1007/s00791-014-0230-y
 */
package eu.mihosoft.vmf.vmftext;

import eu.mihosoft.vmf.vmftext.grammar.CodeLocation;
import eu.mihosoft.vmf.vmftext.grammar.CodeRange;
import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

/**
 * Utility class for ANTLR4 parse trees. The utility methods provided by this class are
 * mostly useful for determining the type of specified element contexts.
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class ParseTreeUtil {

    private ParseTreeUtil() {
        throw new AssertionError("Don't instantiate me!");
    }

    /**
     * Indicates whether the specified element context is a rule block.
     * @param e the element context to check
     * @return {@code true} if the specified context is a rule block; {@code false} otherwise
     */
    public static boolean isRuleBlock(ANTLRv4Parser.ElementContext e) {
        return e.labeledElement().atom()== null && e.labeledElement().block()!=null;
    }

    /**
     * Indicates whether the specified element context is a parser rule.
     * @param e the element context to check
     * @return {@code true} if the specified celement ontext is a parser rule; {@code false} otherwise
     */
    public static boolean isParserRule(ANTLRv4Parser.ElementContext e) {

        if(e.labeledElement() == null || e.labeledElement().atom()==null) return false;

        return e.labeledElement().atom().ruleref() != null;
    }

    private static ANTLRv4Parser.AtomContext getAtom(ANTLRv4Parser.ElementContext e) {
        if(e.labeledElement()!=null&&e.labeledElement().atom()!=null) {
            return e.labeledElement().atom();
        } else if(e.atom()!=null){
            return e.atom();
        }

        return null;
    }

    /**
     * Indicates whether the specified element context is a lexer rule.
     * @param e the element context to check
     * @return {@code true} if the specified element context is a lexer rule; {@code false} otherwise
     */
    public static boolean isLexerRule(ANTLRv4Parser.ElementContext e) {

        ANTLRv4Parser.AtomContext atom = getAtom(e);

        if(atom==null) return false;

        if(atom!=null) {
            String atomText = atom.getText();

            // detect lexer rules with not operator
            if(atomText.length()> 1
                    && atomText.startsWith("~")
                    && Character.isUpperCase(atomText.codePointAt(1)))
                return true;
        }

        if (atom.terminal()!=null && atom.terminal().TOKEN_REF()!=null
                && Character.isUpperCase(atom.terminal().TOKEN_REF().getText().codePointAt(0))){
            return true;
        }

        return false;
    }

    /**
     * Indicates whether the specified element context is a string literal / terminal.
     * @param e the element context to check
     * @return {@code true} if the specified element context is a string literal; {@code false} otherwise
     */
    public static boolean isStringLiteral(ANTLRv4Parser.ElementContext e) {

        ANTLRv4Parser.AtomContext atom = getAtom(e);

        if(atom==null) return false;

        String atomText = atom.getText().trim();

        // detect simple dot (see issue #8)
        if(".".equals(atomText) && isListAssignment(e)) {
            throw new RuntimeException("Cannot label simple dot via '+=' assignment, e.g., 'myLabel+=.'. See issue #8 for updates and explanations.\nparent-rule-text: " + (e.getParent()==null?"<null>":e.getParent().getText()));
        }

        // detect literals with not operator
        if(atomText.startsWith("~'")) return true;

        return atom.terminal() != null &&
                atom.terminal().TOKEN_REF() == null;
    }

    /**
     * Indicates whether the specified element context is negated, i.e. {@code ~[ABC]}
     * @param e the element context to check
     * @return {@code true} if the specified element context is negated; {@code false} otherwise
     */
    public static boolean isNegated(ANTLRv4Parser.ElementContext e) {

        if(isParserRule(e)) {
            // parser rules can't be negated
            return false;
        } else if(e.atom()!=null) {
            return e.atom().notSet()!=null;
        } else if(e.labeledElement()!=null && e.labeledElement().atom()!=null) {
            return e.labeledElement().atom().notSet()!=null;
        }

        return false;
    }

    /**
     * Indicates whether the specified element context is an embedded action, e.g., {@code { Sysste.out.println(..)}}.
     * @param e the element context to check
     * @return {@code true} if the specified element context is an embedded grammar action; {@code false} otherwise
     */
    public static boolean isActionElement(ANTLRv4Parser.ElementContext e) {
        return e.actionBlock()!=null;
    }

    /**
     * Returns the text of the specified element context.
     * @param e element context
     * @return the text of the specified element context
     */
    public static String getElementText(ANTLRv4Parser.ElementContext e) {
        if(isParserRule(e)) {
            return e.labeledElement().atom().ruleref().getText();
        } else if(isLexerRule(e)) {

            if(e.labeledElement()!=null && e.labeledElement().atom()!=null) {
                return e.labeledElement().atom().getText();
            } else if(e.atom()!=null) {
                return e.atom().getText();
            }
            /*else {
                // default case
                return e.labeledElement().atom().terminal().TOKEN_REF().getText();
            }*/
        } else if(isStringLiteral(e)) {

            if(e.labeledElement().atom()!=null) {
                return e.labeledElement().atom().getText();
            }
            /*else {
                // default case
                return e.labeledElement().atom().terminal().STRING_LITERAL().getText();
            }*/
        }

        return null;
    }

    /**
     * Indicates whether the specified element context is labelled.
     * @param e the element context to check
     * @return {@code true} if the specified element context is labeled; {@code false} otherwise
     */
    public static boolean isLabeledElement(ANTLRv4Parser.ElementContext e) {
        return e.labeledElement() != null && e.labeledElement().identifier() != null;
    }

    /**
     * Returns the start location of the specified token.
     * @param t token
     * @return the start location of the specified token
     */
    public static CodeLocation tokenToCodeLocationStart(Token t) {
        return CodeLocation.newBuilder().
                withIndex(t.getStartIndex()).
                withCharPosInLine(t.getCharPositionInLine()).withLine(t.getLine()).build();
    }

    /**
     * Returns the stop/end location of the specified token.
     * @param t token
     * @param ctx parser rule context the token belongs to
     * @return the stop/end location of the specified token
     */
    public static CodeLocation tokenToCodeLocationStop(Token t, ParserRuleContext ctx) {
        int startIndex = t.getStartIndex();
        int stopIndex = t.getStopIndex();
        int diff = stopIndex - startIndex;

        return CodeLocation.newBuilder().
                withIndex(stopIndex).
                withCharPosInLine(t.getCharPositionInLine()+diff).
                withLine(/*TODO 14.02.2018 does not work for multi-line-tokens*/t.getLine()).build();
    }

    /**
     * Returns the code range of the specified context.
     * @param ctx the context to check
     * @return the code range of the specified context
     */
    public static CodeRange ctxToCodeRange(ParserRuleContext ctx) {
        // consider for stop line: https://stackoverflow.com/a/17487805
        CodeLocation start = tokenToCodeLocationStart(ctx.start);
        CodeLocation stop = tokenToCodeLocationStart(ctx.stop);
        return CodeRange.newBuilder().withStart(start).
                withStop(tokenToCodeLocationStop(ctx.stop,ctx))
                .withLength(stop.getIndex()-start.getIndex()+1/*+1 because of inclusive vs. exclusive*/).build();
    }

    /**
     * Indicates whether the specified element context is a labeled block element.
     * @param e element context to check
     * @return {@code true} if the specified element context is a labeled block element; {@code false} otherwise
     */
    public static boolean isLabeledBlockElement(ANTLRv4Parser.ElementContext e) {
        return isLabeledElement(e) && e.labeledElement().block()!=null;
    }

    /**
     * Indicates whether the specified element context is a block element.
     * @param e element context to check
     * @return {@code true} if the specified element context is a block element; {@code false} otherwise
     */
    public static boolean isBlockElement(ANTLRv4Parser.ElementContext e) {
        return e.labeledElement()!=null && e.labeledElement().block()!=null;
    }

    /**
     * Indicates whether the specified element context is an unlabeled block element.
     * @param e element context to check
     * @return {@code true} if the specified element context is an unlabeled block element; {@code false} otherwise
     */
    public static boolean isUnlabeledBlockElement(ANTLRv4Parser.ElementContext e) {
        return e.labeledElement()!=null && !isLabeledElement(e) && e.labeledElement().block()!=null;
    }

    /**
     * Indicates whether the specified element context is labelled via list-assignment ({@code '+='})
     * @param e element context to check
     * @return {@code true} if the specified element context is labelled via list-assignment; {@code false} otherwise
     */
    public static boolean isListAssignment(ANTLRv4Parser.ElementContext e) {
        if(!isLabeledElement(e)) return false;
        return e.labeledElement().PLUS_ASSIGN()!=null;
    }
}