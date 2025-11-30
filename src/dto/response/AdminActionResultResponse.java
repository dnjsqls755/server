package dto.response;

import dto.type.DtoType;

public class AdminActionResultResponse extends DTO {
    private final boolean success;
    private final String message;

    public AdminActionResultResponse(boolean success, String message) {
        super(DtoType.ADMIN_ACTION_RESULT);
        this.success = success;
        this.message = message;
    }

    @Override
    public String toString() {
        return super.toString() + (success ? "OK" : "FAIL") + "|" + escape(message);
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("|", " ").replace("\n", " ");
    }
}
