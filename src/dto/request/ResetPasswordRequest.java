package dto.request;

public class ResetPasswordRequest {
    private String id;
    private String newPassword;

    public ResetPasswordRequest(String message) {
        String[] parts = message.split(":");
        this.id = parts.length > 0 ? parts[0] : "";
        this.newPassword = parts.length > 1 ? parts[1] : "";
    }

    public String getId() {
        return id;
    }

    public String getNewPassword() {
        return newPassword;
    }
}
