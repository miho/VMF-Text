grammar AutoLabelPartial;

program
    : entry* EOF
    ;

// fully unlabeled alternatives -> both labeled automatically
entry
    : element
    | pair
    ;

// partial manual alternative labels: only the first alternative is labeled by
// hand. ANTLR rejects mixed labeling, so auto-labeling must complete it by
// labeling the second alternative while keeping the manual 'NamedElement'.
element
    : IDENTIFIER          # NamedElement
    | INT
    ;

// partial manual element labels with a name conflict: 'identifier' is a manual
// label, and the unlabeled IDENTIFIER would also be named 'identifier', so the
// auto label must be disambiguated instead of clashing.
pair
    : '(' identifier=INT ':' IDENTIFIER ')'
    ;

IDENTIFIER : [a-zA-Z_][a-zA-Z0-9_]* ;
INT : [0-9]+ ;
WS : [ \t\r\n]+ -> channel(HIDDEN) ;

/*<!vmf-text!>
AutoLabel(enabled=true)
*/
