package app;

import dao.ChatDao;
import service.ChatService;
import thread.ServerThread;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ServerApplication {

    public static List<Socket> sockets = new ArrayList<>();

    private Connection connection;
    private ChatDao chatDao;
    private ChatService chatService;

    public ServerApplication() {
        try {
            // 1. Oracle JDBC 드라이버 로딩
            Class.forName("oracle.jdbc.driver.OracleDriver");

            // 2. DB 연결 설정
            String url = "jdbc:oracle:thin:@localhost:1521:xe"; // DB URL
            String user = "scott"; // 사용자명
            String password = "tiger"; // 비밀번호

            connection = DriverManager.getConnection(url, user, password);
            System.out.println("Oracle DB 연결 성공!");

            // 3. DAO 및 Service 초기화
            chatDao = new ChatDao(connection);
            chatService = new ChatService(chatDao);
            

            // 4. 서버 소켓 설정
            ServerSocket serverSocket = new ServerSocket(9000);
            System.out.println("서버 시작됨. 포트 9000");

            while (true) {
                System.out.println("접속 대기중...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("client IP: " + clientSocket.getInetAddress() + " Port: " + clientSocket.getPort());

                sockets.add(clientSocket);

                ServerThread thread = new ServerThread(clientSocket, chatService);
                thread.start();
            }

        } catch (ClassNotFoundException e) {
            System.out.println("Oracle JDBC 드라이버를 찾을 수 없습니다.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("Oracle DB 연결 실패");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}