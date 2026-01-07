#!/bin/bash

# API 테스트 자동화 스크립트
# 사용법: ./scripts/api-test.sh [base_url]

BASE_URL="${1:-http://localhost:8080}"
TEST_USER_LOGIN_ID="qa_user_$(date +%Y%m%d%H%M%S)"
TEST_USER_PASSWORD="Passw0rd!"
TEST_USER_EMAIL="${TEST_USER_LOGIN_ID}@example.com"

# 색상 출력
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 결과 카운터
PASSED=0
FAILED=0

# 헬퍼 함수
print_test() {
    echo -e "${YELLOW}[TEST]${NC} $1"
}

print_pass() {
    echo -e "${GREEN}[PASS]${NC} $1"
    ((PASSED++))
}

print_fail() {
    echo -e "${RED}[FAIL]${NC} $1"
    ((FAILED++))
}

# 1. 헬스 체크
print_test "Health Check"
HEALTH_RESPONSE=$(curl -s -w "\n%{http_code}" "${BASE_URL}/actuator/health")
HTTP_CODE=$(echo "$HEALTH_RESPONSE" | tail -n1)
BODY=$(echo "$HEALTH_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "200" ] && echo "$BODY" | grep -q "UP"; then
    print_pass "Health check"
else
    print_fail "Health check (HTTP: $HTTP_CODE)"
fi

# 2. 회원가입 및 로그인
print_test "회원가입 및 자동 로그인"
SIGNUP_JSON=$(cat <<EOF
{
  "loginId": "${TEST_USER_LOGIN_ID}",
  "password": "${TEST_USER_PASSWORD}",
  "name": "QA테스터",
  "phoneNumber": "01012345678",
  "email": "${TEST_USER_EMAIL}",
  "birth": "1990-01-01",
  "nickname": "QA테스터$(date +%H%M%S)",
  "sex": "M",
  "ideology": "M",
  "profile": "https://example.com/profile.png"
}
EOF
)

SIGNUP_RESPONSE=$(curl -s -w "\n%{http_code}" -H "Content-Type: application/json" -d "$SIGNUP_JSON" "${BASE_URL}/api/signup-and-login")
SIGNUP_HTTP_CODE=$(echo "$SIGNUP_RESPONSE" | tail -n1)
SIGNUP_BODY=$(echo "$SIGNUP_RESPONSE" | sed '$d')

if [ "$SIGNUP_HTTP_CODE" == "201" ]; then
    # jq가 있으면 사용, 없으면 Python이나 sed/grep 조합 사용
    if command -v jq &> /dev/null; then
        ACCESS_TOKEN=$(echo "$SIGNUP_BODY" | jq -r '.accessToken // empty')
        MEMBER_ID=$(echo "$SIGNUP_BODY" | jq -r '.memberId // empty')
    elif command -v python3 &> /dev/null; then
        ACCESS_TOKEN=$(echo "$SIGNUP_BODY" | python3 -c "import sys, json; print(json.load(sys.stdin).get('accessToken', ''))" 2>/dev/null)
        MEMBER_ID=$(echo "$SIGNUP_BODY" | python3 -c "import sys, json; print(json.load(sys.stdin).get('memberId', ''))" 2>/dev/null)
    else
        # macOS와 Linux 모두에서 동작하는 sed 명령어
        ACCESS_TOKEN=$(echo "$SIGNUP_BODY" | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
        MEMBER_ID=$(echo "$SIGNUP_BODY" | grep -o '"memberId":[0-9]*' | head -1 | cut -d':' -f2)
    fi
    
    if [ -z "$ACCESS_TOKEN" ] || [ "$ACCESS_TOKEN" == "null" ] || [ "$ACCESS_TOKEN" == "" ]; then
        print_fail "회원가입 및 로그인 (토큰 추출 실패)"
        echo "Response body: $SIGNUP_BODY"
        echo "Extracted token: '$ACCESS_TOKEN'"
        exit 1
    fi
    
    print_pass "회원가입 및 로그인 (Member ID: $MEMBER_ID, Token: ${ACCESS_TOKEN:0:30}...)"
else
    print_fail "회원가입 및 로그인 (HTTP: $SIGNUP_HTTP_CODE)"
    echo "Response: $SIGNUP_BODY"
    exit 1
fi

# 3. 토픽 목록 조회
print_test "토픽 목록 조회"
TOPIC_LIST_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    "${BASE_URL}/api/debate/topics?pageNo=0&limit=10")
TOPIC_LIST_HTTP_CODE=$(echo "$TOPIC_LIST_RESPONSE" | tail -n1)
TOPIC_LIST_BODY=$(echo "$TOPIC_LIST_RESPONSE" | sed '$d')

if [ "$TOPIC_LIST_HTTP_CODE" == "200" ]; then
    # 첫 번째 토픽 ID 추출
    if command -v jq &> /dev/null; then
        FIRST_TOPIC_ID=$(echo "$TOPIC_LIST_BODY" | jq -r '.topics[0].id // empty')
    else
        FIRST_TOPIC_ID=$(echo "$TOPIC_LIST_BODY" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -n1)
    fi
    
    if [ -z "$FIRST_TOPIC_ID" ] || [ "$FIRST_TOPIC_ID" == "null" ]; then
        FIRST_TOPIC_ID=1  # 기본값 사용
    fi
    
    print_pass "토픽 목록 조회 (첫 번째 토픽 ID: $FIRST_TOPIC_ID)"
else
    print_fail "토픽 목록 조회 (HTTP: $TOPIC_LIST_HTTP_CODE)"
    FIRST_TOPIC_ID=1  # 기본값 사용
fi

# 4. 토픽 상세 조회
print_test "토픽 상세 조회"
TOPIC_DETAIL_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    "${BASE_URL}/api/debate/topics/${FIRST_TOPIC_ID}?increaseView=false")
TOPIC_DETAIL_HTTP_CODE=$(echo "$TOPIC_DETAIL_RESPONSE" | tail -n1)
TOPIC_DETAIL_BODY=$(echo "$TOPIC_DETAIL_RESPONSE" | sed '$d')

