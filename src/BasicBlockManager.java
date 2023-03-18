import java.util.*;

public class BasicBlockManager {
    public List<BasicBlock> blocks = new ArrayList<>();
    public BasicBlock constantBlock;
    public BasicBlock currentBlock;
    public BasicBlock pseudoBlock;
    public int numBlocks = 2;
    public int instructionIDPtr = 1;
    public Map<Integer, Integer> constantInstructionMap = new HashMap<>();


    public BasicBlockManager(){
        this.constantBlock = new BasicBlock("BB0", this);
        this.currentBlock = new BasicBlock("BB1", this);
        //blocks.add(constantBlock);
        blocks.add(currentBlock);
        //currentBlock.domBlocks.add(constantBlock);

        this.pseudoBlock = new BasicBlock("PseudoBlock", this);
        this.pseudoBlock.skip_mode = true;
    }

    public String toGraph(){
        StringBuilder sb = new StringBuilder();
        sb.append("digraph G {\n");
        sb.append(constantBlock.toString());
        for(BasicBlock bb: blocks){
            sb.append(bb.toString());
        }

        sb.append(String.format("%s:s -> %s:n;\n", constantBlock.name, blocks.get(0).name));
        for(BasicBlock bb: blocks){
            if(bb.fall_through!= null){
                sb.append(String.format("%s:s -> %s:n [color=\"green\", label=\"fall-through\"];\n", bb.name, bb.fall_through.name));
            }
            if(bb.branch != null){
                sb.append(String.format("%s:s -> %s:n [color=\"red\", label=\"branch\"];\n", bb.name, bb.branch.name));
            }
            for(BasicBlock dom: bb.domBlocks){
                sb.append(String.format("%s:s -> %s:n [color=blue, style=dotted, label=\"dom\"];\n", dom.name, bb.name));
            }
        }

        sb.append("}\n");
        System.out.println(sb.toString());
        return sb.toString();
    }

    public BasicBlock createBlock(){
        if(currentBlock.skip_mode){
            return currentBlock;
        }
        numBlocks ++;
        BasicBlock bb = new BasicBlock("BB" + (numBlocks - 1), this);
        this.blocks.add(bb);

        bb.domBlocks = new ArrayList<>(currentBlock.domBlocks);
        bb.domBlocks.add(currentBlock);
        bb.instructionTable = new HashMap<>(currentBlock.instructionTable);
        bb.variableMap = new HashMap<>(currentBlock.variableMap);
        bb.invalidatedAddressSet = new HashSet<>(currentBlock.invalidatedAddressSet);
        bb.is_inside_while = currentBlock.is_inside_while;
        return bb;
    }

    public BasicBlock createWhileJoinBlock(){
        BasicBlock bb = createBlock();
        bb.is_while_join = true;
        Instruction fake_kill = new Instruction(-1, "kill", Arrays.asList(-1), Arrays.asList(bb.name));
        fake_kill.next = bb.instructionTable.get("load");
        bb.instructionTable.put("load", fake_kill);
        return bb;
    }

    public int addConstant(int constant){
        if(constantBlock.variableMap.containsKey(String.valueOf(constant))){
            return constantBlock.variableMap.get(String.valueOf(constant));
        }else{
            int instructionID = constantBlock.addInstruction("const", constant);
            constantBlock.variableMap.put(String.valueOf(constant), instructionID);
            constantInstructionMap.put(instructionID, constant);
            return instructionID;
        }
    }

    public void removeBlock(BasicBlock bb){
        numBlocks --;
        this.blocks.remove(bb);
    }


    public void buildIfBlocks(BasicBlock thenBlock, BasicBlock elseBlock, BasicBlock joinBlock){
        if(currentBlock.skip_mode){
            return;
        }

        //joinBlock.name = "join\\n" + joinBlock.name;

        joinBlock.fall_through = currentBlock.fall_through;
        joinBlock.branch = currentBlock.branch;
        joinBlock.join = currentBlock.join;

        currentBlock.branch = elseBlock;
        currentBlock.fall_through = thenBlock;
        currentBlock.join = joinBlock;

        thenBlock.branch = joinBlock;
        thenBlock.join = joinBlock;
        elseBlock.fall_through = joinBlock;
        elseBlock.join = joinBlock;

//        elseBlock.domBlocks = new ArrayList<>(currentBlock.domBlocks);
//        elseBlock.domBlocks.add(currentBlock);
//        thenBlock.domBlocks = new ArrayList<>(currentBlock.domBlocks);
//        thenBlock.domBlocks.add(currentBlock);
//        joinBlock.domBlocks = new ArrayList<>(currentBlock.domBlocks);
//        joinBlock.domBlocks.add(currentBlock);
//
//        elseBlock.instructionTable = new HashMap<>(currentBlock.instructionTable);
//        thenBlock.instructionTable = new HashMap<>(currentBlock.instructionTable);
//        joinBlock.instructionTable = new HashMap<>(currentBlock.instructionTable);
//
//
//        thenBlock.variableMap = new HashMap<>(currentBlock.variableMap);
//        elseBlock.variableMap = new HashMap<>(currentBlock.variableMap);
//        joinBlock.variableMap = new HashMap<>(currentBlock.variableMap);
    }



