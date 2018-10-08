grammar MatchCorrectAltTestTerminalOnly;

// we check for multiple occurrences of name with terminal rule type
// (to check them, we need to call match... in alt unparser)

// depending on the content of name we need to check whether alt 1 or 2 applies
problematicRule :     'terminal1' ':' name = '456'
                   |  'terminal2' ':' name = '123'
                   ;

INT : DIGIT+ ;

fragment DIGIT : [0-9];


IDENTIFIER: [a-zA-Z][a-zA-Z0-9]*;


WS
:   [ \r\t\n]+ -> channel(HIDDEN)
;

MULTILINE_COMMENT
:  '/*' .*? '*/' -> skip
;

LINE_COMMENT
:  '//' .*? '\n' -> skip
;