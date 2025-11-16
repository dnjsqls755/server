package dto.request;

public class LoginRequest {
    private String id;
    private String pw;
    private String nickname; // ✅ 닉네임 추가

    public LoginRequest(String message) {
        String[] value = message.split(",");
        id = value[0];
        pw = value[1];
        if (value.length > 2) {
            nickname = value[2]; // 닉네임이 있으면 추가
        }
    }

    public String getId() { return id; }
    public String getPw() { return pw; }
    public String getNickname() { return nickname; } // ✅ 추가
}
