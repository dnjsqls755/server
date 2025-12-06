package dto.response;

import domain.User;
import dto.type.DtoType;
import java.util.ArrayList;
import java.util.List;

public class AdminRoomMembersResponse extends DTO {
    private final String roomName;
    private final List<User> members;

    public AdminRoomMembersResponse(String roomName, List<User> members) {
        super(DtoType.ADMIN_ROOM_MEMBERS_RESULT);
        this.roomName = roomName == null ? "" : roomName;
        this.members = members == null ? new ArrayList<>() : members;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append(roomName == null ? "" : roomName).append("|");
        for (int i = 0; i < members.size(); i++) {
            User u = members.get(i);
            sb.append(u.getId() == null ? "" : u.getId()).append(",")
              .append(u.getNickName() == null ? "" : u.getNickName()).append(",")
              .append(u.isOnline() ? "1" : "0");
            if (i < members.size() - 1) {
                sb.append(";");
            }
        }
        return sb.toString();
    }
}
