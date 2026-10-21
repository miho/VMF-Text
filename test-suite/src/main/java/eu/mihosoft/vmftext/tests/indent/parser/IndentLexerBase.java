package eu.mihosoft.vmftext.tests.indent.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;

/**
 * #60: lexer base class of the indentation language, modelled on the
 * grammars-v4 Python 3 lexer base (Bart Kiers, MIT) as adapted for the Python
 * trial. It synthesizes NEWLINE / INDENT / DEDENT tokens with exact character
 * ranges (NEWLINE = the line break, INDENT = the indentation, DEDENT = empty;
 * the virtual NEWLINE and DEDENTs that close an unterminated file are empty at
 * the end of the input) and emits what Python's lexer skips — blank lines,
 * comment-only lines, same-level indentation and the remainder after DEDENTs —
 * as hidden-channel tokens, so the token stream is lossless.
 */
public abstract class IndentLexerBase extends Lexer {

    private java.util.LinkedList<Token> tokens = new java.util.LinkedList<>();
    private java.util.Deque<Integer> indents = new java.util.ArrayDeque<>();
    // the last token emitted on the default channel (EOF excluded)
    private Token lastDefault = null;
    private boolean closed = false;

    protected IndentLexerBase(CharStream input) {
        super(input);
    }

    @Override
    public void emit(Token t) {
        super.setToken(t);
        tokens.offer(t);
        if (t.getChannel() == Token.DEFAULT_CHANNEL && t.getType() != EOF) {
            lastDefault = t;
        }
    }

    private boolean lastIsLineEnd() {
        return lastDefault == null || lastDefault.getType() == IndentLexer.NEWLINE || lastDefault.getType() == IndentLexer.DEDENT;
    }

    @Override
    public Token nextToken() {
        if (_input.LA(1) == EOF && !closed) {
            closed = true;
            // unlike the grammars-v4 base, an unterminated last line is closed
            // at every depth, not only inside a block
            boolean unterminated = !lastIsLineEnd();
            if (unterminated || !this.indents.isEmpty()) {
                for (int i = tokens.size() - 1; i >= 0; i--) {
                    if (tokens.get(i).getType() == EOF) {
                        tokens.remove(i);
                    }
                }
                int at = getCharIndex();
                if (unterminated) {
                    // a NEWLINE without text closes the last statement of an unterminated file
                    this.emit(tokenAt(IndentLexer.NEWLINE, at, at - 1));
                }
                while (!indents.isEmpty()) {
                    this.emit(tokenAt(IndentLexer.DEDENT, at, at - 1));
                    indents.pop();
                }
                this.emit(tokenAt(EOF, at, at - 1));
            }
        }
        Token next = super.nextToken();
        return tokens.isEmpty() ? next : tokens.poll();
    }

    private CommonToken tokenAt(int type, int start, int stop) {
        CommonToken t = new CommonToken(this._tokenFactorySourcePair, type, DEFAULT_TOKEN_CHANNEL, start, stop);
        t.setLine(this.lastDefault == null ? 1 : this.lastDefault.getLine());
        if (start > stop) {
            t.setText(""); // an empty range at the end of the input would otherwise read "<EOF>"
        }
        return t;
    }

    private CommonToken hiddenAt(int start, int stop) {
        return new CommonToken(this._tokenFactorySourcePair, IndentLexer.SKIP_, HIDDEN, start, stop);
    }

    static int getIndentationCount(String spaces) {
        int count = 0;
        for (char ch : spaces.toCharArray()) {
            if (ch == '\t') {
                count += 8 - (count % 8);
            } else {
                count++;
            }
        }
        return count;
    }

    protected boolean atStartOfInput() {
        return super.getCharPositionInLine() == 0 && super.getLine() == 1;
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
        int next = _input.LA(1);
        int nextnext = _input.LA(2);
        if ((nextnext != -1 && (next == '\r' || next == '\n' || next == '\f' || next == '#'))
                || (next == -1 && lastIsLineEnd())) {
            // a blank or comment-only line (or trailing blank lines before the
            // end of the input): no NEWLINE/INDENT/DEDENT, the text is hidden
            emit(hiddenAt(start, end));
            return;
        }
        if (nlLen > 0) {
            emit(tokenAt(IndentLexer.NEWLINE, start, start + nlLen - 1));
        }
        int indent = getIndentationCount(spaces);
        int previous = indents.isEmpty() ? 0 : indents.peek();
        if (indent == previous) {
            if (!spaces.isEmpty()) {
                emit(hiddenAt(start + nlLen, end));
            }
        } else if (indent > previous) {
            indents.push(indent);
            emit(tokenAt(IndentLexer.INDENT, start + nlLen, end));
        } else {
            while (!indents.isEmpty() && indents.peek() > indent) {
                emit(tokenAt(IndentLexer.DEDENT, end + 1, end));
                indents.pop();
            }
            if (!spaces.isEmpty()) {
                emit(hiddenAt(start + nlLen, end));
            }
        }
    }

    @Override
    public void reset() {
        tokens = new java.util.LinkedList<>();
        indents = new java.util.ArrayDeque<>();
        lastDefault = null;
        closed = false;
        super.reset();
    }
}
