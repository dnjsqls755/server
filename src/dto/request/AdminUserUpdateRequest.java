package dto.request;

import dto.type.DtoType;

public class AdminUserUpdateRequest extends DTO {
    private final String userId;
    private final String name;
    private final String nickname;
    private final String email;
    private final String phone;
    private final String address;
    private final String detailAddress;
    private final String postalCode;
    private final String gender;
    private final String birthDate;

    public AdminUserUpdateRequest(String message) {
        super(DtoType.ADMIN_USER_UPDATE);
        String[] parts = message.split("\\|", 10);
        this.userId = parts.length > 0 ? parts[0] : "";
        this.name = parts.length > 1 ? parts[1] : "";
        this.nickname = parts.length > 2 ? parts[2] : "";
        this.email = parts.length > 3 ? parts[3] : "";
        this.phone = parts.length > 4 ? parts[4] : "";
        this.address = parts.length > 5 ? parts[5] : "";
        this.detailAddress = parts.length > 6 ? parts[6] : "";
        this.postalCode = parts.length > 7 ? parts[7] : "";
        this.gender = parts.length > 8 ? parts[8] : "";
        this.birthDate = parts.length > 9 ? parts[9] : "";
    }

    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getNickname() { return nickname; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getDetailAddress() { return detailAddress; }
    public String getPostalCode() { return postalCode; }
    public String getGender() { return gender; }
    public String getBirthDate() { return birthDate; }
}
