package dto.response;

import dto.type.DtoType;

public class FileUploadResponse extends DTO {
    private final boolean success;
    private final String message;
    private final long messageId;

    public FileUploadResponse(boolean success, String message, long messageId) {
        super(DtoType.FILE_UPLOAD_RESULT);
        this.success = success;
        this.message = message;
        this.messageId = messageId;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public long getMessageId() { return messageId; }

    @Override
    public String toString() {
        return super.toString() + success + "|" + messageId + "|" + message;
    }
}
