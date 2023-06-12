import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Server {
    private static final int NUMBER_MACHINE_PORT = 12341; // 取号机端口号
    private static final int CALLING_MACHINE_PORT = 12342; // 叫号机端口号
    private static final int DISPLAY_PORT = 12343; // 显示端口号

    private static Queue<Integer> queue = new LinkedList<>();
    private static int nextTicketNumber = 1;

    public static void main(String[] args) {
        try {
            // 创建取号机ServerSocket
            ServerSocket numberMachineServerSocket = new ServerSocket(NUMBER_MACHINE_PORT);
            System.out.println("取号机服务器已启动，等待连接...");

            // 创建叫号机ServerSocket
            ServerSocket callingMachineServerSocket = new ServerSocket(CALLING_MACHINE_PORT);
            System.out.println("叫号机服务器已启动，等待连接...");

            // 创建显示端ServerSocket
            ServerSocket displayServerSocket = new ServerSocket(DISPLAY_PORT);
            System.out.println("显示服务器已启动，等待连接...");

            while (true) {
                // 监听取号机连接
                Socket numberMachineSocket = numberMachineServerSocket.accept();
                System.out.println("取号机已连接：" + numberMachineSocket.getInetAddress());

                // 创建线程处理取号机请求
                NumberMachineClientHandler numberMachineHandler = new NumberMachineClientHandler(numberMachineSocket);
                numberMachineHandler.start();

                // 监听叫号机连接
                Socket callingMachineSocket = callingMachineServerSocket.accept();
                System.out.println("叫号机已连接：" + callingMachineSocket.getInetAddress());

                // 创建线程处理叫号机请求
                CallingMachineClientHandler callingMachineHandler = new CallingMachineClientHandler(
                        callingMachineSocket);
                callingMachineHandler.start();

                // 监听显示端连接
                Socket displaySocket = displayServerSocket.accept();
                System.out.println("显示端已连接：" + displaySocket.getInetAddress());

                // 创建输入流用于接收显示端的请求
                ObjectInputStream displayInputStream = new ObjectInputStream(displaySocket.getInputStream());

                // 创建线程处理显示端请求
                DisplayClientHandler displayHandler = new DisplayClientHandler(displaySocket, displayInputStream);
                displayHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class NumberMachineClientHandler extends Thread {
        private Socket socket;
        private ObjectOutputStream outputStream;
        private ObjectInputStream inputStream;

        private int currentTicketNumber = nextTicketNumber;

        static void displayQueueSize(Queue<Integer> queue) {
            synchronized (queue) {
                System.out.println("当前队列中的号码数量：" + queue.size());
            }
        }

        public NumberMachineClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                outputStream = new ObjectOutputStream(socket.getOutputStream());
                inputStream = new ObjectInputStream(socket.getInputStream());

                while (true) {
                    // 接收取号机的请求
                    Request request = (Request) inputStream.readObject();

                    if (request.getRequestType() == RequestType.GET_NUMBER) {
                        synchronized (queue) {
                            // 添加当前的号码到队列中
                            queue.add(currentTicketNumber);

                            // 获取当前的号码
                            int ticketNumber = currentTicketNumber;
                            System.out.println("取号：" + ticketNumber);

                            // 号码递增加一
                            currentTicketNumber++;

                            // 创建响应对象并发送给取号机
                            Response response = new Response(true, ticketNumber, queue.size(), new ArrayList<>(queue));
                            outputStream.writeObject(response);
                            outputStream.flush();

                            // 在 NumberMachineClientHandler 类的 run() 方法中调用 displayQueueSize()
                            // 显示队列大小
                            displayQueueSize(queue);
                        }
                    }

                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    if (socket != null) {
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    static void displayQueueSize(Queue<Integer> queue) {
        synchronized (queue) {
            System.out.println("当前队列中的号码数量：" + queue.size());
        }
    }

    static class CallingMachineClientHandler extends Thread {
        private Socket socket;
        private ObjectOutputStream outputStream;
        private ObjectInputStream inputStream;

        public CallingMachineClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                outputStream = new ObjectOutputStream(socket.getOutputStream());
                inputStream = new ObjectInputStream(socket.getInputStream());

                while (true) {
                    // 接收叫号机的请求
                    Request request = (Request) inputStream.readObject();

                    if (request.getRequestType() == RequestType.SEND_NUMBER) {
                        synchronized (queue) {
                            if (!queue.isEmpty()) {
                                // 从队列中取出下一个号码
                                int ticketNumber = queue.poll();
                                System.out.println("正在叫号：" + ticketNumber);

                                // 创建响应对象并发送给叫号机
                                Response response = new Response(true, ticketNumber, queue.size(),
                                        new ArrayList<>(queue));
                                outputStream.writeObject(response);
                                outputStream.flush();

                            } else {
                                // 创建响应对象并发送给叫号机
                                Response response = new Response(false, 0, queue.size(), new ArrayList<>(queue));
                                outputStream.writeObject(response);
                                outputStream.flush();
                            }
                        }
                        displayQueueSize(queue);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    if (socket != null) {
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static class DisplayClientHandler extends Thread {
        private Socket socket;
        private ObjectOutputStream outputStream;
        private ObjectInputStream inputStream;

        public DisplayClientHandler(Socket socket, ObjectInputStream inputStream) {
            this.socket = socket;
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            try {
                outputStream = new ObjectOutputStream(socket.getOutputStream());
                while (true) {
                    // 接收显示端的请求
                    Request request = (Request) inputStream.readObject();

                    if (request.getRequestType() == RequestType.QUEUE_SIZE) {
                        synchronized (queue) {
                            int queueSize = queue.size();
                            List<Integer> queueList = new ArrayList<>(queue);

                            // 获取队列头部元素
                            Integer ticketNumber = queue.peek();
                            int ticketNumberValue = (ticketNumber != null) ? ticketNumber.intValue() : 0;

                            Response response = new Response(true, ticketNumberValue, queueSize, queueList);
                            outputStream.writeObject(response);
                            outputStream.flush();
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                    if (socket != null) {
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
