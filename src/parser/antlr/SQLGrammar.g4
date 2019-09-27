grammar SQLGrammar;
// Batch 0 Tokenizer
OPERATOR: '==' | '!=' | '<' | '>' | '<=' | '>=' ;
ALPHA: [a-zA-Z_] ;
DIGIT : [0-9] ;
integer: DIGIT+;
identifier: ALPHA (ALPHA | DIGIT)+ ;
string_literal: '"' (ALPHA | DIGIT)+ '"' ;
// Batch 1
literal: (string_literal | integer) ;
relation_name: identifier ;
attribute_name : identifier;
operand : (attribute_name | literal) ;
type : (('VARCHAR' (integer)) | 'INTEGER') ; // IDK if this works as intended


// Needs whitespace rule from Batch 0 at the end
// Add whitespace rule here