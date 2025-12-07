package dto.request;

import dto.type.DtoType;

public class ProfileImageUpdateRequest extends DTO {
    private String userId;
    private byte[] imageData;

    public ProfileImageUpdateRequest(String userId, byte[] imageData) {
        super(DtoType.PROFILE_IMAGE_UPDATE);
        this.userId = userId;
        this.imageData = imageData;
    }

    public ProfileImageUpdateRequest(String message) {
        super(DtoType.PROFILE_IMAGE_UPDATE);
        String[] parts = message.split("\\|", 2);
        if (parts.length >= 2) {
            this.userId = parts[0];
            this.imageData = java.util.Base64.getDecoder().decode(parts[1]);
        } else {
            this.userId = parts.length > 0 ? parts[0] : "";
            this.imageData = new byte[0];
        }
    }

    public String getUserId() {
        return userId;
    }

    public byte[] getImageData() {
        return imageData;
    }

    @Override
    public String toString() {
        return type + ":" + userId + "|" + java.util.Base64.getEncoder().encodeToString(imageData);
    }
}
