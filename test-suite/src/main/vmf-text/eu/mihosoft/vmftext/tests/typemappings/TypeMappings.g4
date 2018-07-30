grammar TypeMappings;

program: (expressions+=expression ';')*;

expression:
   value = numberLiteral # valueExpression
;

numberLiteral:
   value = INT     # intLiteral
 | value = DOUBLE  # doubleLiteral
;

DOUBLE :
         SIGN? DIGIT+ DOT DIGIT*
       | SIGN? DOT DIGIT+
       | SIGN? DIGIT+
       ;

fragment SIGN :'-'|'+' ;
fragment DIGIT : [0-9];
fragment DOT : '.' ;

INT   : [0-9]+ ;

IDENTIFIER: [a-zA-Z][a-zA-Z0-9]*;

WS
    : [ \t\r\n]+ -> channel(HIDDEN)
;

/*<!vmf-text!>

TypeMap() {
  (rule: INT    -> type: java.lang.Integer) = {
      toType:   'java.lang.Integer.parseInt(entry.getText())',
      toString: 'entry.toString()'
  }
}


//RuleMap() {
//  (first: ValueExpression    -> second: NumberLiteral) = {
//      'first.getValue()',
//      'ValueExpression.newBuilder().withValue(second).build()'
//  }
//}


*/

/*<!vmf-text!>

//interface NumberLiteral extends ValueExpression {
//}

*/


