package org.example.junglebook.enums

enum class DebateTopicStatus {
    PREPARING,  // 준비중 (관리자만 접근 가능)
    DEBATING,   // 토론중 (모든 유저 참여 가능)
    CLOSED,     // 토론 종료 (읽기만 가능)
    VOTING      // 투표중
}

enum class DebateTopicCategory {
    POLITICS,    // 정치
    ECONOMY,     // 경제
    SOCIETY,     // 사회
    CULTURE,     // 문화
    IT_SCIENCE,  // IT/과학
    FOREIGN_AFFAIRS // 외교/국제
}

enum class ArgumentStance {
    PRO,      // 찬성
    CON,      // 반대
    NEUTRAL   // 중립
}

enum class VoteType {
    UPVOTE,
    DOWNVOTE
}