grammar ListGrow;

// A Java-style parameter list: optional outer block, comma-separated
// model-typed items, optional trailing variadic item. This shape is not one of
// the splice-recognized list shapes, so a structural add clears the trivia and
// the formatter has to reconstruct separators. Appending a parameter used to
// drop the comma ("(int a)" + "int b" -> "(int aint b)") because the comma's
// occurrence had no recorded presence state.
call: '(' (params+=param (',' params+=param)* (',' rest=restParam)?)? ')' EOF;

param: type=ID name=ID;

restParam: type=ID '...' name=ID;

ID : [a-zA-Z]+ ;
WS : [ \t\r\n]+ -> channel(HIDDEN) ;
