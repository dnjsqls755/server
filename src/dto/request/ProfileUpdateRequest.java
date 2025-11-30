package dto.request;

public class ProfileUpdateRequest {
    private final String userId;
    private final String nickname;

    public ProfileUpdateRequest(String message) {
        String[] values = message.split(",", 2);
        this.userId = values[0];
        this.nickname = values.length > 1 ? values[1] : "";
    }

    public String getUserId() {
        return userId;
    }

    public String getNickname() {
        return nickname;
    }
}
