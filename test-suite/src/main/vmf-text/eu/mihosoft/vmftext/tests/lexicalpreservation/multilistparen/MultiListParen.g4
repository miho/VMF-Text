grammar MultiListParen;

// Inter-list trailer + opener for the second list:
// ids… ';' '(' nums… ')' — analyzer assigns '(' to nums.prefixCount.
prog: ids+=ID (',' ids+=ID)* ';' '(' nums+=INT (',' nums+=INT)* ')' EOF;

ID  : [a-zA-Z][a-zA-Z0-9]* ;
INT : [0-9]+ ;

WS : [ \t\r\n]+ -> channel(HIDDEN) ;

/*<!vmf-text!>
TypeMap() {
  (INT -> java.lang.Integer) = 'java.lang.Integer.parseInt(entry.getText())'
}
*/
