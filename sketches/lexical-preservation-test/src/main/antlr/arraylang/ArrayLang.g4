grammar ArrayLang;

@header {
package arraylang;
}

array:  '(' values+=DOUBLE (',' values+=DOUBLE)* ')' EOF;

DOUBLE :
         SIGN? DIGIT+ DOT DIGIT*
       | SIGN? DOT DIGIT+
       | SIGN? DIGIT+
       ;

SIGN :'-' ;
DIGIT : [0-9];
DOT : '.' ;

IDENTIFIER: [a-zA-Z][a-zA-Z0-9]*;

WS
    : [ \t\r\n]+ -> channel(HIDDEN)
;

TO_IGNORE
    : '_'+ -> channel(HIDDEN)
;



/*<!vmf-text!>

TypeMap() {
  INT    -> java.lang.Integer via 'java.lang.Integer.parseInt(entry.getText())'
  DOUBLE -> java.lang.Double  via 'java.lang.Double.parseDouble(entry.getText())'
}

*/