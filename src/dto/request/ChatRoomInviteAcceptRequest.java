package dto.request;

import dto.type.DtoType;

public class ChatRoomInviteAcceptRequest extends DTO {
    private final String roomName;
    private final String userId;

    public ChatRoomInviteAcceptRequest(String message) {
        super(DtoType.CHAT_ROOM_INVITE_ACCEPT);
        String[] parts = message.split("\\|", 2);
        this.roomName = parts.length > 0 ? parts[0] : "";
        this.userId = parts.length > 1 ? parts[1] : "";
    }

    public String getRoomName() { return roomName; }
    public String getUserId() { return userId; }
}
