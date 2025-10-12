package dto.request;

public class LoginRequest {

    String id;

    String pw;

    public LoginRequest(String message) {
        String[] value = message.split(",");
        id = value[0];
        pw = value[1];
    }

    public String getId() {
        return id;
    }

    public String getPw() {
        return pw;
    }
}
