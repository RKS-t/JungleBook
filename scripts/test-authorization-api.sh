#!/bin/bash

# API 권한 테스트 스크립트 (실제 API 호출)
BASE_URL="http://localhost:8080"

echo "=========================================="
echo "API 권한 예외 처리 테스트"
echo "=========================================="
echo ""
echo "이 스크립트는 Swagger UI를 사용하는 것을 권장합니다:"
echo "👉 http://localhost:8080/swagger-ui/index.html"
echo ""
echo "또는 아래 방법으로 직접 테스트할 수 있습니다:"
echo ""

# 실제 테스트를 위한 가이드 출력
cat << 'EOF'

## 테스트 방법

### 1. Swagger UI 사용 (권장)
1. 브라우저에서 http://localhost:8080/swagger-ui/index.html 접속
2. /api/login 엔드포인트로 로그인하여 토큰 획득
3. Owner 사용자로 토픽/논증/댓글/게시글 생성
4. Other 사용자로 로그인하여 토큰 획득
5. Other 토큰으로 수정/삭제 시도 → 403 Forbidden 확인

### 2. curl 명령어 사용

# 예시: 토픽 수정 권한 테스트
# (실제 topicId와 토큰을 사용해야 합니다)

# Owner 토큰으로 토픽 생성
OWNER_TOKEN="your_owner_token"
TOPIC_ID=$(curl -s -X POST "$BASE_URL/api/debate/topics" \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "테스트 토픽",
    "description": "테스트 설명",
    "category": "POLITICS",
    "startDate": "2025-01-01",
    "endDate": "2025-12-31"
  }' | jq -r '.id')

# Other 토큰으로 수정 시도 (403 예상)
OTHER_TOKEN="your_other_token"
curl -v -X PUT "$BASE_URL/api/debate/topics/$TOPIC_ID" \
  -H "Authorization: Bearer $OTHER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"수정된 제목"}'

# 예상 응답:
# HTTP/1.1 403 Forbidden
# {
#   "error": "토픽 수정 권한 없음",
#   "message": "토픽을 수정할 권한이 없습니다.",
#   "code": "403",
#   "path": "/api/debate/topics/{topicId}"
# }

EOF

echo ""
echo "=========================================="
echo "테스트할 API 엔드포인트"
echo "=========================================="
echo ""
echo "1. 토픽 수정: PUT /api/debate/topics/{topicId}"
echo "2. 토픽 삭제: DELETE /api/debate/topics/{topicId}"
echo "3. 논증 삭제: DELETE /api/debate/topics/{topicId}/arguments/{argumentId}"
echo "4. 댓글 삭제: DELETE /api/debate/arguments/{argumentId}/replies/{replyId}"
echo "5. 게시글 수정: PUT /api/posts/{postId}"
echo "6. 게시글 삭제: DELETE /api/posts/{postId}"
echo "7. 게시글 댓글 수정: PUT /api/posts/{postId}/replies/{replyId}"
echo "8. 게시글 댓글 삭제: DELETE /api/posts/{postId}/replies/{replyId}"
echo "9. 의의제기: POST /api/debate/arguments/{argumentId}/fallacy/appeal"
echo ""
echo "=========================================="
echo "예상 결과"
echo "=========================================="
echo ""
echo "✅ 성공 케이스:"
echo "   - 작성자가 수정/삭제: HTTP 200 OK 또는 204 No Content"
echo ""
echo "❌ 실패 케이스 (권한 없음):"
echo "   - HTTP 403 Forbidden 또는 400 Bad Request"
echo "   - 응답 본문에 에러 메시지 포함:"
echo "     * '권한'"
echo "     * '작성자'"
echo "     * '접근'"
echo "     * '토픽', '논증', '댓글' 등 관련 키워드"
echo ""

