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

    ChatDao chatDao;

    public ChatService(ChatDao chatDao) {
        this.chatDao = chatDao;
    }

    //회원가입 처리 메서드
    public boolean signupUser(JoinRequest req) {
        try {
            Connection conn = chatDao.getConnection();

            String sql = "INSERT INTO users (user_id, name, password, profile_img, status_msg, nickname, email, phone, address, detail_address, postal_code, gender, birth_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, req.getUserId());
            pstmt.setString(2, req.getName());
            pstmt.setString(3, req.getPassword());
            pstmt.setString(4, req.getProfileImg());
            pstmt.setString(5, req.getStatusMsg());
            pstmt.setString(6, req.getNickname());
            pstmt.setString(7, req.getEmail());
            pstmt.setString(8, req.getPhone());
            pstmt.setString(9, req.getAddress());
            pstmt.setString(10, req.getDetailAddress());
            pstmt.setString(11, req.getPostalCode());
            pstmt.setString(12, req.getGender());
            pstmt.setDate(13, java.sql.Date.valueOf(req.getBirthDate()));

            pstmt.executeUpdate();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //아이디 중복확인 메서드
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

    //닉네임 중복처리 메서드
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

    //로그인 검증 메서드
    public boolean isValidLogin(String userId, String password) {
        try {
            Connection conn = chatDao.getConnection();
            String sql = "SELECT COUNT(*) FROM users WHERE user_id = ? AND password = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, userId);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    //로그인시 사용자 정보 가져오는 메서드
    public User getUserByLogin(String userId, String password) {
        try {
            Connection conn = chatDao.getConnection();
            String sql = "SELECT user_id, nickname FROM users WHERE user_id = ? AND password = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, userId);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String id = rs.getString("user_id");
                String nickname = rs.getString("nickname");
                return new User(id, nickname);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 이미지 경로 업데이트 메서드
    public void updateUserProfileImage(String userId, String imagePath) {
        try {
            Connection conn = chatDao.getConnection();
            String sql = "UPDATE users SET profile_img = ? WHERE user_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, imagePath);
            pstmt.setString(2, userId);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 사용자 정보 추가
    public void addUser(User user) {
        chatDao.addUser(user);
    }
    
    // 사용자 제거
    public void removeUser(String userId) {
        chatDao.removeUser(userId);
    }

    // 접속 중인 사용자 목록 조회
    public List<User> getUsers() {
        return chatDao.getUsers();
    }

    // 사용자 조회
    public User getUser(String userId) {
        return chatDao.getUser(userId).orElse(null);
    }

    // 채팅방 생성
    public ChatRoom createChatRoom(String roomName, String creatorId) {
        try {
            // 이미 존재하는 채팅방인지 확인
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

    // 채팅방 입장 (신규 입장인지 여부 반환)
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

            // 메모리에 이미 있는지 확인 (뒤로가기 후 재입장 케이스)
            boolean alreadyInMemory = room.getUsers().stream()
                .anyMatch(u -> u.getId().equals(userId));
            
            if (!alreadyInMemory) {
                room.addUser(user);
            }
            
            // DB에 채팅방-사용자 관계 저장 (중복 방지 쿼리)
            int inserted = chatDao.addUserToChatRoom(roomName, userId);
            
            // DB에 신규 등록된 경우에만 true 반환
            return inserted > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 채팅방 퇴장
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
                
                // DB에서 채팅방-사용자 관계 삭제
                chatDao.removeUserFromChatRoom(roomName, userId);
            }

            return user;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 채팅방 조회
    public ChatRoom getChatRoom(String roomName) {
        return chatDao.findChatRoomByName(roomName).orElse(null);
    }
    
    // 모든 채팅방 조회 (로비 제외)
    public List<ChatRoom> getAllChatRooms() {
        return chatDao.findAllChatRoomsExceptLobby();
    }

    // 채팅방 사용자 목록 조회
    public List<User> getChatRoomUsers(String roomName) {
        ChatRoom room = chatDao.findChatRoomByName(roomName).orElse(null);
        if (room != null) {
            return room.getUsers();
        }
        return new ArrayList<>();
    }

    // 채팅 메시지 저장
    public void saveChatMessage(String roomName, String userNickname, String content) {
        try {
            // 닉네임으로 사용자 찾기
            User sender = chatDao.findUserByNickname(userNickname).orElse(null);
            if (sender == null) {
                // DB에서 사용자 찾기
                String userId = chatDao.findUserIdByNickname(userNickname);
                if (userId == null) {
                    System.out.println("[ERROR] 사용자를 찾을 수 없음: " + userNickname);
                    return;
                }
                chatDao.saveMessage(roomName, userId, content);
            } else {
                chatDao.saveMessage(roomName, sender.getId(), content);
            }
            System.out.println("[DB] 메시지 저장 완료 - 방: " + roomName + ", 발신자: " + userNickname);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 이전 메시지 불러오기
    public List<ChatDao.ChatMessage> loadChatMessages(String roomName) {
        return chatDao.loadMessages(roomName, 100);
    }

    // TODO: 추가 기능 구현 예정
}


