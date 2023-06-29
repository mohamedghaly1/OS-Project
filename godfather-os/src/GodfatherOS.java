import java.io.*;
import java.util.*;

public class GodfatherOS {


    private int quantum = 15;
    private int process1Arrival=0;
    private int process2Arrival=0;
    private int process3Arrival=0;


    private String[] memory;
    private Vector<Range> freeSpaces = new Vector<>();

    private Queue<String> readyQueue = new LinkedList<>();
    private Queue<String> blockedQueue = new LinkedList<>();

    private Mutex userInput = new Mutex(this);
    private Mutex userOutput = new Mutex(this);
    private Mutex file = new Mutex(this);

    private Vector<String> processesInMemory = new Vector<>();

    int processID = 1;
    int pcbLocation = 0;

    int process1Var=0;
    int process2Var=0;
    int process3Var=0;

    boolean[]hasTempValue = new boolean[4];
    String[]tempValues = new String[4];


    public GodfatherOS() {
        memory = new String[40];
        for (int i = 0; i < 40; i++) memory[i] = new String("");
        freeSpaces.add(new Range(15, 39));
    }

    public void run() {
        long clock = 0;
        int quantumCount = 0;

        String curProcessID = "";
        int pc = -1;
        int allocationStart = -1;
        int allocationEnd = -1;

        while (clock < 50) {
            System.out.println("Clock cycle: " + clock + "\n");

            if (clock == process1Arrival) createProcess("programs/Program_1.txt");
            if (clock == process2Arrival) createProcess("programs/Program_2.txt");
            if (clock == process3Arrival) createProcess("programs/Program_3.txt");

            if (quantumCount == quantum) {
                readyQueue.add(curProcessID);
                for (int i = 0; i <= 10; i += 5) {
                    if (memory[i].equals(curProcessID)) {
                        memory[i + 1] = "ready";
                    }
                }
                quantumCount = 0;
            }


            if (quantumCount == 0) {
                curProcessID = schedulerSelect();
                if(curProcessID.equals("")){
                    clock++;
                    continue;
                }
                System.out.println("Process with ID " + curProcessID + " was chosen !");
                System.out.println("Ready Queue: " + readyQueue.toString());
                System.out.println("Blocked Queue: " + blockedQueue.toString());
                System.out.println("userInput Queue: " + userInput.queue.toString());
                System.out.println("userOutput Queue: " + userOutput.queue.toString());
                System.out.println("file Queue: " + file.queue.toString());
                System.out.println();
                for (int i = 0; i <= 10; i += 5) {
                    if (memory[i].equals(curProcessID)) {
                        if (memory[i + 3].equals("x")) swapInProcess(curProcessID);
                        pc = Integer.parseInt(memory[i + 2]);
                        allocationStart = Integer.parseInt(memory[i + 3]);
                        allocationEnd = Integer.parseInt(memory[i + 4]);
                    }
                }
            }

            for (int i = 0; i <= 10; i += 5) {
                if (memory[i].equals(curProcessID)&&memory[i+1].equals("running")&&memory[i+3].equals("x")) {
                    swapInProcess(curProcessID);
                }
            }
            System.out.println("Currently executing process with ID " + curProcessID);
            String instruction = memory[allocationStart + 3 + pc - 1];
            System.out.println("Instruction being executed : " + instruction);
            System.out.println();
            parseAndExecute(curProcessID, instruction);
            for (int i = 0; i <= 10; i += 5) {
                if (memory[i].equals(curProcessID)) {
                    pc = Integer.parseInt(memory[i + 2]);
                }
            }
            if(blockedQueue.contains(curProcessID)){
                quantumCount = 0;
                clock++;
                System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                continue;
            }
            quantumCount++;
            if (allocationStart + 3 + pc - 1 > allocationEnd) {
                terminateProcess(curProcessID);
                quantumCount = 0;
                clock++;
                System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                continue;
            }


            clock++;
            System.out.println("Memory at the end of the cycle:");
            printMemory();
            System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        }
    }

