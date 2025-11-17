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
import java.io.FileOutputStream;
import java.io.File;
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
            while (true) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
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

            // 1.로그인 검증 
            boolean isValid = chatService.isValidLogin(loginReq.getId(), loginReq.getPw());
            if (!isValid) {
                sendResponse("LOGIN_FAIL");
                return;
            }

            // 2. 닉네임 포함 User 객체 가져오기
            User user = chatService.getUserByLogin(loginReq.getId(), loginReq.getPw());
            if (user == null) {
                sendResponse("LOGIN_FAIL");
                return;
            }

            // 3. 소켓 연결 정보 설정
            user.setSocket(socket);

            // 4. 서버 메모리에 추가 및 로비 입장
            chatService.addUser(user);
            chatService.enterLobby(user);

            // 5. 클라이언트에게 초기 데이터 전송
            sendMessage(new MessageResponse(MessageType.ENTER, ChatDao.LOBBY_CHAT_NAME, user.getNickName(), user.getEnterString()));
            sendMessage(new InitDataResponse(chatService.getChatRooms(), chatService.getUsers()));
            sendMessage(new UserListResponse(ChatDao.LOBBY_CHAT_NAME, chatService.getUsers()));
            break;

        case ID_CHECK:
            boolean isDuplicate = chatService.isUserIdDuplicate(message.trim());
            sendResponse(isDuplicate ? "ID_DUPLICATE" : "ID_OK");
            break;

        case NICKNAME_CHECK:
            boolean isDuplicateNickname = chatService.isNicknameDuplicate(message.trim());
            sendResponse(isDuplicateNickname ? "NICKNAME_DUPLICATE" : "NICKNAME_OK");
            break;

        case SIGNUP:
            JoinRequest joinReq = new JoinRequest(message);
            if (!isValidPassword(joinReq.getPassword())) {
                sendResponse("SIGNUP_INVALID_PASSWORD");
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
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        fos.write(imageBytes);
                    }

                    String imagePath = "profile_images/" + fileName;
                    chatService.updateUserProfileImage(joinReq.getUserId(), imagePath);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    System.out.println("이미지 저장 실패: " + ex.getMessage());
                }
                sendResponse("SIGNUP_SUCCESS");
            } else {
                sendResponse("SIGNUP_FAIL");
            }
            break;


        case CREATE_CHAT:
            CreateChatRoomRequest createChatRoomReq = new CreateChatRoomRequest(message);
            ChatRoom chatRoom = chatService.createChatRoom(
                createChatRoomReq.getName(),
                createChatRoomReq.getUserId()
            );
            chatService.enterChatRoom(chatRoom.getName(), createChatRoomReq.getUserId());

            // 응답 DTO 생성
            CreateChatRoomResponse createChatRoomRes = new CreateChatRoomResponse(chatRoom);
            sendMessage(createChatRoomRes);

            // 채팅방 사용자 리스트 전송
            UserListResponse chatRoomUserListRes = new UserListResponse(
                chatRoom.getName(),
                chatService.getChatRoomUsers(chatRoom.getName())
            );
            sendMessage(chatRoomUserListRes);
            break;

            case ENTER_CHAT:
                // 서버에 채팅방에 들어온 사용자 설정
                EnterChatRequest enterChatReq = new EnterChatRequest(message);
                String enterChatRoomName = enterChatReq.getChatRoomName();
                String userId = enterChatReq.getUserId();
                chatService.enterChatRoom(enterChatRoomName, userId);

                // [to 채팅방에 있는 다른 사용자] 입장 메시지 전송
                User enterUser = chatService.getUser(userId);
                MessageResponse enterChatRoomEnterMessageRes = new MessageResponse(MessageType.ENTER, enterChatRoomName, enterUser.getNickName(), enterUser.getEnterString());
                sendMessage(enterChatRoomEnterMessageRes);

                // [to 채팅방에 있는 모든 사용자 (나 자신 포함)] 사용자 리스트 전송
                UserListResponse enterChatRoomUserListRes = new UserListResponse(enterChatRoomName, chatService.getChatRoomUsers(enterChatRoomName));
                sendMessage(enterChatRoomUserListRes);

                break;
            case EXIT_CHAT:
                ExitChatRequest exitChatReq = new ExitChatRequest(message);
                ChatRoom exitChatRoom = chatService.getChatRoom(exitChatReq.getChatRoomName());
                String exitChatRoomName = exitChatRoom.getName();
                User exitUser = chatService.exitChatRoom(exitChatReq.getChatRoomName(), exitChatReq.getUserId());

                if (exitChatRoom.ieExistUser()) {
                    // [to 채팅방에 있는 다른 사용자] 퇴장 메시지 전송
                    MessageResponse chatRoomExitMessageRes = new MessageResponse(MessageType.EXIT, exitChatReq.getChatRoomName(), exitUser.getNickName(), exitUser.getExitString());
                    sendMessage(chatRoomExitMessageRes);

                    // [to 채팅방에 있는 모든 사용자 (나 자신 포함)] 사용자 리스트 전송
                    UserListResponse exitChatRoomUserListRes = new UserListResponse(exitChatRoomName, chatService.getChatRoomUsers(exitChatRoomName));
                    sendMessage(exitChatRoomUserListRes);
                }

                // 채팅방에 더 이상 사용자가 없는 경우
                // 채팅방 목록 갱신
                else {
                    ChatRoomListResponse chatRoomListRes = new ChatRoomListResponse(chatService.getChatRooms());
                    sendMessage(chatRoomListRes);
                }

                break;
        }
    }

