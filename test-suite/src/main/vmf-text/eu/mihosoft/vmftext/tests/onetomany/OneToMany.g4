grammar OneToMany;

/*
    See ZeroToMany.g4 for an explanation
 */
problematicRule: ('(' (values+= DOUBLE)? ('value:' value=DOUBLE)? ')')+ ('['']')+;

DOUBLE :
         DIGIT+ DOT DIGIT*
       | DOT DIGIT+
       | DIGIT+
       ;

fragment DIGIT : [0-9];
fragment DOT : '.' ;