if [ "$TOPIC_DETAIL_HTTP_CODE" == "200" ]; then
    print_pass "토픽 상세 조회"
elif [ "$TOPIC_DETAIL_HTTP_CODE" == "401" ]; then
    print_fail "토픽 상세 조회 (인증 실패 - 토큰이 유효하지 않거나 만료됨)"
    echo "Token: ${ACCESS_TOKEN:0:50}..."
    echo "Response: $TOPIC_DETAIL_BODY"
elif [ "$TOPIC_DETAIL_HTTP_CODE" == "404" ]; then
    print_fail "토픽 상세 조회 (토픽을 찾을 수 없음: ID ${FIRST_TOPIC_ID})"
else
    print_fail "토픽 상세 조회 (HTTP: $TOPIC_DETAIL_HTTP_CODE)"
    echo "Response: $TOPIC_DETAIL_BODY"
fi

# 5. 게시글 목록 조회 (인증 필요)
print_test "게시글 목록 조회 (인증 필요)"
# 먼저 인증 없이 시도해서 401인지 404인지 확인
POST_LIST_CHECK=$(curl -s -w "\n%{http_code}" "${BASE_URL}/api/posts?boardId=1")
POST_LIST_CHECK_CODE=$(echo "$POST_LIST_CHECK" | tail -n1)

if [ "$POST_LIST_CHECK_CODE" == "401" ]; then
    # 인증이 필요하므로 토큰과 함께 다시 시도
    POST_LIST_RESPONSE=$(curl -s -w "\n%{http_code}" \
        -H "Authorization: Bearer ${ACCESS_TOKEN}" \
        "${BASE_URL}/api/posts?boardId=1")
    POST_LIST_HTTP_CODE=$(echo "$POST_LIST_RESPONSE" | tail -n1)
    POST_LIST_BODY=$(echo "$POST_LIST_RESPONSE" | sed '$d')

    if [ "$POST_LIST_HTTP_CODE" == "200" ]; then
        print_pass "게시글 목록 조회"
    elif [ "$POST_LIST_HTTP_CODE" == "401" ]; then
        print_fail "게시글 목록 조회 (인증 실패 - 토큰이 유효하지 않음)"
        echo "Token: ${ACCESS_TOKEN:0:50}..."
    elif [ "$POST_LIST_HTTP_CODE" == "404" ]; then
        print_fail "게시글 목록 조회 (엔드포인트를 찾을 수 없음 - PostController가 등록되지 않았을 수 있음)"
        echo "Response: $POST_LIST_BODY"
        echo "💡 해결 방법: 서버를 재시작하거나 PostController가 컴포넌트 스캔에 포함되었는지 확인하세요."
    else
        print_fail "게시글 목록 조회 (HTTP: $POST_LIST_HTTP_CODE)"
        echo "Response: $POST_LIST_BODY"
    fi
