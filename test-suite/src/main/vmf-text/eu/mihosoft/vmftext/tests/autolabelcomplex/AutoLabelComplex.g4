grammar AutoLabelComplex;

program
    : statement* EOF
    ;

statement
    : assignment
    | ifStatement
    | block
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

IDENTIFIER : [a-zA-Z_][a-zA-Z0-9_]* ;
INT : [0-9]+ ;
WS : [ \t\r\n]+ -> channel(HIDDEN) ;

/*<!vmf-text!>
AutoLabel(enabled=true)
*/
