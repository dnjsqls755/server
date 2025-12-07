package dto.response;

import dto.type.DtoType;

public class ProfileImageUpdateResponse extends DTO {
    private boolean success;
    private String message;

    public ProfileImageUpdateResponse(boolean success, String message) {
        super(DtoType.PROFILE_IMAGE_UPDATE_RESULT);
        this.success = success;
        this.message = message;
    }

    public ProfileImageUpdateResponse(String payload) {
        super(DtoType.PROFILE_IMAGE_UPDATE_RESULT);
        String[] parts = payload.split("\\|", 2);
        this.success = parts.length > 0 && "true".equals(parts[0]);
        this.message = parts.length > 1 ? parts[1] : "";
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return type + ":" + success + "|" + message;
    }
}