private void sendResponse(String message) throws IOException {
    PrintWriter writer = new PrintWriter(socket.getOutputStream());
    writer.println(message);
    writer.flush();
}

    private void sendMessage(DTO dto) {
        DtoType type = dto.getType();

        try {
            PrintWriter sender = null;
            switch (type) {
                case LOGIN:
                    InitDataResponse initDataResponse = (InitDataResponse) dto;

                    // 로그인 한 자신에게만 전송
                    sender = new PrintWriter(socket.getOutputStream());
                    sender.println(initDataResponse);
                    sender.flush();
                    break;

                    // 채팅방에 표시되는 메시지 전송 (입장 메시지, 대화 메시지)
                case MESSAGE:
                    MessageResponse messageReq = (MessageResponse) dto;

                    ChatRoom chatRoom = chatService.getChatRoom(messageReq.getChatRoomName());
                    List<User> chatUsers = chatRoom.getUsers();

                    // 나를 제외한 사용자에게 모두 출력
                    for (User user : chatUsers) {
                        Socket s = user.getSocket();
                        if (s != socket) {
                            sender = new PrintWriter(s.getOutputStream());
                            sender.println(messageReq);
                            sender.flush();
                        }
                    }
                    break;

                case USER_LIST:
                    // 사용자 리스트 추가
                    // dto 에 담긴 사용자 리스트에 따라 추가
                    UserListResponse userListRes = (UserListResponse) dto;

                    for (User user : userListRes.getUsers()) {
                        Socket s = user.getSocket();
                        sender = new PrintWriter(s.getOutputStream());
                        sender.println(userListRes);
                        sender.flush();
                    }
                    break;

                case CREATE_CHAT:
                    CreateChatRoomResponse createChatRoomResponse = (CreateChatRoomResponse) dto;

                    for (Socket s : ServerApplication.sockets) {
                        sender = new PrintWriter(s.getOutputStream());
                        sender.println(createChatRoomResponse);
                        sender.flush();
                    }
                    break;

                case CHAT_ROOM_LIST:
                    ChatRoomListResponse chatRoomListResponse = (ChatRoomListResponse) dto;

                    for (Socket s : ServerApplication.sockets) {
                        sender = new PrintWriter(s.getOutputStream());
                        sender.println(chatRoomListResponse);
                        sender.flush();
                    }
                    break;
            }
        }
        catch (IOException e) {
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
}
