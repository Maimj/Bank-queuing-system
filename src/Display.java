import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public class Display {
    private static final String SERVER_IP = "127.0.0.1"; // 服务器 IP 地址
    private static final int SERVER_PORT = 12343;

    public static void main(String[] args) {
        try {
            // 连接到服务器
            Socket socket = new Socket(SERVER_IP, SERVER_PORT);
            System.out.println("已连接到服务器：" + socket.getInetAddress());

            // 创建线程处理服务器响应
            ServerResponseHandler responseHandler = new ServerResponseHandler(socket);
            responseHandler.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ServerResponseHandler extends Thread {
        private Socket socket;
        private ObjectOutputStream outputStream;
        private ObjectInputStream inputStream;
        private boolean stopDisplay; // 控制是否继续显示

        public ServerResponseHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                outputStream = new ObjectOutputStream(socket.getOutputStream());
                inputStream = new ObjectInputStream(socket.getInputStream());

                Response lastResponse = null; // 上次接收到的数据
                while (true) {
                    if (stopDisplay) {
                        break; // 如果标志变量为 true，退出循环
                    }

                    // 发送请求给服务器获取队列信息
                    Request request = new Request(RequestType.QUEUE_SIZE);
                    outputStream.writeObject(request);
                    outputStream.flush();

                    // 接收服务器返回的队列信息
                    Response response = (Response) inputStream.readObject();

                    if (lastResponse != null && response.equals(lastResponse)) {
                        stopDisplay = true; // 设置标志变量为 true，停止显示
                        break; // 如果与上次数据相同，跳出循环
                    }

                    lastResponse = response; // 更新上次数据

                    displayResponse(response);

                    // 等待3秒钟
                    Thread.sleep(1000);
                }
            } catch (IOException | ClassNotFoundException | InterruptedException e) {
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

        private void displayResponse(Response response) {
            if (response.isSuccess()) {
                int ticketNumberValue = response.getTicketNumber();
                int ticketNumber = ticketNumberValue - 1;
                if (ticketNumber == -1) {
                    System.out.println("当前没有叫号");
                    List<Integer> queueList = response.getQueueList();
                    System.out.println("服务器队列中的号码列表：" + queueList);
                } else {
                    System.out.println("当前叫号：" + ticketNumber);
                    List<Integer> queueList = response.getQueueList();
                    System.out.println("服务器队列中的号码列表：" + queueList);
                }
            } else {
                System.out.println("无可叫号码");
            }
        }
    }
}
