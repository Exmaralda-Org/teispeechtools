/**
 * Grammar for the Simple EXMARaLDA plain text format:
 */

grammar GenericConvention;

options {tokenVocab=GenericConventionLexer;}

@header{package de.ids.mannheim.clarin.teispeech.tools;}

text : contribution +;

contribution: (anno | incident | content)+;

// TODO: Anführungs‚zeichen‘setzung im Wort?
content: (word D_LINK word | word | punctuation)+;
word: D_WORD;
punctuation: PUNCTUATION;

anno: LEFT_PAREN anno_content RIGHT_PAREN;
anno_content: pause | (uncertain | incomprehensible | a_punctuation)+;

uncertain: A_WORD;
incomprehensible: A_INCOMPREHENSIBLE;
a_punctuation: A_PUNCTUATION;

pause: PAUSE;

incident: LLEFT_PAREN incident_content RRIGHT_PAREN;

incident_content: (i_word i_punctuation?)+;
i_word: I_WORD;
i_punctuation: I_PUNCTUATION;