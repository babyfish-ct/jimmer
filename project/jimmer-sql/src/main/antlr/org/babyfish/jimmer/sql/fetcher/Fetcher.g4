grammar Fetcher;

@header {
package org.babyfish.jimmer.sql.fetcher;
}

fetcher
    :
    type = entityType body = fetchBody EOF
    ;

fetchBody
    :
    '{' (fields += field)* '}'
    ;

field
    :
    prop = Identifier
    ( '(' arguments += argument (',' arguments += argument)* ')' )?
    (body = fetchBody)?
    ','?
    ;

argument
    :
    (name = Identifier)
    ':'
    value = ('true' | 'false' | Number | '<java-code>')
    ;

entityType
    :
    parts += Identifier ('.' parts += Identifier)*
    ;

Identifier
    :
    [A-Za-z][A-Za-z0-9]*
    ;

Number
    :
    [0-9]+
    ;

WS
    :
    [ \r\t\n]+ -> skip
    ;