package dto.request;

public class LogoutRequest {
    
    String userId;
    
    public LogoutRequest(String message) {
        this.userId = message;
    }
    
    public String getUserId() {
        return userId;
    }
}
