package py3trial.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;

/**
 * Adapted from grammars-v4 python/python3/Java/Python3LexerBase.java (MIT,
 * Bart Kiers). One change for VMF-Text's lexical preservation: synthesized
 * tokens get exact character ranges — NEWLINE = the line-break characters,
 * INDENT = the indentation, DEDENT = empty right after the line break, and the
 * virtual NEWLINE/DEDENT/EOF that close an unterminated file are empty at the
 * end of the input. The upstream commonToken() derives a range from the text
 * length and the current position, which puts NEWLINE on the indentation and
 * gives DEDENT a one-character range. Everything the upstream lexer skips
 * (blank lines, newlines inside brackets, same-level indentation) is still
 * skipped: VMF-Text takes the text between two tokens from the input by
 * position (#61), so the token stream does not need to be lossless.
 */
public abstract class Python3LexerBase extends Lexer {

    // A queue where extra tokens are pushed on (see the NEWLINE lexer rule).
    private java.util.LinkedList<Token> tokens = new java.util.LinkedList<>();
    // The stack that keeps track of the indentation level.
    private java.util.Deque<Integer> indents = new java.util.ArrayDeque<>();
    // The amount of opened braces, brackets and parenthesis.
    private int opened = 0;
    // The most recently produced token.
    private Token lastToken = null;

    protected Python3LexerBase(CharStream input) {
        super(input);
    }

    @Override
    public void emit(Token t) {
        super.setToken(t);
        tokens.offer(t);
    }

    @Override
    public Token nextToken() {
        // Check if the end-of-file is ahead and there are still some DEDENTS expected.
        if (_input.LA(1) == EOF && !this.indents.isEmpty()) {
            // Remove any trailing EOF tokens from our buffer.
            for (int i = tokens.size() - 1; i >= 0; i--) {
                if (tokens.get(i).getType() == EOF) {
                    tokens.remove(i);
                }
            }
            int at = getCharIndex();
            // First emit an extra line break that serves as the end of the statement (no text).
            this.emit(tokenAt(Python3Lexer.NEWLINE, at, at - 1));
            // Now emit as much DEDENT tokens as needed.
            while (!indents.isEmpty()) {
                this.emit(tokenAt(Python3Lexer.DEDENT, at, at - 1));
                indents.pop();
            }
            // Put the EOF back on the token stream.
            this.emit(tokenAt(EOF, at, at - 1));
        }

        Token next = super.nextToken();

        if (next.getChannel() == Token.DEFAULT_CHANNEL) {
            // Keep track of the last token on the default channel.
            this.lastToken = next;
        }

        return tokens.isEmpty() ? next : tokens.poll();
    }

    /** A default-channel token with the exact character range [start, stop] (empty for start > stop). */
    private CommonToken tokenAt(int type, int start, int stop) {
        CommonToken t = new CommonToken(this._tokenFactorySourcePair, type, DEFAULT_TOKEN_CHANNEL, start, stop);
        t.setLine(this.lastToken == null ? 1 : this.lastToken.getLine());
        if (start > stop) {
            t.setText(""); // an empty range at the end of the input would otherwise read "<EOF>"
        }
        return t;
    }

    // Calculates the indentation of the provided spaces, taking the
    // following rules into account:
    //
    // "Tabs are replaced (from left to right) by one to eight spaces
    //  such that the total number of characters up to and including
    //  the replacement is a multiple of eight [...]"
    //
    //  -- https://docs.python.org/3.1/reference/lexical_analysis.html#indentation
    static int getIndentationCount(String spaces) {
        int count = 0;
        for (char ch : spaces.toCharArray()) {
            switch (ch) {
                case '\t':
                    count += 8 - (count % 8);
                    break;
                default:
                    // A normal space char.
                    count++;
            }
        }
        return count;
    }

    protected boolean atStartOfInput() {
        return super.getCharPositionInLine() == 0 && super.getLine() == 1;
    }

    protected void openBrace() {
        this.opened++;
    }

    protected void closeBrace() {
        this.opened--;
    }

    protected void onNewLine() {
        String text = getText();
        int start = _tokenStartCharIndex;
        int end = getCharIndex() - 1;
        int nlLen = 0;
        while (nlLen < text.length() && "\r\n\f".indexOf(text.charAt(nlLen)) >= 0) {
            nlLen++;
        }
        String spaces = text.substring(nlLen);

        // Strip newlines inside open clauses except if we are near EOF. We keep NEWLINEs near EOF to
        // satisfy the final newline needed by the single_put rule used by the REPL.
        int next = _input.LA(1);
        int nextnext = _input.LA(2);
        if (opened > 0 || (nextnext != -1 && (next == '\r' || next == '\n' || next == '\f' || next == '#'))) {
            // If we're inside a list or on a blank line, ignore all indents,
            // dedents and line breaks.
            skip();
            return;
        }
        if (nlLen > 0) {
            emit(tokenAt(Python3Lexer.NEWLINE, start, start + nlLen - 1));
        }
        int indent = getIndentationCount(spaces);
        int previous = indents.isEmpty() ? 0 : indents.peek();
        if (indent == previous) {
            // skip indents of the same size as the present indent-size
            skip();
        } else if (indent > previous) {
            indents.push(indent);
            emit(tokenAt(Python3Lexer.INDENT, start + nlLen, end));
        } else {
            // Possibly emit more than 1 DEDENT token (empty, right after the line break: the
            // indentation that follows belongs to the next statement).
            while (!indents.isEmpty() && indents.peek() > indent) {
                this.emit(tokenAt(Python3Lexer.DEDENT, start + nlLen, start + nlLen - 1));
                indents.pop();
            }
        }
    }

    @Override
    public void reset() {
        tokens = new java.util.LinkedList<>();
        indents = new java.util.ArrayDeque<>();
        opened = 0;
        lastToken = null;
        super.reset();
    }
}
