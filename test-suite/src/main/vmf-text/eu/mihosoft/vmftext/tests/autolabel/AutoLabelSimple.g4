grammar AutoLabelSimple;

program
    : statement+ EOF
    ;

statement
    : IDENTIFIER '=' value ';'
    ;

value
    : INT
    | IDENTIFIER
    ;

IDENTIFIER
    : [a-zA-Z_] [a-zA-Z0-9_]*
    ;

INT
    : [0-9]+
    ;

WS
    : [ \t\r\n]+ -> channel(HIDDEN)
    ;

/*<!vmf-text!>
AutoLabel(enabled=true)
*/
