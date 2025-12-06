package dto.response;

import dto.type.DtoType;

public class ChatRoomInviteResultResponse extends DTO {
    private final String message;
    private final boolean success;

    public ChatRoomInviteResultResponse(String message, boolean success) {
        super(DtoType.CHAT_ROOM_INVITE_RESULT);
        this.message = message;
        this.success = success;
    }

    public ChatRoomInviteResultResponse(String messageStr) {
        super(DtoType.CHAT_ROOM_INVITE_RESULT);
        String[] parts = messageStr.split("\\|", 2);
        this.message = parts.length > 0 ? parts[0] : "";
        this.success = parts.length > 1 ? Boolean.parseBoolean(parts[1]) : false;
    }

    public String getMessage() { return message; }
    public boolean isSuccess() { return success; }
}
