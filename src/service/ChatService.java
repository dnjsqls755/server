package service;

import app.ServerApplication;
import dao.ChatDao;
import domain.ChatRoom;
import domain.User;
import exception.ChatRoomExistException;
import exception.ChatRoomNotFoundException;
import exception.UserNotFoundException;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Optional;

import dto.request.JoinRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
public class ChatService {

    ChatDao chatDao;

    public ChatService(ChatDao chatDao) {
        this.chatDao = chatDao;
    }
    //회원가입 처리 메서드
    public boolean signupUser(JoinRequest req) {
        try {
            Connection conn = chatDao.getConnection(); // ChatDao에서 전달받은 DB 연결 사용

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
            pstmt.setDate(13, java.sql.Date.valueOf(req.getBirthDate())); // "YYYY-MM-DD" 형식

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
    //로그인검증 메서드
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
                return new User(id, nickname); // 서버에서 User 객체 생성
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
 // 이미지 경로 업데이트 메서드 추가
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

    public void enterLobby(User user) {
        chatDao.getLobby().addUser(user);
    }

    // 채팅방 입장
    public void enterChatRoom(String chatRoomName, String userId) throws UserNotFoundException, ChatRoomNotFoundException {
        Optional<User> findUser = chatDao.findUserById(userId);
        if (findUser.isEmpty()) {
            throw new UserNotFoundException(userId);
        }

        List<ChatRoom> chatRooms = chatDao.getChatRooms();
        Optional<ChatRoom> findChatRoom = chatRooms.stream()
                .filter(chatRoom -> chatRoom.getName().equals(chatRoomName))
                .findAny();

        if (findChatRoom.isEmpty()) {
            throw new ChatRoomNotFoundException(chatRoomName);
        }

        findChatRoom.get().addUser(findUser.get());
    }

 // 채팅방 생성
    public ChatRoom createChatRoom(String chatRoomName, String userId) throws ChatRoomExistException {
        Optional<ChatRoom> findChatRoom = chatDao.getChatRooms().stream()
                .filter(chatRoom -> chatRoom.getName().equals(chatRoomName))
                .findAny();

        if (findChatRoom.isEmpty()) {
            ChatRoom chatRoom = new ChatRoom(chatRoomName, userId); // creatorId 추가
            chatDao.addChatRoom(chatRoom); // DB 저장 (creatorId 포함)
            return chatRoom;
        } else {
            throw new ChatRoomExistException(chatRoomName);
        }
    }
    public void initChatRooms() {
        List<ChatRoom> dbRooms = chatDao.loadChatRoomsFromDB();
        chatDao.getChatRooms().addAll(dbRooms);
    }
    // 채팅방 나가기
    public User exitChatRoom(String chatRoomName, String userId) throws UserNotFoundException, ChatRoomNotFoundException {
        Optional<ChatRoom> chatRoom = chatDao.findChatRoomByName(chatRoomName);
        if (chatRoom.isEmpty()) {
            throw new ChatRoomNotFoundException(chatRoomName);
        }

        Optional<User> findUser = chatRoom.get().getUsers().stream()
                .filter(user -> user.getId().equals(userId))
                .findAny();

        if (findUser.isEmpty()) {
            throw new UserNotFoundException(userId);
        }

        List<User> users = chatRoom.get().getUsers();
        users.remove(findUser.get());

        if (!chatRoom.get().ieExistUser()) {
            chatDao.getChatRooms().remove(chatRoom.get());
        }

        return findUser.get();
    }

    public List<User> getUsers() {
        return chatDao.getUsers();
    }

    public List<ChatRoom> getChatRooms() { return chatDao.getChatRooms(); }

    public User getUser(String userId) {
        Optional<User> findUser = chatDao.findUserById(userId);
        if (findUser.isEmpty()) {
            System.out.println("[" + userId + "] 아이디의 사용자 없음");
            return null;
        }
        return findUser.get();
    }

    public ChatRoom getChatRoom(String chatRoomName) {
        if (chatRoomName.equals(ChatDao.LOBBY_CHAT_NAME)) {
            return chatDao.getLobby();
        }

        Optional<ChatRoom> findChatRoom = chatDao.findChatRoomByName(chatRoomName);
        if (findChatRoom.isEmpty()) {
            System.out.println("[" + chatRoomName + "] 이름의 채팅방 없음");
            return null;
        }
        return findChatRoom.get();
    }

    public List<User> getChatRoomUsers(String chatRoomName) {
        Optional<ChatRoom> findChatRoom = chatDao.findChatRoomByName(chatRoomName);
        if (findChatRoom.isEmpty()) {
            System.out.println("[" + chatRoomName + "] 이름의 채팅방 없음");
            return null;
        }
        return findChatRoom.get().getUsers();
    }

    public void disconnect(String userId) throws UserNotFoundException, IOException {
        Optional<User> findUser = chatDao.getUser(userId);
        if (findUser.isEmpty()) {
            throw new UserNotFoundException(userId);
        }

        // 사용자가 입장해있는 채팅방에서 사용자 삭제
        List<ChatRoom> chatRooms = chatDao.getChatRooms();
        chatRooms.forEach(chatRoom -> chatRoom.removeUser(findUser.get())); // TODO 스트림 람다식 수정

        // 전체 사용자 리스트에서 제거
        List<User> users = chatDao.getUsers();
        users.remove(findUser.get());

        // 소켓 닫기 및 소켓 리스트에서 제거
        List<Socket> sockets = ServerApplication.sockets;
        Socket clientSocket = findUser.get().getSocket();
        clientSocket.close();
        sockets.remove(findUser.get().getSocket());
    }
}
