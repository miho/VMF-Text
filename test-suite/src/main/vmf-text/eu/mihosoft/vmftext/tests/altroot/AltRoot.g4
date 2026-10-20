grammar AltRoot;

// The root rule itself has alternatives that auto-labeling turns into typed
// sub classes (ExprAlt1 / ExprAlt2). The generated unparser used to reference
// a non-existent 'exprUnparser' for such a root and did not compile.
expr: INT
    | '(' expr ')'
    ;

INT : [0-9]+ ;
WS  : [ \t\r\n]+ -> channel(HIDDEN) ;

/*<!vmf-text!>
AutoLabel(enabled=true)
*/
