grammar Asi;

// Regressions from the JavaScript (ECMAScript) grammar trial,
// experiments/js-grammar-trial:
//  - a semantic predicate {...}? is not an optional element: the grammar
//    rewrites must not wrap it as ({...} {flag})?, which turned it into a
//    plain action and broke the generated unparser grammar ("not a statement");
//  - @parser::members reach the generated unparser grammar (it imports this
//    grammar), so the predicate compiles there as well;
//  - a nested rule that matches EOF (eos below: JavaScript's automatic
//    semicolon insertion) owns the hidden text in front of the end of the
//    input; the unparser must emit it or "x = 1\n" loses its newline.

@parser::members {
    /** true if a line terminator sits on the hidden channel right before the current token */
    boolean lineTerminatorAhead() {
        for (int i = getCurrentToken().getTokenIndex() - 1; i >= 0; i--) {
            org.antlr.v4.runtime.Token t = _input.get(i);
            if (t.getChannel() != org.antlr.v4.runtime.Lexer.HIDDEN) return false;
            if (t.getType() == NL) return true;
        }
        return false;
    }
}

program: statements+=statement* EOF;

statement: name=ID '=' value=INT end=eos;

eos
    : ';'                       # EosSemi
    | EOF                       # EosEof
    | {lineTerminatorAhead()}?  # EosLine
    ;

ID : [a-zA-Z]+ ;
INT: [0-9]+ ;
NL : [\r\n]+ -> channel(HIDDEN) ;
WS : [ \t]+ -> channel(HIDDEN) ;
