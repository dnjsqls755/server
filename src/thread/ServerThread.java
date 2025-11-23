package thread;

import app.ServerApplication;
import dao.ChatDao;
import domain.ChatRoom;
import domain.User;
import dto.request.*;
import dto.response.*;
import dto.type.DtoType;
import dto.type.MessageType;
import exception.ChatRoomExistException;
import exception.ChatRoomNotFoundException;
import exception.UserNotFoundException;
import service.ChatService;
import java.util.UUID;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

import java.io.DataInputStream;
import java.io.File;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class ServerThread extends Thread {

    Socket socket; // 담당자 (not 문지기)

    ChatService chatService;

    public ServerThread(Socket socket, ChatService chatService) {
        this.socket = socket;
        this.chatService = chatService;
    }

    @Override
    public void run() {
        super.run();

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            while (true) {
                String str = reader.readLine();
                if (str == null) {
                    System.out.println("socket error (can't get socket input stream) - client socket closed");
                    try {

                        socket.close();
                        System.out.println("socket closed.");

                        ServerApplication.sockets.remove(socket);
                        return;

                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }

                // HTTP 요청 처리
                if (str.startsWith("POST /address")) {
                    handleHttpAddressRequest(reader);
                    continue;
                }

                String[] token = str.split(":");
                DtoType type = DtoType.valueOf(token[0]);
                String message = token[1];

                processReceiveMessage(type, message);

                Thread.sleep(300);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    private void processReceiveMessage(DtoType type, String message)
            throws UserNotFoundException, ChatRoomNotFoundException, ChatRoomExistException, IOException {
        switch (type) {

        case LOGIN:
            LoginRequest loginReq = new LoginRequest(message);

            // 로그인 검증
            boolean isValid = chatService.isValidLogin(loginReq.getId(), loginReq.getPw());
            if (!isValid) {
                sendResponse("LOGIN_FAIL");
                return;
            }

            // 닉네임 포함 User 객체 가져오기
            User user = chatService.getUserByLogin(loginReq.getId(), loginReq.getPw());
            if (user == null) {
                sendResponse("LOGIN_FAIL");
                return;
            }

            // 소켓 연결 정보 설정
            user.setSocket(socket);

            // 서버 메모리에 이미 있는 사용자라면 제거 (중복 방지)
            chatService.removeUser(user.getId());
            
            // 서버 메모리에 사용자 추가
            chatService.addUser(user);

            // DB에서 채팅방 목록 불러오기
            List<ChatRoom> dbChatRooms = chatService.getAllChatRooms();
            
            // 클라이언트에게 로그인 성공 응답 전송 (채팅방 목록 포함)
            sendMessage(new InitDataResponse(dbChatRooms, chatService.getUsers()));
            
            // 모든 클라이언트에게 업데이트된 사용자 목록 전송
            UserListResponse lobbyUserList = new UserListResponse("Lobby", chatService.getUsers());
            broadcastToAll(lobbyUserList);
            
            System.out.println("[LOGIN] 사용자 로그인 완료: " + user.getId() + " (" + user.getNickName() + ")");
            break;
            
        case LOGOUT:
            LogoutRequest logoutReq = new LogoutRequest(message);
            System.out.println("[LOGOUT] 로그아웃 요청 - 사용자: " + logoutReq.getUserId());
            
            // 사용자 제거
            chatService.removeUser(logoutReq.getUserId());
            
            // 모든 클라이언트에게 업데이트된 사용자 목록 전송
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

                    // 이미지 리사이징 (200x200)
                    File dir = new File("profile_images");
                    if (!dir.exists()) dir.mkdirs();

                    String fileName = UUID.randomUUID() + ".jpg";
                    File file = new File(dir, fileName);
                    
                    // 원본 이미지를 BufferedImage로 변환
                    BufferedImage originalImage = ImageIO.read(new java.io.ByteArrayInputStream(imageBytes));
                    
                    // 200x200으로 리사이징
                    Image scaledImage = originalImage.getScaledInstance(200, 200, Image.SCALE_SMOOTH);
                    BufferedImage resizedImage = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
                    resizedImage.getGraphics().drawImage(scaledImage, 0, 0, null);
                    
                    // 리사이징된 이미지 저장
                    ImageIO.write(resizedImage, "jpg", file);

                    String imagePath = "profile_images/" + fileName;
                    chatService.updateUserProfileImage(joinReq.getUserId(), imagePath);
                    
                    System.out.println("[SIGNUP] 프로필 이미지 리사이징 완료: " + imagePath);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    System.out.println("이미지 저장 실패: " + ex.getMessage());
                }
                sendDtoResponse(DtoType.SIGNUP_SUCCESS);
            } else {
                sendDtoResponse(DtoType.SIGNUP_FAIL);
            }
            break;

        case CREATE_CHAT:
            // 채팅방 생성
            CreateChatRoomRequest createReq = new CreateChatRoomRequest(message);
            System.out.println("[CREATE_CHAT] 채팅방 생성 요청 - 이름: " + createReq.getName() + ", 생성자: " + createReq.getUserId());
            
            ChatRoom newRoom = chatService.createChatRoom(createReq.getName(), createReq.getUserId());
            if (newRoom != null) {
                // 생성자를 채팅방에 입장시킴
                chatService.enterChatRoom(newRoom.getName(), createReq.getUserId());
                
                // 모든 클라이언트에게 새 채팅방 알림
                CreateChatRoomResponse createRes = new CreateChatRoomResponse(newRoom);
                broadcastToAll(createRes);
                
                // 생성자에게 사용자 목록 전송
                List<User> roomUsers = chatService.getChatRoomUsers(newRoom.getName());
                UserListResponse userListRes = new UserListResponse(newRoom.getName(), roomUsers);
                sendMessage(userListRes);
                
                System.out.println("[CREATE_CHAT] 채팅방 생성 완료: " + newRoom.getName());
            }
            break;

        case ENTER_CHAT:
            // 채팅방 입장
            EnterChatRequest enterReq = new EnterChatRequest(message);
            System.out.println("[ENTER_CHAT] 입장 요청 - 사용자: " + enterReq.getUserId() + ", 방: " + enterReq.getChatRoomName());
            
            // 사용자 검증
            User requestUser = chatService.getUser(enterReq.getUserId());
            if (requestUser == null) {
                System.out.println("[ENTER_CHAT] 로그인하지 않은 사용자: " + enterReq.getUserId());
                sendResponse("ENTER_FAIL:로그인이 필요합니다.");
                break;
            }
            
            // 신규 입장인지 확인 (DB에 새로 등록되었는지)
            boolean isNewEntry = chatService.enterChatRoom(enterReq.getChatRoomName(), enterReq.getUserId());
            
            // 이전 대화 내역은 로드하지 않음 (입장 시점 이후 메시지만 표시)
            // 채팅방 입장 시점부터의 대화만 볼 수 있도록 변경
            
            // 신규 입장인 경우에만 입장 메시지 전송
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
            
            // 채팅방 사용자 목록 전송
            List<User> users = chatService.getChatRoomUsers(enterReq.getChatRoomName());
            UserListResponse enterUserList = new UserListResponse(enterReq.getChatRoomName(), users);
            broadcastToRoom(enterReq.getChatRoomName(), enterUserList);
            
            System.out.println("[ENTER_CHAT] 입장 완료 - 현재 인원: " + users.size() + " (신규: " + isNewEntry + ")");
            break;

        case EXIT_CHAT:
            // 채팅방 퇴장
            ExitChatRequest exitReq = new ExitChatRequest(message);
            System.out.println("[EXIT_CHAT] 퇴장 요청 - 사용자: " + exitReq.getUserId() + ", 방: " + exitReq.getChatRoomName());
            
            User exitedUser = chatService.exitChatRoom(exitReq.getChatRoomName(), exitReq.getUserId());
            
            // 퇴장 메시지 전송
            MessageResponse exitMsg = new MessageResponse(
                MessageType.EXIT,
                exitReq.getChatRoomName(),
                exitedUser.getNickName(),
                exitedUser.getNickName() + "님이 퇴장하셨습니다."
            );
            broadcastToRoom(exitReq.getChatRoomName(), exitMsg);
            
            // 남은 사용자들에게 업데이트된 사용자 목록 전송
            ChatRoom exitRoom = chatService.getChatRoom(exitReq.getChatRoomName());
            if (exitRoom != null && exitRoom.ieExistUser()) {
                List<User> remainingUsers = chatService.getChatRoomUsers(exitReq.getChatRoomName());
                UserListResponse exitUserList = new UserListResponse(exitReq.getChatRoomName(), remainingUsers);
                broadcastToRoom(exitReq.getChatRoomName(), exitUserList);
            }
            
            System.out.println("[EXIT_CHAT] 퇴장 완료");
            break;

        case MESSAGE:
            // 채팅 메시지
            MessageResponse chatMsg = new MessageResponse(message);
            System.out.println("[MESSAGE] 메시지 수신 - 방: " + chatMsg.getChatRoomName() + ", 발신자: " + chatMsg.getUserName() + ", 내용: " + chatMsg.getMessage());
            
            // DB에 메시지 저장 (로비가 아닌 채팅방의 일반 채팅만)
            if (chatMsg.getMessageType() == MessageType.CHAT && !"Lobby".equals(chatMsg.getChatRoomName())) {
                chatService.saveChatMessage(chatMsg.getChatRoomName(), chatMsg.getUserName(), chatMsg.getMessage());
            }
            
            // 로비 메시지는 모든 접속자에게, 채팅방 메시지는 해당 채팅방 사용자에게만 전송
            if ("Lobby".equals(chatMsg.getChatRoomName())) {
                broadcastToAll(chatMsg);
            } else {
                broadcastToRoom(chatMsg.getChatRoomName(), chatMsg);
            }
            
            System.out.println("[MESSAGE] 메시지 전송 완료");
            break;

        case USER_LIST:
        case CHAT_ROOM_LIST:
            System.out.println("아직 구현되지 않은 기능: " + type);
            break;

        default:
            System.out.println("알 수 없는 메시지 타입: " + type);
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
    
    // 특정 채팅방의 모든 사용자에게 메시지 전송
    private void broadcastToRoom(String roomName, DTO dto) {
        try {
            ChatRoom room = chatService.getChatRoom(roomName);
            if (room == null) {
                System.out.println("[ERROR] 채팅방을 찾을 수 없음: " + roomName);
                return;
            }
            
            List<User> roomUsers = room.getUsers();
            for (User user : roomUsers) {
                Socket userSocket = user.getSocket();
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
    
    // 모든 접속 중인 사용자에게 메시지 전송
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
    //비밀번호 유효성 규칙
    private boolean isValidPassword(String password) {
        if (password.length() < 8) return false;
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");
        return hasLetter && hasDigit && hasSpecial;
    }
    
    // HTTP POST 요청 처리 (우편번호 검색)
    private void handleHttpAddressRequest(BufferedReader reader) {
        try {
            StringBuilder body = new StringBuilder();
            String line;
            int contentLength = 0;
            
            // HTTP 헤더 읽기
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("Content-Length:")) {
                    contentLength = Integer.parseInt(line.substring(16).trim());
                }
            }
            
            // HTTP 바디 읽기 (JSON)
            if (contentLength > 0) {
                char[] buffer = new char[contentLength];
                reader.read(buffer, 0, contentLength);
                body.append(buffer);
            }
            
            String jsonBody = body.toString();
            System.out.println("[HTTP] Address request: " + jsonBody);
            
            // JSON 파싱 (간단한 문자열 처리)
            String postal = extractJsonValue(jsonBody, "postal");
            String address = extractJsonValue(jsonBody, "address");
            
            if (postal != null && address != null) {
                // ADDRESS_RESULT 메시지로 클라이언트에 전송
                String result = postal + "|" + address;
                sendDtoMessage("ADDRESS_RESULT:" + result);
                System.out.println("[HTTP] Address sent to client: " + result);
            }
            
            // HTTP 응답
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
    
    // 간단한 JSON 값 추출
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
    
    // DtoType 메시지 전송
    private void sendDtoMessage(String message) throws IOException {
        PrintWriter writer = new PrintWriter(socket.getOutputStream());
        writer.println(message);
        writer.flush();
    }
}
