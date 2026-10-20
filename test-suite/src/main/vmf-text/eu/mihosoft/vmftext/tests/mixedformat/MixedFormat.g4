grammar MixedFormat;

// A tiny C-like language for the mixed-mode formatting tests (#40): parsed
// text must stay byte-exact while programmatic subtrees are printed with
// block indentation by BlockIndentSeparatorPolicy.
unit: classes+=classDecl* EOF;

classDecl: 'class' name=ID '{' methods+=method* '}';

method: 'void' name=ID '(' ')' '{' stmts+=stmt* '}';

stmt: call=ID '(' ')' ';';

ID : [a-zA-Z]+ ;
WS : [ \t\r\n]+ -> channel(HIDDEN) ;
