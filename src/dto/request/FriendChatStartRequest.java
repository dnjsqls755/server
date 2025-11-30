package dto.request;

public class FriendChatStartRequest {
    private final String userId;
    private final String friendId;

    public FriendChatStartRequest(String message) {
        String[] values = message.split(",");
        this.userId = values[0];
        this.friendId = values.length > 1 ? values[1] : "";
    }

    public String getUserId() {
        return userId;
    }

    public String getFriendId() {
        return friendId;
    }
}
