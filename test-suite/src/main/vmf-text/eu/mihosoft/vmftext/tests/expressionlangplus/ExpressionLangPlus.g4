grammar ExpressionLangPlus;
prog:   (expressions+=expr ';'?)*;

expr:	left=expr operator=('*'|'/') right=expr   # MultDivOpExpr
    |	left=expr operator=('+'|'-') right=expr   # PlusMinusOpExpr
    |	value=DOUBLE                              # NumberExpr
    |	arrayValue=array                          # ArrayExpr
    |	variable=IDENTIFIER '['index=INT']'       # ArrayIndexExpr
    |   name=IDENTIFIER '('params=paramList?')'   # MethodCallExpr
    |   variable=IDENTIFIER                       # VariableExpr
    |   variable=IDENTIFIER '=' expression=expr   # AssignmentExpr
    |	'(' expression = expr ')'                # ParanExpr
    ;

paramList: elements+=expr (',' elements+=expr)*;

array: '{' (elements+=DOUBLE) (',' (elements+=DOUBLE))* '}'
     ;

INT : DIGIT+ ;

DOUBLE :
         DIGIT+ DOT DIGIT*
       | DOT DIGIT+
       ;

fragment DIGIT : [0-9];
fragment DOT : '.' ;

IDENTIFIER: [a-zA-Z][a-zA-Z0-9]*;

WS
    : [ \t\r\n]+ -> channel(HIDDEN)
;

LINE_COMMENT
    : '//' ~[\r\n]* -> skip
;



/*<!vmf-text!>

TypeMap() {
  INT    -> java.lang.Integer via 'java.lang.Integer.parseInt(entry.getText())'
  DOUBLE -> java.lang.Double  via 'java.lang.Double.parseDouble(entry.getText())'
}

*/

/*<!vmf-text!>

interface EmptyExpr extends Expr {

}

@InterfaceOnly
interface OpExpr extends Expr {
  String getOperator();
  Expr getLeft();
  Expr getRight();
}

interface MultDivOpExpr extends OpExpr {

}

interface PlusMinusOpExpr extends OpExpr {

}

*/






