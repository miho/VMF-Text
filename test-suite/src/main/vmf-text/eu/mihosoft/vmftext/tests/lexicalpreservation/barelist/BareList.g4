grammar BareList;

// Bare delimited list (no brackets): item (',' item)* EOF
// Trivia footprint: size == 2*N + 1  (N values + (N-1) commas + EOF + pad)
bareList: items+=INT (',' items+=INT)* EOF;

INT: SIGN? DIGIT+ ;

fragment SIGN : '-' ;
fragment DIGIT : [0-9] ;

WS : [ \t\r\n]+ -> channel(HIDDEN) ;

/*<!vmf-text!>
TypeMap() {
  (INT -> java.lang.Integer) = 'java.lang.Integer.parseInt(entry.getText())'
}
*/
