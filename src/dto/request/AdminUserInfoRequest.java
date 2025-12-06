package dto.request;

import dto.type.DtoType;

public class AdminUserInfoRequest extends DTO {
    private final String userId;

    public AdminUserInfoRequest(String message) {
        super(DtoType.ADMIN_USER_INFO);
        this.userId = message;
    }

    public String getUserId() { return userId; }
}
