package org.example.junglebook.enums

enum class DebateTopicStatus(val value: Int) {
    PREPARING(0),  // 준비중
    DEBATING(1),   // 토론중
    CLOSED(2),     // 토론 종료
    VOTING(3)      // 투표중
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

enum class DebateReferenceType {
    ARGUMENT, REPLY
}