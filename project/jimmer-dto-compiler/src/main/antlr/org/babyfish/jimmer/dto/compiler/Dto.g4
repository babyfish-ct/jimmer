grammar Dto;

@header {
package org.babyfish.jimmer.dto.compiler;
}

// Parser --------

dto
    :
    exportStatement?
    (importStatements += importStatement)*
    (dtoTypes+=dtoType)*
    EOF
    ;

exportStatement
    :
    'export' typeParts += Identifier ('.' typeParts += Identifier)*
    ('->' 'package' packageParts += Identifier ('.' packageParts += Identifier)*)?
    ;

importStatement
    :
    'import' parts += Identifier ('.' parts += Identifier)*
    (
        '.' '{' importedTypes += importedType (',' importedTypes += importedType)* '}' |
        'as' alias = Identifier
    )?
    ;

importedType
    :
    name = Identifier ('as' alias = Identifier)?
    ;

dtoType
    :
    (doc = DocComment)?
    (annotations += annotation)*
    (modifiers += (Identifier | 'fixed' | 'static' | 'dynamic' | 'fuzzy'))*
    name=Identifier
    ('implements' superInterfaces += typeRef (',' superInterfaces += typeRef)*)?
    body=dtoBody
    ;

dtoBody
    :
    '{'
    ((explicitProps += explicitProp) (',' | ';')?)*
    '}'
    ;

explicitProp
    :
    micro | aliasGroup | positiveProp | negativeProp | userProp
    ;

micro
    :
    '#' name=Identifier
    (
        '(' args+=qualifiedName (',' args+=qualifiedName)* ')'
    )?
    (optional = '?' | required = '!')?
    ;

aliasGroup
    :
    pattern = aliasPattern '{' (props += aliasGroupProp)* '}'
    ;

aliasPattern
    :
    'as' '('
    (prefix = '^')?
    (original = Identifier)?
    (suffix = '$')?
    (translator = '->')
    (replacement = Identifier)?
    ')'
    ;

aliasGroupProp
    :
    micro | positiveProp
    ;

positiveProp
    :
    (doc = DocComment)?
    (annotations += annotation)*
    '+'?
    (modifier = ('fixed' | 'static' | 'dynamic' | 'fuzzy'))?
    (
        func = Identifier
        (flag = '/' (insensitive = Identifier)? (prefix = '^')? (suffix = '$')?)?
        '(' props += Identifier (',' props += Identifier)* ','? ')'
        |
        props += Identifier
    )
    (optional = '?' | required = '!' | recursive = '*')?
    ('as' alias=Identifier)?
    (
        (childDoc = DocComment)?
        (bodyAnnotations += annotation)*
        ('implements' bodySuperInterfaces += typeRef (',' bodySuperInterfaces += typeRef)*)?
        dtoBody
        |
        '->' enumBody
    )?
    ;

negativeProp
    :
    '-' prop = Identifier
    ;

userProp
    :
    (doc = DocComment)?
    (annotations += annotation)*
    prop = Identifier ':' typeRef
    ;

typeRef
    :
    qualifiedName
    ('<' genericArguments += genericArgument (',' genericArguments += genericArgument)? '>')?
    (optional = '?')?
    ;

genericArgument
    :
    wildcard = '*' |
    (modifier = Identifier)? typeRef
    ;

qualifiedName
    :
    parts+=Identifier ('.' parts+=Identifier)*
    ;

annotation
    :
    '@' typeName = qualifiedName ('(' annotationArguments? ')')?
    ;

annotationArguments
    :
    defaultArgument = annotationValue (',' namedArguments += annotationNamedArgument)*
    |
    namedArguments += annotationNamedArgument (',' namedArguments += annotationNamedArgument)*
    ;

annotationNamedArgument
    :
    name = Identifier '=' value = annotationValue
    ;

annotationValue
    :
    annotationSingleValue
    |
    annotationArrayValue
    ;

annotationSingleValue
    :
    booleanToken = BooleanLiteral |
    characterToken = CharacterLiteral |
    stringTokens += StringLiteral ('+' stringTokens += StringLiteral)* |
    integerToken = IntegerLiteral |
    floatingPointToken = FloatingPointLiteral |
    qualifiedPart = qualifiedName classSuffix? |
    annotationPart = annotation |
    nestedAnnotationPart = nestedAnnotation
    ;

annotationArrayValue
    :
    '{' elements += annotationSingleValue (',' elements += annotationSingleValue)* '}'
    |
    '[' elements += annotationSingleValue (',' elements += annotationSingleValue)* ']'
    ;

nestedAnnotation
    :
    typeName = qualifiedName '(' annotationArguments? ')'
    ;

enumBody
    :
    '{' (mappings += enumMapping (','|';')?)+ '}'
    ;

enumMapping
    :
    constant = Identifier ':' value = (StringLiteral | IntegerLiteral)
    ;

classSuffix
    :
    '?'? ('.' | '::') 'class'
    ;

// Lexer --------

Identifier
    :
    [$A-Za-z_][$A-Za-z_0-9]*
    ;

WhiteSpace
    :
    (' ' | '\u0009' | '\u000C' | '\r' | '\n') -> skip
    ;

