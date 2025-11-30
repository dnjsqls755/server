package dto.response;

import dto.type.DtoType;

public class ProfileUpdateResponse extends DTO {
    private final boolean success;
    private final String message;
    private final String nickname;

    public ProfileUpdateResponse(boolean success, String message, String nickname) {
        super(DtoType.PROFILE_UPDATE_RESULT);
        this.success = success;
        this.message = message;
        this.nickname = nickname;
    }

    @Override
    public String toString() {
        return super.toString() + success + "," + message + "," + nickname;
    }
}