    private void parseAndExecute(String processID, String instruction) {

        String[] parsedInstruction = instruction.split(" ");

        boolean stayOnPC = false;

        switch(parsedInstruction[0]){
            case "semWait":
                if(parsedInstruction[1].equals("userInput"))userInput.semWait(processID);
                if(parsedInstruction[1].equals("userOutput"))userOutput.semWait(processID);
                if(parsedInstruction[1].equals("file"))file.semWait(processID);
                break;
            case "semSignal":
                if(parsedInstruction[1].equals("userInput"))userInput.semSignal(processID);
                if(parsedInstruction[1].equals("userOutput"))userOutput.semSignal(processID);
                if(parsedInstruction[1].equals("file"))file.semSignal(processID);
                break;
            case "print":
                String data = readFromMemory(processID,parsedInstruction[1]);
                print(data);
                break;
            case "assign":

                if(parsedInstruction[2].equals("input")){
                    if(hasTempValue[Integer.parseInt(processID)]){
                        hasTempValue[Integer.parseInt(processID)]=false;
                        writeToMemory(processID,parsedInstruction[1],tempValues[Integer.parseInt(processID)]);
                    }else {
                        String val = input();
                        tempValues[Integer.parseInt(processID)] = val;
                        hasTempValue[Integer.parseInt(processID)] = true;
                        stayOnPC = true;
                    }
                }
                else if(parsedInstruction[2].equals("readFile")){
                    if(hasTempValue[Integer.parseInt(processID)]){
                        hasTempValue[Integer.parseInt(processID)]=false;
                        writeToMemory(processID,parsedInstruction[1],tempValues[Integer.parseInt(processID)]);
                    }else {
                        String filePath = readFromMemory(processID,parsedInstruction[3]);
                        String val = readFile(filePath);
                        tempValues[Integer.parseInt(processID)] = val;
                        hasTempValue[Integer.parseInt(processID)] = true;
                        stayOnPC = true;
                    }
                }
                else {
                    String val = readFromMemory(processID,parsedInstruction[2]);
                    writeToMemory(processID,parsedInstruction[1],val);
                }

                break;
            case "writeFile":
                String path = readFromMemory(processID,parsedInstruction[1]);
                String dataWrite = readFromMemory(processID,parsedInstruction[2]);
                writeFile(path,dataWrite);
                break;
            case "readFile":
                String fileName = readFromMemory(processID,parsedInstruction[1]);
                readFile(fileName);
                break;
            case "printFromTo":
                int start = Integer.parseInt(readFromMemory(processID,parsedInstruction[1]));
                int end = Integer.parseInt(readFromMemory(processID,parsedInstruction[2]));
                for(int i=start;i<=end;i++)System.out.print(i+" ");
                System.out.println("\n");
                break;
        }

        if(stayOnPC)return;
        for (int i = 0; i <= 10; i += 5) {
            if (memory[i].equals(processID)) {
                memory[i+2]=""+((Integer.parseInt(memory[i+2]))+1);
            }
        }
    }

    private String readFile(String path){
        String data="";
        try {
            File myObj = new File(path);
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                data += myReader.nextLine();
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("File was not found");
            e.printStackTrace();
        }

        return data;
    }

    private void writeFile(String path,String data){
        try {
            FileWriter fileWriter = new FileWriter(path);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            bufferedWriter.write(data);

            bufferedWriter.close();

        } catch (IOException e1) {
            System.out.println("An error occurred: " + e1.getMessage());
        }
    }

    private String input(){
        Scanner sc = new Scanner(System.in);
        System.out.print("Please enter a value: ");
        return sc.next();
    }

    private void print(String data){
        System.out.println(data);
    }

    private String readFromMemory(String processID,String variableName){

        int variableAdressSpace=-1;
        for(int i=0;i<=10;i+=5){
            if(memory[i].equals(processID)){
                variableAdressSpace=Integer.parseInt(memory[i+3]);
            }
        }

        for(int i=variableAdressSpace;i<=variableAdressSpace+2;i++){
            if(memory[i].equals(""))continue;
            String[] var = memory[i].split("=");
            if(var[0].equals(variableName)){
                return var[1];
            }
        }
        return null;
    }

    private void writeToMemory(String processID,String variableName,String data){

        int variableAdressSpace=-1;
        for(int i=0;i<=10;i+=5){
            if(memory[i].equals(processID)){
                variableAdressSpace=Integer.parseInt(memory[i+3]);
            }
        }
        for(int i=variableAdressSpace;i<=variableAdressSpace+2;i++){
            if(memory[i].equals(""))continue;
            String[] var = memory[i].split("=");
            if(var[0].equals(variableName)){
               memory[i]=variableName+"="+data;
               return;
            }
        }

        if(processID.equals("1")){
            memory[variableAdressSpace+process1Var]=variableName+"="+data;
            process1Var++;
        }
        if(processID.equals("2")){
            memory[variableAdressSpace+process2Var]=variableName+"="+data;
            process2Var++;
        }
        if(processID.equals("3")){
            memory[variableAdressSpace+process3Var]=variableName+"="+data;
            process3Var++;
        }
    }

