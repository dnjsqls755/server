package dto.response;

import dto.type.DtoType;

public class ProfileImageResponse extends DTO {
    private final String userId;
    private final String imageData;  // Base64 encoded image or "DEFAULT" for default image
    private final String name;
    private final String gender;
    private final String birthDate;

    public ProfileImageResponse(String userId, String imageData, String name, String gender, String birthDate) {
        super(DtoType.PROFILE_IMAGE_RESULT);
        this.userId = userId;
        this.imageData = imageData;
        this.name = name != null ? name : "";
        this.gender = gender != null ? gender : "";
        this.birthDate = birthDate != null ? birthDate : "";
    }

    public ProfileImageResponse(String message) {
        super(DtoType.PROFILE_IMAGE_RESULT);
        String[] parts = message.split("\\|", 5);
        this.userId = parts.length > 0 ? parts[0] : "";
        this.imageData = parts.length > 1 ? parts[1] : "DEFAULT";
        this.name = parts.length > 2 ? parts[2] : "";
        this.gender = parts.length > 3 ? parts[3] : "";
        this.birthDate = parts.length > 4 ? parts[4] : "";
    }

    public String getUserId() {
        return userId;
    }

    public String getImageData() {
        return imageData;
    }

    public String getName() {
        return name;
    }

    public String getGender() {
        return gender;
    }

    public String getBirthDate() {
        return birthDate;
    }

    @Override
    public String toString() {
        return super.toString() + userId + "|" + imageData + "|" + name + "|" + gender + "|" + birthDate;
    }
}
