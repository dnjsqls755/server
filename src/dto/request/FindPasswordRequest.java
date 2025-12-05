package dto.request;

public class FindPasswordRequest {
    private String id;
    private String email;

    public FindPasswordRequest(String message) {
        String[] parts = message.split(":");
        this.id = parts.length > 0 ? parts[0] : "";
        this.email = parts.length > 1 ? parts[1] : "";
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }
}
