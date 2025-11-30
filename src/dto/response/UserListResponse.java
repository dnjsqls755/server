package dto.response;

import domain.User;
import dto.type.DtoType;

import java.util.List;

public class UserListResponse extends DTO {

    String chatRoomName;

    List<User> users;

    public UserListResponse(String chatRoomName, List<User> users) {
        super(DtoType.USER_LIST);

        this.chatRoomName = chatRoomName;
        this.users = users;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString()).append(chatRoomName).append("|");
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            sb.append(user.getId()).append(",")
                    .append(user.getNickName() == null ? "" : user.getNickName()).append(",")
                    .append(user.getRole() == null ? "USER" : user.getRole()).append(",")
                    .append(user.isOnline() ? "1" : "0").append(",")
                    .append(user.isBanned() ? "1" : "0");
            if (i < users.size() - 1) {
                sb.append("|");
            }
        }
        return sb.toString();
    }

    public List<User> getUsers() {
        return users;
    }
}
