package dto.response;

import dto.type.DtoType;

public class FindIdResponse extends DTO {
    private boolean success;
    private String userId;
    private String message;

    public FindIdResponse(boolean success, String userId, String message) {
        super(DtoType.FIND_ID_RESULT);
        this.success = success;
        this.userId = userId;
        this.message = message;
    }

    @Override
    public String toString() {
        return super.toString() + success + ":" + (userId != null ? userId : "") + ":" + message;
    }
}
