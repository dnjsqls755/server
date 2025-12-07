package dto.response;

import dto.type.DtoType;
import dto.response.DTO;

public class FileDownloadResponse extends DTO {
    private final String chatRoomName;
    private final long messageId;
    private final String fileName;
    private final String mimeType;
    private final long fileSize;
    private final byte[] fileData;

    public FileDownloadResponse(String chatRoomName, long messageId, String fileName, String mimeType, long fileSize, byte[] fileData) {
        super(DtoType.FILE_DOWNLOAD_RESULT);
        this.chatRoomName = chatRoomName;
        this.messageId = messageId;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.fileData = fileData;
    }

    public String getChatRoomName() { return chatRoomName; }
    public long getMessageId() { return messageId; }
    public String getFileName() { return fileName; }
    public String getMimeType() { return mimeType; }
    public long getFileSize() { return fileSize; }
    public byte[] getFileData() { return fileData; }

    @Override
    public String toString() {
        return super.toString() + chatRoomName + "|" + messageId + "|" + fileName + "|" + mimeType + "|" + fileSize + "|" +
               java.util.Base64.getEncoder().encodeToString(fileData);
    }
}