    public void buildWhileBlocks(BasicBlock previousBlock, BasicBlock joinBlock, BasicBlock bodyBlock, BasicBlock followBlock){
        if(previousBlock.skip_mode){
            return;
        }
        followBlock.fall_through = previousBlock.fall_through;
        followBlock.join = previousBlock.join;
        followBlock.branch = previousBlock.branch;

        previousBlock.join = followBlock;
        previousBlock.fall_through = joinBlock;
        previousBlock.branch = null;

        joinBlock.branch = followBlock;
        joinBlock.fall_through = bodyBlock;
        joinBlock.join = followBlock;
        joinBlock.is_while_join = true;
        joinBlock.is_inside_while = true;

        bodyBlock.fall_through = joinBlock;
        bodyBlock.join = joinBlock;
        bodyBlock.branch = null;
        bodyBlock.is_inside_while = true;

//        followBlock.domBlocks.add(joinBlock);
//        bodyBlock.domBlocks.add(joinBlock);
    }


    public int getFirstInstructionID(BasicBlock block){
        if(block.instructions.isEmpty()){
            return block.addInstruction("<empty>");
        }

        return block.instructions.get(0).id;
    }

    public class BasicBlock{
        private String name;
        List<Instruction> instructions = new ArrayList<>();
        BasicBlock fall_through;
        BasicBlock branch;
        BasicBlock join;
        boolean skip_mode = false;
        boolean is_while_join = false;
        boolean is_inside_while = false;
        HashSet<Integer> invalidatedAddressSet = new HashSet<>();

        Map<String, Instruction> instructionTable = new HashMap<>(){{
            put("add", null);
            put("sub", null);
            put("mul", null);
            put("div", null);
            put("cmp", null);
            put("phi", null);
            put("add (BASE)", null);
            put("load", null);
        }};

        public Map<String, Integer> variableMap = new HashMap<>();

        BasicBlockManager BBManager;
        List<BasicBlock> domBlocks = new ArrayList<>();

        public BasicBlock(String name, BasicBlockManager manager){
            this.name = name;
            this.BBManager = manager;

        }

        public BasicBlock(BasicBlockManager manager){
            this.name = "";
            this.BBManager = manager;

        }

        public int insertInstruction(String name, List<Integer> params){
            return addInstruction(new Instruction(BBManager.instructionIDPtr, name, params), true);
        }

        public int addInstruction(String name, List<Integer> params){
            return addInstruction(new Instruction(BBManager.instructionIDPtr, name, params));
        }

        public int addInstruction(String name, List<Integer> params, List<String> paramsInfo){
            return addInstruction(new Instruction(BBManager.instructionIDPtr, name, params, paramsInfo));
        }

        public int addInstruction(String name){
            return addInstruction(new Instruction(BBManager.instructionIDPtr, name));
        }

        public int addInstruction(String name, int constant){
            return addInstruction(new Instruction(BBManager.instructionIDPtr, name, constant));
        }

        public int addInstruction(Instruction instruction){
            return addInstruction(instruction, false);
        }

        public int addInstruction(Instruction instruction, boolean insertFirst){
            if(BBManager.currentBlock.skip_mode){
                return BBManager.instructionIDPtr;
            }

            if("store".equals(instruction.name) && instruction.params.size()==3){
                return addStoreInstruction(instruction);

            }
            // Handle load instruction separately
            else if("load".equals(instruction.name) && instruction.params.size()==2){
                return addLoadInstruction(instruction);
            }

            int exist_id = findInstruction(instruction);
            if(exist_id != -1){
                return exist_id;
            }
            instruction.id = BBManager.instructionIDPtr;


            if(!instructions.isEmpty() && instructions.get(0).isEmpty()){
                instruction.id = instructions.get(0).id;
                instructions.set(0, instruction);
                return instruction.id;
            }else {
                if(insertFirst){
                   instructions.add(0, instruction);
                }else{
                    instructions.add(instruction);
                }
                return BBManager.instructionIDPtr++;
            }

        }

        public int addStoreInstruction(Instruction instruction){
            int value_addr = instruction.params.get(0);
            int array_addr = instruction.params.get(1);
            int off_set = instruction.params.get(2);
            killAddress(array_addr);
            // Kill the address in the join block as well
            BasicBlock joinBlock = this.join;
            while(joinBlock  != null){
                joinBlock.killAddress(array_addr);
                joinBlock = joinBlock.join;
            }
            int adda_id = addInstruction("adda", Arrays.asList(array_addr, off_set));

            return addInstruction("store", Arrays.asList(value_addr, adda_id));
        }

