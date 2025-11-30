package dto.response;

import domain.ChatRoom;
import dto.type.DtoType;

import java.util.List;

public class AdminChatRoomListResponse extends DTO {
    private final List<ChatRoom> chatRooms;

    public AdminChatRoomListResponse(List<ChatRoom> chatRooms) {
        super(DtoType.ADMIN_CHATROOM_LIST);
        this.chatRooms = chatRooms;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        for (int i = 0; i < chatRooms.size(); i++) {
            ChatRoom room = chatRooms.get(i);
            sb.append(room.getName()).append(",").append(room.getMemberCount());
            if (i < chatRooms.size() - 1) {
                sb.append("|");
            }
        }
        return sb.toString();
    }

    public List<ChatRoom> getChatRooms() {
        return chatRooms;
    }
}
