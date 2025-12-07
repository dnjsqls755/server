package dto.response;

import dto.type.DtoType;

public class FileMessageResponse extends DTO {
    private final String chatRoomName;
    private final String senderNickname;
    private final long messageId;
    private final String fileName;
    private final String mimeType;
    private final long fileSize;
    private final String sentAt;

    public FileMessageResponse(String chatRoomName, String senderNickname, long messageId,
                               String fileName, String mimeType, long fileSize, String sentAt) {
        super(DtoType.FILE_MESSAGE);
        this.chatRoomName = chatRoomName;
        this.senderNickname = senderNickname;
        this.messageId = messageId;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.sentAt = sentAt;
    }

    public String getChatRoomName() { return chatRoomName; }
    public String getSenderNickname() { return senderNickname; }
    public long getMessageId() { return messageId; }
    public String getFileName() { return fileName; }
    public String getMimeType() { return mimeType; }
    public long getFileSize() { return fileSize; }
    public String getSentAt() { return sentAt; }

    @Override
    public String toString() {
        return super.toString() + chatRoomName + "|" + senderNickname + "|" + messageId + "|" + 
               fileName + "|" + mimeType + "|" + fileSize + "|" + sentAt;
    }
}
