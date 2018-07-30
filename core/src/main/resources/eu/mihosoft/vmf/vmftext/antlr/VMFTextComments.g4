grammar VMFTextComments;

program: (comments+=vmfTextComment | string | unknowns)*;

vmfTextComment: text=VMF_TEXT_MULTILINE_COMMENT ;


string: stringDoubleQuotes | stringSingleQuote;
stringDoubleQuotes : STRING_DOUBLE;
stringSingleQuote : STRING_SINGLE;


unknowns : UNKNOWN+ ;

// see here: http://stackoverflow.com/questions/16045209/antlr-how-to-escape-quote-symbol-in-quoted-string
// additionally we disabled greedy by using *? instead of *
STRING_DOUBLE
    :   '"' (~('"' | '\\' | '\r' | '\n') | '\\' ('"' | '\\'))*? '"'
    ;

STRING_SINGLE
    :   '\'' (~('\'' | '\\' | '\r' | '\n') | '\\' ('\'' | '\\'))*? '\''
    ;


VMF_TEXT_MULTILINE_COMMENT
    :    '/*<!vmf-text!>' .*? '*/'
    ;

// all other characters
UNKNOWN  : . ;