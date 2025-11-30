package dto.response;

import dto.type.DtoType;

public class FriendOperationResponse extends DTO {
    private final boolean success;
    private final String message;

    public FriendOperationResponse(DtoType type, boolean success, String message) {
        super(type);
        this.success = success;
        this.message = message;
    }

    @Override
    public String toString() {
        return super.toString() + success + "," + message;
    }
}
