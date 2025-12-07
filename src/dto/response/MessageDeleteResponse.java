package dto.response;

import dto.type.DtoType;

public class MessageDeleteResponse extends DTO {
    private final String chatRoomName;
    private final long messageId;

    public MessageDeleteResponse(String chatRoomName, long messageId) {
        super(DtoType.MESSAGE_DELETE);
        this.chatRoomName = chatRoomName;
        this.messageId = messageId;
    }

    public String getChatRoomName() {
        return chatRoomName;
    }

    public long getMessageId() {
        return messageId;
    }

    @Override
    public String toString() {
        return super.toString() + chatRoomName + "|" + messageId;
    }
}
