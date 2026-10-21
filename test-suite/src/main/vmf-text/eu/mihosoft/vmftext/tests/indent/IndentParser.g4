parser grammar IndentParser;

// #60: the parser half of the indentation language (see IndentLexer.g4). A
// block is NEWLINE INDENT stmt+ DEDENT, as in Python; the three tokens are
// properties of the block, so a block built from scratch needs their values
// (IndentTriviaSupport.fillIndentationTokens) and the statements inside it
// are indented by Formatter.newIndentationTokenFormatter().
options { tokenVocab = IndentLexer; }

unit : stmts+=stmt* EOF ;

stmt
    : target=NAME '=' value=expr nl=NEWLINE                                   # AssignStmt
    | callee=NAME '(' (args+=expr (',' args+=expr)*)? ')' nl=NEWLINE          # CallStmt
    | 'return' value=expr nl=NEWLINE                                          # ReturnStmt
    | 'pass' nl=NEWLINE                                                       # PassStmt
    | 'if' cond=expr ':' body=block ('else' ':' elseBody=block)?              # IfStmt
    ;

block : nl=NEWLINE indent=INDENT stmts+=stmt+ dedent=DEDENT ;

expr
    : name=NAME    # NameExpr
    | num=NUMBER   # NumExpr
    ;
