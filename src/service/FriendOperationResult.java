package service;

public class FriendOperationResult {
    private final boolean success;
    private final String message;

    public FriendOperationResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
