grammar Fetcher;

@header {
package org.babyfish.jimmer.sql.fetcher.impl.antlr;
}

fetcher
    :
    '{' (fields += field)+ '}'
    ;

field
    :
    (plus = '+')? (positiveField | SCALARS | SELECTABLE)
    |
    (minus = '-') IDENTIFIER
    ;

positiveField
    :
    IDENTIFIER (':' '{' (arguments += argument) (',' (arguments += argument))* '}')? fetcher?
    ;

argument
    :
    IDENTIFIER ':' INTEGER
    ;

SCALARS
    :
    '@scalars'
    ;

SELECTABLE
    :
    '@selectable'
    ;

IDENTIFIER
    :
    Letter LetterOrDigit*
    ;

INTEGER
    :
    [0-9]+
    ;

fragment LetterOrDigit
    :
    Letter
    | [0-9]
    ;

fragment Letter
    : [a-zA-Z$_]
    | ~[\u0000-\u007F\uD800-\uDBFF]
    | [\uD800-\uDBFF] [\uDC00-\uDFFF]
    ;

WS
    :
    [ \t\r\n\u000C]+ -> channel(HIDDEN)
    ;

COMMENT
    :
    '/*' .*? '*/' -> channel(HIDDEN)
    ;

LINE_COMMENT
    :
    '//' ~[\r\n]*
    -> channel(HIDDEN)
    ;