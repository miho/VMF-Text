grammar BlockLiftManual;

// Hand-labeled grammar (no AutoLabel) with literal-only nested alternative
// blocks. Without lifting (#37) the chosen alternative is not in the model and
// the unparser drops the text: "int[] a << 1;" came back as "inta1;".
prog: stmts+=stmt+ EOF;

stmt: type=ID ('[' ']' | '[]')? name=ID ('<' '<' | '>' '>' '>' | '>' '>') value=INT ';';

ID  : [a-zA-Z]+ ;
INT : [0-9]+ ;
WS  : [ \t\r\n]+ -> channel(HIDDEN) ;
