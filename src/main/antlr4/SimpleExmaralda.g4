/**
 * Grammar for the Simple EXMARaLDA plain text format:
 */

grammar SimpleExmaralda;

options {tokenVocab=SimpleExmaraldaLexer;}

@header{package de.ids.mannheim.clarin.teispeech.tools;}

transcript :  line+ ;

line : turn | empty_line ;

empty_line : NEWLINE ;

turn : speaker COLON content+ ;

content : (action? text+ comment? | action) conti?;

conti : CONTI;

speaker :  HWORD;

action : LBRACKET aword+ RBRACKET | LLPAREN aword+ RRPAREN;
aword: AWORD | CAWORD;
comment : LBRACE IWORD+ RBRACE;

text : (word | marked) ;
word :  WORD ;
marked : LANGLE MWORD+ T_RANGLE MARK_ID RANGLE;

