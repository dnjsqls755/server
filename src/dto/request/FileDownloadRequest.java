package dto.request;

import dto.type.DtoType;

public class FileDownloadRequest extends DTO {
    private final long messageId;
    private final String chatRoomName;

    public FileDownloadRequest(String message) {
        super(DtoType.FILE_DOWNLOAD);
        String[] parts = message.split("\\|", 2);
        this.messageId = parts.length > 0 ? Long.parseLong(parts[0]) : 0;
        this.chatRoomName = parts.length > 1 ? parts[1] : "";
    }

    public long getMessageId() { return messageId; }
    public String getChatRoomName() { return chatRoomName; }
}
