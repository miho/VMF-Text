grammar LexemeLang;

array: '(' values+=DOUBLE (',' values+=DOUBLE)* ')' EOF;

DOUBLE :
         SIGN? DIGIT+ DOT DIGIT*
       | SIGN? DOT DIGIT+
       | SIGN? DIGIT+
       ;
fragment SIGN :'-' ;
fragment DIGIT : [0-9];
fragment DOT : '.' ;
WS : [ \t\r\n]+ -> channel(HIDDEN) ;

/*<!vmf-text!>
TypeMap() {
  DOUBLE -> java.lang.Double  via 'java.lang.Double.parseDouble(entry.getText())'
}
*/
