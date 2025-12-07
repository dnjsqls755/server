package dto.request;

import dto.type.DtoType;

public class ProfileImageRequest extends DTO {
    private final String userId;

    public ProfileImageRequest(String userId) {
        super(DtoType.PROFILE_IMAGE);
        this.userId = userId;
    }

    public ProfileImageRequest(String message, boolean parse) {
        super(DtoType.PROFILE_IMAGE);
        this.userId = message.trim();
    }

    public String getUserId() {
        return userId;
    }

    @Override
    public String toString() {
        return super.toString() + userId;
    }
}
