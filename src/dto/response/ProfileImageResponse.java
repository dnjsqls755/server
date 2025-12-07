package dto.response;

import dto.type.DtoType;

public class ProfileImageResponse extends DTO {
    private final String userId;
    private final String imageData;  // Base64 encoded image or "DEFAULT" for default image

    public ProfileImageResponse(String userId, String imageData) {
        super(DtoType.PROFILE_IMAGE_RESULT);
        this.userId = userId;
        this.imageData = imageData;
    }

    public String getUserId() {
        return userId;
    }

    public String getImageData() {
        return imageData;
    }

    @Override
    public String toString() {
        return super.toString() + userId + "|" + imageData;
    }
}
