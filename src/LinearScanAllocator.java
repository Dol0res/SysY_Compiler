import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef;
import org.bytedeco.llvm.LLVM.LLVMModuleRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;

import java.util.*;

import static org.bytedeco.llvm.global.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.LLVMGetNextInstruction;

public class LinearScanAllocator implements RegisterAllocator {
    // 实现线性扫描算法需要的具体逻辑，这里只是示例，实际实现可能更复杂
    private int stackSize; // 当前函数需要的栈内存大小
    private HashMap<String, Integer> registerMap= new HashMap<>();
    //private HashMap<String, Integer> stackMap= new HashMap<>();
    private HashMap<String, Integer> stackMap= new HashMap<>();
    HashMap<String, Interval> variableIntervals = new HashMap<>();
    //List<String> variables=new ArrayList<>();
    private static final int regNum = 24;
    private static class Interval {
        int start;
        int end;

        Interval(int start, int end) {
            this.start = start;
            this.end = end;
        }
        public void setEnd(int end) {
            this.end = end;
        }
    }
    ArrayList<String> registers = new ArrayList<>();
    @Override
    public void allocate(String variable) {
        // 实现线性扫描算法的寄存器分配策略
//        if(registerMap.containsKey(variable))return;
//        if(getStack(variable)==-1) {
            stackSize -= 4; // 假设每个变量占用 4 字节的栈空间
            stackMap.put(variable, stackSize);
        //}
    }
    boolean[] regIsAssigned =new boolean[regNum];
    List<String> activeIntervals = new ArrayList<>();

    public void allocateRegisters() {
        // 将活跃区间按照开始位置排序
        // Step 1: Sort intervals by start position
        List<HashMap.Entry<String, Interval>> sortedIntervals = new ArrayList<>(variableIntervals.entrySet());
        sortedIntervals.sort(Comparator.comparingInt(e -> e.getValue().start));

        // Step 2: Track active intervals

        for (HashMap.Entry<String, Interval> entry : sortedIntervals) {
            String variable = entry.getKey();
            Interval interval = entry.getValue();

            // Step 3: Expire old intervals
            expireOldIntervals(activeIntervals,interval.start);

            // Step 4: Assign register to current interval

            //assignRegister(variable);
            boolean flag=false;
            for(int i=0;i<regNum;i++) {
                if(!regIsAssigned[i]) {
                    registerMap.put(variable, i);
                    regIsAssigned[i]=true;
                    flag=true;
                    break;
                }
            }
            if(!flag){
                spillAtInterval(variable);
                stackSize+=4;
            }else {
                activeIntervals.add(variable);
                activeIntervals.sort(Comparator.comparingInt(e ->variableIntervals.get(e).end ));
            }

            // Step 5: Add current interval to active intervals
        }

        // Step 6: Cleanup: Expire all active intervals
        //expireOldIntervals()
    }

    @Override
    public int getStackSize() {
        return stackSize; // 线性扫描算法可能不需要栈空间大小
    }

    private void expireOldIntervals(List<String> active, int currentStart) {
        Iterator<String> iterator = active.iterator();
        while (iterator.hasNext()) {

            String variable = iterator.next();
            Interval interval =  variableIntervals.get(variable);
            if (interval.end < currentStart) {
                iterator.remove();
                int r = registerMap.get(variable);
                regIsAssigned[r] = false;
                //freeRegisters.add(interval.register);
            } else {
                break;
            }
        }
    }
    private String spillAtInterval(String current) {
        String spillV = activeIntervals.get((activeIntervals.size()-1));
        Interval spillI = variableIntervals.get(spillV);
        Interval cI = variableIntervals.get(current);
        if (cI.end < spillI.end) {
            int r = registerMap.get(spillV);
            registerMap.put(current,r);
            registerMap.put(spillV,-1);
            activeIntervals.remove(spillV);
            activeIntervals.add(current);
        }
        return current;
    }

