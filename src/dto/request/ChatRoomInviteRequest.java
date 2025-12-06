package dto.request;

import dto.type.DtoType;

public class ChatRoomInviteRequest extends DTO {
    private final String roomName;
    private final String senderUserId;
    private final String targetNickname;

    public ChatRoomInviteRequest(String message) {
        super(DtoType.CHAT_ROOM_INVITE);
        String[] parts = message.split("\\|", 3);
        this.roomName = parts.length > 0 ? parts[0] : "";
        this.senderUserId = parts.length > 1 ? parts[1] : "";
        this.targetNickname = parts.length > 2 ? parts[2] : "";
    }

    public String getRoomName() { return roomName; }
    public String getSenderUserId() { return senderUserId; }
    public String getTargetNickname() { return targetNickname; }
}
