package dao;

import domain.ChatRoom;
import domain.User;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
public class ChatDao {

    private List<User> users = new ArrayList<>(); // 접속 중인 모든 사용자 리스트
    private List<ChatRoom> chatRooms = new ArrayList<>();
    private ChatRoom lobby; // 로비 채팅방 정보

    public final static String LOBBY_CHAT_NAME = "Lobby";

    private Connection connection; // Oracle DB 연결 객체

    // 생성자에서 DB 연결 객체를 받음
    public ChatDao(Connection connection) {
        this.connection = connection;

        // 로비 채팅방 기본 생성
        lobby = new ChatRoom(LOBBY_CHAT_NAME);
    }
    

    public void addUser(User user) {
        users.add(user);
    }
    
    public void removeUser(String userId) {
        users.removeIf(user -> user.getId().equals(userId));
    }
    
    // TODO: 아래 채팅방 관련 메서드들은 필요시 구현
    
    //채팅방 생성 및 DB저장
    public void addChatRoom(ChatRoom chatRoom) {
        chatRooms.add(chatRoom);
        String sql = "INSERT INTO ChatRooms (room_id, room_name, creator_id) VALUES (chatroom_seq.NEXTVAL, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, chatRoom.getName());
            pstmt.setString(2, chatRoom.getCreatorId()); 
            pstmt.executeUpdate();
            System.out.println("채팅방 [" + chatRoom.getName() + "] DB 저장 완료");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }              
    
