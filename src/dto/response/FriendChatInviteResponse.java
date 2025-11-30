package dto.response;

import dto.type.DtoType;

public class FriendChatInviteResponse extends DTO {
    private final String roomName;
    private final String inviterId;
    private final String inviterNickname;

    public FriendChatInviteResponse(String roomName, String inviterId, String inviterNickname) {
        super(DtoType.FRIEND_CHAT_INVITE);
        this.roomName = roomName;
        this.inviterId = inviterId;
        this.inviterNickname = inviterNickname;
    }

    @Override
    public String toString() {
        return super.toString() + roomName + "," + inviterId + "," + inviterNickname;
    }
}
