package dto.request;

public class FriendAddRequest {
    private final String userId;
    private final String friendNickname;

    public FriendAddRequest(String message) {
        String[] values = message.split(",");
        this.userId = values[0];
        this.friendNickname = values.length > 1 ? values[1] : "";
    }

    public String getUserId() {
        return userId;
    }

    public String getFriendNickname() {
        return friendNickname;
    }
}
