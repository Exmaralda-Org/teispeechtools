/**
 * Lexer for the Simple EXMARaLDA plain text format:
 */

lexer grammar SimpleExmaraldaLexer;

@header{package de.ids.mannheim.clarin.teispeech.tools;}

SPACE : [\t ]+ -> channel(HIDDEN);

HWORD : ~[:\t \n\r]+;
COLON : ':' -> mode(NORMAL);
NEWLINE : ('\r\n'|'\n\r'|'\r'|'\n');

mode COMMENTED_ACTION;
CAWORD : ~[\t \]\n\r]+;
RRPAREN: '))' -> mode(NORMAL);
CASPACE : [\t ]+ -> channel(HIDDEN);



mode ACTION;
AWORD : ~[\t \]\n\r]+;
RBRACKET: ']' -> mode(NORMAL);
ASPACE : [\t ]+ -> channel(HIDDEN);


mode INFO;
IWORD : ~[\t }\n\r]+;
RBRACE: '}' -> mode(NORMAL);
ISPACE : [\t ]+ -> channel(HIDDEN);

mode MARK_TEXT;
MWORD : ~[>\n\r]+;
T_RANGLE : '>' -> mode(MARK);
MSPACE : [\t ]+ -> channel(HIDDEN);

mode MARK;
MARK_ID : ~[>\n\r]+;
RANGLE : '>' -> mode(NORMAL);


mode NORMAL;
WORD : (~[ \t[{<>}\]\r\n] | '\\'[[{<>}])+ ;
// PUNCT : [;,:.!?()]+ ;
LANGLE : '<' -> mode(MARK_TEXT);
LBRACKET : '[' -> mode(ACTION);
LBRACE : '{'  -> mode(INFO);
LLPAREN : '))' -> mode(COMMENTED_ACTION);
NSPACE : [\t ]+ -> channel(HIDDEN);
CONTI : FNEWLINE [\t ]+;
NNEWLINE : FNEWLINE -> mode(DEFAULT_MODE), skip;
fragment FNEWLINE : ('\r\n'|'\n\r'|'\r'|'\n');

