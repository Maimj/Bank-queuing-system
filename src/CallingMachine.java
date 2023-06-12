import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class CallingMachine {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 12342; // 服务器监听的端口号

    public static void main(String[] args) {
        try {
            // 连接服务器
            Socket serverSocket = new Socket(SERVER_ADDRESS, SERVER_PORT);

            // 创建对象输入输出流
            ObjectOutputStream outputStream = new ObjectOutputStream(serverSocket.getOutputStream());
            ObjectInputStream inputStream = new ObjectInputStream(serverSocket.getInputStream());

            while (true) {
                // 模拟用户输入操作
                System.out.println("按下 Enter 键以叫号");
                Scanner scanner = new Scanner(System.in);
                scanner.nextLine();

                // 创建请求对象并发送给服务器
                Request request = new Request(RequestType.SEND_NUMBER);
                outputStream.writeObject(request);
                outputStream.flush();

                // 接收服务器的响应
                Response response = (Response) inputStream.readObject();

                if (response.isSuccess()) {
                    // 获取服务器发送的号码并显示
                    int ticketNumber = response.getTicketNumber();
                    System.out.println("正在叫号：" + ticketNumber);
                } else {
                    // 无可叫号码
                    System.out.println("无可叫号码");
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
