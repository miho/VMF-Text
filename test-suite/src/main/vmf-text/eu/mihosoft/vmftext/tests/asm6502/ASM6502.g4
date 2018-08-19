/*
BSD License

Copyright (c) 2013, Tom Everett
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of Tom Everett nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

grammar ASM6502;

prog
   : (lines+=line) +
   ;

line
   :
     comment=myComment          eol=EOL    # lineComment
   | instr=instruction          eol=EOL    # instructionStatement
   | instr=assemblerInstruction eol=EOL    # asmInstructionStatement
   | name=identifier ':'        eol=EOL    # labelStatement
   |                            eol=EOL    # emptyStatement
   ;

instruction
   : label=identifier? instructionCode=opcode args=argumentlist? comment=myComment?
   ;

assemblerInstruction
   : arg=argument? instructionCode=assemblerOpcode args=argumentlist? comment=myComment?
   ;

assemblerOpcode
   : asmInstruction=ASSEMBLER_INSTRUCTION
   ;

argumentlist
   : arguments+=argument (',' arguments+=argument)*
   ;

argument
   : pref=prefix? (number=numberValue | name=identifier | string=stringValue | op='*') ((op='+' | op='-') rightNumber=numberValue)?
   | '(' arg=argument ')'
   ;

prefix
   : '#'
   ;

stringValue
   : value=STRING
   ;

identifier
   : value=NAME
   ;

numberValue
   : value=NUMBER
   ;

myComment
   : comment=COMMENT
   ;

opcode
   :code=ADC
   |code=AND
   |code=ASL
   |code=BCC
   |code=BCS
   |code=BEQ
   |code=BIT
   |code=BMI
   |code=BNE
   |code=BPL
   |code=BRA
   |code=BRK
   |code=BVC
   |code=BVS
   |code=CLC
   |code=CLD
   |code=CLI
   |code=CLV
   |code=CMP
   |code=CPX
   |code=CPY
   |code=DEC
   |code=DEX
   |code=DEY
   |code=EOR
   |code=INC
   |code=INX
   |code=INY
   |code=JMP
   |code=JSR
   |code=LDA
   |code=LDY
   |code=LDX
   |code=LSR
   |code=NOP
   |code=ORA
   |code=PHA
   |code=PHX
   |code=PHY
   |code=PHP
   |code=PLA
   |code=PLP
   |code=PLY
   |code=ROL
   |code=ROR
   |code=RTI
   |code=RTS
   |code=SBC
   |code=SEC
   |code=SED
   |code=SEI
   |code=STA
   |code=STX
   |code=STY
   |code=STZ
   |code=TAX
   |code=TAY
   |code=TSX
   |code=TXA
   |code=TXS
   |code=TYA
   ;

ASSEMBLER_INSTRUCTION
   : 'ORG' | 'EQU' | 'ASC' | 'DS' | 'DFC' | '='
   ;


fragment A
   : ('a' | 'A')
   ;


fragment B
   : ('b' | 'B')
   ;


fragment C
   : ('c' | 'C')
   ;


fragment D
   : ('d' | 'D')
   ;


fragment E
   : ('e' | 'E')
   ;


fragment F
   : ('f' | 'F')
   ;


fragment G
   : ('g' | 'G')
   ;


fragment H
   : ('h' | 'H')
   ;


fragment I
   : ('i' | 'I')
   ;


fragment J
   : ('j' | 'J')
   ;


fragment K
   : ('k' | 'K')
   ;


fragment L
   : ('l' | 'L')
   ;


fragment M
   : ('m' | 'M')
   ;


fragment N
   : ('n' | 'N')
   ;


fragment O
   : ('o' | 'O')
   ;


fragment P
   : ('p' | 'P')
   ;


fragment Q
   : ('q' | 'Q')
   ;


fragment R
   : ('r' | 'R')
   ;


fragment S
   : ('s' | 'S')
   ;


fragment T
   : ('t' | 'T')
   ;


fragment U
   : ('u' | 'U')
   ;


fragment V
   : ('v' | 'V')
   ;


fragment W
   : ('w' | 'W')
   ;


fragment X
   : ('x' | 'X')
   ;


fragment Y
   : ('y' | 'Y')
   ;


fragment Z
   : ('z' | 'Z')
   ;

/*
* opcodes
*/

ADC
   : A D C
   ;


AND
   : A N D
   ;


ASL
   : A S L
   ;


BCC
   : B C C
   ;


BCS
   : B C S
   ;


BEQ
   : B E Q
   ;


BIT
   : B I T
   ;


BMI
   : B M I
   ;


BNE
   : B N E
   ;


BPL
   : B P L
   ;


BRA
   : B R A
   ;


BRK
   : B R K
   ;


BVC
   : B V C
   ;


BVS
   : B V S
   ;


CLC
   : C L C
   ;


CLD
   : C L D
   ;


CLI
   : C L I
   ;


CLV
   : C L V
   ;


CMP
   : C M P
   ;


CPX
   : C P X
   ;


CPY
   : C P Y
   ;


DEC
   : D E C
   ;


DEX
   : D E X
   ;


DEY
   : D E Y
   ;


EOR
   : E O R
   ;


INC
   : I N C
   ;


INX
   : I N X
   ;


INY
   : I N Y
   ;


JMP
   : J M P
   ;


JSR
   : J S R
   ;


LDA
   : L D A
   ;


LDY
   : L D Y
   ;


LDX
   : L D X
   ;


LSR
   : L S R
   ;


NOP
   : N O P
   ;


ORA
   : O R A
   ;


PHA
   : P H A
   ;


PHX
   : P H X
   ;


PHY
   : P H Y
   ;


PHP
   : P H P
   ;


PLA
   : P L A
   ;


PLP
   : P L P
   ;


PLY
   : P L Y
   ;


ROL
   : R O L
   ;


ROR
   : R O R
   ;


RTI
   : R T I
   ;


RTS
   : R T S
   ;


SBC
   : S B C
   ;


SEC
   : S E C
   ;


SED
   : S E D
   ;


SEI
   : S E I
   ;


STA
   : S T A
   ;


STX
   : S T X
   ;


STY
   : S T Y
   ;


STZ
   : S T Z
   ;


TAX
   : T A X
   ;


TAY
   : T A Y
   ;


TSX
   : T S X
   ;


TXA
   : T X A
   ;


TXS
   : T X S
   ;


TYA
   : T Y A
   ;


NAME
   : [a-zA-Z] [a-zA-Z0-9."]*
   ;


NUMBER
   : '$'? [0-9a-fA-F] +
   ;


COMMENT
   : ';' ~[\r\n]*
   ;


STRING
   : '"' ~ ["]* '"'
   ;


EOL
   : [\r\n]+
   ;

WS
   : [ \t] -> channel(HIDDEN)
   ;