import java.util.*;

public class Parser {
    private final List<Tokenizer.Token> tokenList;
    private int p;
    private Tokenizer.Token token;
    private int len;
    private int heapPointer = 0;

    private BasicBlockManager BBManager = new BasicBlockManager();

    private Map<String, Integer> instructionHistoryMap = new HashMap<>();

    private Map<String, List<Integer>> arrayDimensionsMap = new HashMap<>();

    public Parser(List<Tokenizer.Token> tokens){
        this.tokenList = tokens;
        this.p = 0;
        if(this.tokenList.size() != 0){
            this.token = this.tokenList.get(0);
        }else {
            this.token = null;
        }
        this.len = tokens.size();


    }

    public void interpret(){
        computation();

    }

    public void printSSA(){
        this.BBManager.toGraph();
    }

    public String getSSA(){
        return this.BBManager.toGraph();
    }

    private void next(){
        this.p += 1;

        if(p < len){
            this.token = tokenList.get(p);
        }
        else{
            this.token = null;
        }

    }


    private void computation(){
        if(this.token.type == Tokenizer.TokenType.MAIN){
            next();
            while (token.type == Tokenizer.TokenType.VAR ||
                    token.type == Tokenizer.TokenType.ARRAY){
                varDecl();
            }
            while (token.type == Tokenizer.TokenType.VOID ||
                    token.type == Tokenizer.TokenType.FUNCTION){
                funcDecl();
            }

            if (token.type == Tokenizer.TokenType.CURLY_BRACKET_LEFT){
                next();
            }

            statSequence();

            if (token.type == Tokenizer.TokenType.CURLY_BRACKET_RIGHT){
                next();
            }

            if (token.type == Tokenizer.TokenType.END){
                next();
            }

        }


    }

    private void funcBody(){
        while (token.type == Tokenizer.TokenType.VAR ||
                token.type == Tokenizer.TokenType.ARRAY){
            varDecl();
        }

        if (token.type == Tokenizer.TokenType.CURLY_BRACKET_LEFT){
            next();
        }

        statSequence();

        if (token.type == Tokenizer.TokenType.CURLY_BRACKET_RIGHT){
            next();
        }
    }

    private void formalParam(){
        if (token.type == Tokenizer.TokenType.PARENTHESIS_LEFT){
            next();
        }

        if(token.type == Tokenizer.TokenType.IDENTIFIER){
            next();

            while(token.type == Tokenizer.TokenType.COMMA){
                next();
                if(token.type == Tokenizer.TokenType.IDENTIFIER){
                    next();
                }
            }
        }

        if (token.type == Tokenizer.TokenType.PARENTHESIS_RIGHT){
            next();
        }
    }

    private void funcDecl(){
        boolean isVoid = false;
        if(token.type == Tokenizer.TokenType.VOID){
            next();
            isVoid = true;
        }

        if(token.type == Tokenizer.TokenType.FUNCTION){
            next();
        }

        String identifier = token.value;
        next();

        formalParam();

        if(token.type == Tokenizer.TokenType.SEMI_COLUMN){
            next();
        }

        funcBody();

        if(token.type == Tokenizer.TokenType.SEMI_COLUMN){
            next();
        }
    }


    private void varDecl(){
        List<Integer> dimensions = typeDecl();
        boolean is_array = dimensions.size() > 0;
        int size = 1;
        if(is_array){
            for(int d : dimensions){
                size *= d;
            }
            size *= 4;
        }
        String identifier = token.value;
        next();

        if(is_array){
            if(heapPointer == 0){
                BBManager.constantBlock.variableMap.put(identifier, 0);
            }else{
                int addr = BBManager.addConstant(heapPointer);
                BBManager.constantBlock.variableMap.put(identifier, addr);
            }
            heapPointer += size;
            arrayDimensionsMap.put(identifier, dimensions);
        }else{
            BBManager.currentBlock.variableMap.put(identifier, 0);
        }


        while(token.type == Tokenizer.TokenType.COMMA){
            next();
            identifier = token.value;
            if(is_array){
                if(heapPointer == 0){
                    BBManager.constantBlock.variableMap.put(identifier, 0);
                }else{
                    int addr = BBManager.addConstant(heapPointer);
                    BBManager.constantBlock.variableMap.put(identifier, addr);
                }
                arrayDimensionsMap.put(identifier, dimensions);
                heapPointer += size;
            }else{
                BBManager.currentBlock.variableMap.put(identifier, 0);
            }
            next();
        }

        if(token.type == Tokenizer.TokenType.SEMI_COLUMN){
            next();
        }

    }

