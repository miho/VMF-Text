package py3trial.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;

/**
 * Adapted from grammars-v4 python/python3/Java/Python3LexerBase.java (MIT,
 * Bart Kiers). Changes for VMF-Text's lexical preservation:
 * <ul>
 *   <li>synthesized tokens get exact character ranges (NEWLINE = the
 *       line-break characters, INDENT = the indentation, DEDENT = empty; the
 *       virtual NEWLINE/DEDENT/EOF of an unterminated file are empty at the end
 *       of the input) — the upstream commonToken() derives bogus ranges from the
 *       text length;</li>
 *   <li>text the upstream lexer skips inside onNewLine() (blank lines, newlines
 *       inside brackets, same-level indentation, leftover indentation after
 *       DEDENTs) becomes a hidden-channel token, so the token stream is
 *       lossless (the grammar's SKIP_ rule goes to the hidden channel too).</li>
 * </ul>
 */
public abstract class Python3LexerBase extends Lexer {

    private java.util.LinkedList<Token> tokens = new java.util.LinkedList<>();
    private java.util.Deque<Integer> indents = new java.util.ArrayDeque<>();
    private int opened = 0;
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
        if (_input.LA(1) == EOF && !this.indents.isEmpty()) {
            for (int i = tokens.size() - 1; i >= 0; i--) {
                if (tokens.get(i).getType() == EOF) {
                    tokens.remove(i);
                }
            }
            int at = getCharIndex();
            // a NEWLINE without text closes the last statement of an unterminated file
            this.emit(tokenAt(Python3Lexer.NEWLINE, at, at - 1));
            while (!indents.isEmpty()) {
                this.emit(tokenAt(Python3Lexer.DEDENT, at, at - 1));
                indents.pop();
            }
            this.emit(tokenAt(EOF, at, at - 1));
        }
        Token next = super.nextToken();
        if (next.getChannel() == Token.DEFAULT_CHANNEL) {
            this.lastToken = next;
        }
        return tokens.isEmpty() ? next : tokens.poll();
    }

    private CommonToken tokenAt(int type, int start, int stop) {
        CommonToken t = new CommonToken(this._tokenFactorySourcePair, type, DEFAULT_TOKEN_CHANNEL, start, stop);
        t.setLine(this.lastToken == null ? 1 : this.lastToken.getLine());
        if (start > stop) {
            t.setText(""); // an empty range at the end of the input would otherwise read "<EOF>"
        }
        return t;
    }

    private CommonToken hiddenAt(int start, int stop) {
        return new CommonToken(this._tokenFactorySourcePair, Python3Lexer.SKIP_, HIDDEN, start, stop);
    }

    static int getIndentationCount(String spaces) {
        int count = 0;
        for (char ch : spaces.toCharArray()) {
            switch (ch) {
                case '\t':
                    count += 8 - (count % 8);
                    break;
                default:
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
        int next = _input.LA(1);
        int nextnext = _input.LA(2);
        if (opened > 0 || (nextnext != -1 && (next == '\r' || next == '\n' || next == '\f' || next == '#'))) {
            // inside brackets or a blank line: no NEWLINE/INDENT/DEDENT, the text is hidden
            emit(hiddenAt(start, end));
            return;
        }
        if (nlLen > 0) {
            emit(tokenAt(Python3Lexer.NEWLINE, start, start + nlLen - 1));
        }
        int indent = getIndentationCount(spaces);
        int previous = indents.isEmpty() ? 0 : indents.peek();
        if (indent == previous) {
            if (!spaces.isEmpty()) {
                emit(hiddenAt(start + nlLen, end));
            }
        } else if (indent > previous) {
            indents.push(indent);
            emit(tokenAt(Python3Lexer.INDENT, start + nlLen, end));
        } else {
            while (!indents.isEmpty() && indents.peek() > indent) {
                emit(tokenAt(Python3Lexer.DEDENT, end + 1, end));
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
        opened = 0;
        lastToken = null;
        super.reset();
    }
}
