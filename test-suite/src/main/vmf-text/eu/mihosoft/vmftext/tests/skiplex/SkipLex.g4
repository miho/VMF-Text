grammar SkipLex;

// #61: white-space and line comments are skipped by the lexer (-> skip), block
// comments go to the hidden channel. Skipped text is in no token at all; VMF-Text
// takes the trivia between two tokens from the input by character position, so
// both kinds round-trip, also when they alternate inside one gap.
unit: stmts+=stmt* EOF ;

stmt: name=ID '=' value=expr ';' ;

expr: terms+=term (ops+=('+' | '-') terms+=term)* ;

term
    : id=ID              # IdTerm
    | num=NUM            # NumTerm
    | '(' inner=expr ')' # ParenTerm
    ;

ID : [a-z]+ ;
NUM : [0-9]+ ;
WS : [ \t\r\n]+ -> skip ;
LINE_COMMENT : '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT : '/*' .*? '*/' -> channel(HIDDEN) ;
