grammar RuleMap;

// A single-alternative wrapper rule ('expression' -> ValueExpression) around
// 'numberLiteral'. The RuleMap below flattens the wrapper at its reference
// sites so that Program.expressions is a list of NumberLiteral directly.

program: (expressions+=expression ';')*;

expression:
   value = numberLiteral # valueExpression
;

numberLiteral:
   value = INT     # intLiteral
 | value = DOUBLE  # doubleLiteral
;

INT    : [0-9]+ ;
DOUBLE : [0-9]+ '.' [0-9]* ;

WS
    : [ \t\r\n]+ -> channel(HIDDEN)
;

/*<!vmf-text!>

RuleMap() {
  (first: ValueExpression -> second: NumberLiteral) = {
      'first.getValue()',
      'ValueExpression.newBuilder().withValue(second).build()'
  }
}

*/
