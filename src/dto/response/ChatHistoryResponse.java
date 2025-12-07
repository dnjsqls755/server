package dto.response;

import dto.type.DtoType;
import java.util.List;

public class ChatHistoryResponse extends DTO {

    private String chatRoomName;
    private List<HistoryEntry> entries;

    public static class HistoryEntry {
        public final String nickname;
        public final String time; // HH:MM
        public final String content;
        public final String messageType;      // "TEXT", "IMAGE", "FILE"
        public final long messageId;
        public final String fileName;
        public final String mimeType;
        public final long fileSize;

        // 텍스트 메시지
        public HistoryEntry(String nickname, String time, String content) {
            this.nickname = nickname;
            this.time = time;
            this.content = content;
            this.messageType = "TEXT";
            this.messageId = 0;
            this.fileName = null;
            this.mimeType = null;
            this.fileSize = 0;
        }

        // 파일 메시지
        public HistoryEntry(String nickname, String time, String content,
                          String messageType, long messageId, String fileName, String mimeType, long fileSize) {
            this.nickname = nickname;
            this.time = time;
            this.content = content;
            this.messageType = messageType;
            this.messageId = messageId;
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.fileSize = fileSize;
        }
    }

    public ChatHistoryResponse(String chatRoomName, List<HistoryEntry> entries) {
        super(DtoType.CHAT_HISTORY);
        this.chatRoomName = chatRoomName;
        this.entries = entries;
    }

    @Override
    public String toString() {
        // 형식: CHAT_HISTORY:roomName|nick,time,content,msgType,msgId,fileName,mimeType,fileSize|...
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString()).append(chatRoomName);
        for (HistoryEntry e : entries) {
            sb.append("|")
              .append(escape(e.nickname)).append(",")
              .append(escape(e.time)).append(",")
              .append(escape(e.content)).append(",")
              .append(escape(e.messageType)).append(",")
              .append(e.messageId).append(",")
              .append(escape(e.fileName != null ? e.fileName : "")).append(",")
              .append(escape(e.mimeType != null ? e.mimeType : "")).append(",")
              .append(e.fileSize);
        }
        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("|", " ")  // 구분자 교체
                .replace(",", " ")  // 콤마 교체
                .replace("\n", " ");
    }
}