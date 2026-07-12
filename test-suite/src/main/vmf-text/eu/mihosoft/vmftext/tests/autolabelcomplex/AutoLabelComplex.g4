grammar AutoLabelComplex;

program
    : statement* EOF
    ;

statement
    : assignment
    | ifStatement
    | block
    | dims
    ;

assignment
    : IDENTIFIER '=' expression ';'
    ;

ifStatement
    : 'if' '(' expression ')' statement ('else' statement)?
    ;

block
    : '{' statement* '}'
    ;

expression
    : term (('+' | '-') term)*
    ;

term
    : factor (('*' | '/') factor)*
    ;

factor
    : INT
    | IDENTIFIER
    | '(' expression ')'
    | '[' expression (',' expression)* ']'
    ;

// a repeated multi-token block is not a token set, so it must not receive a
// single block label (ANTLR error 130); its literals are captured element-wise
dims
    : IDENTIFIER ('[' ']')* ';'
    ;

IDENTIFIER : [a-zA-Z_][a-zA-Z0-9_]* ;
INT : [0-9]+ ;
WS : [ \t\r\n]+ -> channel(HIDDEN) ;

/*<!vmf-text!>
AutoLabel(enabled=true)
*/
