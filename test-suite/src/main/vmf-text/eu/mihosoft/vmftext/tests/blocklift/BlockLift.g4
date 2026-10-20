grammar BlockLift;

prog: stmts+=stmt+ EOF;

// A nested block whose alternatives are partly literal-only. Auto-labeling
// lifts it into a synthetic rule 'stmtChoice' with one typed sub class per
// alternative; before, 'return 1;' / 'break;' unparsed as ';' because the
// chosen alternative left nothing in the model.
stmt: ('goto' ID | 'continue' | 'break' | 'return' expr? | 'echo' INT+ | 'with' ID ('.' | '->') ID) ';';

expr: INT;

ID  : [a-z]+ ;
INT : [0-9]+ ;
WS  : [ \t\r\n]+ -> channel(HIDDEN) ;

/*<!vmf-text!>
AutoLabel(enabled=true)
*/
