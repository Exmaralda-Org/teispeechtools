/**
 * Grammar for the Simple EXMARaLDA plain text format:
 */

grammar GenericConvention;

options {tokenVocab=GenericConventionLexer;}

@header{package de.ids.mannheim.clarin.teispeech.tools;}

text : contribution +;

contribution: (anno | content)+;

content: (word | incomprehensible | punctuation)+;

word: D_WORD | A_WORD ;
punctuation: PUNCTUATION | A_PUNCTUATION;

anno: LEFT_PAREN anno_content RIGHT_PAREN;
anno_content: pause | (uncertain | incomprehensible )+;

a_word: A_WORD;

uncertain: (word | punctuation)+;
incomprehensible: A_INCOMPREHENSIBLE | INCOMPREHENSIBLE;
a_punctuation: A_PUNCTUATION;

pause: PAUSE;
