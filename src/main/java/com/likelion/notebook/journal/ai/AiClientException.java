package com.likelion.notebook.journal.ai;

/** AI 호출 실패 (네트워크, 키 오류, 응답 형식 오류 등). 서비스에서 500 INTERNAL_ERROR로 바꿔서 응답 */
public class AiClientException extends RuntimeException {

    public AiClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