    private List<Integer> typeDecl(){
        List<Integer> dimensions = new ArrayList<>();
        if(token.type == Tokenizer.TokenType.VAR){
            next();
        }else if(token.type == Tokenizer.TokenType.ARRAY){
            next();
            // Add size constant
            this.BBManager.addConstant(4);

            if(token.type == Tokenizer.TokenType.BRACKET_LEFT){
                next();
            }

            int n = Integer.parseInt(token.value);
            next();
            dimensions.add(n);

            if(token.type == Tokenizer.TokenType.BRACKET_RIGHT){
                next();
            }

            while(token.type == Tokenizer.TokenType.BRACKET_LEFT){
                next();

                // Read array length
                n = Integer.parseInt(token.value);
                next();
                dimensions.add(n);

                if(token.type == Tokenizer.TokenType.BRACKET_RIGHT){
                    next();
                }
            }

        }

        return dimensions;
    }

    private void statSequence(){
        statement();

        while(token.type == Tokenizer.TokenType.SEMI_COLUMN){
            next();
            statement();
        }

//        if(token.type == Tokenizer.TokenType.SEMI_COLUMN){
//            next();
//        }
    }

    private void statement(){
        if(token.type == Tokenizer.TokenType.LET){
            assignment();
        }
        else if(token.type == Tokenizer.TokenType.CALL){
            funcCall();
        }
        else if(token.type == Tokenizer.TokenType.IF){
            ifStatement();
        }
        else if(token.type == Tokenizer.TokenType.WHILE){
            whileStatement();
        }
        else if(token.type == Tokenizer.TokenType.RETURN){
            returnStatement();
        }else{
            return;
        }
    }


    private void returnStatement(){
        if(token.type == Tokenizer.TokenType.RETURN){
            next();
            if (token.type == Tokenizer.TokenType.NUMBER ||
                    token.type == Tokenizer.TokenType.IDENTIFIER ||
                    token.type == Tokenizer.TokenType.CALL){
                expression();
            }
        }
    }

