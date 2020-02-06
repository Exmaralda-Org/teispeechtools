/**
 * Lexer for the Simple EXMARaLDA plain text format:
 */

lexer grammar SimpleExmaraldaLexer;

@header{package de.ids.mannheim.clarin.teispeech.tools;}

channels {
    BACKSLASH
}

SPACE : [\t ]+ -> channel(HIDDEN);

START_PROLOG: '---' NEWLINE -> mode(HEADER);

HWORD : ~[:\t \n\r\-]~[:\t \n\r]+;
COLON : ':' -> mode(NORMAL);
NEWLINE : ('\r\n'|'\n\r'|'\r'|'\n');

mode HEADER;
DURATION: 'duration' | 'length' | 'time';
LANGUAGE: 'lang''uage'? -> mode(LANG);
HCOLON : ':';
HSPACE : [\t ]+ -> channel(HIDDEN);
HNEWLINE : ('\r\n'|'\n\r'|'\r'|'\n');
OFFSET: 'offset';
INT: [0-9]+;
FLOATING: [0-9]+[.][0-9]+([eE][0-9]+)?;
UNIT: 's'|'sec';
END_PROLOG: ('---'|'...') -> mode(NORMAL);

mode LANG;
LSPACE: [\t ]+ -> channel(HIDDEN);
LCOLON: ':';
LANG_CODE: ~('-'|[:\t \n\r])~[:\t \n\r]+ -> mode(HEADER);

mode COMMENTED_ACTION;
RRPAREN: '))' -> mode(NORMAL);
CAWORD : ~[)\t \n\r]+ | ')';
CASPACE : [\t ]+ -> channel(HIDDEN);

mode ACTION;
AWORD : ~[\t \]\n\r]+;
RBRACKET: ']' -> mode(NORMAL);
ASPACE : [\t ]+ -> channel(HIDDEN);
A_DIRECT_CONTI : '\\' [\t ]* FNEWLINE [\t ]+ -> channel(BACKSLASH);


mode INFO;
IWORD : ~[\t }\n\r]+;
RBRACE: '}' -> mode(NORMAL);
ISPACE : [\t ]+ -> channel(HIDDEN);
I_DIRECT_CONTI : '\\' [\t ]* FNEWLINE [\t ]+ -> channel(BACKSLASH);

mode MARK_TEXT;
MWORD : ~[>\n\r]+;
T_RANGLE : '>' -> mode(MARK);
MSPACE : [\t ]+ -> channel(HIDDEN);

mode MARK;
MARK_ID : ~[>\n\r]+;
RANGLE : '>' -> mode(NORMAL);


mode NORMAL;
LLPAREN : '((' -> mode(COMMENTED_ACTION);
WORD : (~[() \t[{<>}\]\r\n] | '\\'[[{<>}])+ | '('|')' ;
// PUNCT : [;,:.!?()]+ ;
LANGLE : '<' -> mode(MARK_TEXT);
LBRACKET : '[' -> mode(ACTION);
LBRACE : '{' -> mode(INFO);
NSPACE : [\t ]+ -> channel(HIDDEN);
DIRECT_CONTI : '\\' [\t ]* FNEWLINE [\t ]+ -> channel(BACKSLASH);
CONTI : FNEWLINE [\t ]+;
NNEWLINE : FNEWLINE -> mode(DEFAULT_MODE), skip;
fragment FNEWLINE : ('\r\n'|'\n\r'|'\r'|'\n');

