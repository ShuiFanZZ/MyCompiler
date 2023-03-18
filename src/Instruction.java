import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Instruction {
    public int id;
    public String name;
    public List<Integer> params;
    public List<String> paramsInfo;
    public Instruction next;
    public boolean isRemoved = false;
    private boolean isConstant = false;
    private int constant = 0;

    public Instruction(int id, String name, List<Integer> params, List<String> paramsInfo) {
        this.id = id;
        this.name = name;
        this.params = params;
        this.paramsInfo = paramsInfo;
    }

    public Instruction(int id, String name, List<Integer> params) {
        this.id = id;
        this.name = name;
        this.params = params;
        this.paramsInfo = new ArrayList<>();
    }
    public Instruction(int id, String name) {
        this.id = id;
        this.name = name;
        this.params = new ArrayList<>();
        this.paramsInfo = new ArrayList<>();
    }

    public Instruction(int id, String name, int constant) {
        this.id = id;
        this.name = name;
        this.constant = constant;
        this.isConstant = true;
    }

    public boolean isEmpty(){
        return "<empty>".equals(this.name);
    }

    @Override
    public String toString() {
        if(isRemoved){
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(id);
        sb.append(": ");
        if(isEmpty()){
            sb.append("\\<empty\\>");
        }else{
            sb.append(name);
        }

        if(isConstant){
            sb.append(String.format(" #%d", constant));
        }else{
            for(int i : params){
                sb.append(String.format(" (%d)", i));
            }
        }


        return sb.toString();
    }

    public String hash(){
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        if(isConstant){
            sb.append(String.format(" #%d", constant));
        }else{
            for(int i : params){
                sb.append(String.format(" (%d)", i));
            }
        }


        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return obj.getClass().equals(this.getClass()) && obj.toString().equals(this.toString());
    }
}