    private void whileStatement(){
        if(token.type == Tokenizer.TokenType.WHILE){
            next();
        }
        BasicBlockManager.BasicBlock previousBlock = BBManager.currentBlock;
        BasicBlockManager.BasicBlock joinBlock = BBManager.createWhileJoinBlock();

        BBManager.currentBlock = joinBlock;

        BasicBlockManager.BasicBlock bodyBlock = BBManager.pseudoBlock;
        BasicBlockManager.BasicBlock followBlock = BBManager.pseudoBlock;

        // Parse join block
        List<Instruction> relationInstructions = relation();
        Instruction pseudoInstruction = relationInstructions.get(0);
        Instruction cmpInstruction = relationInstructions.get(1);
        Instruction branchOnInstruction = relationInstructions.get(2);
        //BBManager.currentBlock = previousBlock;
        boolean skip_body = false;
        if(pseudoInstruction.name.equals("<false>")){ // we know in advance that we won't enter the loop
            skip_body = true;
            followBlock = previousBlock;
            BBManager.removeBlock(joinBlock);
        }
        else {
            int cmpInstructionID = joinBlock.addInstruction(cmpInstruction.name, cmpInstruction.params, cmpInstruction.paramsInfo);
            branchOnInstruction.params.add(cmpInstructionID);
            joinBlock.addInstruction(branchOnInstruction);

            bodyBlock = BBManager.createBlock();

            followBlock = BBManager.createBlock();
            BBManager.buildWhileBlocks(previousBlock, joinBlock, bodyBlock, followBlock);
        }


        if(token.type == Tokenizer.TokenType.DO){
            next();
        }

        BBManager.currentBlock = bodyBlock;
        statSequence();

        if(token.type == Tokenizer.TokenType.OD){
            next();
        }

        if(skip_body){
            BBManager.currentBlock = previousBlock;
            return;
        }

        // If body block is nested, set it to the end
        while(bodyBlock.join != joinBlock){
            bodyBlock = bodyBlock.join;
        }



        HashMap<String, int[]> variableChnageMap = new HashMap<>();// Variable : <old id, new id> in join block
        for(String var : joinBlock.variableMap.keySet()){
            if(bodyBlock.variableMap.containsKey(var)){
                int addr1 = joinBlock.variableMap.get(var);
                int addr2 = bodyBlock.variableMap.get(var);
                if(addr1 != addr2){

                    int addr3 = joinBlock.insertInstruction("phi", Arrays.asList(addr1, addr2));
                    joinBlock.variableMap.put(var, addr3);
                    variableChnageMap.put(var, new int[]{addr1, addr3});
                }
            }
        }

        Map<Integer, Integer> repeatedLoadsMap = joinBlock.findRepeatedLoadsInWhileJoin();
        // Substitute all instruction params that have been modified to corresponding phi functions
        for(Instruction instr : joinBlock.instructions){
            List<Integer> newParams = new ArrayList<>();
            if(instr.name.equals("phi")){
                continue;
            }
            for(int i = 0; i < instr.params.size(); i++){
                if(i < instr.paramsInfo.size() && variableChnageMap.containsKey(instr.paramsInfo.get(i))){
                    int[] addr_pair = variableChnageMap.get(instr.paramsInfo.get(i));
                    if(addr_pair[0] == instr.params.get(i)){
                        newParams.add(addr_pair[1]);
                    }else{
                        newParams.add(instr.params.get(i));
                    }
                }else{
                    newParams.add(instr.params.get(i));
                }


            }
//            for(int param : instr.params){
//                newParams.add(variableChnageMap.getOrDefault(param, param));
//            }
            for(int i = 0; i < newParams.size(); i++){
                if(repeatedLoadsMap.containsKey(newParams.get(i))){
                    newParams.set(i, repeatedLoadsMap.get(newParams.get(i)));
                }
            }
            instr.params = newParams;

        }

        // Traverse all blocks that are dominated by join block, and substitute variable address involved in phi func
        for(BasicBlockManager.BasicBlock block : BBManager.blocks){
            if(block.domBlocks.contains(joinBlock)){
                // Update each variable in variable map if it is still using old values, skip if the value has been changed
                for(String changed_var : variableChnageMap.keySet()){
                    int old_val = variableChnageMap.get(changed_var)[0];
                    int new_val = variableChnageMap.get(changed_var)[1];

                    if(block.variableMap.get(changed_var) == old_val){
                        block.variableMap.put(changed_var, new_val);
                    }
                }
                // Update the address of the instruction
                for(Instruction instr : block.instructions){
                    List<Integer> newParams = new ArrayList<>();
//                    for(int param : instr.params){
//                        newParams.add(variableChnageMap.getOrDefault(param, param));
//                    }
                    for(int i = 0; i < instr.params.size(); i++){
                        if(i < instr.paramsInfo.size() && variableChnageMap.containsKey(instr.paramsInfo.get(i))){
                            int[] addr_pair = variableChnageMap.get(instr.paramsInfo.get(i));
                            if(addr_pair[0] == instr.params.get(i)){
                                newParams.add(addr_pair[1]);
                            }else{
                                newParams.add(instr.params.get(i));
                            }
                        }else{
                            newParams.add(instr.params.get(i));
                        }


                    }

                    for(int i = 0; i < newParams.size(); i++){
                        if(repeatedLoadsMap.containsKey(newParams.get(i))){
                            newParams.set(i, repeatedLoadsMap.get(newParams.get(i)));
                        }
                    }
                    instr.params = newParams;
                }
            }
        }

        // Add branch instruction in the last body block back to join block
        ArrayList<Integer> branchTo = new ArrayList<>();
        bodyBlock.addInstruction("bra", branchTo);
        branchTo.add(BBManager.getFirstInstructionID(joinBlock));
        // The branch instruction of the while loop condition points to follow block
        branchOnInstruction.params.add(BBManager.getFirstInstructionID(followBlock));


        followBlock.variableMap = new HashMap<>(joinBlock.variableMap);
        //followBlock.instructionTable = new HashMap<>(joinBlock.instructionTable);
        BBManager.currentBlock = followBlock;
    }

