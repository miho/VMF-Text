grammar RuleMapTokenBearing;

// Token-bearing wrapper ('expr' with '(' / ')') around 'numberLiteral'.
// The RuleMap flattens ParenExpr -> NumberLiteral; shell LexicalInfo on the
// target restores wrapper-owned trivia/terminals for byte-exact round-trip
// (issue #32).

program: (expressions+=expr ';')*;

expr:
   '(' value = numberLiteral ')' # parenExpr
;

numberLiteral:
   value = INT     # intLiteral
 | value = DOUBLE  # doubleLiteral
;

INT    : [0-9]+ ;
DOUBLE : [0-9]+ '.' [0-9]* ;

WS : [ \t\r\n]+ -> channel(HIDDEN) ;

COMMENT : '/*' .*? '*/' -> channel(HIDDEN) ;

/*<!vmf-text!>
RuleMap() {
  (first: ParenExpr -> second: NumberLiteral) = {
      'first.getValue()',
      'ParenExpr.newBuilder().withValue(second).build()'
  }
}
*/
