grammar RuleMatcher;
prog:	expressions+=expr*;

// expr: name=IDENTIFIER;


expr:	left=expr operator=('*'|'/') right=expr      # MultDivOpExpr
    |	left=expr operator=('+'|'-') right=expr      # PlusMinusOpExpr
    |	value=DOUBLE                                 # NumberExpr
    |	'(' expression = expr ')'                    # ParanExpr
    ;

nested locals[int xyz, double abc] :	'(' ('id' id=DOUBLE | 'name' name=IDENTIFIER ) ')' ';'?
    |   '('  'otherName' otherName=IDENTIFIER ')' ';'?
    ;

nested2: '(' ('a''b' | 'c');

array:
        '(' values+=DOUBLE (',' values+=DOUBLE)* ')'
    ;

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