package dto.request;

import dto.type.DtoType;

public class AdminForceLogoutRequest extends DTO {
    private String userId;

    public AdminForceLogoutRequest(String userId) {
        super(DtoType.ADMIN_FORCE_LOGOUT);
        this.userId = userId;
    }

    public AdminForceLogoutRequest(String message, boolean parse) {
        super(DtoType.ADMIN_FORCE_LOGOUT);
        this.userId = message;
    }

    public String getUserId() {
        return userId;
    }

    @Override
    public String toString() {
        return super.toString() + userId;
    }
}
