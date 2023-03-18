import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Tokenizer {
    public final String s;
    private int p;
    private final int len;

    public Tokenizer(String s){
        this.s = s;
        this.p = 0;
        this.len = s.length();
    }

    public List<Token> tokenize(){
        List<Token> tokens = new ArrayList<>();

        while(p < len){
            char c = s.charAt(p);
            if(c == ';'){
                tokens.add(new Token(TokenType.SEMI_COLUMN, String.valueOf(c)));
                p++;
            }
            else if (c == '.'){
                tokens.add(new Token(TokenType.END, String.valueOf(c)));
                p++;
            }
            else if (c == ','){
                tokens.add(new Token(TokenType.COMMA, String.valueOf(c)));
                p++;
            }
            else if (c == '('){
                tokens.add(new Token(TokenType.PARENTHESIS_LEFT, String.valueOf(c)));
                p++;
            }
            else if (c == ')'){
                tokens.add(new Token(TokenType.PARENTHESIS_RIGHT, String.valueOf(c)));
                p++;
            }
            else if (c == '{'){
                tokens.add(new Token(TokenType.CURLY_BRACKET_LEFT, String.valueOf(c)));
                p++;
            }
            else if (c == '}'){
                tokens.add(new Token(TokenType.CURLY_BRACKET_RIGHT, String.valueOf(c)));
                p++;
            }
            else if (c == '['){
                tokens.add(new Token(TokenType.BRACKET_LEFT, String.valueOf(c)));
                p++;
            }
            else if (c == ']'){
                tokens.add(new Token(TokenType.BRACKET_RIGHT, String.valueOf(c)));
                p++;
            }
            else if(c == '<'){
                p++;
                if(s.charAt(p) == '-'){
                    tokens.add(new Token(TokenType.ASSIGNMENT, "<-"));
                    p++;
                }
                else if(s.charAt(p) == '='){
                    tokens.add(new Token(TokenType.LESS_EQUAL, "<="));
                    p++;
                }
                else{
                    tokens.add(new Token(TokenType.LESS, "<"));
                }
            }
            else if(c == '>'){
                p++;
                if(s.charAt(p) == '='){
                    tokens.add(new Token(TokenType.GREATER_EQUAL, ">="));
                    p++;
                }
                else{
                    tokens.add(new Token(TokenType.GREATER, ">"));
                }
            }
            else if(c == '='){
                p++;
                if(s.charAt(p) == '='){
                    tokens.add(new Token(TokenType.EQUAL, "=="));
                    p++;
                }
            }
            else if(c == '!'){
                p++;
                if(s.charAt(p) == '='){
                    tokens.add(new Token(TokenType.NOT_EQUAL, "!="));
                    p++;
                }
            }
            else if(c == '+'){
                tokens.add(new Token(TokenType.PLUS, String.valueOf(c)));
                p++;
            }
            else if(c == '-'){
                tokens.add(new Token(TokenType.MINUS, String.valueOf(c)));
                p++;
            }
            else if(c == '*'){
                tokens.add(new Token(TokenType.MULTIPLY, String.valueOf(c)));
                p++;
            }
            else if(c == '/'){
                tokens.add(new Token(TokenType.DIVIDE, String.valueOf(c)));
                p++;
            }
            else if(Character.isDigit(c)){
                tokens.add(new Token(TokenType.NUMBER, readWord()));
            }
            else if(Character.isAlphabetic(c)){
                String word = readWord();
                if("main".equals(word)){
                    tokens.add(new Token(TokenType.MAIN, word));
                }
                else if("var".equals(word)){
                    tokens.add(new Token(TokenType.VAR, word));
                }
                else if("let".equals(word)){
                    tokens.add(new Token(TokenType.LET, word));
                }
                else if("array".equals(word)){
                    tokens.add(new Token(TokenType.ARRAY, word));
                }
                else if("void".equals(word)){
                    tokens.add(new Token(TokenType.VOID, word));
                }
                else if("function".equals(word)){
                    tokens.add(new Token(TokenType.FUNCTION, word));
                }
                else if("return".equals(word)){
                    tokens.add(new Token(TokenType.RETURN, word));
                }
                else if("call".equals(word)){
                    tokens.add(new Token(TokenType.CALL, word));
                }
                else if("if".equals(word)){
                    tokens.add(new Token(TokenType.IF, word));
                }
                else if("then".equals(word)){
                    tokens.add(new Token(TokenType.THEN, word));
                }
                else if("else".equals(word)){
                    tokens.add(new Token(TokenType.ELSE, word));
                }
                else if("fi".equals(word)){
                    tokens.add(new Token(TokenType.FI, word));
                }
                else if("while".equals(word)){
                    tokens.add(new Token(TokenType.WHILE, word));
                }
                else if("do".equals(word)){
                    tokens.add(new Token(TokenType.DO, word));
                }
                else if("od".equals(word)){
                    tokens.add(new Token(TokenType.OD, word));
                }
                else if("InputNum".equals(word)){
                    tokens.add(new Token(TokenType.INPUT_NUM, word));
                }
                else if("OutputNum".equals(word)){
                    tokens.add(new Token(TokenType.OUTPUT_NUM, word));
                }
                else if("OutputNewLine".equals(word)){
                    tokens.add(new Token(TokenType.OUTPUT_NEWLINE, word));
                }
                else{
                    tokens.add(new Token(TokenType.IDENTIFIER, word));
                }
            }
            else{
                p++;
            }

        }


        return Collections.unmodifiableList(tokens);
    }


    private String readWord(){
        StringBuilder result = new StringBuilder();
        if (this.p >= this.len){
            return "";
        }

        while(Character.isDigit(s.charAt(p)) || Character.isAlphabetic(s.charAt(p))){
            result.append(s.charAt(p));
            p++;
        }

        return result.toString();
    }

    public enum TokenType{
        // Basic
        MAIN,
        SEMI_COLUMN,
        COMMA,
        END,

        // Parenthesis
        PARENTHESIS_LEFT,
        PARENTHESIS_RIGHT,
        CURLY_BRACKET_LEFT,
        CURLY_BRACKET_RIGHT,
        BRACKET_LEFT,
        BRACKET_RIGHT,


        // Variable Assignment
        LET,
        VAR,
        ARRAY,
        IDENTIFIER,
        ASSIGNMENT,
        NUMBER,

        // Operator
        PLUS,
        MINUS,
        MULTIPLY,
        DIVIDE,

        // Compare
        EQUAL,
        NOT_EQUAL,
        GREATER,
        LESS,
        GREATER_EQUAL,
        LESS_EQUAL,

        // Function
        VOID,
        FUNCTION,
        RETURN,
        CALL,

        // If Statement
        IF,
        THEN,
        ELSE,
        FI,

        // While Loop
        WHILE,
        DO,
        OD,

        // Predefined Functions
        INPUT_NUM,
        OUTPUT_NUM,
        OUTPUT_NEWLINE,

    };
    public static class Token{
        public TokenType type;
        public String value;

        public Token(TokenType type, String value){
            this.type = type;
            this.value = value;
        }
    }

    public static void main(String[] args){
        Tokenizer tokenizer = new Tokenizer("computation var i <- 2 * 3; var abracadabra <- 7; (((abracadabra * i))); i - 5 - 1 . ");
        List<Token> tokens = tokenizer.tokenize();
        System.out.println(tokens.size());
    }

}
