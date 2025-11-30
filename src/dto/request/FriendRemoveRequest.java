package dto.request;

public class FriendRemoveRequest {
    private final String userId;
    private final String friendId;

    public FriendRemoveRequest(String message) {
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
