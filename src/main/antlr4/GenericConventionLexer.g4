/**
 * Lexer for the Simple EXMARaLDA plain text format:
 */

lexer grammar GenericConventionLexer;

@header{package de.ids.mannheim.clarin.teispeech.tools;}

LEFT_PAREN : '(' -> mode(ANNO);

//LLEFT_PAREN : '((' -> mode(INCIDENT);

D_WORD : WORD;
D_LINK: '_';

L_BRACKET: '[' -> skip;
R_BRACKET: ']' -> skip; 

fragment WORDCHAR: [_\p{Letter}\p{Number}\u203F\u2040\u2054\uFE33\uFE34\uFE4D\uFE4E\uFE4F\uFF3F&-];
fragment WORD: (WORDCHAR|'\'')+;
fragment PUNCT: '\\'[+()]|[\p{Pd}\p{Pi}\p{Pf}\p{Po}{}<>];
fragment WHITE: [\p{Zs}\u0009\u000A\u000B\u000C\u000D\u001C\u001D\u001E\u001F];

PUNCTUATION: PUNCT+ {!getText().contains("'")}?;

INCOMPREHENSIBLE: '+++'+;

SPACE: WHITE+ -> channel(HIDDEN);


mode ANNO;
RIGHT_PAREN: ')' -> mode(DEFAULT_MODE);
PAUSE: '-'+;
MICROPAUSE: '.';
A_INCOMPREHENSIBLE: '+++'+;
A_PUNCTUATION: (PUNCT | [[\]'])+;

A_SPACE: WHITE+ -> channel(HIDDEN);
A_WORD : WORD;

