grammar MultiAltList;

// Multi-alt rule: list shapes live on alt 1 and alt 2 (alt 0 has no list).
// Ensures ListShapeAnalyzer inspects every alternative, not only alt 0.
prog
    : 'flag' EOF
    | 'nums' '(' nums+=INT (',' nums+=INT)* ')' EOF
    | 'ids' ids+=ID (',' ids+=ID)* EOF
    ;

ID  : [a-zA-Z][a-zA-Z0-9]* ;
INT : [0-9]+ ;
WS  : [ \t\r\n]+ -> channel(HIDDEN) ;

/*<!vmf-text!>
TypeMap() {
  (INT -> java.lang.Integer) = 'java.lang.Integer.parseInt(entry.getText())'
}
*/
