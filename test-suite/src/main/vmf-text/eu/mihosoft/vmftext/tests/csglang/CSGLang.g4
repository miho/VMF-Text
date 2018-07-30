grammar CSGLang;

@members {
  //
}

geometry: statements+=statement* EOF ;

statement:
          pointObject = point        # pointStatement
       |  shapeObject = shape        # shapeStatement
       |  comment     = LINE_COMMENT # commentStatement
       ;

point  :
         'point'   name=IDENTIFIER + '(' ('anchor:')? anchor=IDENTIFIER  ',' ('x:')? x=DOUBLE ',' ('y:')? y=DOUBLE ')'
       | 'point'   name=IDENTIFIER + '('                                     ('x:')? x=DOUBLE ',' ('y:')? y=DOUBLE ')'
       ;

shape :
         'rect'    name=IDENTIFIER + '(' ('p1:')? p1=IDENTIFIER? ',' ('p2:')? p2=IDENTIFIER? ')'                     # rectangle
       | 'circle'  name=IDENTIFIER + '(' ('p1:')? p1=IDENTIFIER? ',' ('p2:')? p2=IDENTIFIER? ')'                     # circle
       | 'shape'   name=IDENTIFIER + '(' ('p:')?  p1=IDENTIFIER? ',' ('expression:')? expression=cSGExpression ')'   # cSG
       | 'fxShape'                                                                                                   # fxShape
       ;

cSGExpression : left=cSGExpression '+' right=cSGExpression # UnionOperatorNode
     | left=cSGExpression '-' right=cSGExpression          # DifferenceOperatorNode
     | left=cSGExpression ':' right=cSGExpression          # IntersectionOperatorNode
     | '(' inner=cSGExpression ')'                         # ParanNode
     | object=shape                                        # CSGNode
     | objectName=IDENTIFIER                               # CSGVarNode
     ;

// for checking JavaFX text fields etc. via matchIdentifierAlt0(...)
identifier: text=IDENTIFIER;

LINE_COMMENT
    : '//' ~[\r\n]*
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

//LINE_COMMENT
//    : '//' ~[\r\n]* -> skip
//;

/*<!vmf-text!>

TypeMap() {
  INT    -> java.lang.Integer via 'java.lang.Integer.parseInt(entry.getText())'

  DOUBLE -> java.lang.Double  via (
      'java.lang.Double.parseDouble(entry.getText())',
      'String.format(java.util.Locale.US,"%.2f",entry)'
  )

//  INACTIVE -> java.lang.Boolean via (
//      '"--".equals(entry.getText())',
//      'entry?"--":""'
//      )
}

*/

/*<!vmf-text!>

@InterfaceOnly
interface WithName {
    String getName();
}

interface Shape extends WithName{
}

interface Point extends WithName{
}


interface Geometry {
    @Contains(opposite="root")
    Statement[] getStatements();
}

interface Statement {
    @Container(opposite="statements")
    Geometry getRoot();
}

interface PointStatement {
    @Contains(opposite="root")
    Point getPointObject();
}

interface ShapeStatement {
    @Contains(opposite="root")
    Shape getShapeObject();
}

interface Point {
  @Container(opposite="pointObject")
  PointStatement getRoot();

  //@DelegateTo(className="eu.mihosoft.vmftext.tutorial.PointDelegate")
  //String toString();
}

interface Bounds {
  Point getP1();
  Point getP2();
}

interface Shape {
  Object getUserData();

  // @DelegateTo(className="eu.mihosoft.vmftext.tutorial.ShapeDelegate")
  // Bounds bounds();

  Bounds getBounds();


  @Container(opposite="shapeObject")
  ShapeStatement getRoot();
}

*/