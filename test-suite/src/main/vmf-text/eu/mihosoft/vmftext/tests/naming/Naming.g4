grammar Naming;

// Auto-label naming (#47, JavaScript and Python trials):
//  - a lexer rule that is a single literal (Var: 'var';) is a keyword: no
//    property, but keyword context for its successor (If sum Else sum -> ifSum, elseSum);
//  - the operands of `term Plus term` are left / right;
//  - snake_case rule names give camelCase property and synthetic rule names
//    (simpleStmts, simpleStmtChoice); since #63 the rule itself is renamed
//    before any other pass, so its class is FileInput / SimpleStmt as well.
file_input: simple_stmt* EOF ;

simple_stmt: Var ID '=' (sum | cond) NEWLINE ;

sum
    : term Plus term
    | term
    ;

cond: If sum Else sum ;

term: ID | NUM ;

Var : 'var' ;
If : 'if' ;
Else : 'else' ;
Plus : '+' ;
ID : [a-z]+ ;
NUM : [0-9]+ ;
NEWLINE : '\r'? '\n' ;
WS : [ \t]+ -> channel(HIDDEN) ;

/*<!vmf-text!>
AutoLabel(enabled=true)
*/
