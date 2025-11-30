package dto.request;

import dto.type.DtoType;

public class AdminMessageDeleteRequest extends DTO {
    private long messageId;

    public AdminMessageDeleteRequest(long messageId) {
        super(DtoType.ADMIN_MESSAGE_DELETE);
        this.messageId = messageId;
    }

    public AdminMessageDeleteRequest(String message, boolean parse) {
        super(DtoType.ADMIN_MESSAGE_DELETE);
        try {
            this.messageId = Long.parseLong(message.trim());
        } catch (NumberFormatException e) {
            this.messageId = 0;
        }
    }

    public long getMessageId() {
        return messageId;
    }

    @Override
    public String toString() {
        return super.toString() + messageId;
    }
}
