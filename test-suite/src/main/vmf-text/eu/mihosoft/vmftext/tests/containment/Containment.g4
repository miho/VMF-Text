grammar Containment;

// Containment model: every rule-typed property of the generated model is a
// containment property whose opposite is CodeElement.parent. VMF maintains the
// parent, equals/hashCode are structural over the subtree (lexical metadata is
// ignored), setting a child into another parent releases it from the previous
// one, and traversal from an element stays within its subtree.
program: calls+=call* EOF ;

call: name=ID '(' (args+=argument (',' args+=argument)*)? ')' ';' ;

argument: (key=ID '=')? value=expr ;

expr: left=term (op=('+' | '-') right=term)? ;

term
    : id=ID              # IdTerm
    | num=NUM            # NumTerm
    | '(' inner=expr ')' # ParenTerm
    ;

ID : [a-z]+ ;
NUM : [0-9]+ ;
WS : [ \t\r\n]+ -> channel(HIDDEN) ;
