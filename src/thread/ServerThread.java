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
import java.nio.file.Files;
import java.nio.file.Path;
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

                int sep = str.indexOf(':');
                String typeToken = sep >= 0 ? str.substring(0, sep) : str;
                String payload = sep >= 0 ? str.substring(sep + 1) : "";

                try {
                    DtoType type = DtoType.valueOf(typeToken);
                    processReceiveMessage(type, payload);
                } catch (IllegalArgumentException e) {
                    System.err.println("[ERROR] 알 수 없는 DtoType: '" + typeToken + "' (원본: " + str + ")");
                }
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

            // 모든 채팅방 가져오기
            List<ChatRoom> allChatRooms = chatService.getAllChatRooms();
            
            // 사용자에게 표시할 채팅방 필터링 (일반 채팅방 + 본인이 포함된 1:1 채팅방)
            List<ChatRoom> userChatRooms = new ArrayList<>();
            for (ChatRoom room : allChatRooms) {
                String roomName = room.getName();
                // 일반 채팅방이거나, 본인 ID가 포함된 1:1 채팅방만 추가
                if (!roomName.startsWith("DM-") || roomName.contains(user.getId())) {
                    userChatRooms.add(room);
                }
            }
            
            sendMessage(new InitDataResponse(userChatRooms, chatService.getUsers()));

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
            System.out.println("[SIGNUP] 회원가입 요청 - userId: " + joinReq.getUserId() + ", nickname: " + joinReq.getNickname());
            
            if (!isValidPassword(joinReq.getPassword())) {
                System.out.println("[SIGNUP] 비밀번호 검증 실패 - 8자 이상, 영문/숫자/특수문자 필요");
                sendDtoResponse(DtoType.SIGNUP_INVALID_PASSWORD);
                break;
            }

            boolean success = chatService.signupUser(joinReq);
            if (success) {
                try {
                    System.out.println("[SIGNUP] DB 저장 성공, 프로필 이미지 수신 대기...");
                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    int length = dis.readInt();
                    System.out.println("[SIGNUP] 이미지 크기: " + length + " bytes");
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
                    System.err.println("[SIGNUP] 이미지 처리 실패: " + ex.getMessage());
                    ex.printStackTrace();
                }
                sendDtoResponse(DtoType.SIGNUP_SUCCESS);
                System.out.println("[SIGNUP] 회원가입 완료 - userId: " + joinReq.getUserId());
            } else {
                System.err.println("[SIGNUP] 회원가입 실패 - DB 저장 실패");
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
                
                // 1:1 채팅방이 아닌 경우에만 전체 브로드캐스트
                if (!newRoom.getName().startsWith("DM-")) {
                    broadcastToAll(createRes);
                }
                // 1:1 채팅방인 경우 해당 사용자들에게만 전송은 FRIEND_CHAT_START에서 처리됨

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

            // 사용자 목록을 보낸 후 이전 대화 내역 전송 (ChatPanel이 준비된 후)
            if (!"Lobby".equals(enterReq.getChatRoomName())) {
                try {
                    Thread.sleep(100); // ChatPanel 생성 대기
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
                
                List<ChatDao.ChatMessage> history = chatService.loadChatMessages(enterReq.getChatRoomName());
                if (!history.isEmpty()) {
                    List<ChatHistoryResponse.HistoryEntry> entries = new ArrayList<>();
                    for (ChatDao.ChatMessage msg : history) {
                        if ("IMAGE".equals(msg.getMessageType()) || "FILE".equals(msg.getMessageType())) {
                            // 파일 메시지
                            entries.add(new ChatHistoryResponse.HistoryEntry(
                                msg.getNickname(), msg.getSentAt(), msg.getContent(),
                                msg.getMessageType(), msg.getMessageId(), msg.getFileName(),
                                msg.getMimeType(), msg.getFileSize()
                            ));
                        } else {
                            // 텍스트 메시지
                            entries.add(new ChatHistoryResponse.HistoryEntry(msg.getNickname(), msg.getSentAt(), msg.getContent()));
                        }
                    }
                    sendMessage(new ChatHistoryResponse(enterReq.getChatRoomName(), entries));
                    System.out.println("[ENTER_CHAT] 이전 대화 " + entries.size() + "개 전송 완료 - 방: " + enterReq.getChatRoomName());
                } else {
                    System.out.println("[ENTER_CHAT] 이전 대화 없음 - 방: " + enterReq.getChatRoomName());
                }
            }

            System.out.println("[ENTER_CHAT] 입장 완료 - 방: " + enterReq.getChatRoomName() + ", 현재 인원: " + users.size() + " (신규: " + isNewEntry + ")");
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
            } else if (exitRoom == null) {
                // 방이 삭제되었으면 모든 사용자에게 채팅방 목록 갱신
                System.out.println("[자동삭제] 채팅방 목록 갱신 브로드캐스트: " + exitReq.getChatRoomName());
                broadcastToAll(new ChatRoomListResponse(chatService.getAllChatRooms()));
            }

            System.out.println("[EXIT_CHAT] 퇴장 완료");
            break;

        case MESSAGE: {
            MessageRequest msgReq = new MessageRequest(message);
            MessageType mt = msgReq.getMessageType();
            String room = msgReq.getChatRoomName();
            String userField = msgReq.getUserName();
            String content = msgReq.getMessage();

            if (mt == MessageType.WHISPER) {
                String[] parts = userField.split(",", 2);
                String from = parts.length > 0 ? parts[0].trim() : "";
                String to = parts.length > 1 ? parts[1].trim() : "";

                List<User> roomUsers = chatService.getChatRoomUsers(room);
                boolean sentToTarget = false;
                boolean sentToSender = false;

                for (User u : roomUsers) {
                    String nick = u.getNickName();
                    if (nick.equals(to)) {
                        sendMessageToUser(u, new MessageResponse(MessageType.WHISPER, room, from + "," + to, content));
                        sentToTarget = true;
                    }
                    if (nick.equals(from)) {
                        sendMessageToUser(u, new MessageResponse(MessageType.WHISPER, room, from + "," + to, content));
                        sentToSender = true;
                    }
                }

                // 귓속말이 실패한 경우 발신자에게만 알림
                if (!sentToTarget && sentToSender) {
                    User senderUser = chatService.getUserByNickname(from);
                    if (senderUser != null) {
                        sendMessageToUser(senderUser,
                                new MessageResponse(MessageType.WHISPER, room, from + "," + to,
                                        "[안내] 대상 사용자를 찾을 수 없습니다."));
                    }
                }

                System.out.println("[WHISPER] " + from + " → " + to + " @" + room + " : " + content + " (toSent=" + sentToTarget + ")");
            } else {
                MessageResponse chatMsg = new MessageResponse(mt, room, userField, content);
                System.out.println("[MESSAGE] 메시지 수신 - 방 " + room + ", 발신자 " + userField + ", 내용: " + content);

                if (mt == MessageType.CHAT && !"Lobby".equals(room)) {
                    chatService.saveChatMessage(room, userField, content);
                }

                if ("Lobby".equals(room)) {
                    broadcastToAll(chatMsg);
                } else {
                    broadcastToRoom(room, chatMsg);
                }
                System.out.println("[MESSAGE] 메시지 전송 완료");
            }
            break;
        }

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

                // 1:1 채팅방 생성 시 당사자들에게만 알림
                if (createdNew) {
                    CreateChatRoomResponse createRes = new CreateChatRoomResponse(directRoom);
                    sendMessage(createRes); // 생성자에게 전송
                    
                    User friendUser = chatService.getUser(chatStartReq.getFriendId());
                    if (friendUser != null) {
                        sendMessageToUser(friendUser, createRes); // 친구에게도 전송
                    }
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

        case FRIEND_CHAT_INVITE_ACCEPT:
            {
                String[] parts = message.split("\\|", 2);
                String acceptRoom = parts.length > 0 ? parts[0] : "";
                String acceptUserId = parts.length > 1 ? parts[1] : "";
                if (!acceptRoom.isEmpty() && !acceptUserId.isEmpty()) {
                    chatService.enterChatRoom(acceptRoom, acceptUserId);
                    List<User> roomUsers = chatService.getChatRoomUsers(acceptRoom);
                    UserListResponse list = new UserListResponse(acceptRoom, roomUsers);
                    broadcastToRoom(acceptRoom, list);
                }
            }
            break;

        case FRIEND_CHAT_INVITE_DECLINE:
            {
                String[] parts = message.split("\\|", 2);
                String declineRoom = parts.length > 0 ? parts[0] : "";
                String declineUserId = parts.length > 1 ? parts[1] : "";
                ChatRoom room = chatService.getChatRoom(declineRoom);
                if (room != null) {
                    String inviterId = room.getCreatorId();
                    if (inviterId != null) {
                        User inviter = chatService.getUser(inviterId);
                        if (inviter != null) {
                            FriendOperationResult result = new FriendOperationResult(false, "상대방이 1:1 채팅 요청을 거부했습니다.");
                            sendMessageToUser(inviter, new FriendOperationResponse(DtoType.FRIEND_CHAT_INVITE_RESULT, result.isSuccess(), result.getMessage()));
                        }
                    }
                    
                    // 거절 시 생성된 빈 방 자동 삭제
                    if (room.getUsers().isEmpty() || 
                        (room.getUsers().size() == 1 && room.getUsers().get(0).getId().equals(inviterId))) {
                        System.out.println("[INVITE_DECLINE] 빈 채팅방 자동 삭제: " + declineRoom);
                        boolean deleted = chatService.deleteChatRoom(declineRoom);
                        if (deleted) {
                            broadcastToAll(new ChatRoomListResponse(chatService.getAllChatRooms()));
                        }
                    }
                }
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
                sendMessage(new AdminActionResultResponse(false, "관리자 권한이 없습니다."));
                break;
            }
            AdminForceLogoutRequest forceLogoutReq = new AdminForceLogoutRequest(message, true);
            boolean forcedLogout = handleForceLogout(forceLogoutReq.getUserId(), "관리자에 의해 로그아웃되었습니다.");
            sendMessage(new AdminActionResultResponse(forcedLogout, forcedLogout ? "강제 로그아웃되었습니다." : "해당 사용자를 찾을 수 없습니다.")); 
            if (forcedLogout) {
                sendAdminSnapshot();
                broadcastToAll(new UserListResponse("Lobby", chatService.getUsers()));
            }
            break;

        case ADMIN_FORCE_EXIT:
            if (!isAdmin()) {
                sendMessage(new AdminActionResultResponse(false, "관리자 권한이 없습니다."));
                break;
            }
            AdminForceExitRequest forceExitReq = new AdminForceExitRequest(message, true);
            boolean forcedExit = handleForceExit(forceExitReq.getUserId(), forceExitReq.getRoomName(), "관리자에 의해 퇴장되었습니다.");
            sendMessage(new AdminActionResultResponse(forcedExit, forcedExit ? "채팅방에서 강제 퇴장시켰습니다." : "사용자/채팅방을 찾을 수 없습니다."));
            if (forcedExit) {
                sendAdminSnapshot();
            }
            break;

        case ADMIN_BAN:
            if (!isAdmin()) {
                sendMessage(new AdminActionResultResponse(false, "관리자 권한이 없습니다."));
                break;
            }
            AdminBanRequest banReq = new AdminBanRequest(message);
            boolean banUpdated = chatService.updateBanStatus(banReq.getUserId(), banReq.isBanned());
            if (banUpdated && banReq.isBanned()) {
                handleForceLogout(banReq.getUserId(), "계정이 차단되어 로그아웃되었습니다.");
            }
            sendMessage(new AdminActionResultResponse(banUpdated, banUpdated ? (banReq.isBanned() ? "사용자가 차단되었습니다." : "차단이 해제되었습니다.") : "차단 상태 변경에 실패했습니다."));
            if (banUpdated) {
                sendAdminSnapshot();
                broadcastToAll(new UserListResponse("Lobby", chatService.getUsers()));
            }
            break;

        case ADMIN_USER_UPDATE:
            if (!isAdmin()) {
                sendMessage(new AdminActionResultResponse(false, "관리자 권한이 없습니다."));
                break;
            }
            AdminUserUpdateRequest updateReq = new AdminUserUpdateRequest(message);
            boolean userUpdated = chatService.updateUserInfo(updateReq.getUserId(), updateReq.getName(), updateReq.getNickname(), 
                                                             updateReq.getEmail(), updateReq.getPhone(),
                                                             updateReq.getAddress(), updateReq.getDetailAddress(),
                                                             updateReq.getPostalCode(), updateReq.getGender(),
                                                             updateReq.getBirthDate());
            sendMessage(new AdminActionResultResponse(userUpdated, userUpdated ? "사용자 정보가 수정되었습니다." : "사용자 정보 수정에 실패했습니다."));
            if (userUpdated) {
                sendAdminSnapshot();
            }
            break;

        case ADMIN_USER_INFO:
            if (!isAdmin()) {
                sendMessage(new AdminActionResultResponse(false, "관리자 권한이 없습니다."));
                break;
            }
            AdminUserInfoRequest infoReq = new AdminUserInfoRequest(message);
            ChatDao.AdminUserDetails details = chatService.getAdminUserDetails(infoReq.getUserId());
            if (details != null) {
                sendMessage(new AdminUserInfoResponse(details.name, infoReq.getUserId(), details.nickname, details.email,
                                                       details.phone, details.address, details.detailAddress, details.postalCode,
                                                       details.gender, details.birthDate));
            } else {
                sendMessage(new AdminActionResultResponse(false, "사용자 정보를 찾을 수 없습니다."));
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

        case ADMIN_ROOM_MEMBERS:
            if (!isAdmin()) {
                sendMessage(new AdminActionResultResponse(false, "관리자 권한이 없습니다."));
                break;
            }
            AdminRoomMembersRequest membersReq = new AdminRoomMembersRequest(message, true);
            List<User> members = chatService.getRoomMembers(membersReq.getRoomName());
            sendMessage(new AdminRoomMembersResponse(membersReq.getRoomName(), members));
            break;

        case ADMIN_MESSAGE_DELETE:
            if (!isAdmin()) {
                sendMessage(new AdminActionResultResponse(false, "관리자 권한이 없습니다."));
                break;
            }
            AdminMessageDeleteRequest deleteReq = new AdminMessageDeleteRequest(message, true);
            
            // 메시지 ID로 채팅방 조회
            String messageRoomName = chatService.getRoomNameByMessageId(deleteReq.getMessageId());
            
            // DB에서 메시지 삭제
            boolean deleted = chatService.deleteMessage(deleteReq.getMessageId());
            
            // 관리자에게 응답
            sendMessage(new AdminActionResultResponse(deleted, deleted ? "메시지가 삭제되었습니다." : "메시지 삭제에 실패했습니다."));
            
            // 채팅방의 모든 사용자에게 채팅 이력 새로고침 브로드캐스트
            if (deleted && messageRoomName != null && !messageRoomName.isEmpty()) {
                // 삭제된 메시지가 속한 채팅방의 모든 메시지 이력 조회
                java.util.List<ChatHistoryResponse.HistoryEntry> historyEntries = chatService.getMessageHistory(messageRoomName);
                
                // 채팅 이력을 CHAT_HISTORY 형식으로 브로드캐스트
                ChatHistoryResponse historyResponse = new ChatHistoryResponse(messageRoomName, historyEntries);
                broadcastToRoom(messageRoomName, historyResponse);
                
                System.out.println("[ADMIN] 메시지 삭제: ID=" + deleteReq.getMessageId() + ", 방=" + messageRoomName + " (채팅 이력 새로고침 브로드캐스트됨)");
            } else {
                System.out.println("[ADMIN] 메시지 삭제 실패: ID=" + deleteReq.getMessageId());
            }
            break;

        case ADMIN_ROOM_DELETE:
            if (!isAdmin()) {
                sendMessage(new AdminActionResultResponse(false, "관리자 권한이 없습니다."));
                break;
            }
            AdminRoomDeleteRequest roomDeleteReq = new AdminRoomDeleteRequest(message, true);
            boolean roomDeleted = handleRoomDeletion(roomDeleteReq.getRoomName());
            if (roomDeleted) {
                sendMessage(new AdminActionResultResponse(true, "채팅방이 삭제되었습니다."));
            } else {
                sendMessage(new AdminActionResultResponse(false, "채팅방 삭제에 실패했습니다."));
            }
            break;

        case FIND_ID:
            FindIdRequest findIdReq = new FindIdRequest(message);
            System.out.println("[FIND_ID] 아이디 찾기 요청 - 이름: " + findIdReq.getName() + ", 이메일: " + findIdReq.getEmail());
            
            String foundUserId = chatService.findUserIdByNameAndEmail(findIdReq.getName(), findIdReq.getEmail());
            if (foundUserId != null) {
                sendMessage(new FindIdResponse(true, foundUserId, "아이디를 찾았습니다."));
                System.out.println("[FIND_ID] 아이디 찾기 성공 - " + foundUserId);
            } else {
                sendMessage(new FindIdResponse(false, null, "일치하는 회원 정보가 없습니다."));
                System.out.println("[FIND_ID] 아이디 찾기 실패 - 일치하는 정보 없음");
            }
            break;

        case FIND_PASSWORD:
            FindPasswordRequest findPwReq = new FindPasswordRequest(message);
            System.out.println("[FIND_PASSWORD] 비밀번호 찾기 요청 - ID: " + findPwReq.getId() + ", 이메일: " + findPwReq.getEmail());
            
            boolean verified = chatService.verifyUserForPasswordReset(findPwReq.getId(), findPwReq.getEmail());
            if (verified) {
                sendMessage(new FindPasswordResponse(true, "본인 확인이 완료되었습니다."));
                System.out.println("[FIND_PASSWORD] 본인 확인 성공");
            } else {
                sendMessage(new FindPasswordResponse(false, "아이디 또는 이메일이 일치하지 않습니다."));
                System.out.println("[FIND_PASSWORD] 본인 확인 실패");
            }
            break;

        case RESET_PASSWORD:
            ResetPasswordRequest resetPwReq = new ResetPasswordRequest(message);
            System.out.println("[RESET_PASSWORD] 비밀번호 재설정 요청 - ID: " + resetPwReq.getId());
            
            boolean updated = chatService.updatePassword(resetPwReq.getId(), resetPwReq.getNewPassword());
            if (updated) {
                sendMessage(new ResetPasswordResponse(true, "비밀번호가 성공적으로 변경되었습니다."));
                System.out.println("[RESET_PASSWORD] 비밀번호 재설정 성공");
            } else {
                sendMessage(new ResetPasswordResponse(false, "비밀번호 변경에 실패했습니다."));
                System.out.println("[RESET_PASSWORD] 비밀번호 재설정 실패");
            }
            break;

        case USER_LIST:
        case CHAT_ROOM_LIST:
            System.out.println("?? ???? ?? ??: " + type);
            break;

        case CHAT_ROOM_INVITE:
            ChatRoomInviteRequest inviteReq = new ChatRoomInviteRequest(message);
            System.out.println("[CHAT_ROOM_INVITE] 채팅방 초대 요청 - 방: " + inviteReq.getRoomName() + ", 대상: " + inviteReq.getTargetNickname() + ", 발신자: " + inviteReq.getSenderUserId());
            
            // 채팅방 존재 확인
            ChatRoom targetRoom = chatService.getChatRoom(inviteReq.getRoomName());
            if (targetRoom == null) {
                sendMessage(new ChatRoomInviteResultResponse("채팅방을 찾을 수 없습니다.", false));
                break;
            }
            
            // 1:1 채팅방은 초대 불가 (DM- 접두사 확인)
            if (targetRoom.getName().startsWith("DM-")) {
                sendMessage(new ChatRoomInviteResultResponse("1:1 채팅방은 초대할 수 없습니다.", false));
                break;
            }
            
            // 대상 사용자 조회
            User targetUser = chatService.getUserByNickname(inviteReq.getTargetNickname());
            if (targetUser == null) {
                sendMessage(new ChatRoomInviteResultResponse("사용자를 찾을 수 없습니다.", false));
                break;
            }
            
            // 이미 방에 입장한 사용자인지 확인
            boolean alreadyInRoom = targetRoom.getUsers().stream()
                    .anyMatch(u -> u.getId().equals(targetUser.getId()));
            if (alreadyInRoom) {
                sendMessage(new ChatRoomInviteResultResponse("이미 채팅방에 참여 중인 사용자입니다.", false));
                break;
            }
            
            // 초대 대상에게 초대 알림 전송
            User senderUser = chatService.getUser(inviteReq.getSenderUserId());
            if (senderUser != null) {
                ChatRoomInviteResponse inviteNotification = new ChatRoomInviteResponse(
                        inviteReq.getRoomName(),
                        inviteReq.getSenderUserId(),
                        senderUser.getNickName()
                );
                sendMessageToUser(targetUser, inviteNotification);
                sendMessage(new ChatRoomInviteResultResponse("초대를 보냈습니다.", true));
                System.out.println("[CHAT_ROOM_INVITE] 초대 완료 - 방: " + inviteReq.getRoomName() + ", 대상: " + inviteReq.getTargetNickname());
            } else {
                sendMessage(new ChatRoomInviteResultResponse("발신자를 찾을 수 없습니다.", false));
            }
            break;

        case CHAT_ROOM_INVITE_ACCEPT:
            ChatRoomInviteAcceptRequest acceptReq = new ChatRoomInviteAcceptRequest(message);
            System.out.println("[CHAT_ROOM_INVITE_ACCEPT] 초대 수락 - 방: " + acceptReq.getRoomName() + ", 사용자: " + acceptReq.getUserId());
            
            // 채팅방 입장 처리
            boolean enterSuccess = chatService.enterChatRoom(acceptReq.getRoomName(), acceptReq.getUserId());
            if (enterSuccess) {
                // 해당 채팅방의 모든 사용자에게 사용자 목록 업데이트
                List<User> updatedUsers = chatService.getChatRoomUsers(acceptReq.getRoomName());
                UserListResponse updateUserList = new UserListResponse(acceptReq.getRoomName(), updatedUsers);
                broadcastToRoom(acceptReq.getRoomName(), updateUserList);
                
                // 입장 메시지 브로드캐스트
                User acceptedUser = chatService.getUser(acceptReq.getUserId());
                if (acceptedUser != null) {
                    MessageResponse enterMsg = new MessageResponse(
                            MessageType.ENTER,
                            acceptReq.getRoomName(),
                            acceptedUser.getNickName(),
                            acceptedUser.getNickName() + "님이 입장하셨습니다."
                    );
                    broadcastToRoom(acceptReq.getRoomName(), enterMsg);
                }
                
                System.out.println("[CHAT_ROOM_INVITE_ACCEPT] 입장 완료 - 방: " + acceptReq.getRoomName() + ", 사용자: " + acceptReq.getUserId());
            } else {
                System.out.println("[CHAT_ROOM_INVITE_ACCEPT] 입장 실패 - 방: " + acceptReq.getRoomName() + ", 사용자: " + acceptReq.getUserId());
            }
            break;

        case FILE_UPLOAD:
            FileUploadRequest fileReq = new FileUploadRequest(message);
            try {
                Path uploadDir = Path.of("uploaded_files");
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }

                String uniqueFileName = UUID.randomUUID() + "_" + fileReq.getFileName();
                Path filePath = uploadDir.resolve(uniqueFileName);
                Files.write(filePath, fileReq.getFileData());

                long messageId = chatService.saveFileMessage(
                    fileReq.getChatRoomName(),
                    fileReq.getSenderId(),
                    fileReq.getFileName(),
                    filePath.toString(),
                    fileReq.getFileSize(),
                    fileReq.getMimeType()
                );
                
                if (messageId > 0) {
                    sendMessage(new FileUploadResponse(true, "파일 업로드 성공", messageId));
                    User sender = chatService.getUser(fileReq.getSenderId());
                    String nickname = sender != null ? sender.getNickName() : fileReq.getSenderId();
                    FileMessageResponse fileMsg = new FileMessageResponse(
                        fileReq.getChatRoomName(),
                        nickname,
                        messageId,
                        fileReq.getFileName(),
                        fileReq.getMimeType(),
                        fileReq.getFileSize(),
                        new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date())
                    );
                    broadcastToRoom(fileReq.getChatRoomName(), fileMsg);
                } else {
                    sendMessage(new FileUploadResponse(false, "파일 저장 실패", -1));
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendMessage(new FileUploadResponse(false, "파일 업로드 오류: " + e.getMessage(), -1));
            }
            break;

        case FILE_DOWNLOAD:
            FileDownloadRequest downloadReq = new FileDownloadRequest(message);
            try {
                ChatDao.FileInfo fileInfo = chatService.getFileInfo(downloadReq.getMessageId());
                if (fileInfo != null) {
                    byte[] fileData = Files.readAllBytes(Path.of(fileInfo.filePath));
                    FileDownloadResponse downloadResp = new FileDownloadResponse(
                        downloadReq.getChatRoomName(),
                        downloadReq.getMessageId(),
                        fileInfo.fileName,
                        fileInfo.mimeType,
                        fileInfo.fileSize,
                        fileData
                    );
                    sendMessage(downloadResp);
                } else {
                    sendMessage(new FileUploadResponse(false, "파일 정보를 찾을 수 없습니다.", -1));
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendMessage(new FileUploadResponse(false, "파일 다운로드 오류: " + e.getMessage(), -1));
            }
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
