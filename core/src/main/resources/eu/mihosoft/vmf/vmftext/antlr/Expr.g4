grammar Expr;	
	
prog:   (expressions+=expr NEWLINE)*;

prefixUnaryOpExpr :  op=PREFIXUNARYOP  right=expr;
postfixUnaryOpExpr : left=expr op=PREFIXUNARYOP;

unaryOpExpr :  prefixUnaryOpExpr
            |  postfixUnaryOpExpr
            ;

constantIntegerExpr: value=INT  ;  // extends constantExpr
constantBooleanExpr: value=BOOL ;  // extends constantExpr

constantExpr : constantIntegerExpr
             | constantBooleanExpr
             ;


// expr -> Expression {
//   
// }
// 
expr:
        unaryOpExpr                 
    |   left=expr op=BINOP right=expr 
    |   constantExpr               
    |   '(' expr ')'                  
    ;


/*
expr:	left=expr op=UNARYOP              # unaryOpExpr
    |   left=expr op=BINOP right=expr     # binOpExpr
    |	constant=INT                      # constantExpr
    |   '(' expr ')'                      # 
    ;
*/
// expr:	expr '*' expr # times
//    |   expr '/' expr # div
//    |	expr '+' expr # plus
//    |   expr '-' expr # minus
//    |	INT           # constant
//    |	'(' expr ')'  # parantheses
//    ;

NEWLINE : [\r\n]+ ;
INT     : [0-9]+ ;
BINOP      : ('*'|'/'|'+'|'-');
PREFIXUNARYOP    : ('++'|'--'|'!');
POSTFIXUNARYOP    : ('++'|'--');

BOOL : 'true' | 'false';





