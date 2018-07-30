grammar CSGLang;

@members {
  //
}

geometry: statements+=statement* 'result' '=' result=IDENTIFIER EOF ;

statement:
          pointObject = point     # pointStatement
       |  shapeObject = shape     # shapeStatement
       ;

point  :
          'point'   name=IDENTIFIER + '(' ('anchor:')? anchor=IDENTIFIER   ',' ('x:')? x=DOUBLE ',' ('y:')? y=DOUBLE ')'
       |  'point'   name=IDENTIFIER + '('                                      ('x:')? x=DOUBLE ',' ('y:')? y=DOUBLE ')'
       ;

shape :
         'rect'    name=IDENTIFIER + '(' ('p1:')? p1=IDENTIFIER? ',' ('p2:')? p2=IDENTIFIER? ')' # rectangle
       | 'circle'  name=IDENTIFIER + '(' ('p1:')? p1=IDENTIFIER? ',' ('p2:')? p2=IDENTIFIER? ')' # circle
       | 'shape'   name=IDENTIFIER + '('  expression=cSGExpression ')'                           # cSG
       ;

cSGExpression : left=cSGExpression '+' right=cSGExpression # UnionOperatorNode
     | left=cSGExpression '-' right=cSGExpression          # DifferenceOperatorNode
     | left=cSGExpression ':' right=cSGExpression          # IntersectionOperatorNode
     | '(' inner=cSGExpression ')'                         # ParanNode
     | object=shape                                        # CsgNode
     | objectName=IDENTIFIER                               # CsgVarNode
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

LINE_COMMENT
    : '//' ~[\r\n]* -> skip
;

/*<!vmf-text!>

TypeMap() {
  INT    -> java.lang.Integer via "java.lang.Integer.parseInt(entry.getText())"
  DOUBLE -> java.lang.Double  via "java.lang.Double.parseDouble(entry.getText())"
}

*/

/*<!vmf-text!>

interface Bounds {
  Point getP1();
  Point getP2();
}

interface Shape {

  String getUserData();

  @DelegateTo(className="eu.mihosoft.vmftext.tutorial.ShapeDelegate")
  Bounds bounds();
}

*/