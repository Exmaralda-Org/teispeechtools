/**
 * Grammar for the Simple EXMARaLDA plain text format:
 */

parser grammar SimpleExmaralda;

options {tokenVocab=SimpleExmaraldaLexer;}

@header{package de.ids.mannheim.clarin.teispeech.tools;}

transcript : prolog? line+ ;

prolog: START_PROLOG language? duration offset? END_PROLOG;
language: LANGUAGE LCOLON lang_code HNEWLINE;
lang_code: LANG_CODE;
duration: DURATION HCOLON timeData UNIT? HNEWLINE?;
offset: OFFSET HCOLON timeData UNIT? HNEWLINE?;
timeData: FLOATING;

line : turn | empty_line ;

empty_line : NEWLINE ;

turn : speaker COLON content+ ;

content : (action? text+ comment? | action | caction) conti?;

conti : CONTI;

speaker : HWORD;

action : LBRACKET aword+ RBRACKET;
caction: LLPAREN aword+ RRPAREN;

aword: AWORD | CAWORD;
comment : LBRACE IWORD+ RBRACE;

text : (word | marked) ;
word : WORD ;
marked : LANGLE MWORD+ T_RANGLE MARK_ID RANGLE;
