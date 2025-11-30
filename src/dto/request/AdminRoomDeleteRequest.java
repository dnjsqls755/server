package dto.request;

import dto.type.DtoType;

public class AdminRoomDeleteRequest extends DTO {
    private String roomName;

    public AdminRoomDeleteRequest(String roomName) {
        super(DtoType.ADMIN_ROOM_DELETE);
        this.roomName = roomName;
    }

    public AdminRoomDeleteRequest(String message, boolean parse) {
        super(DtoType.ADMIN_ROOM_DELETE);
        this.roomName = message;
    }

    public String getRoomName() {
        return roomName;
    }

    @Override
    public String toString() {
        return super.toString() + roomName;
    }
}
