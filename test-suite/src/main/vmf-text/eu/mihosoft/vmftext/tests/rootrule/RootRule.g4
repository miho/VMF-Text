grammar RootRule;

// The first parser rule is deliberately NOT the start rule. Without the
// Root(rule=...) metadata below VMF-Text would make 'item' the model root
// (historical convention: first rule = root), as happened with the C grammar
// whose compilationUnit comes last.
item: name=ID;

itemList: items+=item (',' items+=item)* EOF;

ID : [a-z]+ ;
WS : [ \t\r\n]+ -> channel(HIDDEN) ;

/*<!vmf-text!>
Root(rule=itemList)
*/
