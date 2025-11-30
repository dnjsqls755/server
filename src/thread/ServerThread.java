package thread;

import app.ServerApplication;
import dao.ChatDao;
import domain.ChatRoom;
import domain.User;
import dto.request.*;
import dto.response.*;
import dto.response.DTO;
import dto.type.DtoType;
import dto.type.MessageType;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import service.ChatService;
import service.FriendOperationResult;

public class ServerThread extends Thread {

    private final Socket socket;
    private final ChatService chatService;
    private User currentUser;

    public ServerThread(Socket socket, ChatService chatService) {
        this.socket = socket;
        this.chatService = chatService;
    }

    @Override
    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            while (true) {
                String str = reader.readLine();
                if (str == null) {
                    System.out.println("socket error (can't get socket input stream) - client socket closed");
                    cleanupConnection();
                    return;
                }

                if (str.startsWith("POST /address")) {
                    handleHttpAddressRequest(reader);
                    continue;
                }

                String[] token = str.split(":");
                DtoType type = DtoType.valueOf(token[0]);
                String message = token.length > 1 ? token[1] : "";

                processReceiveMessage(type, message);
                Thread.sleep(300);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            cleanupConnection();
        }
    }

    private void processReceiveMessage(DtoType type, String message) throws IOException {
        switch (type) {

        case LOGIN:
            LoginRequest loginReq = new LoginRequest(message);
            User user = chatService.getUserByLogin(loginReq.getId(), loginReq.getPw());
            if (user == null) {
                sendDtoResponse(DtoType.LOGIN_FAIL);
                return;
            }
            if (user.isBanned()) {
                sendDtoResponse(DtoType.LOGIN_BANNED);
                return;
            }

            user.setSocket(socket);
            chatService.removeUser(user.getId());
            chatService.addUser(user);
            currentUser = user;

            List<ChatRoom> dbChatRooms = chatService.getAllChatRooms();
            sendMessage(new InitDataResponse(dbChatRooms, chatService.getUsers()));

            sendMessage(new FriendListResponse(chatService.getFriends(user.getId())));

            UserListResponse lobbyUserList = new UserListResponse("Lobby", chatService.getUsers());
            broadcastToAll(lobbyUserList);

            if (isAdmin()) {
                sendAdminSnapshot();
            }


            System.out.println("[LOGIN] 사용자 로그인 완료: " + user.getId() + " (" + user.getNickName() + ")");
            break;

        case LOGOUT:
            LogoutRequest logoutReq = new LogoutRequest(message);
            System.out.println("[LOGOUT] 로그아웃 요청 - 사용자 " + logoutReq.getUserId());

            chatService.removeUser(logoutReq.getUserId());
            if (currentUser != null && logoutReq.getUserId().equals(currentUser.getId())) {
                currentUser = null;
            }

            UserListResponse updatedUserList = new UserListResponse("Lobby", chatService.getUsers());
            broadcastToAll(updatedUserList);

            System.out.println("[LOGOUT] 로그아웃 완료");
            break;

        case ID_CHECK:
            boolean isDuplicate = chatService.isUserIdDuplicate(message.trim());
            sendDtoResponse(isDuplicate ? DtoType.ID_DUPLICATE : DtoType.ID_OK);
            break;

        case NICKNAME_CHECK:
            boolean isDuplicateNickname = chatService.isNicknameDuplicate(message.trim());
            sendDtoResponse(isDuplicateNickname ? DtoType.NICKNAME_DUPLICATE : DtoType.NICKNAME_OK);
            break;

        case SIGNUP:
            JoinRequest joinReq = new JoinRequest(message);
            if (!isValidPassword(joinReq.getPassword())) {
                sendDtoResponse(DtoType.SIGNUP_INVALID_PASSWORD);
                break;
            }

            boolean success = chatService.signupUser(joinReq);
            if (success) {
                try {
                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    int length = dis.readInt();
                    byte[] imageBytes = new byte[length];
                    dis.readFully(imageBytes);

                    File dir = new File("profile_images");
                    if (!dir.exists()) dir.mkdirs();

                    String fileName = UUID.randomUUID() + ".jpg";
                    File file = new File(dir, fileName);

                    BufferedImage originalImage = ImageIO.read(new java.io.ByteArrayInputStream(imageBytes));
                    Image scaledImage = originalImage.getScaledInstance(200, 200, Image.SCALE_SMOOTH);
                    BufferedImage resizedImage = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
                    resizedImage.getGraphics().drawImage(scaledImage, 0, 0, null);
                    ImageIO.write(resizedImage, "jpg", file);

                    String imagePath = "profile_images/" + fileName;
                    chatService.updateUserProfileImage(joinReq.getUserId(), imagePath);

                    System.out.println("[SIGNUP] 프로필 이미지 리사이징 완료: " + imagePath);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    System.out.println("이미지 처리 실패: " + ex.getMessage());
                }
                sendDtoResponse(DtoType.SIGNUP_SUCCESS);
            } else {
                sendDtoResponse(DtoType.SIGNUP_FAIL);
            }
            break;

        case CREATE_CHAT:
            CreateChatRoomRequest createReq = new CreateChatRoomRequest(message);
            System.out.println("[CREATE_CHAT] 채팅방 생성 요청 - 이름: " + createReq.getName() + ", 생성자 " + createReq.getUserId());

            ChatRoom newRoom = chatService.createChatRoom(createReq.getName(), createReq.getUserId());
            if (newRoom != null) {
                chatService.enterChatRoom(newRoom.getName(), createReq.getUserId());
                CreateChatRoomResponse createRes = new CreateChatRoomResponse(newRoom);
                broadcastToAll(createRes);

                List<User> roomUsers = chatService.getChatRoomUsers(newRoom.getName());
                UserListResponse userListRes = new UserListResponse(newRoom.getName(), roomUsers);
                sendMessage(userListRes);

                System.out.println("[CREATE_CHAT] 채팅방 생성 완료: " + newRoom.getName());
            }
            break;

        case ENTER_CHAT:
            EnterChatRequest enterReq = new EnterChatRequest(message);
            System.out.println("[ENTER_CHAT] 입장 요청 - 사용자 " + enterReq.getUserId() + ", 방 " + enterReq.getChatRoomName());

            User requestUser = chatService.getUser(enterReq.getUserId());
            if (requestUser == null) {
                System.out.println("[ENTER_CHAT] 로그인하지 않은 사용자 " + enterReq.getUserId());
                sendResponse("ENTER_FAIL:로그인이 필요합니다");
                break;
            }

            boolean isNewEntry = chatService.enterChatRoom(enterReq.getChatRoomName(), enterReq.getUserId());

            if (isNewEntry) {
                User enteredUser = chatService.getUser(enterReq.getUserId());
                MessageResponse enterMsg = new MessageResponse(
                        MessageType.ENTER,
                        enterReq.getChatRoomName(),
                        enteredUser.getNickName(),
                        enteredUser.getNickName() + "님이 입장하셨습니다."
                );
                broadcastToRoom(enterReq.getChatRoomName(), enterMsg);
            }

            List<User> users = chatService.getChatRoomUsers(enterReq.getChatRoomName());
            UserListResponse enterUserList = new UserListResponse(enterReq.getChatRoomName(), users);
            broadcastToRoom(enterReq.getChatRoomName(), enterUserList);

            if (!"Lobby".equals(enterReq.getChatRoomName())) {
                List<ChatDao.ChatMessage> history = chatService.loadChatMessages(enterReq.getChatRoomName());
                List<ChatHistoryResponse.HistoryEntry> entries = new ArrayList<>();
                for (ChatDao.ChatMessage msg : history) {
                    entries.add(new ChatHistoryResponse.HistoryEntry(msg.getNickname(), msg.getSentAt(), msg.getContent()));
                }
                sendMessage(new ChatHistoryResponse(enterReq.getChatRoomName(), entries));
            }

            System.out.println("[ENTER_CHAT] 입장 완료 - 현재 인원: " + users.size() + " (신규: " + isNewEntry + ")");
            break;

        case EXIT_CHAT:
            ExitChatRequest exitReq = new ExitChatRequest(message);
            System.out.println("[EXIT_CHAT] 퇴장 요청 - 사용자 " + exitReq.getUserId() + ", 방 " + exitReq.getChatRoomName());

            User exitedUser = chatService.exitChatRoom(exitReq.getChatRoomName(), exitReq.getUserId());

            if (exitedUser != null) {
                MessageResponse exitMsg = new MessageResponse(
                        MessageType.EXIT,
                        exitReq.getChatRoomName(),
                        exitedUser.getNickName(),
                        exitedUser.getNickName() + "님이 퇴장하셨습니다."
                );
                broadcastToRoom(exitReq.getChatRoomName(), exitMsg);
            }

            ChatRoom exitRoom = chatService.getChatRoom(exitReq.getChatRoomName());
            if (exitRoom != null && exitRoom.ieExistUser()) {
                List<User> remainingUsers = chatService.getChatRoomUsers(exitReq.getChatRoomName());
                UserListResponse exitUserList = new UserListResponse(exitReq.getChatRoomName(), remainingUsers);
                broadcastToRoom(exitReq.getChatRoomName(), exitUserList);
            }

            System.out.println("[EXIT_CHAT] 퇴장 완료");
            break;

        case MESSAGE:
            MessageResponse chatMsg = new MessageResponse(message);
            System.out.println("[MESSAGE] 메시지 수신 - 방 " + chatMsg.getChatRoomName() + ", 발신자 " + chatMsg.getUserName() + ", 내용: " + chatMsg.getMessage());

            if (chatMsg.getMessageType() == MessageType.CHAT && !"Lobby".equals(chatMsg.getChatRoomName())) {
                chatService.saveChatMessage(chatMsg.getChatRoomName(), chatMsg.getUserName(), chatMsg.getMessage());
            }

            if ("Lobby".equals(chatMsg.getChatRoomName())) {
                broadcastToAll(chatMsg);
            } else {
                broadcastToRoom(chatMsg.getChatRoomName(), chatMsg);
            }

            System.out.println("[MESSAGE] 메시지 전송 완료");
            break;

        case FRIEND_ADD:
            FriendAddRequest addReq = new FriendAddRequest(message);
            FriendOperationResult addResult = chatService.addFriendByNickname(addReq.getUserId(), addReq.getFriendNickname());
            sendMessage(new FriendOperationResponse(DtoType.FRIEND_ADD_RESULT, addResult.isSuccess(), addResult.getMessage()));
            if (addResult.isSuccess()) {
                sendMessage(new FriendListResponse(chatService.getFriends(addReq.getUserId())));
                String friendId = chatService.findUserIdByNickname(addReq.getFriendNickname());
                if (friendId != null) {
                    User friendUser = chatService.getUser(friendId);
                    if (friendUser != null) {
                        sendMessageToUser(friendUser, new FriendListResponse(chatService.getFriends(friendId)));
                    }
                }
            }
            break;

        case FRIEND_REMOVE:
            FriendRemoveRequest removeReq = new FriendRemoveRequest(message);
            FriendOperationResult removeResult = chatService.removeFriend(removeReq.getUserId(), removeReq.getFriendId());
            sendMessage(new FriendOperationResponse(DtoType.FRIEND_REMOVE_RESULT, removeResult.isSuccess(), removeResult.getMessage()));
            if (removeResult.isSuccess()) {
                sendMessage(new FriendListResponse(chatService.getFriends(removeReq.getUserId())));
                User friendUser = chatService.getUser(removeReq.getFriendId());
                if (friendUser != null) {
                    sendMessageToUser(friendUser, new FriendListResponse(chatService.getFriends(removeReq.getFriendId())));
                }
            }
            break;

        case FRIEND_CHAT_START:
            FriendChatStartRequest chatStartReq = new FriendChatStartRequest(message);
            String roomName = buildDirectChatRoomName(chatStartReq.getUserId(), chatStartReq.getFriendId());
            boolean createdNew = false;

            ChatRoom directRoom = chatService.getChatRoom(roomName);
            if (directRoom == null) {
                directRoom = chatService.createChatRoom(roomName, chatStartReq.getUserId());
                createdNew = directRoom != null;
            }

            if (directRoom != null) {
                chatService.enterChatRoom(roomName, chatStartReq.getUserId());

                if (createdNew) {
                    CreateChatRoomResponse createRes = new CreateChatRoomResponse(directRoom);
                    broadcastToAll(createRes);
                }

                String inviterNickname = chatService.findNicknameByUserId(chatStartReq.getUserId());
                User friendUser = chatService.getUser(chatStartReq.getFriendId());
                if (friendUser != null && inviterNickname != null) {
                    FriendChatInviteResponse inviteRes = new FriendChatInviteResponse(roomName, chatStartReq.getUserId(), inviterNickname);
                    sendMessageToUser(friendUser, inviteRes);
                }

                List<User> roomUsers = chatService.getChatRoomUsers(roomName);
                UserListResponse directUserList = new UserListResponse(roomName, roomUsers);
                broadcastToRoom(roomName, directUserList);
            } else {
                sendResponse("FRIEND_CHAT_FAIL");
            }
            break;

        case PROFILE_UPDATE:
            ProfileUpdateRequest profileReq = new ProfileUpdateRequest(message);
            FriendOperationResult profileResult = chatService.updateNickname(profileReq.getUserId(), profileReq.getNickname());
            sendMessage(new ProfileUpdateResponse(profileResult.isSuccess(), profileResult.getMessage(), profileReq.getNickname()));
            if (profileResult.isSuccess()) {
                UserListResponse refreshed = new UserListResponse("Lobby", chatService.getUsers());
                broadcastToAll(refreshed);
            }
            break;

        case ADMIN_INIT:
            if (!isAdmin()) {
                sendMessage(new AdminActionResultResponse(false, "??? ??? ?????."));
                break;
            }
            sendAdminSnapshot();
            break;

        case ADMIN_FORCE_LOGOUT:
            if (!isAdmin()) {
                sendMessage(new AdminActionResultResponse(false, "??? ??? ?????."));
                break;
            }
            AdminForceLogoutRequest forceLogoutReq = new AdminForceLogoutRequest(message, true);
            boolean forcedLogout = handleForceLogout(forceLogoutReq.getUserId(), "???? ?? ?????????.");
            sendMessage(new AdminActionResultResponse(forcedLogout, forcedLogout ? "???? ?????????." : "?? ???? ?? ? ????."));
            if (forcedLogout) {
                sendAdminSnapshot();
                broadcastToAll(new UserListResponse("Lobby", chatService.getUsers()));
            }
            break;

        case ADMIN_FORCE_EXIT:
            if (!isAdmin()) {
                sendMessage(new AdminActionResultResponse(false, "??? ??? ?????."));
                break;
            }
            AdminForceExitRequest forceExitReq = new AdminForceExitRequest(message, true);
            boolean forcedExit = handleForceExit(forceExitReq.getUserId(), forceExitReq.getRoomName(), "???? ?? ???????.");
            sendMessage(new AdminActionResultResponse(forcedExit, forcedExit ? "????? ?? ???????." : "???/???? ?? ? ????."));
            if (forcedExit) {
                sendAdminSnapshot();
            }
            break;

        case ADMIN_BAN:
            if (!isAdmin()) {
                sendMessage(new AdminActionResultResponse(false, "??? ??? ?????."));
                break;
            }
            AdminBanRequest banReq = new AdminBanRequest(message, true);
            boolean banUpdated = chatService.updateBanStatus(banReq.getUserId(), banReq.isBanned());
            if (banUpdated && banReq.isBanned()) {
                handleForceLogout(banReq.getUserId(), "???? ?? ???????.");
            }
            sendMessage(new AdminActionResultResponse(banUpdated, banUpdated ? (banReq.isBanned() ? "???? ??????." : "??? ??? ??????.") : "?? ?? ??? ??????."));
            if (banUpdated) {
                sendAdminSnapshot();
                broadcastToAll(new UserListResponse("Lobby", chatService.getUsers()));
            }
            break;

        case ADMIN_MESSAGE_SEARCH:
            if (!isAdmin()) {
                sendMessage(new AdminActionResultResponse(false, "??? ??? ?????."));
                break;
            }
            AdminMessageSearchRequest searchReq = new AdminMessageSearchRequest(message, true);
            sendMessage(new AdminMessageSearchResponse(chatService.searchMessages(searchReq.getNickname(), searchReq.getRoomName())));
            break;

        case ADMIN_MESSAGE_DELETE:
            if (!isAdmin()) {
                sendMessage(new AdminActionResultResponse(false, "??? ??? ?????."));
                break;
            }
            AdminMessageDeleteRequest deleteReq = new AdminMessageDeleteRequest(message, true);
            boolean deleted = chatService.deleteMessage(deleteReq.getMessageId());
            sendMessage(new AdminActionResultResponse(deleted, deleted ? "???? ??????." : "??? ??? ??????."));
            break;

        case ADMIN_ROOM_DELETE:
            if (!isAdmin()) {
                sendMessage(new AdminActionResultResponse(false, "??? ??? ?????."));
                break;
            }
            AdminRoomDeleteRequest roomDeleteReq = new AdminRoomDeleteRequest(message, true);
            boolean roomDeleted = handleRoomDeletion(roomDeleteReq.getRoomName());
            if (roomDeleted) {
                sendMessage(new AdminActionResultResponse(true, "???? ??????."));
            } else {
                sendMessage(new AdminActionResultResponse(false, "??? ??? ??????."));
            }
            break;

        case USER_LIST:
        case CHAT_ROOM_LIST:
            System.out.println("?? ???? ?? ??: " + type);
            break;

        default:
            System.out.println("???? ?? ??? ??: " + type);
            break;
        }
    }

    private void sendResponse(String message) throws IOException {
        PrintWriter writer = new PrintWriter(socket.getOutputStream());
        writer.println(message);
        writer.flush();
    }

    private void sendDtoResponse(DtoType type) throws IOException {
        PrintWriter writer = new PrintWriter(socket.getOutputStream());
        writer.println(type.toString());
        writer.flush();
    }

    private void sendMessage(DTO dto) {
        try {
            PrintWriter sender = new PrintWriter(socket.getOutputStream());
            sender.println(dto);
            sender.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessageToUser(User user, DTO dto) {
        try {
            Socket userSocket = user.getSocket();
            if (userSocket != null && !userSocket.isClosed()) {
                PrintWriter sender = new PrintWriter(userSocket.getOutputStream());
                sender.println(dto);
                sender.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void broadcastToRoom(String roomName, DTO dto) {
        try {
            ChatRoom room = chatService.getChatRoom(roomName);
            if (room == null) {
                System.out.println("[ERROR] 채팅방을 찾을 수 없음: " + roomName);
                return;
            }

            List<User> roomUsers = room.getUsers();
            for (User u : roomUsers) {
                Socket userSocket = u.getSocket();
                if (userSocket != null && !userSocket.isClosed()) {
                    PrintWriter sender = new PrintWriter(userSocket.getOutputStream());
                    sender.println(dto);
                    sender.flush();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void broadcastToAll(DTO dto) {
        try {
            for (Socket s : ServerApplication.sockets) {
                if (s != null && !s.isClosed()) {
                    PrintWriter sender = new PrintWriter(s.getOutputStream());
                    sender.println(dto);
                    sender.flush();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isAdmin() {
        return currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRole());
    }

    private void sendAdminSnapshot() {
        sendMessage(new AdminUserListResponse(chatService.getAllUsersWithStatus()));
        sendMessage(new AdminChatRoomListResponse(chatService.getChatRoomsWithCounts()));
    }

    private void sendDirectToUser(User target, DtoType type, String payload) {
        try {
            Socket userSocket = target.getSocket();
            if (userSocket != null && !userSocket.isClosed()) {
                PrintWriter sender = new PrintWriter(userSocket.getOutputStream());
                sender.println(type + ":" + payload);
                sender.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean handleForceLogout(String userId, String reason) {
        User target = chatService.getUser(userId);
        if (target != null) {
            chatService.removeUser(userId);
            sendDirectToUser(target, DtoType.FORCE_LOGOUT, reason);
            try {
                if (target.getSocket() != null) {
                    target.getSocket().close();
                }
            } catch (Exception ignore) { }
            return true;
        }
        chatService.removeUser(userId);
        return false;
    }

    private boolean handleForceExit(String userId, String roomName, String reason) {
        User removed = chatService.exitChatRoom(roomName, userId);
        if (removed != null) {
            sendDirectToUser(removed, DtoType.FORCE_EXIT, roomName + "|" + reason);
            List<User> remaining = chatService.getChatRoomUsers(roomName);
            UserListResponse list = new UserListResponse(roomName, remaining);
            broadcastToRoom(roomName, list);
            return true;
        }
        chatService.removeUserFromRoom(roomName, userId);
        ChatRoom room = chatService.getChatRoom(roomName);
        if (room != null) {
            UserListResponse list = new UserListResponse(roomName, chatService.getChatRoomUsers(roomName));
            broadcastToRoom(roomName, list);
        }
        return true;
    }

    private boolean handleRoomDeletion(String roomName) {
        if (ChatDao.LOBBY_CHAT_NAME.equals(roomName)) {
            return false;
        }
        ChatRoom room = chatService.getChatRoom(roomName);
        List<User> targets = room != null ? new ArrayList<>(room.getUsers()) : new ArrayList<>();
        boolean deleted = chatService.deleteChatRoom(roomName);
        if (deleted) {
            for (User u : targets) {
                sendDirectToUser(u, DtoType.FORCE_EXIT, roomName + "|채팅방이 관리자에 의해 종료되었습니다.");
            }
            broadcastToAll(new ChatRoomListResponse(chatService.getAllChatRooms()));
            sendAdminSnapshot();
            return true;
        }
        return false;
    }

    private boolean isValidPassword(String password) {
        if (password.length() < 8) return false;
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");
        return hasLetter && hasDigit && hasSpecial;
    }

    private void handleHttpAddressRequest(BufferedReader reader) {
        try {
            StringBuilder body = new StringBuilder();
            String line;
            int contentLength = 0;

            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("Content-Length:")) {
                    contentLength = Integer.parseInt(line.substring(16).trim());
                }
            }

            if (contentLength > 0) {
                char[] buffer = new char[contentLength];
                reader.read(buffer, 0, contentLength);
                body.append(buffer);
            }

            String jsonBody = body.toString();
            System.out.println("[HTTP] Address request: " + jsonBody);

            String postal = extractJsonValue(jsonBody, "postal");
            String address = extractJsonValue(jsonBody, "address");

            if (postal != null && address != null) {
                String result = postal + "|" + address;
                sendDtoMessage("ADDRESS_RESULT:" + result);
                System.out.println("[HTTP] Address sent to client: " + result);
            }

            PrintWriter writer = new PrintWriter(socket.getOutputStream());
            writer.println("HTTP/1.1 200 OK");
            writer.println("Access-Control-Allow-Origin: *");
            writer.println("Content-Type: application/json");
            writer.println("Content-Length: 15");
            writer.println();
            writer.println("{\"ok\":true}");
            writer.flush();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int startIndex = json.indexOf(searchKey);
        if (startIndex == -1) return null;

        startIndex = json.indexOf("\"", startIndex + searchKey.length()) + 1;
        int endIndex = json.indexOf("\"", startIndex);

        if (startIndex > 0 && endIndex > startIndex) {
            return json.substring(startIndex, endIndex);
        }
        return null;
    }

    private void sendDtoMessage(String message) throws IOException {
        PrintWriter writer = new PrintWriter(socket.getOutputStream());
        writer.println(message);
        writer.flush();
    }

    private String buildDirectChatRoomName(String userId, String friendId) {
        if (userId.compareTo(friendId) < 0) {
            return "DM-" + userId + "-" + friendId;
        }
        return "DM-" + friendId + "-" + userId;
    }

    private void cleanupConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("socket closed.");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            ServerApplication.sockets.remove(socket);
            if (currentUser != null) {
                chatService.removeUser(currentUser.getId());
                UserListResponse updatedUserList = new UserListResponse("Lobby", chatService.getUsers());
                broadcastToAll(updatedUserList);
                currentUser = null;
            }
        }
    }
}
