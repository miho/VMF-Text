/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 by Bart Kiers
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * Project      : python3-parser; an ANTLR4 grammar for Python 3
 *                https://github.com/bkiers/python3-parser
 * Developed by : Bart Kiers, bart@big-o.nl
 */

grammar Python3;

tokens {
    INDENT,
    DEDENT
}

@parser::members {
    // Python3ParserBase (grammars-v4): both predicates are constant.
    public boolean CannotBePlusMinus() { return true; }
    public boolean CannotBeDotLpEq() { return true; }
}

@lexer::members {
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

// ---- parser rules (grammars-v4 Python3Parser.g4) ----
// Insert here @header for C++ parser.

// All comments that start with "///" are copy-pasted from
// The Python Language Reference

single_input
    : NEWLINE
    | simple_stmts
    | compound_stmt NEWLINE
    ;

file_input
    : (NEWLINE | stmt)* EOF
    ;

eval_input
    : testlist NEWLINE* EOF
    ;

decorator
    : '@' dotted_name ('(' arglist? ')')? NEWLINE
    ;

decorators
    : decorator+
    ;

decorated
    : decorators (classdef | funcdef | async_funcdef)
    ;

async_funcdef
    : ASYNC funcdef
    ;

funcdef
    : 'def' name parameters ('->' test)? ':' block
    ;

parameters
    : '(' typedargslist? ')'
    ;

typedargslist
    : (
        tfpdef ('=' test)? (',' tfpdef ('=' test)?)* (
            ',' (
                '*' tfpdef? (',' tfpdef ('=' test)?)* (',' ('**' tfpdef ','?)?)?
                | '**' tfpdef ','?
            )?
        )?
        | '*' tfpdef? (',' tfpdef ('=' test)?)* (',' ('**' tfpdef ','?)?)?
        | '**' tfpdef ','?
    )
    ;

tfpdef
    : name (':' test)?
    ;

varargslist
    : (
        vfpdef ('=' test)? (',' vfpdef ('=' test)?)* (
            ',' (
                '*' vfpdef? (',' vfpdef ('=' test)?)* (',' ('**' vfpdef ','?)?)?
                | '**' vfpdef (',')?
            )?
        )?
        | '*' vfpdef? (',' vfpdef ('=' test)?)* (',' ('**' vfpdef ','?)?)?
        | '**' vfpdef ','?
    )
    ;

vfpdef
    : name
    ;

stmt
    : simple_stmts
    | compound_stmt
    ;

simple_stmts
    : simple_stmt (';' simple_stmt)* ';'? NEWLINE
    ;

simple_stmt
    : (
        expr_stmt
        | del_stmt
        | pass_stmt
        | flow_stmt
        | import_stmt
        | global_stmt
        | nonlocal_stmt
        | assert_stmt
    )
    ;

expr_stmt
    : testlist_star_expr (
        annassign
        | augassign (yield_expr | testlist)
        | ('=' (yield_expr | testlist_star_expr))*
    )
    ;

annassign
    : ':' test ('=' test)?
    ;

testlist_star_expr
    : (test | star_expr) (',' (test | star_expr))* ','?
    ;

augassign
    : (
        '+='
        | '-='
        | '*='
        | '@='
        | '/='
        | '%='
        | '&='
        | '|='
        | '^='
        | '<<='
        | '>>='
        | '**='
        | '//='
    )
    ;

// For normal and annotated assignments, additional restrictions enforced by the interpreter
del_stmt
    : 'del' exprlist
    ;

pass_stmt
    : 'pass'
    ;

flow_stmt
    : break_stmt
    | continue_stmt
    | return_stmt
    | raise_stmt
    | yield_stmt
    ;

break_stmt
    : 'break'
    ;

continue_stmt
    : 'continue'
    ;

return_stmt
    : 'return' testlist?
    ;

yield_stmt
    : yield_expr
    ;

raise_stmt
    : 'raise' (test ('from' test)?)?
    ;

import_stmt
    : import_name
    | import_from
    ;

import_name
    : 'import' dotted_as_names
    ;

// note below: the ('.' | '...') is necessary because '...' is tokenized as ELLIPSIS
import_from
    : (
        'from' (('.' | '...')* dotted_name | ('.' | '...')+) 'import' (
            '*'
            | '(' import_as_names ')'
            | import_as_names
        )
    )
    ;

import_as_name
    : name ('as' name)?
    ;

dotted_as_name
    : dotted_name ('as' name)?
    ;

import_as_names
    : import_as_name (',' import_as_name)* ','?
    ;

dotted_as_names
    : dotted_as_name (',' dotted_as_name)*
    ;

dotted_name
    : name ('.' name)*
    ;

global_stmt
    : 'global' name (',' name)*
    ;

nonlocal_stmt
    : 'nonlocal' name (',' name)*
    ;

assert_stmt
    : 'assert' test (',' test)?
    ;

compound_stmt
    : if_stmt
    | while_stmt
    | for_stmt
    | try_stmt
    | with_stmt
    | funcdef
    | classdef
    | decorated
    | async_stmt
    | match_stmt
    ;

async_stmt
    : ASYNC (funcdef | with_stmt | for_stmt)
    ;

if_stmt
    : 'if' test ':' block ('elif' test ':' block)* ('else' ':' block)?
    ;

while_stmt
    : 'while' test ':' block ('else' ':' block)?
    ;

for_stmt
    : 'for' exprlist 'in' testlist ':' block ('else' ':' block)?
    ;

try_stmt
    : (
        'try' ':' block (
            (except_clause ':' block)+ ('else' ':' block)? ('finally' ':' block)?
            | 'finally' ':' block
        )
    )
    ;

with_stmt
    : 'with' with_item (',' with_item)* ':' block
    ;

with_item
    : test ('as' expr)?
    ;

// NB compile.c makes sure that the default except clause is last
except_clause
    : 'except' (test ('as' name)?)?
    ;

block
    : simple_stmts
    | NEWLINE INDENT stmt+ DEDENT
    ;

match_stmt
    : 'match' subject_expr ':' NEWLINE INDENT case_block+ DEDENT
    ;

subject_expr
    : star_named_expression ',' star_named_expressions?
    | test
    ;

star_named_expressions
    : ',' star_named_expression+ ','?
    ;

star_named_expression
    : '*' expr
    | test
    ;

case_block
    : 'case' patterns guard? ':' block
    ;

guard
    : 'if' test
    ;

patterns
    : open_sequence_pattern
    | pattern
    ;

pattern
    : as_pattern
    | or_pattern
    ;

as_pattern
    : or_pattern 'as' pattern_capture_target
    ;

or_pattern
    : closed_pattern ('|' closed_pattern)*
    ;

closed_pattern
    : literal_pattern
    | capture_pattern
    | wildcard_pattern
    | value_pattern
    | group_pattern
    | sequence_pattern
    | mapping_pattern
    | class_pattern
    ;

literal_pattern
    : signed_number { this.CannotBePlusMinus() }?
    | complex_number
    | strings
    | 'None'
    | 'True'
    | 'False'
    ;

literal_expr
    : signed_number { this.CannotBePlusMinus() }?
    | complex_number
    | strings
    | 'None'
    | 'True'
    | 'False'
    ;

complex_number
    : signed_real_number '+' imaginary_number
    | signed_real_number '-' imaginary_number
    ;

signed_number
    : NUMBER
    | '-' NUMBER
    ;

signed_real_number
    : real_number
    | '-' real_number
    ;

real_number
    : NUMBER
    ;

imaginary_number
    : NUMBER
    ;

capture_pattern
    : pattern_capture_target
    ;

pattern_capture_target
    : /* cannot be '_' */ name { this.CannotBeDotLpEq() }?
    ;

wildcard_pattern
    : '_'
    ;

value_pattern
    : attr { this.CannotBeDotLpEq() }?
    ;

attr
    : name ('.' name)+
    ;

name_or_attr
    : attr
    | name
    ;

group_pattern
    : '(' pattern ')'
    ;

sequence_pattern
    : '[' maybe_sequence_pattern? ']'
    | '(' open_sequence_pattern? ')'
    ;

open_sequence_pattern
    : maybe_star_pattern ',' maybe_sequence_pattern?
    ;

maybe_sequence_pattern
    : maybe_star_pattern (',' maybe_star_pattern)* ','?
    ;

maybe_star_pattern
    : star_pattern
    | pattern
    ;

star_pattern
    : '*' pattern_capture_target
    | '*' wildcard_pattern
    ;

mapping_pattern
    : '{' '}'
    | '{' double_star_pattern ','? '}'
    | '{' items_pattern ',' double_star_pattern ','? '}'
    | '{' items_pattern ','? '}'
    ;

items_pattern
    : key_value_pattern (',' key_value_pattern)*
    ;

key_value_pattern
    : (literal_expr | attr) ':' pattern
    ;

double_star_pattern
    : '**' pattern_capture_target
    ;

class_pattern
    : name_or_attr '(' ')'
    | name_or_attr '(' positional_patterns ','? ')'
    | name_or_attr '(' keyword_patterns ','? ')'
    | name_or_attr '(' positional_patterns ',' keyword_patterns ','? ')'
    ;

positional_patterns
    : pattern (',' pattern)*
    ;

keyword_patterns
    : keyword_pattern (',' keyword_pattern)*
    ;

keyword_pattern
    : name '=' pattern
    ;

test
    : or_test ('if' or_test 'else' test)?
    | lambdef
    ;

test_nocond
    : or_test
    | lambdef_nocond
    ;

lambdef
    : 'lambda' varargslist? ':' test
    ;

lambdef_nocond
    : 'lambda' varargslist? ':' test_nocond
    ;

or_test
    : and_test ('or' and_test)*
    ;

and_test
    : not_test ('and' not_test)*
    ;

not_test
    : 'not' not_test
    | comparison
    ;

comparison
    : expr (comp_op expr)*
    ;

// <> isn't actually a valid comparison operator in Python. It's here for the
// sake of a __future__ import described in PEP 401 (which really works :-)
comp_op
    : '<'
    | '>'
    | '=='
    | '>='
    | '<='
    | '<>'
    | '!='
    | 'in'
    | 'not' 'in'
    | 'is'
    | 'is' 'not'
    ;

star_expr
    : '*' expr
    ;

expr
    : atom_expr
    | expr '**' expr
    | ('+' | '-' | '~')+ expr
    | expr ('*' | '@' | '/' | '%' | '//') expr
    | expr ('+' | '-') expr
    | expr ('<<' | '>>') expr
    | expr '&' expr
    | expr '^' expr
    | expr '|' expr
    ;

//expr: xor_expr ('|' xor_expr)*;
//xor_expr: and_expr ('^' and_expr)*;
//and_expr: shift_expr ('&' shift_expr)*;
//shift_expr: arith_expr (('<<'|'>>') arith_expr)*;
//arith_expr: term (('+'|'-') term)*;
//term: factor (('*'|'@'|'/'|'%'|'//') factor)*;
//factor: ('+'|'-'|'~') factor | power;
//power: atom_expr ('**' factor)?;
atom_expr
    : AWAIT? atom trailer*
    ;

atom
    : '(' (yield_expr | testlist_comp)? ')'
    | '[' testlist_comp? ']'
    | '{' dictorsetmaker? '}'
    | name
    | NUMBER
    | STRING+
    | '...'
    | 'None'
    | 'True'
    | 'False'
    ;

name
    : NAME
    | '_'
    | 'match'
    ;

testlist_comp
    : (test | star_expr) (comp_for | (',' (test | star_expr))* ','?)
    ;

trailer
    : '(' arglist? ')'
    | '[' subscriptlist ']'
    | '.' name
    ;

subscriptlist
    : subscript_ (',' subscript_)* ','?
    ;

subscript_
    : test
    | test? ':' test? sliceop?
    ;

sliceop
    : ':' test?
    ;

exprlist
    : (expr | star_expr) (',' (expr | star_expr))* ','?
    ;

testlist
    : test (',' test)* ','?
    ;

dictorsetmaker
    : (
        ((test ':' test | '**' expr) (comp_for | (',' (test ':' test | '**' expr))* ','?))
        | ((test | star_expr) (comp_for | (',' (test | star_expr))* ','?))
    )
    ;

classdef
    : 'class' name ('(' arglist? ')')? ':' block
    ;

arglist
    : argument (',' argument)* ','?
    ;

// The reason that keywords are test nodes instead of NAME is that using NAME
// results in an ambiguity. ast.c makes sure it's a NAME.
// "test '=' test" is really "keyword '=' test", but we have no such token.
// These need to be in a single rule to avoid grammar that is ambiguous
// to our LL(1) parser. Even though 'test' includes '*expr' in star_expr,
// we explicitly match '*' here, too, to give it proper precedence.
// Illegal combinations and orderings are blocked in ast.c:
// multiple (test comp_for) arguments are blocked; keyword unpackings
// that precede iterable unpackings are blocked; etc.
argument
    : (test comp_for? | test '=' test | '**' test | '*' test)
    ;

comp_iter
    : comp_for
    | comp_if
    ;

comp_for
    : ASYNC? 'for' exprlist 'in' or_test comp_iter?
    ;

comp_if
    : 'if' test_nocond comp_iter?
    ;

// not used in grammar, but may appear in "node" passed from Parser to Compiler
encoding_decl
    : name
    ;

yield_expr
    : 'yield' yield_arg?
    ;

yield_arg
    : 'from' test
    | testlist
    ;

strings
    : STRING+
    ;

// ---- lexer rules (grammars-v4 Python3Lexer.g4) ----
// All comments that start with "///" are copy-pasted from
// The Python Language Reference




// Insert here @header for C++ lexer.

/*
 * lexer rules
 */

STRING: STRING_LITERAL | BYTES_LITERAL;

NUMBER: INTEGER | FLOAT_NUMBER | IMAG_NUMBER;

INTEGER: DECIMAL_INTEGER | OCT_INTEGER | HEX_INTEGER | BIN_INTEGER;

AND        : 'and';
AS         : 'as';
ASSERT     : 'assert';
ASYNC      : 'async';
AWAIT      : 'await';
BREAK      : 'break';
CASE       : 'case';
CLASS      : 'class';
CONTINUE   : 'continue';
DEF        : 'def';
DEL        : 'del';
ELIF       : 'elif';
ELSE       : 'else';
EXCEPT     : 'except';
FALSE      : 'False';
FINALLY    : 'finally';
FOR        : 'for';
FROM       : 'from';
GLOBAL     : 'global';
IF         : 'if';
IMPORT     : 'import';
IN         : 'in';
IS         : 'is';
LAMBDA     : 'lambda';
MATCH      : 'match';
NONE       : 'None';
NONLOCAL   : 'nonlocal';
NOT        : 'not';
OR         : 'or';
PASS       : 'pass';
RAISE      : 'raise';
RETURN     : 'return';
TRUE       : 'True';
TRY        : 'try';
UNDERSCORE : '_';
WHILE      : 'while';
WITH       : 'with';
YIELD      : 'yield';

NEWLINE: ({this.atStartOfInput()}? SPACES | ( '\r'? '\n' | '\r' | '\f') SPACES?) {this.onNewLine();};

/// identifier   ::=  id_start id_continue*
NAME: ID_START ID_CONTINUE*;

/// stringliteral   ::=  [stringprefix](shortstring | longstring)
/// stringprefix    ::=  "r" | "u" | "R" | "U" | "f" | "F"
///                      | "fr" | "Fr" | "fR" | "FR" | "rf" | "rF" | "Rf" | "RF"
STRING_LITERAL: ( [rR] | [uU] | [fF] | ( [fF] [rR]) | ( [rR] [fF]))? ( SHORT_STRING | LONG_STRING);

/// bytesliteral   ::=  bytesprefix(shortbytes | longbytes)
/// bytesprefix    ::=  "b" | "B" | "br" | "Br" | "bR" | "BR" | "rb" | "rB" | "Rb" | "RB"
BYTES_LITERAL: ( [bB] | ( [bB] [rR]) | ( [rR] [bB])) ( SHORT_BYTES | LONG_BYTES);

/// decimalinteger ::=  nonzerodigit digit* | "0"+
DECIMAL_INTEGER: NON_ZERO_DIGIT DIGIT* | '0'+;

/// octinteger     ::=  "0" ("o" | "O") octdigit+
OCT_INTEGER: '0' [oO] OCT_DIGIT+;

/// hexinteger     ::=  "0" ("x" | "X") hexdigit+
HEX_INTEGER: '0' [xX] HEX_DIGIT+;

/// bininteger     ::=  "0" ("b" | "B") bindigit+
BIN_INTEGER: '0' [bB] BIN_DIGIT+;

/// floatnumber   ::=  pointfloat | exponentfloat
FLOAT_NUMBER: POINT_FLOAT | EXPONENT_FLOAT;

/// imagnumber ::=  (floatnumber | intpart) ("j" | "J")
IMAG_NUMBER: ( FLOAT_NUMBER | INT_PART) [jJ];

DOT                : '.';
ELLIPSIS           : '...';
STAR               : '*';
OPEN_PAREN         : '(' {this.openBrace();};
CLOSE_PAREN        : ')' {this.closeBrace();};
COMMA              : ',';
COLON              : ':';
SEMI_COLON         : ';';
POWER              : '**';
ASSIGN             : '=';
OPEN_BRACK         : '[' {this.openBrace();};
CLOSE_BRACK        : ']' {this.closeBrace();};
OR_OP              : '|';
XOR                : '^';
AND_OP             : '&';
LEFT_SHIFT         : '<<';
RIGHT_SHIFT        : '>>';
ADD                : '+';
MINUS              : '-';
DIV                : '/';
MOD                : '%';
IDIV               : '//';
NOT_OP             : '~';
OPEN_BRACE         : '{' {this.openBrace();};
CLOSE_BRACE        : '}' {this.closeBrace();};
LESS_THAN          : '<';
GREATER_THAN       : '>';
EQUALS             : '==';
GT_EQ              : '>=';
LT_EQ              : '<=';
NOT_EQ_1           : '<>';
NOT_EQ_2           : '!=';
AT                 : '@';
ARROW              : '->';
ADD_ASSIGN         : '+=';
SUB_ASSIGN         : '-=';
MULT_ASSIGN        : '*=';
AT_ASSIGN          : '@=';
DIV_ASSIGN         : '/=';
MOD_ASSIGN         : '%=';
AND_ASSIGN         : '&=';
OR_ASSIGN          : '|=';
XOR_ASSIGN         : '^=';
LEFT_SHIFT_ASSIGN  : '<<=';
RIGHT_SHIFT_ASSIGN : '>>=';
POWER_ASSIGN       : '**=';
IDIV_ASSIGN        : '//=';

SKIP_: ( SPACES | COMMENT | LINE_JOINING) -> channel(HIDDEN);

UNKNOWN_CHAR: .;

/*
 * fragments
 */

/// shortstring     ::=  "'" shortstringitem* "'" | '"' shortstringitem* '"'
/// shortstringitem ::=  shortstringchar | stringescapeseq
/// shortstringchar ::=  <any source character except "\" or newline or the quote>
fragment SHORT_STRING:
    '\'' (STRING_ESCAPE_SEQ | ~[\\\r\n\f'])* '\''
    | '"' ( STRING_ESCAPE_SEQ | ~[\\\r\n\f"])* '"'
;
/// longstring      ::=  "'''" longstringitem* "'''" | '"""' longstringitem* '"""'
fragment LONG_STRING: '\'\'\'' LONG_STRING_ITEM*? '\'\'\'' | '"""' LONG_STRING_ITEM*? '"""';

/// longstringitem  ::=  longstringchar | stringescapeseq
fragment LONG_STRING_ITEM: LONG_STRING_CHAR | STRING_ESCAPE_SEQ;

/// longstringchar  ::=  <any source character except "\">
fragment LONG_STRING_CHAR: ~'\\';

/// stringescapeseq ::=  "\" <any source character>
fragment STRING_ESCAPE_SEQ: '\\' . | '\\' NEWLINE;

/// nonzerodigit   ::=  "1"..."9"
fragment NON_ZERO_DIGIT: [1-9];

/// digit          ::=  "0"..."9"
fragment DIGIT: [0-9];

/// octdigit       ::=  "0"..."7"
fragment OCT_DIGIT: [0-7];

/// hexdigit       ::=  digit | "a"..."f" | "A"..."F"
fragment HEX_DIGIT: [0-9a-fA-F];

/// bindigit       ::=  "0" | "1"
fragment BIN_DIGIT: [01];

/// pointfloat    ::=  [intpart] fraction | intpart "."
fragment POINT_FLOAT: INT_PART? FRACTION | INT_PART '.';

/// exponentfloat ::=  (intpart | pointfloat) exponent
fragment EXPONENT_FLOAT: ( INT_PART | POINT_FLOAT) EXPONENT;

/// intpart       ::=  digit+
fragment INT_PART: DIGIT+;

/// fraction      ::=  "." digit+
fragment FRACTION: '.' DIGIT+;

/// exponent      ::=  ("e" | "E") ["+" | "-"] digit+
fragment EXPONENT: [eE] [+-]? DIGIT+;

/// shortbytes     ::=  "'" shortbytesitem* "'" | '"' shortbytesitem* '"'
/// shortbytesitem ::=  shortbyteschar | bytesescapeseq
fragment SHORT_BYTES:
    '\'' (SHORT_BYTES_CHAR_NO_SINGLE_QUOTE | BYTES_ESCAPE_SEQ)* '\''
    | '"' ( SHORT_BYTES_CHAR_NO_DOUBLE_QUOTE | BYTES_ESCAPE_SEQ)* '"'
;

/// longbytes      ::=  "'''" longbytesitem* "'''" | '"""' longbytesitem* '"""'
fragment LONG_BYTES: '\'\'\'' LONG_BYTES_ITEM*? '\'\'\'' | '"""' LONG_BYTES_ITEM*? '"""';

/// longbytesitem  ::=  longbyteschar | bytesescapeseq
fragment LONG_BYTES_ITEM: LONG_BYTES_CHAR | BYTES_ESCAPE_SEQ;

/// shortbyteschar ::=  <any ASCII character except "\" or newline or the quote>
fragment SHORT_BYTES_CHAR_NO_SINGLE_QUOTE:
    [\u0000-\u0009]
    | [\u000B-\u000C]
    | [\u000E-\u0026]
    | [\u0028-\u005B]
    | [\u005D-\u007F]
;

fragment SHORT_BYTES_CHAR_NO_DOUBLE_QUOTE:
    [\u0000-\u0009]
    | [\u000B-\u000C]
    | [\u000E-\u0021]
    | [\u0023-\u005B]
    | [\u005D-\u007F]
;

/// longbyteschar  ::=  <any ASCII character except "\">
fragment LONG_BYTES_CHAR: [\u0000-\u005B] | [\u005D-\u007F];

/// bytesescapeseq ::=  "\" <any ASCII character>
fragment BYTES_ESCAPE_SEQ: '\\' [\u0000-\u007F];

fragment SPACES: [ \t]+;

fragment COMMENT: '#' ~[\r\n\f]*;

fragment LINE_JOINING: '\\' SPACES? ( '\r'? '\n' | '\r' | '\f');

// TODO: ANTLR seems lack of some Unicode property support...
//$ curl https://www.unicode.org/Public/13.0.0/ucd/PropList.txt | grep Other_ID_
//1885..1886    ; Other_ID_Start # Mn   [2] MONGOLIAN LETTER ALI GALI BALUDA..MONGOLIAN LETTER ALI GALI THREE BALUDA
//2118          ; Other_ID_Start # Sm       SCRIPT CAPITAL P
//212E          ; Other_ID_Start # So       ESTIMATED SYMBOL
//309B..309C    ; Other_ID_Start # Sk   [2] KATAKANA-HIRAGANA VOICED SOUND MARK..KATAKANA-HIRAGANA SEMI-VOICED SOUND MARK
//00B7          ; Other_ID_Continue # Po       MIDDLE DOT
//0387          ; Other_ID_Continue # Po       GREEK ANO TELEIA
//1369..1371    ; Other_ID_Continue # No   [9] ETHIOPIC DIGIT ONE..ETHIOPIC DIGIT NINE
//19DA          ; Other_ID_Continue # No       NEW TAI LUE THAM DIGIT ONE

fragment UNICODE_OIDS: '\u1885' ..'\u1886' | '\u2118' | '\u212e' | '\u309b' ..'\u309c';

fragment UNICODE_OIDC: '\u00b7' | '\u0387' | '\u1369' ..'\u1371' | '\u19da';

/// id_start     ::=  <all characters in general categories Lu, Ll, Lt, Lm, Lo, Nl, the underscore, and characters with the Other_ID_Start property>
fragment ID_START:
    '_'
    | [\p{L}]
    | [\p{Nl}]
    //| [\p{Other_ID_Start}]
    | UNICODE_OIDS
;

/// id_continue  ::=  <all characters in id_start, plus characters in the categories Mn, Mc, Nd, Pc and others with the Other_ID_Continue property>
fragment ID_CONTINUE:
    ID_START
    | [\p{Mn}]
    | [\p{Mc}]
    | [\p{Nd}]
    | [\p{Pc}]
    //| [\p{Other_ID_Continue}]
    | UNICODE_OIDC
;

/*<!vmf-text!>
AutoLabel(enabled=true)
Root(rule=file_input)
*/
// Source: https://github.com/antlr/grammars-v4/tree/master/python/python3 (Python3Lexer.g4 + Python3Parser.g4 + Java/Python3LexerBase.java, MIT licence above), combined for VMF-Text.
