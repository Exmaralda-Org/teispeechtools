/**
 * Grammar for the Simple EXMARaLDA plain text format:
 */

grammar GenericConvention;

options {tokenVocab=GenericConventionLexer;}

@header{package de.ids.mannheim.clarin.teispeech.tools;}

text : contribution +;

// TODO: incidents have been taken care of by square bracket annotation?
//contribution: (anno | incident | content)+;
contribution: (anno | content)+;

// TODO: Anführungs‚zeichen‘setzung im Wort?
content: (assimilated | word | punctuation)+;

word: D_WORD | A_WORD;
link: D_LINK | A_LINK;
punctuation: PUNCTUATION | A_PUNCTUATION;

assimilated: link word;

anno: LEFT_PAREN anno_content RIGHT_PAREN;
anno_content: pause | (uncertain | incomprehensible )+;

a_word: A_WORD;

uncertain: (assimilated | word | punctuation)+;
incomprehensible: A_INCOMPREHENSIBLE;
a_punctuation: A_PUNCTUATION;

pause: PAUSE;

// incident: LLEFT_PAREN incident_content RRIGHT_PAREN;
// 
// incident_content: (i_word i_punctuation?)+;
// i_word: I_WORD;
// i_punctuation: I_PUNCTUATION;
