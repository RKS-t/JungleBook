#!/bin/bash

# 모든 서비스 및 컨트롤러 예외 처리 테스트 스크립트
BASE_URL="http://localhost:8080"

echo "=========================================="
echo "전체 서비스 및 컨트롤러 예외 처리 테스트"
echo "=========================================="
echo ""

# 색상 코드
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

FAILED=0
PASSED=0

test_exception() {
  local method=$1
  local url=$2
  local token=$3
  local data=$4
  local test_name=$5
  local expected_status=$6
  
  printf "%-50s " "$test_name"
  
  if [ -z "$data" ]; then
    RESPONSE=$(curl -s -w "\n%{http_code}" -X "$method" "$url" \
      ${token:+-H "Authorization: Bearer $token"} \
      -H "Content-Type: application/json" 2>/dev/null)
  else
    RESPONSE=$(curl -s -w "\n%{http_code}" -X "$method" "$url" \
      ${token:+-H "Authorization: Bearer $token"} \
      -H "Content-Type: application/json" \
      -d "$data" 2>/dev/null)
  fi
  
  HTTP_CODE=$(echo "$RESPONSE" | tail -1)
  BODY=$(echo "$RESPONSE" | sed '$d')
  
  if [ "$HTTP_CODE" = "$expected_status" ]; then
    echo -e "${GREEN}✓ 통과 (HTTP $HTTP_CODE)${NC}"
    PASSED=$((PASSED+1))
    return 0
  else
    echo -e "${RED}✗ 실패 (HTTP $HTTP_CODE, 예상: $expected_status)${NC}"
    echo "  └─ Response: $BODY"
    FAILED=$((FAILED+1))
    return 1
  fi
}

# 사용자 로그인
echo "테스트 사용자 로그인 중..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/login" \
  -H "Content-Type: application/json" \
  -d '{
    "loginId": "auth_test_owner",
    "password": "Test1234!"
  }')

TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo -e "${RED}로그인 실패${NC}"
  exit 1
fi

echo -e "${GREEN}로그인 성공${NC}"
echo ""

# 1. Debate 관련 테스트
echo "1. Debate 서비스 예외 처리 테스트"
echo "=========================================="

# 존재하지 않는 토픽 조회
test_exception "GET" "$BASE_URL/api/debate/topics/99999" "" "" "존재하지 않는 토픽 조회" "404" || true

# 존재하지 않는 논증 조회
test_exception "GET" "$BASE_URL/api/debate/topics/1/arguments/99999" "" "" "존재하지 않는 논증 조회" "404" || true

# 존재하지 않는 댓글 조회
test_exception "GET" "$BASE_URL/api/debate/arguments/1/replies/99999" "" "" "존재하지 않는 댓글 조회" "404" || true

echo ""
echo "2. Post 서비스 예외 처리 테스트"
echo "=========================================="

# 존재하지 않는 게시글 조회
test_exception "GET" "$BASE_URL/api/posts/99999" "" "" "존재하지 않는 게시글 조회" "404" || true

# 존재하지 않는 게시글 댓글 조회
test_exception "GET" "$BASE_URL/api/posts/1/replies/99999" "" "" "존재하지 않는 게시글 댓글 조회" "404" || true

echo ""
echo "3. Member 서비스 예외 처리 테스트"
echo "=========================================="

# 이미 테스트 완료된 항목들
echo "Member 서비스는 이미 테스트 완료되었습니다."

echo ""
echo "=========================================="
echo "테스트 결과: 통과 $PASSED, 실패 $FAILED"
echo "=========================================="

if [ $FAILED -eq 0 ]; then
  echo -e "${GREEN}모든 테스트 통과! ✓${NC}"
  exit 0
else
  echo -e "${RED}$FAILED 개 테스트 실패${NC}"
  exit 1
fi

