import java.io.Serializable;
import java.util.List;
import java.util.Queue;

public class Response implements Serializable {
    private boolean success;
    private int ticketNumber;
    private int queueSize;
    private List<Integer> queueList;

    public Response(boolean success, int ticketNumber, int queueSize, List<Integer> queueList) {
        this.success = success;
        this.ticketNumber = ticketNumber;
        this.queueSize = queueSize;
        this.queueList = queueList;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getTicketNumber() {
        return ticketNumber;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public List<Integer> getQueueList() {
        return queueList;
    }


}
