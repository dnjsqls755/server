package dto.response;

import domain.User;
import dto.type.DtoType;

import java.util.List;

public class AdminUserListResponse extends DTO {
    private final List<User> users;

    public AdminUserListResponse(List<User> users) {
        super(DtoType.ADMIN_USER_LIST);
        this.users = users;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
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
