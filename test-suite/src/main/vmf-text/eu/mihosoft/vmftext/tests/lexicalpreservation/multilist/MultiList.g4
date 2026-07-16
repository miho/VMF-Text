grammar MultiList;

// Two bare delimited primitive lists on one rule — needs list-shape hints
// so splicing one list does not clear the other.
// Shape: ids+=ID (',' ids+=ID)* ';' nums+=INT (',' nums+=INT)* EOF
prog: ids+=ID (',' ids+=ID)* ';' nums+=INT (',' nums+=INT)* EOF;

ID  : [a-zA-Z][a-zA-Z0-9]* ;
INT : [0-9]+ ;

WS : [ \t\r\n]+ -> channel(HIDDEN) ;

/*<!vmf-text!>
TypeMap() {
  (INT -> java.lang.Integer) = 'java.lang.Integer.parseInt(entry.getText())'
}
*/
