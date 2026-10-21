grammar NameClash;

// #73: a parser rule and a token share a name (label / Label), as identifier /
// Identifier do in grammars-v4 JavaScript. ANTLR forbids both spellings as a
// label (error 69: rule name, error 70: token name), so auto-labeling names the
// properties after what they hold: a reference to the rule is labelNode (a Label
// node), a reference to the token labelToken (its text). A keyword in front still
// gives the keyword-context name (gotoLabel).
unit: stmts+=stmt* EOF ;

stmt
    : label ':' NUM ';'
    | 'goto' label ';'
    ;

label
    : Label
    | '$' Label
    ;

Label : [a-z]+ ;
NUM : [0-9]+ ;
WS : [ \t\r\n]+ -> channel(HIDDEN) ;

/*<!vmf-text!>
AutoLabel(enabled=true)
*/
