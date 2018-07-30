grammar GrammarVMF;


prog:   (expressions+=expr NEWLINE)*;


/**

classes:

prog {
    @Contains(opposite="parent")
    Expr[] getExpressions();
}

expr {
   @Container(opposite="expressions")
   prog getParent();
}


operatorExpr {
  Operator getOp();
}

constantExpr {
  Object getValue();
}

operator {

}

*/

expr:
        op=PREFIXUNARYOP  expression=expr  # prefixUnaryOpExpr   // extends unaryOpExpr extends operatorExpr
    |   expression=expr op=PREFIXUNARYOP   # postfixUnaryOpExpr  // extends unaryOpExpr extends operatorExpr
    |   left=expr op=BINOP right=expr      # binaryOperatorExpr  // extends operatorExpr
    |   value=INT                          # constantIntegerExpr // extends constantExpr
    |   value=BOOL                         # constantBooleanExpr // extends constantExpr
    |   '(' expr ')'                       # paranExpr           // ignore
    ;

/**
mappings:
INT             -> int
BOOL            -> boolean
BINOP           -> Operator
PREFIXUNARYOP   -> Operator
POSTFIXUNARYOP  -> Operator
*/

NEWLINE : [\r\n]+ ;
INT     : [0-9]+ ;
BINOP      : ('*'|'/'|'+'|'-');
PREFIXUNARYOP    : ('++'|'--'|'!');
POSTFIXUNARYOP    : ('++'|'--');

BOOL : 'true' | 'false';