    private void ifStatement(){
        if(token.type == Tokenizer.TokenType.IF){
            next();
        }
        boolean skip_then = false;
        boolean skip_else = false;
        List<Instruction> relationInstructions = relation();
        Instruction pseudoInstruction = relationInstructions.get(0);
        Instruction cmpInstruction = relationInstructions.get(1);
        Instruction branchConditionInstruction = relationInstructions.get(2);
        if(pseudoInstruction.name.equals("<true>")){
            skip_else = true;
        }else if(pseudoInstruction.name.equals("<false>")){
            skip_then = true;
        }else{
            int cmpInstructionID = BBManager.currentBlock.addInstruction(cmpInstruction.name, cmpInstruction.params, cmpInstruction.paramsInfo);
            branchConditionInstruction.params.add(cmpInstructionID);
            BBManager.currentBlock.addInstruction(branchConditionInstruction);
        }


        BasicBlockManager.BasicBlock ifBlock = BBManager.currentBlock;
        BasicBlockManager.BasicBlock elseBlock = BBManager.pseudoBlock;
        BasicBlockManager.BasicBlock thenBlock = BBManager.pseudoBlock;
        BasicBlockManager.BasicBlock joinBlock = BBManager.pseudoBlock;



        if(!skip_else && !skip_then){
            elseBlock = BBManager.createBlock();
            thenBlock = BBManager.createBlock();
            joinBlock = BBManager.createBlock();
            BBManager.buildIfBlocks(thenBlock, elseBlock, joinBlock);
        }
        else if (skip_else){
            thenBlock = BBManager.currentBlock;
            joinBlock = BBManager.currentBlock;
        }else {
            elseBlock = BBManager.currentBlock;
            joinBlock = BBManager.currentBlock;
        }



        if(token.type == Tokenizer.TokenType.THEN){
            next();
        }else{
            System.out.println("Missing then-statement");
            System.exit(-1);
        }

        BBManager.currentBlock = thenBlock;
        statSequence();


        if(token.type == Tokenizer.TokenType.ELSE){
            next();

            BBManager.currentBlock = elseBlock;
            statSequence();

        }

        if(token.type == Tokenizer.TokenType.FI){
            next();
        }else{
            System.out.println("Expecting fi to end an if-statement");
            System.exit(-1);
        }

        if(skip_else || skip_then){
            while(joinBlock.join != null){
                joinBlock = joinBlock.join;
            }
            BBManager.currentBlock = joinBlock;
            return;
        }

        // Join Process Begins Here
        while(thenBlock.join != joinBlock){
            thenBlock = thenBlock.join;
        }
        ArrayList<Integer> branchTo = new ArrayList<>();
        thenBlock.addInstruction("bra", branchTo);
        branchTo.add(BBManager.getFirstInstructionID(joinBlock));

        branchConditionInstruction.params.add(BBManager.getFirstInstructionID(elseBlock));


        while(elseBlock.join != joinBlock){
            elseBlock = elseBlock.join;
        }

        for(String var : joinBlock.variableMap.keySet()){
            if(thenBlock.variableMap.containsKey(var) && elseBlock.variableMap.containsKey(var)){
                int addr1 = thenBlock.variableMap.get(var);
                int addr2 = elseBlock.variableMap.get(var);
                if(addr1 != addr2){
                    int addr3 = joinBlock.addInstruction("phi", Arrays.asList(addr1, addr2));
                    joinBlock.variableMap.put(var, addr3);
                }
            }
        }



        BBManager.currentBlock = joinBlock;
    }

