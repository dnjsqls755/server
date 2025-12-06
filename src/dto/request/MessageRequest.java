package dto.request;

import dto.type.DtoType;
import dto.type.MessageType;

public class MessageRequest extends DTO {
    MessageType messageType;
    String chatRoomName;
    String userName;
    String message;

    public MessageRequest(MessageType messageType, String chatRoomName, String userName, String message) {
        super(DtoType.MESSAGE);
        this.messageType = messageType;
        this.chatRoomName = chatRoomName;
        this.userName = userName;
        this.message = message;
    }

    public MessageRequest(String raw) {
        super(DtoType.MESSAGE);
        // messageType|chatRoomName|userName|message (pipe separator to allow commas inside fields)
        String[] parts = raw.split("\\|", 4);
        this.messageType = MessageType.valueOf(parts[0]);
        this.chatRoomName = parts.length > 1 ? parts[1] : "";
        this.userName = parts.length > 2 ? parts[2] : "";
        this.message = parts.length > 3 ? parts[3] : "";
    }

    public MessageType getMessageType() { return messageType; }
    public String getChatRoomName() { return chatRoomName; }
    public String getUserName() { return userName; }
    public String getMessage() { return message; }

    @Override
    public String toString() {
        return super.toString() + messageType + "|" + chatRoomName + "|" + userName + "|" + message;
    }
}
