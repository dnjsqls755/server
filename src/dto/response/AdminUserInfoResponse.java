package dto.response;

import dto.type.DtoType;

public class AdminUserInfoResponse extends DTO {
    private final String name;
    private final String userId;
    private final String nickname;
    private final String email;
    private final String phone;
    private final String address;
    private final String detailAddress;
    private final String postalCode;
    private final String gender;
    private final String birthDate;

    public AdminUserInfoResponse(String name, String userId, String nickname, String email, String phone,
                                  String address, String detailAddress, String postalCode,
                                  String gender, String birthDate) {
        super(DtoType.ADMIN_USER_INFO_RESULT);
        this.name = name;
        this.userId = userId;
        this.nickname = nickname;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.detailAddress = detailAddress;
        this.postalCode = postalCode;
        this.gender = gender;
        this.birthDate = birthDate;
    }

    @Override
    public String toString() {
        return super.toString() + name + "|" + userId + "|" + nickname + "|" + email + "|" + phone + "|" + 
               address + "|" + detailAddress + "|" + postalCode + "|" + gender + "|" + birthDate;
    }
}
