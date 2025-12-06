package dto.request;

import dto.type.DtoType;

public class AdminRoomMembersRequest extends DTO {
    private final String roomName;

    public AdminRoomMembersRequest(String roomName) {
        super(DtoType.ADMIN_ROOM_MEMBERS);
        this.roomName = roomName;
    }

    public AdminRoomMembersRequest(String message, boolean parse) {
        super(DtoType.ADMIN_ROOM_MEMBERS);
        this.roomName = message;
    }

    public String getRoomName() {
        return roomName;
    }

    @Override
    public String toString() {
        return super.toString() + (roomName == null ? "" : roomName);
    }
}
