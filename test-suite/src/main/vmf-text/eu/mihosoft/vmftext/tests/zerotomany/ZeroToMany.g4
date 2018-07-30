grammar ZeroToMany;

/*
 In the initial implementation (pre 17.01.2018) could end up in recursion:

 1.) for sub-rules like ('['']')* with only terminal elements inside the sub-rule

     Reason: we didn't break after first unparse of the rule (remember: many means as many as we want)

     We fixed it by only unparsing such a rule once. The only possibility for a rule to be unparsed multiple
     times is if it indicates that it has unconsumed properties/elements.

 2.) for ( '(' value=DOUBLE ')' )*

     Reason: we didn't check correctly whether value was consumed, we only did that for list types

 3.) for ( '(' (value+=DOUBLE)? ')' )*

     Reason: we didn't check correctly whether the values list had unconsumed elements

 */
problematicRule: ('(' (values+= DOUBLE)? ('value:' value=DOUBLE)? ')')* ('['']')*;

DOUBLE :
         DIGIT+ DOT DIGIT*
       | DOT DIGIT+
       | DIGIT+
       ;

fragment DIGIT : [0-9];
fragment DOT : '.' ;