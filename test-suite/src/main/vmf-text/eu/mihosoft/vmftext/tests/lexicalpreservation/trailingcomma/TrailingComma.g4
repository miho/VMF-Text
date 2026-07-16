grammar TrailingComma;

// Parenthesized primitive list with optional trailing comma.
prog: '(' values+=INT (',' values+=INT)* ','? ')' EOF;

INT : [0-9]+ ;
WS  : [ \t\r\n]+ -> channel(HIDDEN) ;

/*<!vmf-text!>
TypeMap() {
  (INT -> java.lang.Integer) = 'java.lang.Integer.parseInt(entry.getText())'
}
*/