    private void createProcess(String path) {

        Vector<String> buffer = new Vector<>();
        int sizeNeeded = 3;

        try {
            File myObj = new File(path);
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                buffer.add(data);
                sizeNeeded++;
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("Program was not found");
            e.printStackTrace();
        }

        int allocationStart = allocateMemory(sizeNeeded);
        int allocationEnd = allocationStart + sizeNeeded - 1;

        memory[pcbLocation] = "" + processID;
        memory[pcbLocation + 1] = "ready";
        memory[pcbLocation + 2] = "1";
        memory[pcbLocation + 3] = "" + allocationStart;
        memory[pcbLocation + 4] = "" + allocationEnd;

        int j = 0;
        for (int i = allocationStart + 3; i <= allocationEnd; i++) {
            memory[i] = buffer.get(j);
            j++;
        }

        readyQueue.add(processID + "");


        System.out.println("Process with ID " + processID + " was created !");
        System.out.println("Ready Queue: " + readyQueue.toString());
        System.out.println("Blocked Queue: " + blockedQueue.toString());
        System.out.println("userInput Queue: " + userInput.queue.toString());
        System.out.println("userOutput Queue: " + userOutput.queue.toString());
        System.out.println("file Queue: " + file.queue.toString());
        System.out.println();

        processID++;
        pcbLocation += 5;


    }

    private void terminateProcess(String processID) {
        System.out.println("Process with ID " + processID + " finished !");
        System.out.println("Ready Queue: " + readyQueue.toString());
        System.out.println("Blocked Queue: " + blockedQueue.toString());
        System.out.println("userInput Queue: " + userInput.queue.toString());
        System.out.println("userOutput Queue: " + userOutput.queue.toString());
        System.out.println("file Queue: " + file.queue.toString());
        System.out.println();
        for (int i = 0; i <= 10; i += 5) {
            if (memory[i].equals(processID)) {
                Range r = new Range(Integer.parseInt(memory[i + 3]), Integer.parseInt(memory[i + 4]));

                memory[i + 1] = "finished";
                memory[i + 3] = "x";
                memory[i + 4] = "x";

                freeSpaces.add(r);
                Collections.sort(freeSpaces);

                int toBeDeleted = -1;
                for (int j = 0; j < freeSpaces.size() - 1; j++) {
                    if (freeSpaces.get(j).getEnd() + 1 == freeSpaces.get(j + 1).getStart()) {
                        freeSpaces.get(j).setEnd(freeSpaces.get(j + 1).getEnd());
                        toBeDeleted = j + 1;
                        break;
                    }
                }

                if (toBeDeleted != -1) {
                    freeSpaces.remove(toBeDeleted);
                }

            }
        }
    }

    void blockProcess(String processID) {
        blockedQueue.add(processID);
        for (int i = 0; i <= 10; i += 5) {
            if (memory[i].equals(processID)) {
                memory[i + 1] = "blocked";
            }
        }
        System.out.println("Process with ID " + processID + " got blocked !");
        System.out.println("Ready Queue: " + readyQueue.toString());
        System.out.println("Blocked Queue: " + blockedQueue.toString());
        System.out.println("userInput Queue: " + userInput.queue.toString());
        System.out.println("userOutput Queue: " + userOutput.queue.toString());
        System.out.println("file Queue: " + file.queue.toString());
        System.out.println();

    }

    void unblockProcess(String processID) {
        blockedQueue.remove(processID);
        for (int i = 0; i <= 10; i += 5) {
            if (memory[i].equals(processID)) {
                memory[i + 1] = "ready";
            }
        }
        readyQueue.add(processID);
        System.out.println("Process with ID " + processID + " got unblocked !");
        System.out.println("Ready Queue: " + readyQueue.toString());
        System.out.println("Blocked Queue: " + blockedQueue.toString());
        System.out.println("userInput Queue: " + userInput.queue.toString());
        System.out.println("userOutput Queue: " + userOutput.queue.toString());
        System.out.println("file Queue: " + file.queue.toString());
        System.out.println();

    }

    private int allocateMemory(int sizeNeeded) {
        boolean done = false;
        int start = 0;

        while (!done) {
            Range toBeDeleted = null;

            for (Range r : freeSpaces) {
                if (r.canHold(sizeNeeded)) {
                    start = r.getStart();
                    r.setStart(r.getStart() + sizeNeeded);
                    if (!r.isValid()) toBeDeleted = r;
                    done = true;
                    break;
                }
            }

            if (done) {
                if (toBeDeleted != null) freeSpaces.remove(toBeDeleted);
                return start;
            }
            swapOutProcess();

        }

        return -1;
    }


