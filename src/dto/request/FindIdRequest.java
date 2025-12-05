package dto.request;

public class FindIdRequest {
    private String name;
    private String email;

    public FindIdRequest(String message) {
        String[] parts = message.split(":");
        this.name = parts.length > 0 ? parts[0] : "";
        this.email = parts.length > 1 ? parts[1] : "";
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}
