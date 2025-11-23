package dto.type;

public enum DtoType {
    LOGIN,
    LOGOUT,//로그아웃
    ID_CHECK,//아이디 중복확인 요청
    ID_OK,//아이디 사용가능
    ID_DUPLICATE,//아이디 중복
    NICKNAME_CHECK,//닉네임 중복확인 요청
    NICKNAME_OK,//닉네임 사용가능
    NICKNAME_DUPLICATE,//닉네임 중복
    CREATE_CHAT,
    ENTER_CHAT, EXIT_CHAT,
    MESSAGE,
    SIGNUP,
    SIGNUP_SUCCESS,//회원가입 성공
    SIGNUP_FAIL,//회원가입 실패
    SIGNUP_INVALID_PASSWORD,//비밀번호 형식 오류
    ADDRESS_RESULT,//우편번호 검색 결과
    USER_LIST, CHAT_ROOM_LIST,
    CHAT_HISTORY,//채팅방 이전 대화 목록
}