        public int addLoadInstruction(Instruction instruction){
            int array_addr = instruction.params.get(0);
            int off_set = instruction.params.get(1);
            instruction.paramsInfo = new ArrayList<>();
            Instruction exist = instructionTable.get("load");
            while(exist != null){
                if(exist.name.equals("kill") && exist.params.get(0).equals(array_addr)){
                    break;
                }
                if(instruction.hash().equals(exist.hash())){

                    instruction.paramsInfo.add(String.valueOf(exist.id));
                    if(is_inside_while){
                        break;
                    }else{
                        return exist.id;
                    }
                }
                exist = exist.next;
            }

            int adda_id = addInstruction("adda", Arrays.asList(array_addr, off_set));
            instructions.add(new Instruction(BBManager.instructionIDPtr, "load", Arrays.asList(adda_id), instruction.paramsInfo));
            instruction.id = BBManager.instructionIDPtr;

            instruction.next = instructionTable.get("load");
            instructionTable.put("load", instruction);

            return BBManager.instructionIDPtr++;
        }

        public Map<Integer, Integer> findRepeatedLoadsInWhileJoin(){
            Map<Integer, Integer> result = new HashMap<>();
            if(is_while_join){
                for(BasicBlock block : this.BBManager.blocks){
                    if(block.domBlocks.contains(this)){
                        result.putAll(block.findRepeatedLoads());

                    }
                }

            }
            for(int old_id : result.keySet()){
                int new_id = result.get(old_id);
                while(result.containsKey(new_id)){
                    new_id = result.get(new_id);
                }
                result.put(old_id, new_id);

            }

            return result;
        }

        private Map<Integer, Integer> findRepeatedLoads(){
            Map<Integer, Integer> result = new HashMap<>();
            for(int i = 0; i < instructions.size(); i++){
                Instruction instruction = instructions.get(i);
                if(instruction.name.equals("load") && !instruction.paramsInfo.isEmpty()){
                    int array_addr = instructions.get(i - 1).params.get(0);
                    int off_set = instructions.get(i - 1).params.get(1);
                    boolean repeated = false;
                    Instruction exist = instructionTable.get("load");
                    while(exist != null && exist.id != instruction.id){
                        exist = exist.next;

                    }
                    if(exist != null) exist = exist.next;
                    while(exist != null){
                        if(exist.name.equals("kill") && exist.params.get(0).equals(array_addr)){
                            break;
                        }
                        else if(exist.name.equals("load") &&
                                exist.params.get(0).equals(array_addr) &&
                                exist.params.get(1).equals(off_set))
                        {
                            repeated = true;
                            break;
                        }
                        exist = exist.next;
                    }

                    if(repeated){
                        result.put(instruction.id, exist.id);
                        instructions.get(i).isRemoved = true;
                        instructions.get(i-1).isRemoved = true;
                    }
                }
            }

            return result;
        }

        public int getVariableAddress(String identifier){
            if(this.variableMap.containsKey(identifier)){
                return this.variableMap.get(identifier);
            }

            this.variableMap.put(identifier, 0);
            return 0;
        }

        public void killAddress(int addr){
            if(is_while_join){
                Instruction instruction = instructionTable.get("load");
                while(instruction.params.get(0) != -1){

                    instruction = instruction.next;
                }
                Instruction fake_kill = new Instruction(-1, "kill", Arrays.asList(-1), Arrays.asList(this.name));
                fake_kill.next = instruction.next;
                instruction.next = fake_kill;
                instruction.params = Arrays.asList(addr);
                instruction.paramsInfo = new ArrayList<>();

            }else{
                Instruction instruction = new Instruction(-1,"kill", Arrays.asList(addr));
                instruction.next = instructionTable.get("load");
                instructionTable.put("load", instruction);
            }

        }


        private int findInstruction(Instruction instruction){
            if(!instructionTable.containsKey(instruction.name)){
                return -1;
            }

            Instruction exist = instructionTable.get(instruction.name);
            if(instruction.name.equals("load") && invalidatedAddressSet.contains(instruction.params.get(0))){
                exist = null;
                invalidatedAddressSet.remove(instruction.params.get(0));
            }

            while(exist != null){
                if(instruction.hash().equals(exist.hash())){
                    return exist.id;
                }

                exist = exist.next;
            }

            instruction.next = instructionTable.get(instruction.name);
            instructionTable.put(instruction.name, instruction);

            return -1;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(name);
            sb.append(" ");
            sb.append("[shape=record, label=");
            sb.append("\"<b>");
            sb.append(name);
            sb.append("| {");
            if(!instructions.isEmpty()){
                //sb.append(instructions.get(0).toString());
                for(int i = 0; i < instructions.size(); i++){
                    if(instructions.get(i).isRemoved){
                        continue;
                    }
                    sb.append(String.format("%s|", instructions.get(i)));
                }
                sb.deleteCharAt(sb.length() - 1);
            }
            sb.append("}");
            if(!"BB0".equals(this.name)){
                sb.append("| ");
                for(String var: this.variableMap.keySet()){
                    sb.append(String.format("%s:(%d)\\n", var, this.variableMap.get(var)));
                }
            }


            sb.append("\"];\n");
            return sb.toString();
        }
    }
}