    public Optional<User> findUserById(String id) {
        return users.stream().filter(user -> user.getId().equals(id)).findAny();
        // TODO: DB에서 사용자 조회하는 코드로 변경 가능
    }
    //로비제외 모든 채팅방 조회
    public List<ChatRoom> findAllChatRoomsExceptLobby() {
        List<ChatRoom> rooms = new ArrayList<>();
        String sql = "SELECT room_name, creator_id FROM ChatRooms WHERE room_name != ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ChatDao.LOBBY_CHAT_NAME);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                rooms.add(new ChatRoom(rs.getString("room_name"), rs.getString("creator_id")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rooms;
    }
    //채팅방 이름으로 채팅방 조회
    public Optional<ChatRoom> findChatRoomByName(String name) {
        // 1차: 메모리에서 먼저 검색 (사용자 목록 등 상태 유지)
        for (ChatRoom room : chatRooms) {
            if (room.getName().equals(name)) {
                return Optional.of(room);
            }
        }

        // 2차: DB에서 조회 후 메모리에 등록
        String sql = "SELECT room_name, creator_id FROM ChatRooms WHERE room_name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String roomName = rs.getString("room_name");
                String creatorId = rs.getString("creator_id");
                ChatRoom newRoom = new ChatRoom(roomName, creatorId);
                chatRooms.add(newRoom); // 메모리에 캐싱
                return Optional.of(newRoom);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public ChatRoom getLobby() {
        return lobby;
    }

    public List<User> getUsers() {
        return users;
    }

    public void updateOnlineStatus(String userId, boolean online) {
        String sql = "UPDATE users SET is_online = ? WHERE user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, online ? 1 : 0);
            pstmt.setString(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Optional<User> getUser(String userId) {
        return users.stream().filter(user -> user.getId().equals(userId)).findAny();
    }

    public Optional<User> findUserByNickname(String nickname) {
        return users.stream()
                .filter(user -> user.getNickName().equals(nickname))
                .findAny();
    }

    public List<ChatRoom> getChatRooms() {
        return chatRooms;
    }

    public Connection getConnection() {
        return connection;
    }

    public void refreshChatRoomsFromDb() {
        List<ChatRoom> rooms = findAllChatRoomsExceptLobby();
        chatRooms.clear();
        chatRooms.addAll(rooms);
    }

    public void saveMessage(String chatRoomName, String senderId, String content) {
        String sql = "INSERT INTO Messages (message_id, room_id, sender_id, content) " +
                "VALUES (messages_seq.NEXTVAL, (SELECT room_id FROM ChatRooms WHERE room_name = ?), ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, chatRoomName);
            pstmt.setString(2, senderId);
            pstmt.setString(3, content);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 채팅 메시지 내부 클래스
    public static class ChatMessage {
        private String nickname;
        private String content;
        private String sentAt;

        public ChatMessage(String nickname, String content, String sentAt) {
            this.nickname = nickname;
            this.content = content;
            this.sentAt = sentAt;
        }

        public String getNickname() { return nickname; }
        public String getContent() { return content; }
        public String getSentAt() { return sentAt; }
    }

    // 채팅방의 이전 메시지 불러오기 (시간 포함)
    public List<ChatMessage> loadMessages(String roomName, int limit) {
        List<ChatMessage> messages = new ArrayList<>();
        // 최근 100개(요청 limit) 메시지 그대로 가져와 시간 표시 (닉네임 항상 표시)
        String sql = "SELECT u.nickname, m.content, TO_CHAR(m.sent_at,'HH24:MI') AS sent_time " +
                     "FROM Messages m " +
                     "JOIN ChatRooms r ON m.room_id = r.room_id " +
                     "JOIN users u ON m.sender_id = u.user_id " +
                     "WHERE r.room_name = ? " +
                     "ORDER BY m.sent_at DESC FETCH FIRST ? ROWS ONLY";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, roomName);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                messages.add(new ChatMessage(
                        rs.getString("nickname"),
                        rs.getString("content"),
                        rs.getString("sent_time")
                ));
            }
            System.out.println("[DB] 최근 메시지 로드 완료 - 방:" + roomName + ", 개수:" + messages.size());
        } catch (SQLException e) {
            System.err.println("[DB ERROR] 최근 메시지 로드 실패: " + e.getMessage());
            e.printStackTrace();
        }

        // 최신순으로 가져왔으니 UI에 과거->현재 순으로 보여주려면 역순 정렬
        java.util.Collections.reverse(messages);
        return messages;
    }

    // 채팅방에 사용자 추가 (DB) - 영향받은 행 수 반환
    public int addUserToChatRoom(String roomName, String userId) {
        String sql = "INSERT INTO ChatRoomUsers (room_id, user_id) " +
                     "SELECT r.room_id, ? FROM ChatRooms r " +
                     "WHERE r.room_name = ? " +
                     "AND NOT EXISTS (SELECT 1 FROM ChatRoomUsers cu WHERE cu.room_id = r.room_id AND cu.user_id = ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, roomName);
            pstmt.setString(3, userId);
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    // 채팅방에서 사용자 제거 (DB)
    public void removeUserFromChatRoom(String roomName, String userId) {
        String sql = "DELETE FROM ChatRoomUsers " +
                     "WHERE room_id = (SELECT room_id FROM ChatRooms WHERE room_name = ?) " +
                     "AND user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, roomName);
            pstmt.setString(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 닉네임으로 사용자 ID 찾기 (DB)
    public String findUserIdByNickname(String nickname) {
        String sql = "SELECT user_id FROM users WHERE nickname = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nickname);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("user_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String findNicknameByUserId(String userId) {
        String sql = "SELECT nickname FROM users WHERE user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("nickname");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 친구 목록 조회
    public List<User> getFriends(String userId) {
        List<User> friends = new ArrayList<>();
        String sql = "SELECT u.user_id, u.nickname FROM Friends f JOIN Users u ON f.friend_id = u.user_id WHERE f.user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                friends.add(new User(rs.getString("user_id"), rs.getString("nickname")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return friends;
    }

    public boolean areFriends(String userId, String friendId) {
        String sql = "SELECT COUNT(*) FROM Friends WHERE user_id = ? AND friend_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, friendId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean addFriendship(String userId, String friendId) {
        String sql = "INSERT INTO Friends (friendship_id, user_id, friend_id) VALUES (friendship_seq.NEXTVAL, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, friendId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean removeFriendship(String userId, String friendId) {
        String sql = "DELETE FROM Friends WHERE user_id = ? AND friend_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, friendId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateNickname(String userId, String newNickname) {
        String sql = "UPDATE users SET nickname = ? WHERE user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, newNickname);
            pstmt.setString(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<User> findAllUsersWithStatus() {
        List<User> result = new ArrayList<>();
        String sql = "SELECT user_id, nickname, role, NVL(is_online, 0) AS is_online, NVL(is_banned, 0) AS is_banned FROM users";
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                result.add(new User(
                        rs.getString("user_id"),
                        rs.getString("nickname"),
                        rs.getString("role"),
                        rs.getInt("is_online") == 1,
                        rs.getInt("is_banned") == 1
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public boolean updateBanStatus(String userId, boolean banned) {
        String sql = "UPDATE users SET is_banned = ? WHERE user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, banned ? 1 : 0);
            pstmt.setString(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<ChatRoom> findChatRoomsWithCounts() {
        List<ChatRoom> rooms = new ArrayList<>();
        String sql = "SELECT r.room_name, r.creator_id, " +
                "(SELECT COUNT(*) FROM ChatRoomUsers cu WHERE cu.room_id = r.room_id) AS member_count " +
                "FROM ChatRooms r";
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                rooms.add(new ChatRoom(
                        rs.getString("room_name"),
                        rs.getString("creator_id"),
                        rs.getInt("member_count")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rooms;
    }

    public boolean deleteChatRoom(String roomName) {
        if (LOBBY_CHAT_NAME.equals(roomName)) {
            return false;
        }

        String findRoomSql = "SELECT room_id FROM ChatRooms WHERE room_name = ?";
        try (PreparedStatement findStmt = connection.prepareStatement(findRoomSql)) {
            findStmt.setString(1, roomName);
            ResultSet rs = findStmt.executeQuery();
            if (!rs.next()) {
                return false;
            }
            int roomId = rs.getInt("room_id");

            try (PreparedStatement deleteUsers = connection.prepareStatement("DELETE FROM ChatRoomUsers WHERE room_id = ?")) {
                deleteUsers.setInt(1, roomId);
                deleteUsers.executeUpdate();
            }

            try (PreparedStatement deleteMessages = connection.prepareStatement("DELETE FROM Messages WHERE room_id = ?")) {
                deleteMessages.setInt(1, roomId);
                deleteMessages.executeUpdate();
            }

            try (PreparedStatement deleteRoom = connection.prepareStatement("DELETE FROM ChatRooms WHERE room_id = ?")) {
                deleteRoom.setInt(1, roomId);
                deleteRoom.executeUpdate();
            }

            chatRooms.removeIf(r -> r.getName().equals(roomName));
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<AdminMessageRecord> searchMessages(String nicknameFilter, String roomNameFilter) {
        List<AdminMessageRecord> result = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT m.message_id, r.room_name, u.nickname, m.content, TO_CHAR(m.sent_at, 'YYYY-MM-DD HH24:MI:SS') AS sent_at " +
                "FROM Messages m JOIN ChatRooms r ON m.room_id = r.room_id " +
                "JOIN Users u ON m.sender_id = u.user_id WHERE 1=1 ");
        List<String> params = new ArrayList<>();
        if (nicknameFilter != null && !nicknameFilter.isBlank()) {
            sql.append("AND LOWER(u.nickname) LIKE LOWER(?) ");
            params.add("%" + nicknameFilter + "%");
        }
        if (roomNameFilter != null && !roomNameFilter.isBlank()) {
            sql.append("AND LOWER(r.room_name) LIKE LOWER(?) ");
            params.add("%" + roomNameFilter + "%");
        }
        sql.append("ORDER BY m.sent_at DESC FETCH FIRST 200 ROWS ONLY");

        try (PreparedStatement pstmt = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setString(i + 1, params.get(i));
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                result.add(new AdminMessageRecord(
                        rs.getLong("message_id"),
                        rs.getString("room_name"),
                        rs.getString("nickname"),
                        rs.getString("content"),
                        rs.getString("sent_at")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public boolean deleteMessage(long messageId) {
        String sql = "DELETE FROM Messages WHERE message_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, messageId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static class AdminMessageRecord {
        private final long id;
        private final String roomName;
        private final String nickname;
        private final String content;
        private final String sentAt;

        public AdminMessageRecord(long id, String roomName, String nickname, String content, String sentAt) {
            this.id = id;
            this.roomName = roomName;
            this.nickname = nickname;
            this.content = content;
            this.sentAt = sentAt;
        }

        public long getId() { return id; }
        public String getRoomName() { return roomName; }
        public String getNickname() { return nickname; }
        public String getContent() { return content; }
        public String getSentAt() { return sentAt; }
    }

    // 아이디 찾기: 이름과 이메일로 사용자 ID 조회
    public String findUserIdByNameAndEmail(String name, String email) {
        String sql = "SELECT user_id FROM Users WHERE name = ? AND email = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("user_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 비밀번호 찾기: 아이디와 이메일로 사용자 확인
    public boolean verifyUserByIdAndEmail(String userId, String email) {
        String sql = "SELECT COUNT(*) FROM Users WHERE user_id = ? AND email = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, email);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 비밀번호 재설정
    public boolean updatePassword(String userId, String newPassword) {
        String sql = "UPDATE Users SET password = ? WHERE user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, newPassword);
            pstmt.setString(2, userId);
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 사용자 정보 수정 (관리자용)
    public boolean updateUserInfo(String userId, String nickname, String email, String phone,
                                   String address, String detailAddress, String postalCode,
                                   String gender, String birthDate) {
        String sql = "UPDATE Users SET nickname = ?, email = ?, phone = ?, address = ?, " +
                     "detail_address = ?, postal_code = ?, gender = ?, birth_date = ? WHERE user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nickname);
            pstmt.setString(2, email);
            pstmt.setString(3, phone);
            pstmt.setString(4, address);
            pstmt.setString(5, detailAddress);
            pstmt.setString(6, postalCode);
            pstmt.setString(7, gender);
            
            // 생년월일 처리
            if (birthDate != null && !birthDate.trim().isEmpty()) {
                try {
                    pstmt.setDate(8, java.sql.Date.valueOf(birthDate));
                } catch (IllegalArgumentException e) {
                    pstmt.setDate(8, null);
                }
            } else {
                pstmt.setDate(8, null);
            }
            
            pstmt.setString(9, userId);
            int rows = pstmt.executeUpdate();
            
            // 메모리상 사용자 객체도 업데이트
            if (rows > 0) {
                User u = getUser(userId).orElse(null);
                if (u != null) {
                    u.setNickName(nickname);
                }
            }
            
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // DB에서 사용자 전체 정보 조회
    public String[] getUserFullInfo(String userId) {
        String sql = "SELECT nickname, email, phone, address, detail_address, postal_code, gender, " +
                     "TO_CHAR(birth_date, 'YYYY-MM-DD') AS birth_date FROM Users WHERE user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new String[]{
                    rs.getString("nickname") != null ? rs.getString("nickname") : "",
                    rs.getString("email") != null ? rs.getString("email") : "",
                    rs.getString("phone") != null ? rs.getString("phone") : "",
                    rs.getString("address") != null ? rs.getString("address") : "",
                    rs.getString("detail_address") != null ? rs.getString("detail_address") : "",
                    rs.getString("postal_code") != null ? rs.getString("postal_code") : "",
                    rs.getString("gender") != null ? rs.getString("gender") : "",
                    rs.getString("birth_date") != null ? rs.getString("birth_date") : ""
                };
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new String[]{"", "", "", "", "", "", "", ""};
    }
}
