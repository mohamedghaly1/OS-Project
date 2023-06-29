import java.util.LinkedList;
import java.util.Queue;

public class Mutex {

    Queue<String> queue = new LinkedList<>();
    int value=1;
    String ownerID;
    GodfatherOS os;

    public Mutex(GodfatherOS os){
        this.os = os;
    }

    void semWait(String processID) {
        if (value == 1) {
            ownerID = processID;
            value = 0;
        } else {
            queue.add(processID);
            os.blockProcess(processID);
        }
    }
    void semSignal(String processID) {
        if(ownerID == processID) {
            if (queue.isEmpty())
                value = 1;
            else {
                String newProcessID = queue.remove();
                os.unblockProcess(newProcessID);
                ownerID=newProcessID;
            }
        }
    }
}
