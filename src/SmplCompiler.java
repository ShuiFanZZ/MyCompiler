import java.io.*;
import java.util.Scanner;

public class SmplCompiler {
    public static void main(String[] args) throws FileNotFoundException {
        String FILE_DIR = "test/";
        String FILE_NAME = "test.txt";
        String filepath = FILE_DIR + FILE_NAME;


        if(args.length == 1){
            filepath = args[0];
        }

        String output_path = filepath + ".dot";

        if(args.length == 2){
            output_path = args[1];

        }
        String exp = new Scanner(new File(filepath)).useDelimiter("\\Z").next();


        Tokenizer tokenizer = new Tokenizer(exp);

        Parser parser = new Parser(tokenizer.tokenize());
        parser.interpret();
        String graph_text = parser.getSSA();
        PrintStream out = new PrintStream(new FileOutputStream(output_path));
        out.print(graph_text);
    }
}
