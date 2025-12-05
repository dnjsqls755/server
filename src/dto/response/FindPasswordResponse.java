package dto.response;

import dto.type.DtoType;

public class FindPasswordResponse extends DTO {
    private boolean success;
    private String message;

    public FindPasswordResponse(boolean success, String message) {
        super(DtoType.FIND_PASSWORD_RESULT);
        this.success = success;
        this.message = message;
    }

    @Override
    public String toString() {
        return super.toString() + success + ":" + message;
    }
}
