package dto.request;

import dto.type.DtoType;

public class AdminMessageSearchRequest extends DTO {
    private String nickname;
    private String roomName;

    public AdminMessageSearchRequest(String nickname, String roomName) {
        super(DtoType.ADMIN_MESSAGE_SEARCH);
        this.nickname = nickname;
        this.roomName = roomName;
    }

    public AdminMessageSearchRequest(String message, boolean parse) {
        super(DtoType.ADMIN_MESSAGE_SEARCH);
        String[] parts = message.split("\\|", -1);
        this.nickname = parts.length > 0 ? parts[0] : "";
        this.roomName = parts.length > 1 ? parts[1] : "";
    }

    public String getNickname() {
        return nickname;
    }

    public String getRoomName() {
        return roomName;
    }

    @Override
    public String toString() {
        return super.toString() + (nickname == null ? "" : nickname) + "|" + (roomName == null ? "" : roomName);
    }
}
