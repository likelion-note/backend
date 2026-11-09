package com.likelion.notebook.journal.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 성장일지 관련 enum 모음. 한글 라벨은 프론트 표시용 */
public final class JournalEnums {

    private JournalEnums() {
    }

    /** 상태: 임시 저장 → 검토 대기 → 공개 / 돌려받음 → (고쳐서) 검토 대기 */
    public enum Status {
        DRAFT, PENDING, REJECTED, APPROVED
    }

    /** 공개 범위. 작성할 때 필수 선택 */
    public enum Scope {
        NOW,  // 지금 같은 기수에게 (같은 팀원 제외)
        NEXT  // 다음 기수부터
    }

    @Getter
    @RequiredArgsConstructor
    public enum Activity {
        SESSION("정기 세션"), IDEATHON("아이디어톤"), HACKATHON("해커톤"),
        MIDTERM("중간발표"), DEMODAY("데모데이"), ETC("기타");
        private final String label;
    }

    @Getter
    @RequiredArgsConstructor
    public enum Timing {
        FAR("2주 이상 전"), MID("1~2주 전"), WEEK("행사 주간"), AFTER("행사 후");
        private final String label;
    }

    /** AI가 정하는 분류. 어느 기능에서 노트를 쓰는지 */
    public enum Category {
        MEETING,    // 회의 도우미 팁에 연결
        ROLE_GUIDE, // 역할별 가이드
        COLLAB      // 기획↔개발 소통
    }

    /** 상황 태그 12개 고정. AI 출력도 이 목록 안에서만 */
    @Getter
    @RequiredArgsConstructor
    public enum Tag {
        ROLE_SPLIT("역할 분담"), MEETING_RUN("회의 진행"), CONFLICT("의견 충돌"),
        TOPIC_PICK("주제 선정"), TOPIC_CHANGE("주제 변경"), DELAY("일정 지연"),
        PLAN_DOC("기획서 작성"), PLAN_DEV("기획↔개발 전달"), DESIGN_COLLAB("디자인 협업"),
        STACK("기술 스택 선택"), RELEASE("발표·배포 준비"), PARTICIPATION("팀원 참여도");
        private final String label;
    }
}