    private Result funcCall(){
        if(token.type == Tokenizer.TokenType.CALL){
            next();
        }

        String functionName = token.value;
        next();

        List<Result> results = new ArrayList<>();
        if(token.type == Tokenizer.TokenType.PARENTHESIS_LEFT){
            next();

            results.add(expression());

            while(token.type == Tokenizer.TokenType.COMMA){
                next();
                results.add(expression());
            }
        }

        if(token.type == Tokenizer.TokenType.PARENTHESIS_RIGHT){
            next();
        }

        int instuctionID = 0;
        if(functionName.equals("InputNum")){
            instuctionID = BBManager.currentBlock.addInstruction("read");
        }else if(functionName.equals("OutputNum")){

            instuctionID = BBManager.currentBlock.addInstruction("write",
                    Arrays.asList(getInstructionID(results.get(0))),
                    Arrays.asList(results.get(0).identifier));
        }

        return new Result(Result.Type.register, instuctionID);
    }

    private void assignment(){
        if(token.type == Tokenizer.TokenType.LET){
            next();
        }

        Result id_result = designator();
        String identifier = id_result.identifier;

        if(token.type == Tokenizer.TokenType.ASSIGNMENT){
            next();
        }else{
            System.out.println("Expecting Assignment Symbol: <-");
            System.exit(-1);
        }

        Result result = expression();
        if(id_result.type == Result.Type.variable){
            BBManager.currentBlock.variableMap.put(identifier, getInstructionID(result));
        }
        else if(id_result.type == Result.Type.array){
            BBManager.currentBlock.addInstruction("store", Arrays.asList(getInstructionID(result), id_result.value, id_result.offset));
        }
    }

    private List<Instruction> relation(){
        Result r1 = expression();
        String op_name = "<empty>";
        Tokenizer.TokenType compType = token.type;
        if(token.type == Tokenizer.TokenType.EQUAL){
            next();
            op_name = "bne";
        }
        else if(token.type == Tokenizer.TokenType.NOT_EQUAL){
            next();
            op_name = "beq";
        }
        else if(token.type == Tokenizer.TokenType.GREATER){
            next();
            op_name = "ble";
        }
        else if(token.type == Tokenizer.TokenType.GREATER_EQUAL){
            next();
            op_name = "blt";
        }
        else if(token.type == Tokenizer.TokenType.LESS){
            next();
            op_name = "bge";
        }
        else if(token.type == Tokenizer.TokenType.LESS_EQUAL){
            next();
            op_name = "bgt";
        }else{
            System.out.println(String.format("Expecting relation symbol but got \"%s\".", token.value));
            System.exit(-1);
        }

        Result r2 = expression();

        // If both results are constant, then we know the comparison in advance
        List<Instruction> instructions = new ArrayList<>();

        if(r1.type == Result.Type.constant && r2.type == Result.Type.constant){
            boolean comp = switch (compType){
                case EQUAL -> r1.value == r2.value;
                case NOT_EQUAL -> r1.value != r2.value;
                case GREATER -> r1.value > r2.value;
                case GREATER_EQUAL -> r1.value >= r2.value;
                case LESS -> r1.value < r2.value;
                case LESS_EQUAL -> r1.value <= r2.value;
                default -> false;
            };

            if(comp){
                op_name = "<true>";
            }else{
                op_name = "<false>";
            }
            instructions.add(new Instruction(0, op_name, Collections.emptyList()));
        }else{
            instructions.add(new Instruction(0, "<unknown>", Collections.emptyList()));
        }



        Instruction cmpInstruction = new Instruction(0, "cmp",
                Arrays.asList(getInstructionID(r1), getInstructionID(r2)),
                Arrays.asList(r1.identifier, r2.identifier));
        Instruction branchInstruction = new Instruction(0, op_name, new ArrayList<>());
        instructions.add(cmpInstruction);
        instructions.add(branchInstruction);
        return instructions;

    }


    private Result expression(){
        Result r1 = term();
        while(token.type == Tokenizer.TokenType.PLUS ||
            token.type == Tokenizer.TokenType.MINUS){
            if (token.type == Tokenizer.TokenType.PLUS){
                next();
                Result r2 = term();
                r1 = calculate(r1, r2, '+');
            }else {
                next();
                Result r2 = term();
                r1 = calculate(r1, r2, '-');
            }
        }


        return r1;
    }

