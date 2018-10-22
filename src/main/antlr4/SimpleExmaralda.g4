/**
 * Grammar for the Simple EXMARaLDA plain text format:
 */

grammar SimpleExmaralda;

options {tokenVocab=SimpleExmaraldaLexer;}


transcript :  line+ ;

line : turn | empty_line ;

empty_line : NEWLINE ;

turn : speaker (action | action? text info? | info)+ ;

speaker :  HWORD+;

action : AWORD+ ;

text : word+ ;
word : (WORD | marked) ;
marked : MWORD+ MARK_ID ;

info : IWORD+ ;
