package dto.request;

import dto.type.DtoType;

public class FileUploadRequest extends DTO {
    private final String chatRoomName;
    private final String senderId;
    private final String fileName;
    private final String mimeType;
    private final long fileSize;
    private final byte[] fileData;

    public FileUploadRequest(String message) {
        super(DtoType.FILE_UPLOAD);
        String[] parts = message.split("\\|", 6);
        this.chatRoomName = parts.length > 0 ? parts[0] : "";
        this.senderId = parts.length > 1 ? parts[1] : "";
        this.fileName = parts.length > 2 ? parts[2] : "";
        this.mimeType = parts.length > 3 ? parts[3] : "";
        this.fileSize = parts.length > 4 ? Long.parseLong(parts[4]) : 0;
        this.fileData = parts.length > 5 ? java.util.Base64.getDecoder().decode(parts[5]) : new byte[0];
    }

    public String getChatRoomName() { return chatRoomName; }
    public String getSenderId() { return senderId; }
    public String getFileName() { return fileName; }
    public String getMimeType() { return mimeType; }
    public long getFileSize() { return fileSize; }
    public byte[] getFileData() { return fileData; }
}
