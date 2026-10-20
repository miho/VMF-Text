"""Builds a combined Python3.g4 for the VMF-Text trial from grammars-v4's split
python/python3 grammar (Python3Lexer.g4 + Python3Parser.g4 + Java base classes).

Adaptations (all documented in the trial README):
  - one combined grammar (no tokenVocab / superClass, see #39);
  - the lexer base class is inlined as @lexer::members with exact token
    positions (NEWLINE = the line-break chars, INDENT = the indentation
    spaces, DEDENT = empty) so lexical preservation can reproduce the input;
  - text the original lexer skips (whitespace, comments, line joining, blank
    lines, newlines inside brackets, same-level indentation) goes to the
    hidden channel instead;
  - the two parser-base predicates are inlined as @parser::members;
  - VMF-Text metadata: AutoLabel(enabled=true), Root(rule=file_input).
"""
import re, sys, os
SRC = sys.argv[1]
OUT = sys.argv[2]
lexer = open(os.path.join(SRC, 'Python3Lexer.g4'), encoding='utf-8').read().replace('\r\n', '\n')
parser = open(os.path.join(SRC, 'Python3Parser.g4'), encoding='utf-8').read().replace('\r\n', '\n')

def header(text):
    m = re.match(r'(/\*.*?\*/\s*)', text, re.S)
    return m.group(1) if m else ''

def body_after_grammar_decl(text):
    i = re.search(r'^(lexer |parser )?grammar\s+\w+\s*;\s*$', text, re.M)
    return text[i.end():]

lex_body = body_after_grammar_decl(lexer)
par_body = body_after_grammar_decl(parser)
# drop the options blocks (superClass / tokenVocab)
lex_body = re.sub(r'^options\s*\{.*?\}\s*$', '', lex_body, flags=re.S | re.M)
par_body = re.sub(r'^options\s*\{.*?\}\s*$', '', par_body, flags=re.S | re.M)
# keep the tokens block from the lexer (INDENT, DEDENT), remove it from the body
tokens = re.search(r'^tokens\s*\{.*?\}\s*$', lex_body, flags=re.S | re.M).group(0).strip()
lex_body = lex_body.replace(tokens, '', 1)
# skipped text goes to the hidden channel
n_skip = len(re.findall(r'->\s*skip\s*;', lex_body))
lex_body = re.sub(r'->\s*skip\s*;', '-> channel(HIDDEN);', lex_body)

lexer_members = r'''@lexer::members {
    // Adapted from grammars-v4 python/python3/Java/Python3LexerBase.java (MIT, see
    // header). Inlined because a combined grammar cannot set a lexer superClass.
    // Changes for VMF-Text: synthesized tokens get exact character ranges
    // (NEWLINE = the line-break characters, INDENT = the indentation, DEDENT =
    // empty), and text the original skipped (blank lines, newlines inside
    // brackets, same-level indentation) becomes a hidden-channel token so the
    // token stream is lossless.
    private java.util.LinkedList<Token> tokens = new java.util.LinkedList<>();
    private java.util.Deque<Integer> indents = new java.util.ArrayDeque<>();
    private int opened = 0;
    private Token lastToken = null;

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
            this.emit(tokenAt(NEWLINE, at, at - 1));
            while (!indents.isEmpty()) {
                this.emit(tokenAt(Python3Parser.DEDENT, at, at - 1));
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
        return new CommonToken(this._tokenFactorySourcePair, SKIP_, HIDDEN, start, stop);
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

    boolean atStartOfInput() {
        return super.getCharPositionInLine() == 0 && super.getLine() == 1;
    }

    void openBrace() {
        this.opened++;
    }

    void closeBrace() {
        this.opened--;
    }

    void onNewLine() {
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
            emit(tokenAt(NEWLINE, start, start + nlLen - 1));
        }
        int indent = getIndentationCount(spaces);
        int previous = indents.isEmpty() ? 0 : indents.peek();
        if (indent == previous) {
            if (!spaces.isEmpty()) {
                emit(hiddenAt(start + nlLen, end));
            }
        } else if (indent > previous) {
            indents.push(indent);
            emit(tokenAt(Python3Parser.INDENT, start + nlLen, end));
        } else {
            while (!indents.isEmpty() && indents.peek() > indent) {
                emit(tokenAt(Python3Parser.DEDENT, end + 1, end));
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
'''

parser_members = '''@parser::members {
    // Python3ParserBase (grammars-v4): both predicates are constant.
    public boolean CannotBePlusMinus() { return true; }
    public boolean CannotBeDotLpEq() { return true; }
}
'''

out = header(parser) + 'grammar Python3;\n\n' + tokens + '\n\n' + parser_members + '\n' + lexer_members + '\n'
out += '// ---- parser rules (grammars-v4 Python3Parser.g4) ----\n' + par_body.strip('\n') + '\n\n'
out += '// ---- lexer rules (grammars-v4 Python3Lexer.g4) ----\n' + lex_body.strip('\n') + '\n\n'
out += '/*<!vmf-text!>\nAutoLabel(enabled=true)\nRoot(rule=file_input)\n*/\n'
out += '// Source: https://github.com/antlr/grammars-v4/tree/master/python/python3 (Python3Lexer.g4 + Python3Parser.g4 + Java/Python3LexerBase.java, MIT licence above), combined for VMF-Text.\n'
os.makedirs(os.path.dirname(OUT), exist_ok=True)
open(OUT, 'w', encoding='utf-8', newline='\n').write(out)
print(f"wrote {OUT}: {out.count(chr(10))} lines, {n_skip} skip rule(s) moved to the hidden channel")
