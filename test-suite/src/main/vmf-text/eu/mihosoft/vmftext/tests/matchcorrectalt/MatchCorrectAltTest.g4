grammar MatchCorrectAltTest;

// we check for multiple occurrences of name with lexer rule

// depending on the content of name we need to check whether alt 1, 2 or 3 applies
problematicRule :     'name'':'   name = IDENTIFIER
                   |  'number'':' name = INT
                   |  'terminal' ':' name = '123'
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