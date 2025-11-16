package domain;

import dto.request.LoginRequest;
import java.net.Socket;
import java.util.Date;

public class User {

    private Socket socket; // 현재 입장한 채팅방의 소켓
    private String pw; // 비밀번호
    private String id; // 아이디: 사용자 식별자
    private String nickname; // 이름: 채팅방에서 사용되는 이름
    private Date createdAt; // 로그인 시점

    // ✅ 기본 생성자
    public User() {
        this.createdAt = new Date();
    }

    // ✅ 두 개의 String을 받는 생성자
    public User(String id, String nickname) {
        this.id = id;
        this.nickname = nickname;
        this.createdAt = new Date();
    }

    // ✅ Socket과 LoginRequest를 받는 생성자
    public User(Socket socket, LoginRequest req) {
        this.socket = socket;
        this.id = req.getId();
        this.pw = req.getPw();
        this.nickname = req.getNickname(); // 닉네임 설정 추가
        this.createdAt = new Date();
    }

    // ✅ Getter 메서드
    public String getId() { return id; }
    public String getPw() { return pw; }
    public String getNickName() { return nickname; }
    public Date getCreatedAt() { return createdAt; }
    public Socket getSocket() { return socket; }

    // ✅ Setter 메서드
    public void setId(String id) { this.id = id; }
    public void setPw(String pw) { this.pw = pw; }
    public void setNickName(String nickname) { this.nickname = nickname; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public void setSocket(Socket socket) { this.socket = socket; }

    // ✅ 입장/퇴장 메시지
    public String getEnterString() {
        return "[" + nickname + "]님이 입장했습니다.";
    }

    public String getExitString() {
        return "[" + nickname + "]님이 퇴장했습니다.";
    }
}
