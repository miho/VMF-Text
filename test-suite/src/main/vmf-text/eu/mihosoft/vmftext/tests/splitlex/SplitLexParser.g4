parser grammar SplitLexParser;

// #39: the parser half of a split grammar. VMF-Text names the model after the
// parser grammar minus its "Parser" suffix (SplitLexModel, SplitLexModelParser)
// and generates the lexer from the tokenVocab grammar next to this file.
options { tokenVocab = SplitLexLexer; }

unit: items+=item* EOF ;

item: DEF name=ID EQ value=NUM nl=NEWLINE ;
