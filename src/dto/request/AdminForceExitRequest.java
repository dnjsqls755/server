package dto.request;

import dto.type.DtoType;

public class AdminForceExitRequest extends DTO {
    private final String userId;
    private final String roomName;

    public AdminForceExitRequest(String userId, String roomName) {
        super(DtoType.ADMIN_FORCE_EXIT);
        this.userId = userId;
        this.roomName = roomName;
    }

    public AdminForceExitRequest(String message, boolean parse) {
        super(DtoType.ADMIN_FORCE_EXIT);
        String[] parts = message.split(",", 2);
        this.userId = parts.length > 0 ? parts[0] : "";
        this.roomName = parts.length > 1 ? parts[1] : "";
    }

    public String getUserId() {
        return userId;
    }

    public String getRoomName() {
        return roomName;
    }

    @Override
    public String toString() {
        return super.toString() + userId + "," + roomName;
    }
}
