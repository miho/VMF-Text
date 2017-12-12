grammar RuleMatcher;
prog:	expressions+=expr*;


expr:	left=expr operator=('*'|'/') right=expr ';'? {System.out.println("ALT-1:")}
    |	left=expr operator=('+'|'-') right=expr ';'? {System.out.println("ALT-2:")}
    |	value=DOUBLE                            ';'? {System.out.println("ALT-3:")}
    |	'(' expression = expr ')'                  ? {System.out.println("ALT-4:")}
    ;

expr2:	expr ('*'{System.out.println("OUT:*")}|'/'{System.out.println("OUT:/")}) expr ';'? {System.out.println("ALT-1:")};


DOUBLE :
         SIGN? DIGIT+ DOT DIGIT*
       | SIGN? DOT DIGIT+
       | SIGN? DIGIT+
       ;

fragment SIGN :'-' ;
fragment DIGIT : [0-9];
fragment DOT : '.' ;

IDENTIFIER: [a-zA-Z][a-zA-Z0-9]*;

WS
    : [ \t\r\n]+ -> channel(HIDDEN)
;

LINE_COMMENT
    : '//' ~[\r\n]* -> skip
;