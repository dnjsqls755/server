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
        public HistoryEntry(String nickname, String time, String content) {
            this.nickname = nickname;
            this.time = time;
            this.content = content;
        }
    }

    public ChatHistoryResponse(String chatRoomName, List<HistoryEntry> entries) {
        super(DtoType.CHAT_HISTORY);
        this.chatRoomName = chatRoomName;
        this.entries = entries;
    }

    @Override
    public String toString() {
        // 형식: CHAT_HISTORY:roomName|nick,time,content|nick,time,content
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString()).append(chatRoomName);
        for (HistoryEntry e : entries) {
            sb.append("|")
              .append(escape(e.nickname)).append(",")
              .append(escape(e.time)).append(",")
              .append(escape(e.content));
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