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

fragment WORDCHAR: [\p{Letter}\p{Number}\u203F\u2040\u2054\uFE33\uFE34\uFE4D\uFE4E\uFE4F\uFF3F&-];
fragment WORD: WORDCHAR+;
fragment PUNCT: '\\'[+()]|[\p{Pd}\p{Pi}\p{Pf}\p{Po}{}<>];

PUNCTUATION: PUNCT+;

INCOMPREHENSIBLE: '+++'+;

SPACE: [\p{Zs}\n\r]+ -> channel(HIDDEN);

//mode INCIDENT;
//
//RRIGHT_PAREN : '))' -> mode(DEFAULT_MODE);
//I_WORD : WORD;
//I_PUNCTUATION: (PUNCT | [[\]])+;
//I_LINK: '_'+;
//I_SPACE: [\p{Zs}]+ -> channel(HIDDEN);


mode ANNO;
RIGHT_PAREN: ')' -> mode(DEFAULT_MODE);
PAUSE: '.'+;
A_INCOMPREHENSIBLE: '+++'+;
A_PUNCTUATION: (PUNCT | [[\]])+;

A_SPACE: [\p{Zs}]+ -> channel(HIDDEN);
A_WORD : WORD;
A_LINK: '_';

