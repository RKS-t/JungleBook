#!/bin/bash

# 간단한 권한 테스트 스크립트
BASE_URL="http://localhost:8080"

echo "=========================================="
echo "API 권한 예외 처리 테스트"
echo "=========================================="
echo ""
echo "이 스크립트는 이미 생성된 데이터를 사용합니다."
echo "먼저 다음을 확인하세요:"
echo "1. 서버가 실행 중인지 (http://localhost:8080)"
echo "2. 데이터베이스에 테스트 데이터가 있는지"
echo ""
echo "Swagger UI에서 직접 테스트하는 것을 권장합니다:"
echo "http://localhost:8080/swagger-ui/index.html"
echo ""
echo "또는 아래 명령어로 직접 테스트할 수 있습니다:"
echo ""
echo "# 예시: 토픽 수정 권한 테스트"
echo "curl -X PUT \"$BASE_URL/api/debate/topics/{topicId}\" \\"
echo "  -H \"Authorization: Bearer {OTHER_USER_TOKEN}\" \\"
echo "  -H \"Content-Type: application/json\" \\"
echo "  -d '{\"title\":\"수정된 제목\"}'"
echo ""
echo "예상 응답: HTTP 403 Forbidden 또는 400 Bad Request"
echo "응답 본문에 에러 메시지가 포함되어야 합니다."
echo ""

