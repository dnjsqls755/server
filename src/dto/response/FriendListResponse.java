package dto.response;

import domain.User;
import dto.type.DtoType;

import java.util.List;

public class FriendListResponse extends DTO {
    private final List<User> friends;

    public FriendListResponse(List<User> friends) {
        super(DtoType.FRIEND_LIST);
        this.friends = friends;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        for (User user : friends) {
            sb.append(user.getId()).append(",").append(user.getNickName()).append("|");
        }
        if (!friends.isEmpty()) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    public List<User> getFriends() {
        return friends;
    }
}