    private Result calculate(Result r1, Result r2, char op){
        Result result = null;
        if(r1.type == Result.Type.constant && r2.type == Result.Type.constant){
            int new_val = switch (op) {
                case '+' -> r1.value + r2.value;
                case '-' -> r1.value - r2.value;
                case '/' -> r1.value / r2.value;
                case '*' -> r1.value * r2.value;
                default -> 0;
            };

            result = new Result(Result.Type.constant, new_val);
        }else{


            String op_name = switch (op) {
                case '+' -> "add";
                case '-' -> "sub";
                case '/' -> "div";
                case '*' -> "mul";
                default -> "";
            };
            int addr1 = getInstructionID(r1);
            int addr2 = getInstructionID(r2);

            // both pointing to constant instructions
//            if(BBManager.constantInstructionMap.containsKey(addr1) &&
//                    BBManager.constantInstructionMap.containsKey(addr2)){
//                return calculate(new Result(Result.Type.constant, BBManager.constantInstructionMap.get(addr1)),
//                        new Result(Result.Type.constant, BBManager.constantInstructionMap.get(addr2)), op);
//            }


            int instructionID = BBManager.currentBlock.addInstruction(op_name,
                    Arrays.asList(addr1, addr2),
                    Arrays.asList(r1.identifier, r2.identifier));
            result = new Result(Result.Type.register, instructionID);
        }

        return result;
    }

    private int getInstructionID(Result result){
        if(result.type == Result.Type.register){
            return result.value;
        }
        else if(result.type == Result.Type.constant){

            return BBManager.addConstant(result.value);
        }
        else if(result.type == Result.Type.array){
            return BBManager.currentBlock.addInstruction("load", Arrays.asList(result.value, result.offset));
        }
        else {
            return BBManager.currentBlock.getVariableAddress(result.identifier);
        }
    }

    private Result term(){
        Result r1 = factor();

        while(token.type == Tokenizer.TokenType.MULTIPLY ||
                token.type == Tokenizer.TokenType.DIVIDE){
            if(token.type == Tokenizer.TokenType.MULTIPLY){
                next();
                Result r2 = factor();
                r1 = calculate(r1, r2, '*');
            }else {
                next();
                Result r2 = factor();
                r1 = calculate(r1, r2, '/');
            }
        }

        return r1;
    }

    private Result factor(){
        Result result = null;
        if(token.type == Tokenizer.TokenType.IDENTIFIER){
            result = designator();

//            if(BBManager.constantInstructionMap.containsKey(result.value)){
//                result = new Result(Result.Type.constant, BBManager.constantInstructionMap.get(result.value));
//            }else if(result.value == 0){
//                result = new Result(Result.Type.constant, 0);
//            }

//            if(Result.Type.array == result.type){
//                result.value = BBManager.currentBlock.addInstruction("load", Arrays.asList(result.value));
//                result.type = Result.Type.register;
//                result.identifier = "<Addr>";
//            }

        }else if(token.type == Tokenizer.TokenType.NUMBER){
            result = new Result(Result.Type.constant, Integer.parseInt(token.value));
            next();
        }else if(token.type == Tokenizer.TokenType.PARENTHESIS_LEFT){
            next();
            result = expression();
            if(token.type == Tokenizer.TokenType.PARENTHESIS_RIGHT){
                next();
            }
        }else if(token.type == Tokenizer.TokenType.CALL){
            result = funcCall();
        }

        return result;
    }

