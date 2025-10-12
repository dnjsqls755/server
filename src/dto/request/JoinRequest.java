package dto.request;

public class JoinRequest {
    private String userId;
    private String name;
    private String password;
    private String profileImg;
    private String statusMsg;
    private String nickname;
    private String email;
    private String phone;
    private String address;
    private String detailAddress;
    private String postalCode;
    private String gender;
    private String birthDate; // 문자열로 받아서 변환

    public JoinRequest(String message) {
        String[] tokens = message.split(",");
        this.userId = tokens[0];
        this.name = tokens[1];
        this.password = tokens[2];
        this.profileImg = tokens[3];
        this.statusMsg = tokens[4];
        this.nickname = tokens[5];
        this.email = tokens[6];
        this.phone = tokens[7];
        this.address = tokens[8];
        this.detailAddress = tokens[9];
        this.postalCode = tokens[10];
        this.gender = tokens[11];
        this.birthDate = tokens[12];
    }


public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getPassword() { return password; }
    public String getProfileImg() { return profileImg; }
    public String getStatusMsg() { return statusMsg; }
    public String getNickname() { return nickname; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getDetailAddress() { return detailAddress; }
    public String getPostalCode() { return postalCode; }
    public String getGender() { return gender; }
    public String getBirthDate() { return birthDate; }

}