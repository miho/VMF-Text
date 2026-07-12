grammar AutoLabelPartial;

program
    : entry* EOF
    ;

// fully unlabeled alternatives -> all labeled automatically
entry
    : element
    | pair
    | triple
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

// manual labels occupy both the base name and its first numeric suffix: the
// auto label for the unlabeled IDENTIFIER must skip 'identifier2' and use
// 'identifier3' instead of silently colliding with the manual label.
triple
    : '<' identifier=INT identifier2=INT IDENTIFIER '>'
    ;

IDENTIFIER : [a-zA-Z_][a-zA-Z0-9_]* ;
INT : [0-9]+ ;
WS : [ \t\r\n]+ -> channel(HIDDEN) ;

/*<!vmf-text!>
AutoLabel(enabled=true)
*/