    private Result designator(){

        String identifier = token.value;
        next();


        Result result = new Result(Result.Type.variable, 0);
        result.identifier = identifier;

        if(token.type != Tokenizer.TokenType.BRACKET_LEFT){
            result.value =  BBManager.currentBlock.getVariableAddress(identifier);
            return result;
        }

        // If Array
        result.type = Result.Type.array;

        List<Integer> dimension_addr = new ArrayList<>();
        while(token.type == Tokenizer.TokenType.BRACKET_LEFT){
            next();

            Result r = expression();
            int addr2 = getInstructionID(r);
            dimension_addr.add(addr2);


            if(token.type == Tokenizer.TokenType.BRACKET_RIGHT){
                next();
            }

        }

        int[] array_addr = findArrayAddress(identifier, dimension_addr);
        result.value = array_addr[0];
        result.offset = array_addr[1];

        return result;
    }

    private String hashArrayElement(String identifier, List<Integer> dim_addresses){
        StringBuilder hashBuilder = new StringBuilder(identifier);
        for(int i : dim_addresses){
            hashBuilder.append(String.format("(%d)", i));
        }
        return hashBuilder.toString();
    }

    private int[] findArrayAddress(String identifier, List<Integer> dim_addresses){

        // TODO: decide whether to add a hashmap
//        String hash = hashArrayElement(identifier, dim_addresses);
//        if(BBManager.currentBlock.variableMap.containsKey(hash)){
//            return BBManager.currentBlock.variableMap.get(hash);
//        }

        List<Integer> dimensions = arrayDimensionsMap.get(identifier);
        int d = 1;
        int addr1 = 0;

        for(int dim_address : dim_addresses){
            int addr2 = dim_address;
            int multiplier = 1;
            for(int i = d; i < dimensions.size(); i++){
                multiplier *= dimensions.get(i);
            }
            if(multiplier > 1){
                int multiplier_addr = BBManager.addConstant(multiplier);
                addr2 = BBManager.currentBlock.addInstruction("mul", Arrays.asList(addr2, multiplier_addr));
            }
            if(addr1 == 0){
                addr1 = addr2;
            }else{
                addr1 = BBManager.currentBlock.addInstruction("add", Arrays.asList(addr1, addr2));
            }

            d++;
        }

        int multiplier_addr = BBManager.addConstant(4);
        int off_set = BBManager.currentBlock.addInstruction("mul", Arrays.asList(addr1, multiplier_addr));
        int base_addr = BBManager.currentBlock.addInstruction("add (BASE)", Arrays.asList(BBManager.constantBlock.getVariableAddress(identifier)));
        //int addr = BBManager.currentBlock.addInstruction("adda", Arrays.asList(base_addr, off_set));
        return new int[]{base_addr, off_set};
    }

    static class Result{
        enum Type{
            constant,
            variable,
            register,
            array
        }

        public Type type;
        public int value;
        public String identifier = "<Addr>";
        public int offset = 0;


        public Result(Type type, int value) {
            this.type = type;
            this.value = value;
        }
    }

    public static void main(String[] args){
//        System.out.println("Enter your expression (press ENTER to finish):");
//        Scanner scanner = new Scanner(System.in);
//        String exp = scanner.nextLine();
//
//        String exp = """
//                        main\s
//                        var a, b, c, d, e; {\s
//                           let a <- call InputNum();\s
//                           let b <- a;\s
//                           let c <- 1;\s
//                           let d <- b + c ;\s
//                           let e <- a + b;\s
//                           if 1<0
//                           then
//                               let d<-a+b;
//
//                           else
//                                let d<-b+c;
//                                if e > 1 then
//                                    let c<-e+d;
//                                else
//                                    let e<-c;
//                                fi;
//
//                           fi;
//                           call OutputNum(a*b)\s
//                        }.\s
//                """;

//        String exp = """
//                        main\s
//                        var a, b, c, d, e; {\s
//                           let a <- call InputNum();\s
//                           let b <- 2;\s
//                           let c <- 1;\s
//                           let d <- b+c;
//                           call OutputNum(a*d)\s
//                        }.\s
//                """;

        String exp = """
                main
                var a,b,c;
                {
                	let a <- 2000;
                	let b <- 10 + 10;
                	while a > 0 do
                		let a <- a - (10 + 10);
                	od;
                }.
                
                """;
        Tokenizer tokenizer = new Tokenizer(exp);

        Parser parser = new Parser(tokenizer.tokenize());
        parser.interpret();
        parser.printSSA();
    }
}
