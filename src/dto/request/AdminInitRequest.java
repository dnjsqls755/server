package dto.request;

import dto.type.DtoType;

public class AdminInitRequest extends DTO {
    public AdminInitRequest() {
        super(DtoType.ADMIN_INIT);
    }

    public AdminInitRequest(String message) {
        super(DtoType.ADMIN_INIT);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
