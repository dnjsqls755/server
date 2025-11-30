package dto.request;

import dto.type.DtoType;

public class AdminBanRequest extends DTO {
    private final String userId;
    private final boolean banned;

    public AdminBanRequest(String userId, boolean banned) {
        super(DtoType.ADMIN_BAN);
        this.userId = userId;
        this.banned = banned;
    }

    public AdminBanRequest(String message) {
        super(DtoType.ADMIN_BAN);
        String[] parts = message.split(",", 2);
        this.userId = parts.length > 0 ? parts[0] : "";
        this.banned = parts.length > 1 && "1".equals(parts[1]);
    }

    public String getUserId() {
        return userId;
    }

    public boolean isBanned() {
        return banned;
    }

    @Override
    public String toString() {
        return super.toString() + userId + "," + (banned ? "1" : "0");
    }
}
