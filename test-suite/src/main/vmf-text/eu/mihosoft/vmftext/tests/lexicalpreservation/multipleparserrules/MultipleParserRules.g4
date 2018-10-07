grammar MultipleParserRules;

program: rules+=rule1+ EOF;

rule1 : left=INTEGER '+' right=INTEGER;


INTEGER :
         SIGN? DIGIT+
       ;

fragment SIGN :'-' ;
fragment DIGIT : [0-9];

IDENTIFIER: [a-zA-Z][a-zA-Z0-9]*;

WS
    : [ \t\r\n]+ -> channel(HIDDEN)
;

LINE_COMMENT
    : '//' ~[\r\n]* -> skip
;

/*<!vmf-text!>

TypeMap() {
  INT    -> java.lang.Integer via 'java.lang.Integer.parseInt(entry.getText())'
}

*/