elif [ "$POST_LIST_CHECK_CODE" == "404" ]; then
    print_fail "게시글 목록 조회 (엔드포인트를 찾을 수 없음 - PostController가 등록되지 않았을 수 있음)"
    echo "💡 해결 방법: 서버를 재시작하거나 PostController가 컴포넌트 스캔에 포함되었는지 확인하세요."
else
    # 인증이 필요 없는 경우
    print_pass "게시글 목록 조회 (인증 불필요)"
fi

# 6. 게시글 생성
print_test "게시글 생성"
POST_CREATE_JSON=$(cat <<EOF
{
  "title": "테스트 게시글 $(date +%H%M%S)",
  "content": "이것은 자동화 테스트로 생성된 게시글입니다.",
  "contentHtml": "<p>이것은 자동화 테스트로 생성된 게시글입니다.</p>",
  "noticeYn": false
}
EOF
)

POST_CREATE_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "$POST_CREATE_JSON" \
    "${BASE_URL}/api/posts?boardId=1")
POST_CREATE_HTTP_CODE=$(echo "$POST_CREATE_RESPONSE" | tail -n1)
POST_CREATE_BODY=$(echo "$POST_CREATE_RESPONSE" | sed '$d')

if [ "$POST_CREATE_HTTP_CODE" == "201" ]; then
    # jq가 있으면 사용, 없으면 sed/grep 조합 사용
    if command -v jq &> /dev/null; then
        POST_ID=$(echo "$POST_CREATE_BODY" | jq -r '.id // empty')
    else
        POST_ID=$(echo "$POST_CREATE_BODY" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
    fi
    
    if [ -z "$POST_ID" ] || [ "$POST_ID" == "null" ]; then
        print_fail "게시글 생성 (ID 추출 실패)"
        echo "Response: $POST_CREATE_BODY"
        POST_ID=""
    else
        print_pass "게시글 생성 (Post ID: $POST_ID)"
    fi
elif [ "$POST_CREATE_HTTP_CODE" == "401" ]; then
    print_fail "게시글 생성 (인증 실패 - 토큰이 유효하지 않거나 만료됨)"
    echo "Token: ${ACCESS_TOKEN:0:50}..."
    POST_ID=""
elif [ "$POST_CREATE_HTTP_CODE" == "404" ]; then
    print_fail "게시글 생성 (엔드포인트를 찾을 수 없음 - PostController가 등록되지 않았을 수 있음)"
    echo "Response: $POST_CREATE_BODY"
    echo "💡 해결 방법: 서버를 재시작하거나 PostController가 컴포넌트 스캔에 포함되었는지 확인하세요."
    POST_ID=""
else
    print_fail "게시글 생성 (HTTP: $POST_CREATE_HTTP_CODE)"
    echo "Response: $POST_CREATE_BODY"
    POST_ID=""
fi

# 7. 게시글 상세 조회
if [ ! -z "$POST_ID" ]; then
    print_test "게시글 상세 조회"
    POST_DETAIL_RESPONSE=$(curl -s -w "\n%{http_code}" \
        -H "Authorization: Bearer ${ACCESS_TOKEN}" \
        "${BASE_URL}/api/posts/${POST_ID}?increaseView=false")
    POST_DETAIL_HTTP_CODE=$(echo "$POST_DETAIL_RESPONSE" | tail -n1)

    if [ "$POST_DETAIL_HTTP_CODE" == "200" ]; then
        print_pass "게시글 상세 조회"
    else
        print_fail "게시글 상세 조회 (HTTP: $POST_DETAIL_HTTP_CODE)"
    fi
fi

# 8. 로그인 ID 중복 체크
print_test "로그인 ID 중복 체크"
DUPLICATE_CHECK_RESPONSE=$(curl -s -w "\n%{http_code}" \
    "${BASE_URL}/api/check/login-id?loginId=${TEST_USER_LOGIN_ID}")
DUPLICATE_CHECK_HTTP_CODE=$(echo "$DUPLICATE_CHECK_RESPONSE" | tail -n1)

if [ "$DUPLICATE_CHECK_HTTP_CODE" == "200" ]; then
    print_pass "로그인 ID 중복 체크"
else
    print_fail "로그인 ID 중복 체크 (HTTP: $DUPLICATE_CHECK_HTTP_CODE)"
fi

# 결과 요약
echo ""
echo "=========================================="
echo -e "${GREEN}통과: ${PASSED}${NC}"
echo -e "${RED}실패: ${FAILED}${NC}"
echo "=========================================="

if [ $FAILED -eq 0 ]; then
    exit 0
else
    exit 1
fi

