package dto.request;

public class JoinRequest {
    private String userId;
    private String name;
    private String password;
    private String profileImg;
    private String nickname;
    private String email;
    private String phone;
    private String address;
    private String detailAddress;
    private String postalCode;
    private String gender;
    private String birthDate;

    public JoinRequest(String message) {
        String[] tokens = message.split(",", -1);
        this.userId = tokens[0];
        this.name = tokens[1];
        this.password = tokens[2];
        this.profileImg = tokens[3];
        this.nickname = tokens[4];
        this.email = tokens[5];
        this.phone = tokens[6];
        this.address = tokens[7];
        this.detailAddress = tokens[8];
        this.postalCode = tokens[9];
        this.gender = tokens[10];
        this.birthDate = tokens[11];
    }


public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getPassword() { return password; }
    public String getProfileImg() { return profileImg; }
    public String getNickname() { return nickname; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getDetailAddress() { return detailAddress; }
    public String getPostalCode() { return postalCode; }
    public String getGender() { return gender; }
    public String getBirthDate() { return birthDate; }

}