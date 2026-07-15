grammar DotAsTerminal;

/*
 * Exercise labeled parser wildcards (issue #8).
 *
 * In a parser rule, '.' matches any token except EOF. Labeling it
 * (label=. / label+=.) must produce token-typed String properties —
 * the same shape as an explicit catch-all lexer rule such as ANY.
 *
 * Note: WS must be declared before the ANY catch-all so spaces are not
 * emitted as default-channel tokens that labeled '.' would collect.
 */

// Single labeled wildcard
labeledDotSingle: value=. EOF;

// List of labeled wildcards (the original DotAsTerminal motivation)
labeledDotList: (items+=.)+ EOF;

// Mixed alternatives: prefer WORD, otherwise collect remaining tokens via +=.
textToExpand:
    (words+=WORD | ignored+=.)*
    EOF
;

// Explicit ANY lexer-rule workaround (regression: must keep working)
mainRuleWorking: myLabel=ANY;

WORD: [a-zA-Z]+;
INT: [0-9]+;
PUNCT: [.,;:!?];
HASH_OPEN: '<##';
HASH_CLOSE: '##>';

MY_LEXER_RULE: 'abc';

WS
:   [ \r\t\n]+ -> channel(HIDDEN)
;

ANY: .;
