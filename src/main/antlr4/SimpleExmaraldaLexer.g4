/**
 * Lexer for the Simple EXMARaLDA plain text format:
 */

lexer grammar SimpleExmaraldaLexer;

SPACE : [\t ]+ -> channel(HIDDEN);


HWORD : ~[:\t \n\r]+;
COLON : ':' -> mode(NORMAL), skip;
NEWLINE : ('\r\n'|'\n\r'|'\r'|'\n');

mode ACTION;
AWORD : ~[\t \]\n\r]+;
RBRACKET: ']' -> mode(NORMAL), skip;
ASPACE : [\t ]+ -> channel(HIDDEN);


mode INFO;
IWORD : ~[\t }\n\r]+;
RBRACE: '}' -> mode(NORMAL), skip;
ISPACE : [\t ]+ -> channel(HIDDEN);

mode MARK_TEXT;
MWORD : ~[>\n\r]+;
T_RANGLE : '>' -> mode(MARK), skip;
MSPACE : [\t ]+ -> channel(HIDDEN);

mode MARK;
MARK_ID : ~[>\n\r]+;
RANGLE : '>' -> mode(NORMAL), skip;


mode NORMAL;
WORD : (~[ \t[{<>}\]\r\n] | '\\'[[{<>}])+ ;
// PUNCT : [;,:.!?()]+ ;
LANGLE : '<' -> mode(MARK_TEXT), skip;
LBRACKET : '[' -> mode(ACTION), skip;
LBRACE : '{'  -> mode(INFO), skip;
NSPACE : [\t ]+ -> channel(HIDDEN);
CONTI : FNEWLINE [\t ]+ -> skip;
NNEWLINE : FNEWLINE -> mode(DEFAULT_MODE), skip;
fragment FNEWLINE : ('\r\n'|'\n\r'|'\r'|'\n');