    @Override
    public void init(LLVMBasicBlockRef bb) {
        // Add registers starting with 's'
        for (int i = 0; i <= 11; i++) {
            registers.add("s" + i);
        }

        // Add registers starting with 'a'
        for (int i = 0; i <= 7; i++) {
            registers.add("a" + i);
        }
        for (int i = 3; i <= 6; i++) {
            registers.add("t" + i);
        }
        int i=0;

        for (LLVMValueRef inst = LLVMGetFirstInstruction(bb); inst != null; inst = LLVMGetNextInstruction(inst)) {
            // Get the opcode of the instruction
            int opcode = LLVMGetInstructionOpcode(inst);
            String name;
            // Handle specific instructions (e.g., store, load)
            LLVMValueRef destOperand;
            destOperand = inst;
            int i0=0;
            while(i0<=2) {
                //if(LLVMIsAGlobalValue(destOperand) != null)continue;
                name = LLVMGetValueName(destOperand).getString();
                if (!Objects.equals(name, "") && LLVMIsAGlobalValue(destOperand) == null) {
                    if (!variableIntervals.containsKey(name)) {
                        variableIntervals.put(name, new Interval(i, i)); // 示例，需要根据实际情况设置区间
                    } else {
                        variableIntervals.get(name).setEnd(i); // 示例，需要根据实际情况设置区间结束
                    }
                }
                destOperand = LLVMGetOperand(inst, i0);
                i0++;
            }

            i++;
        }
        allocateRegisters();
        stackSize= Math.min(800,((int) (stackSize/16+1))*16);
    }

    @Override
    public int getStack(String variable) {
        Integer value = stackMap.get(variable);
        if (value == null) {
            return -1;
        }
        return value;
    }

    @Override
    public void storeNew(String name) {
        if(registerMap.containsKey(name)&& registerMap.get(name)!=-1){
            int i = registerMap.get(name);
            String reg = registers.get(i);
            AsmBuilder.op1("mv", reg , "t0");

        }else{
            if(getStack(name)==-1) allocate(name);
            AsmBuilder.op1("sw", "t0" , getStack(name) + "(sp)");
        }
    }

    @Override
    public void storeNew(String name1, String name2) {
        String reg1 = "t0";
        if(!name1.equals("")&&registerMap.containsKey(name1) && registerMap.get(name1)!=-1){
            int i = registerMap.get(name1);
            reg1 = registers.get(i);
        }
        if(registerMap.containsKey(name2)&& registerMap.get(name2)!=-1){

            String reg2 = "t1";

            if(!name2.equals("")) {

                int i = registerMap.get(name2);
                reg2 = registers.get(i);
            }

            AsmBuilder.op1("mv", reg2 , reg1);

        }else{
            if(getStack(name2)==-1) allocate(name2);
            AsmBuilder.op1("sw", reg1 , getStack(name2) + "(sp)");
        }
        //todo: name12?
        //todo: getstack
    }

    @Override
    public void loadNew(String name, int i) {
        if(registerMap.containsKey(name)&& registerMap.get(name)!=-1){
//            int i = registerMap.get(name);
//            String reg = registers.get(i);
//            AsmBuilder.op1("mv", reg , "t0");

        }else{
            if(getStack(name)==-1) allocate(name);
            AsmBuilder.op1("lw", "t" + String.valueOf(i), getStack(name) + "(sp)");
        }
    }

    @Override
    public void op2(LLVMValueRef inst) {
        int opcode = LLVMGetInstructionOpcode(inst);
        String op = determineOpcode(opcode);
        String name1=LLVMGetValueName(LLVMGetOperand(inst, 0)).getString();
        String name2=LLVMGetValueName(LLVMGetOperand(inst, 1)).getString();
        String reg1 = "t0";
        String reg2 = "t1";
        if(!name1.equals("")&&registerMap.containsKey(name1)&& registerMap.get(name1)!=-1){
            int i = registerMap.get(name1);
            reg1 = registers.get(i);
        }
        if(!name2.equals("")&&registerMap.containsKey(name2)&& registerMap.get(name2)!=-1) {

            int i = registerMap.get(name2);
            reg2 = registers.get(i);
        }
//        if(opcode==LLVMSRem){
//            AsmBuilder.op2("div", "t0", reg1, reg2);
//            AsmBuilder.op1("mv","t0", reg1);
//        }else
            AsmBuilder.op2(op, "t0", reg1, reg2);

        String name = LLVMGetValueName(inst).getString();
        storeNew(name);
    }

    @Override
    public void ret(String name) {
        if(registerMap.containsKey(name)&& registerMap.get(name)!=-1){
            int i = registerMap.get(name);
            String reg = registers.get(i);
            AsmBuilder.op1("mv", "a0" , reg);

        }else{
            if(getStack(name)==-1) allocate(name);
            AsmBuilder.op1("lw", "a0", getStack(name) + "(sp)");
        }

    }

    public static String determineOpcode(int opcode) {
        switch (opcode) {
            case LLVMAdd:
                return "add";
            case LLVMSub:
                return "sub";
            case LLVMMul:
                return "mul";
            case LLVMSDiv:
                return "div";
            case LLVMSRem:
                return "rem";
            case LLVMLoad:
                return "load";
            // 添加更多操作码的处理
            default:
                return "unknown"; // 如果未识别的操作码，返回 "unknown"
        }
    }
}


