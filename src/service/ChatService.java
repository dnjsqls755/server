package service;

import dao.ChatDao;
import domain.ChatRoom;
import domain.User;
import dto.request.JoinRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ChatService {

    private final ChatDao chatDao;

    public ChatService(ChatDao chatDao) {
        this.chatDao = chatDao;
    }

    public boolean signupUser(JoinRequest req) {
        try {
            Connection conn = chatDao.getConnection();

            String sql = "INSERT INTO users (user_id, name, password, profile_img, nickname, email, phone, address, detail_address, postal_code, gender, birth_date) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, req.getUserId());
            pstmt.setString(2, req.getName());
            pstmt.setString(3, req.getPassword());
            pstmt.setString(4, req.getProfileImg());
            pstmt.setString(5, req.getNickname());
            pstmt.setString(6, req.getEmail());
            pstmt.setString(7, req.getPhone());
            pstmt.setString(8, req.getAddress());
            pstmt.setString(9, req.getDetailAddress());
            pstmt.setString(10, req.getPostalCode());
            pstmt.setString(11, req.getGender());
            
            // 생년월일 처리 - null 또는 빈 문자열 체크
            if (req.getBirthDate() != null && !req.getBirthDate().trim().isEmpty()) {
                try {
                    pstmt.setDate(12, java.sql.Date.valueOf(req.getBirthDate()));
                } catch (IllegalArgumentException e) {
                    System.err.println("[SIGNUP] 생년월일 형식 오류: " + req.getBirthDate() + " (yyyy-MM-dd 형식 필요)");
                    pstmt.setDate(12, null);
                }
            } else {
                pstmt.setDate(12, null);
            }

            int rows = pstmt.executeUpdate();
            System.out.println("[SIGNUP] DB INSERT 성공 - userId: " + req.getUserId() + ", nickname: " + req.getNickname());
            return rows > 0;
        } catch (Exception e) {
            System.err.println("[SIGNUP ERROR] 회원가입 실패 - userId: " + req.getUserId());
            e.printStackTrace();
            return false;
        }
    }

    public boolean isUserIdDuplicate(String userId) {
        try {
            Connection conn = chatDao.getConnection();
            String sql = "SELECT COUNT(*) FROM users WHERE user_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isNicknameDuplicate(String nickname) {
        try {
            Connection conn = chatDao.getConnection();
            String sql = "SELECT COUNT(*) FROM users WHERE nickname = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, nickname);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public User getUserByLogin(String userId, String password) {
        try {
            Connection conn = chatDao.getConnection();
            String sql = "SELECT user_id, nickname, role, NVL(is_banned, 0) AS is_banned " +
                    "FROM users WHERE user_id = ? AND password = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, userId);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String id = rs.getString("user_id");
                String nickname = rs.getString("nickname");
                String role = rs.getString("role");
                boolean banned = rs.getInt("is_banned") == 1;
                return new User(id, nickname, role, false, banned);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void addUser(User user) {
        user.setOnline(true);
        chatDao.addUser(user);
        chatDao.updateOnlineStatus(user.getId(), true);
    }

    public void removeUser(String userId) {
        chatDao.removeUser(userId);
        chatDao.updateOnlineStatus(userId, false);
    }

    public List<User> getUsers() {
        return chatDao.getUsers();
    }

    public User getUser(String userId) {
        return chatDao.getUser(userId).orElse(null);
    }

    public User getUserByNickname(String nickname) {
        return chatDao.findUserByNickname(nickname).orElse(null);
    }

    public ChatRoom createChatRoom(String roomName, String creatorId) {
        try {
            if (chatDao.findChatRoomByName(roomName).isPresent()) {
                System.out.println("[ERROR] 이미 존재하는 채팅방: " + roomName);
                return null;
            }

            ChatRoom newRoom = new ChatRoom(roomName, creatorId);
            chatDao.addChatRoom(newRoom);
            return newRoom;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean enterChatRoom(String roomName, String userId) {
        try {
            User user = chatDao.getUser(userId).orElse(null);
            if (user == null) {
                System.out.println("[ERROR] 사용자를 찾을 수 없음: " + userId);
                return false;
            }

            ChatRoom room = chatDao.findChatRoomByName(roomName).orElse(null);
            if (room == null) {
                System.out.println("[ERROR] 채팅방을 찾을 수 없음: " + roomName);
                return false;
            }

            boolean alreadyInMemory = room.getUsers().stream()
                    .anyMatch(u -> u.getId().equals(userId));

            if (!alreadyInMemory) {
                room.addUser(user);
            }

            int inserted = chatDao.addUserToChatRoom(roomName, userId);
            return inserted > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public User exitChatRoom(String roomName, String userId) {
        try {
            ChatRoom room = chatDao.findChatRoomByName(roomName).orElse(null);
            if (room == null) {
                System.out.println("[ERROR] 채팅방을 찾을 수 없음: " + roomName);
                return null;
            }

            User user = room.getUsers().stream()
                    .filter(u -> u.getId().equals(userId))
                    .findFirst()
                    .orElse(null);

            if (user != null) {
                room.getUsers().remove(user);
                chatDao.removeUserFromChatRoom(roomName, userId);
                
                // 빈 채팅방 자동 삭제 (로비 제외)
                if (!"Lobby".equals(roomName) && room.getUsers().isEmpty()) {
                    System.out.println("[자동삭제] 빈 채팅방 삭제: " + roomName);
                    chatDao.deleteChatRoom(roomName);
                }
            }

            return user;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void removeUserFromRoom(String roomName, String userId) {
        chatDao.removeUserFromChatRoom(roomName, userId);
    }

    public ChatRoom getChatRoom(String roomName) {
        return chatDao.findChatRoomByName(roomName).orElse(null);
    }

    public List<ChatRoom> getAllChatRooms() {
        return chatDao.findAllChatRoomsExceptLobby();
    }

    public List<User> getChatRoomUsers(String roomName) {
        ChatRoom room = chatDao.findChatRoomByName(roomName).orElse(null);
        if (room != null) {
            return room.getUsers();
        }
        return new ArrayList<>();
    }

    public List<User> getFriends(String userId) {
        return chatDao.getFriends(userId);
    }

    public List<User> getRoomMembers(String roomName) {
        return chatDao.findRoomMembers(roomName);
    }

    public FriendOperationResult addFriendByNickname(String userId, String friendNickname) {
        try {
            String friendId = chatDao.findUserIdByNickname(friendNickname);
            if (friendId == null) {
                return new FriendOperationResult(false, "존재하지 않는 닉네임입니다.");
            }
            if (friendId.equals(userId)) {
                return new FriendOperationResult(false, "본인은 친구로 추가할 수 없습니다.");
            }
            if (chatDao.areFriends(userId, friendId)) {
                return new FriendOperationResult(false, "이미 친구입니다.");
            }

            boolean added = chatDao.addFriendship(userId, friendId) && chatDao.addFriendship(friendId, userId);
            if (added) {
                return new FriendOperationResult(true, "친구로 추가했습니다.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new FriendOperationResult(false, "친구 추가에 실패했습니다.");
    }

    public FriendOperationResult removeFriend(String userId, String friendId) {
        try {
            if (!chatDao.areFriends(userId, friendId)) {
                return new FriendOperationResult(false, "친구 관계가 아닙니다.");
            }
            boolean removed = chatDao.removeFriendship(userId, friendId) && chatDao.removeFriendship(friendId, userId);
            if (removed) {
                return new FriendOperationResult(true, "친구를 삭제했습니다.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new FriendOperationResult(false, "친구 삭제에 실패했습니다.");
    }

    public String findNicknameByUserId(String userId) {
        return chatDao.findNicknameByUserId(userId);
    }

    public String findUserIdByNickname(String nickname) {
        return chatDao.findUserIdByNickname(nickname);
    }

    public FriendOperationResult updateNickname(String userId, String newNickname) {
        try {
            String ownerId = chatDao.findUserIdByNickname(newNickname);
            if (ownerId != null && !ownerId.equals(userId)) {
                return new FriendOperationResult(false, "이미 사용 중인 닉네임입니다.");
            }
            boolean ok = chatDao.updateNickname(userId, newNickname);
            if (ok) {
                // 메모리 사용자 객체 닉네임 업데이트
                User me = chatDao.getUser(userId).orElse(null);
                if (me != null) {
                    me.setNickName(newNickname);
                }
                return new FriendOperationResult(true, "닉네임을 변경했습니다.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new FriendOperationResult(false, "닉네임 변경에 실패했습니다.");
    }

    public void saveChatMessage(String roomName, String userNickname, String content) {
        try {
            User sender = chatDao.findUserByNickname(userNickname).orElse(null);
            if (sender == null) {
                String userId = chatDao.findUserIdByNickname(userNickname);
                if (userId == null) {
                    System.out.println("[ERROR] 사용자를 찾을 수 없음: " + userNickname);
                    return;
                }
                chatDao.saveMessage(roomName, userId, content);
            } else {
                chatDao.saveMessage(roomName, sender.getId(), content);
            }
            System.out.println("[DB] 메시지 저장 완료 - 방 " + roomName + ", 발신자 " + userNickname);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<ChatDao.ChatMessage> loadChatMessages(String roomName) {
        List<ChatDao.ChatMessage> raw = chatDao.loadMessages(roomName, 100);
        List<ChatDao.ChatMessage> filtered = new ArrayList<>();
        for (ChatDao.ChatMessage msg : raw) {
            // 귓속말은 히스토리에서 제외 (내용 prefix로 식별)
            if (msg.getContent() != null && msg.getContent().startsWith("[WHISPER")) {
                continue;
            }
            filtered.add(msg);
        }
        return filtered;
    }

    public List<User> getAllUsersWithStatus() {
        return chatDao.findAllUsersWithStatus();
    }

    public boolean updateBanStatus(String userId, boolean banned) {
        boolean ok = chatDao.updateBanStatus(userId, banned);
        if (ok) {
            // 메모리 사용자 객체의 차단 상태 동기화
            User u = chatDao.getUser(userId).orElse(null);
            if (u != null) {
                u.setBanned(banned);
                // 차단된 경우 온라인 상태도 즉시 반영 (선택)
                if (banned) {
                    u.setOnline(false);
                }
            }
        }
        return ok;
    }

    public List<ChatRoom> getChatRoomsWithCounts() {
        return chatDao.findChatRoomsWithCounts();
    }

    public boolean deleteChatRoom(String roomName) {
        return chatDao.deleteChatRoom(roomName);
    }

    public List<ChatDao.AdminMessageRecord> searchMessages(String nickname, String roomName) {
        return chatDao.searchMessages(nickname, roomName);
    }

    public boolean deleteMessage(long messageId) {
        return chatDao.deleteMessage(messageId);
    }

    // 아이디 찾기: 이름과 이메일로 사용자 ID 조회
    public String findUserIdByNameAndEmail(String name, String email) {
        return chatDao.findUserIdByNameAndEmail(name, email);
    }

    // 비밀번호 찾기: 아이디와 이메일로 사용자 확인
    public boolean verifyUserForPasswordReset(String userId, String email) {
        return chatDao.verifyUserByIdAndEmail(userId, email);
    }

    // 비밀번호 재설정
    public boolean updatePassword(String userId, String newPassword) {
        return chatDao.updatePassword(userId, newPassword);
    }

    public ChatDao.AdminUserDetails getAdminUserDetails(String userId) {
        return chatDao.getUserDetails(userId);
    }

    public boolean updateUserInfo(String userId, String name, String nickname, String email, String phone,
                                   String address, String detailAddress, String postalCode,
                                   String gender, String birthDate) {
        boolean ok = chatDao.updateUserInfo(userId, name, nickname, email, phone, address, detailAddress, postalCode, gender, birthDate);
        if (ok) {
            // メモリ使用者オブジェクト ニックネーム同期
            User me = chatDao.getUser(userId).orElse(null);
            if (me != null) {
                me.setNickName(nickname);
            }
        }
        return ok;
    }

    public String[] getUserFullInfo(String userId) {
        return chatDao.getUserFullInfo(userId);
    }

    public long saveFileMessage(String chatRoomName, String senderId, String fileName,
                                String filePath, long fileSize, String mimeType) {
        return chatDao.saveFileMessage(chatRoomName, senderId, fileName, filePath, fileSize, mimeType);
    }

    public ChatDao.FileInfo getFileInfo(long messageId) {
        return chatDao.getFileInfo(messageId);
    }

    public String getRoomNameByMessageId(long messageId) {
        return chatDao.getRoomNameByMessageId(messageId);
    }

    public java.util.List<dto.response.ChatHistoryResponse.HistoryEntry> getMessageHistory(String chatRoomName) {
        return chatDao.getMessageHistory(chatRoomName);
    }

    public String getProfileImagePath(String userId) {
        return chatDao.getProfileImagePath(userId);
    }

    public boolean updateProfileImagePath(String userId, String imagePath) {
        return chatDao.updateProfileImagePath(userId, imagePath);
    }

    public boolean updateUserProfileImage(String userId, String imagePath) {
        return updateProfileImagePath(userId, imagePath);
    }
}
