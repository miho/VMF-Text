grammar ItemLift;

// Python-trial regression (#54): an optional property inside a repeated block,
// (',' param ('=' NUM)?)*, captured as parallel lists loses its iteration —
// Python's default arguments drifted to the first parameters. Auto-labeling
// lifts such a block into a per-item rule (paramsItems+=paramsItem*), so
// each iteration keeps its own optional default.
unit: decls+=decl* EOF;

decl: 'def' ID '(' params? ')' ';' ;

params: param ('=' NUM)? (',' param ('=' NUM)?)* ;

param: ID ;

ID : [a-zA-Z]+ ;
NUM : [0-9]+ ;
WS : [ \t\r\n]+ -> channel(HIDDEN) ;

/*<!vmf-text!>
AutoLabel(enabled=true)
*/
