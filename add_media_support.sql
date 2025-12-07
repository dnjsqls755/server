-- Messages 테이블에 미디어 지원 컬럼 추가
ALTER TABLE Messages ADD (
    message_type VARCHAR2(30) DEFAULT 'TEXT' NOT NULL,
    file_name    VARCHAR2(255),
    file_path    VARCHAR2(500),
    file_size    NUMBER,
    mime_type    VARCHAR2(100)
);

-- message_type: 텍스트/이미지/파일/파일메시지/시스템
ALTER TABLE Messages ADD CONSTRAINT chk_message_type
    CHECK (message_type IN ('TEXT', 'IMAGE', 'FILE', 'FILE_MESSAGE', 'SYSTEM'));

COMMIT;
