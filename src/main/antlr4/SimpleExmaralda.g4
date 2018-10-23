/**
 * Grammar for the Simple EXMARaLDA plain text format:
 */

grammar SimpleExmaralda;

options {tokenVocab=SimpleExmaraldaLexer;}


transcript :  line+ ;

line : turn | empty_line ;

empty_line : NEWLINE ;

turn : speaker content+ ;

content : (action | action? text info? ) conti?;

conti : CONTI;

speaker :  HWORD;

action : AWORD+ ;

text : (word | marked)+ ;
word :  WORD ;
marked : MWORD+ MARK_ID ;

info : IWORD+ ;