    private void swapOutProcess() {
        String chosenProcessID = "";
        Range r;

        Hashtable<String, Range> processesInMemory = new Hashtable<>();

        for (int i = 0; i <= 10; i += 5) {
            if (memory[i + 3].equals("x") || memory[i + 3].equals("")) continue;
            processesInMemory.put(memory[i], new Range(Integer.parseInt(memory[i + 3]), Integer.parseInt(memory[i + 4])));
        }

        int bestSize = -1;

        Enumeration<String> e = processesInMemory.keys();
        while (e.hasMoreElements()) {
            String processID = e.nextElement();
            int start = processesInMemory.get(processID).getStart();
            int end = processesInMemory.get(processID).getEnd();
            int size = end - start + 1;
            if (size > bestSize) {
                chosenProcessID = processID;
                bestSize = size;
            }
        }

        System.out.println("Process with ID " + chosenProcessID + " was swapped out !");
        r = processesInMemory.get(chosenProcessID);

        for (int i = 0; i <= 10; i += 5) {
            if (memory[i].equals(chosenProcessID)) {
                memory[i + 3] = "x";
                memory[i + 4] = "x";
            }
        }

        String data = "";
        for (int i = r.getStart(); i <= r.getEnd(); i++) {
            data += memory[i];
            data += "\n";
        }
        try {
            FileWriter fileWriter = new FileWriter("disk/" + chosenProcessID + ".txt");
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            bufferedWriter.write(data);

            bufferedWriter.close();

        } catch (IOException e1) {
            System.out.println("An error occurred: " + e1.getMessage());
        }

        freeSpaces.add(r);
        Collections.sort(freeSpaces);

        int toBeDeleted = -1;
        for (int i = 0; i < freeSpaces.size() - 1; i++) {
            if (freeSpaces.get(i).getEnd() + 1 == freeSpaces.get(i + 1).getStart()) {
                freeSpaces.get(i).setEnd(freeSpaces.get(i + 1).getEnd());
                toBeDeleted = i + 1;
                break;
            }
        }

        if (toBeDeleted != -1) {
            freeSpaces.remove(toBeDeleted);
        }

    }

    private void swapInProcess(String processID) {
        System.out.println("Process with ID " + processID + " was swapped in !");
        for (int i = 0; i <= 10; i += 5) {
            if (memory[i].equals(processID)) {

                Vector<String> buffer = new Vector<>();

                try {
                    File myObj = new File("disk/" + processID + ".txt");
                    Scanner myReader = new Scanner(myObj);
                    while (myReader.hasNextLine()) {
                        String data = myReader.nextLine();
                        buffer.add(data);
                    }
                    myReader.close();
                } catch (FileNotFoundException e) {
                    System.out.println("File was not found");
                    e.printStackTrace();
                }

                int start = allocateMemory(buffer.size());
                memory[i + 3] = "" + start;
                memory[i + 4] = "" + (start + buffer.size() - 1);
                for (int k = start; k <start+buffer.size(); k++) {
                    memory[k] = buffer.get(k-start);
                }

            }
        }
    }


    private String schedulerSelect() {
        if (readyQueue.isEmpty()) return "";
        else {
            String processID = readyQueue.remove();
            for (int i = 0; i <= 10; i += 5) {
                if (memory[i].equals(processID)) {
                    memory[i + 1] = "running";
                }
            }
            return processID;
        }
    }

    private void printMemory() {
        Hashtable<String, Range> processAllocations = new Hashtable<>();

        for (int i = 0; i <= 10; i += 5) {
            if ((!memory[i + 3].equals("x")) && (!memory[i + 3].equals(""))) {
                processAllocations.put(memory[i], new Range(Integer.parseInt(memory[i + 3]), Integer.parseInt(memory[i + 4])));
            }
        }

        System.out.println("PID   ADR    DATA");

        System.out.println("----------------------\n     Kernel Space");
//        System.out.println("----------------------");

        for (int i = 0; i <= 14; i++) {
            System.out.println("      " + i + ":    " + memory[i]);
        }

        System.out.println("----------------------");
        System.out.println("     User Space");

        for (int i = 15; i < 40; i++) {

            Enumeration<String> e = processAllocations.keys();
            boolean flag = false;

            while (e.hasMoreElements()) {
                String processID = e.nextElement();
                Range r = processAllocations.get(processID);
                if (r.includes(i)) {
                    System.out.println(" " + processID + "    " + i + ":    " + memory[i]);
                    flag = true;
                    break;
                }
            }

            if (flag) continue;
            else System.out.println("      " + i + ":    " + memory[i]);
        }

    }

    private static class Range implements Comparable {
        private int start;
        private int end;

        public Range(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public boolean includes(int x) {
            return x >= start && x <= end;
        }

        public boolean canHold(int size) {
            return (end - start + 1) >= size;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public void setStart(int start) {
            this.start = start;
        }

        public void setEnd(int end) {
            this.end = end;
        }

        public boolean isValid() {
            return start <= end;
        }

        public String toString() {
            return "(" + start + "," + end + ")";
        }

        public int compareTo(Object o) {
            Range r = (Range) o;
            if (start == r.start && end == r.end) return 0;
            else if (end < r.start) return -1;
            else return 1;
        }
    }

    public static void main(String[] args) {
        GodfatherOS gos = new GodfatherOS();
        gos.run();
    }


}