DocComment
    :
    ('/**' .*? '*/')
    ;

BlockComment
    :
    ('/*' .*? '*/') -> skip
    ;

LineComment
    :
    ('//' ~[\r\n]* ('\r\n' | '\r' | '\n')?) -> skip
    ;

BooleanLiteral
    :
    'true' | 'false'
    ;

CharacterLiteral
	:
	'\'' SingleCharacter '\''
	|
	'\'' EscapeSequence '\''
	;

fragment
SingleCharacter
	:
	~['\\\r\n]
	;

StringLiteral
	:
	'"' StringCharacters? '"'
	;

fragment
StringCharacters
	:
	StringCharacter+
	;

fragment
StringCharacter
	:
	~["\\\r\n] | EscapeSequence
	;

fragment
EscapeSequence
	:	'\\' [btnfr"'\\]
	|	OctalEscape
    |   UnicodeEscape // This is not in the spec but prevents having to preprocess the input
	;

fragment
OctalEscape
	:	'\\' OctalDigit
	|	'\\' OctalDigit OctalDigit
	|	'\\' ZeroToThree OctalDigit OctalDigit
	;

fragment
ZeroToThree
	:	[0-3]
	;

fragment
UnicodeEscape
    :   '\\' 'u'+  HexDigit HexDigit HexDigit HexDigit
    ;

IntegerLiteral
	:	DecimalIntegerLiteral
	|	HexIntegerLiteral
	|	OctalIntegerLiteral
	|	BinaryIntegerLiteral
	;

fragment
DecimalIntegerLiteral
	:	DecimalNumeral IntegerTypeSuffix?
	;

fragment
HexIntegerLiteral
	:	HexNumeral IntegerTypeSuffix?
	;

fragment
OctalIntegerLiteral
	:	OctalNumeral IntegerTypeSuffix?
	;

fragment
BinaryIntegerLiteral
	:	BinaryNumeral IntegerTypeSuffix?
	;

fragment
IntegerTypeSuffix
	:	[lL]
	;

fragment
DecimalNumeral
	:	'0'
	|	NonZeroDigit (Digits? | Underscores Digits)
	;

fragment
Digits
	:	Digit (DigitsAndUnderscores? Digit)?
	;

fragment
Digit
	:	'0'
	|	NonZeroDigit
	;

fragment
NonZeroDigit
	:	[1-9]
	;

fragment
DigitsAndUnderscores
	:	DigitOrUnderscore+
	;

fragment
DigitOrUnderscore
	:	Digit
	|	'_'
	;

fragment
Underscores
	:	'_'+
	;

fragment
HexNumeral
	:	'0' [xX] HexDigits
	;

fragment
HexDigits
	:	HexDigit (HexDigitsAndUnderscores? HexDigit)?
	;

fragment
HexDigit
	:	[0-9a-fA-F]
	;

fragment
HexDigitsAndUnderscores
	:	HexDigitOrUnderscore+
	;

fragment
HexDigitOrUnderscore
	:	HexDigit
	|	'_'
	;

fragment
OctalNumeral
	:	'0' Underscores? OctalDigits
	;

fragment
OctalDigits
	:	OctalDigit (OctalDigitsAndUnderscores? OctalDigit)?
	;

fragment
OctalDigit
	:	[0-7]
	;

fragment
OctalDigitsAndUnderscores
	:	OctalDigitOrUnderscore+
	;

fragment
OctalDigitOrUnderscore
	:	OctalDigit
	|	'_'
	;

fragment
BinaryNumeral
	:	'0' [bB] BinaryDigits
	;

fragment
BinaryDigits
	:	BinaryDigit (BinaryDigitsAndUnderscores? BinaryDigit)?
	;

fragment
BinaryDigit
	:	[01]
	;

fragment
BinaryDigitsAndUnderscores
	:	BinaryDigitOrUnderscore+
	;

fragment
BinaryDigitOrUnderscore
	:	BinaryDigit
	|	'_'
	;

FloatingPointLiteral
	:	DecimalFloatingPointLiteral
	|	HexadecimalFloatingPointLiteral
	;

fragment
DecimalFloatingPointLiteral
	:	Digits '.' Digits? ExponentPart? FloatTypeSuffix?
	|	'.' Digits ExponentPart? FloatTypeSuffix?
	|	Digits ExponentPart FloatTypeSuffix?
	|	Digits FloatTypeSuffix
	;

fragment
ExponentPart
	:	ExponentIndicator SignedInteger
	;

fragment
ExponentIndicator
	:	[eE]
	;

fragment
SignedInteger
	:	Sign? Digits
	;

fragment
Sign
	:	[+-]
	;

fragment
FloatTypeSuffix
	:	[fFdD]
	;

fragment
HexadecimalFloatingPointLiteral
	:	HexSignificand BinaryExponent FloatTypeSuffix?
	;

fragment
HexSignificand
	:	HexNumeral '.'?
	|	'0' [xX] HexDigits? '.' HexDigits
	;

fragment
BinaryExponent
	:	BinaryExponentIndicator SignedInteger
	;

fragment
BinaryExponentIndicator
	:	[pP]
	;