package dto.response;

import dao.ChatDao;
import dto.type.DtoType;

import java.util.List;

public class AdminMessageSearchResponse extends DTO {
    private final List<ChatDao.AdminMessageRecord> messages;

    public AdminMessageSearchResponse(List<ChatDao.AdminMessageRecord> messages) {
        super(DtoType.ADMIN_MESSAGE_RESULT);
        this.messages = messages;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        for (int i = 0; i < messages.size(); i++) {
            ChatDao.AdminMessageRecord record = messages.get(i);
            sb.append(record.getId()).append(",")
                    .append(escape(record.getRoomName())).append(",")
                    .append(escape(record.getNickname())).append(",")
                    .append(escape(record.getContent())).append(",")
                    .append(escape(record.getSentAt()));
            if (i < messages.size() - 1) {
                sb.append("|");
            }
        }
        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("|", " ").replace(",", " ").replace("\n", " ");
    }

    public List<ChatDao.AdminMessageRecord> getMessages() {
        return messages;
    }
}
