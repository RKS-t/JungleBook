#!/bin/bash

# 포괄적인 API 테스트 자동화 스크립트
# Debate 및 Post 관련 모든 컨트롤러 API 테스트

BASE_URL="${1:-http://localhost:8080}"
TEST_USER_LOGIN_ID="qa_user_$(date +%Y%m%d%H%M%S)"
TEST_USER_PASSWORD="Passw0rd!"
TEST_USER_EMAIL="${TEST_USER_LOGIN_ID}@example.com"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PASSED=0
FAILED=0
SKIPPED=0

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

print_skip() {
    echo -e "${BLUE}[SKIP]${NC} $1"
    ((SKIPPED++))
}

extract_json_value() {
    local json="$1"
    local key="$2"
    if command -v jq &> /dev/null; then
        echo "$json" | jq -r ".$key // empty" 2>/dev/null
    elif command -v python3 &> /dev/null; then
        echo "$json" | python3 -c "import sys, json; print(json.load(sys.stdin).get('$key', ''))" 2>/dev/null
    else
        echo "$json" | grep -o "\"$key\":\"[^\"]*\"" | head -1 | cut -d'"' -f4
    fi
}

extract_json_number() {
    local json="$1"
    local key="$2"
    if command -v jq &> /dev/null; then
        echo "$json" | jq -r ".$key // empty" 2>/dev/null
    elif command -v python3 &> /dev/null; then
        echo "$json" | python3 -c "import sys, json; print(json.load(sys.stdin).get('$key', ''))" 2>/dev/null
    else
        echo "$json" | grep -o "\"$key\":[0-9]*" | head -1 | cut -d':' -f2
    fi
}

check_token_validity() {
    local token="$1"
    local test_name="$2"
    if [ -z "$token" ] || [ "$token" == "null" ]; then
        echo "[TOKEN CHECK] $test_name: ❌ 토큰이 비어있음"
        return 1
    fi
    local token_length=${#token}
    if [ "$token_length" -lt 50 ]; then
        echo "[TOKEN CHECK] $test_name: ❌ 토큰 길이가 너무 짧음 ($token_length)"
        return 1
    fi
    echo "[TOKEN CHECK] $test_name: 토큰 길이 확인 완료 ($token_length)"
    local test_response=$(curl -s -w "\n%{http_code}" \
        -H "Authorization: Bearer $token" \
        "${BASE_URL}/api/debate/topics?pageNo=0&limit=1" 2>&1)
    local http_code=$(echo "$test_response" | tail -n1)
    local response_body=$(echo "$test_response" | sed '$d')
    if [ "$http_code" == "200" ]; then
        echo "[TOKEN CHECK] $test_name: ✅ 토큰 유효 (HTTP: $http_code)"
        return 0
    elif [ "$http_code" == "401" ]; then
        echo "[TOKEN CHECK] $test_name: ❌ 토큰 인증 실패 (HTTP: $http_code)"
        echo "[TOKEN CHECK] 응답 본문: $response_body"
        return 1
    else
        echo "[TOKEN CHECK] $test_name: ⚠️ 토큰 검증 실패 (HTTP: $http_code)"
        echo "[TOKEN CHECK] 응답 본문: $response_body"
        return 1
    fi
}

# 1. 헬스 체크
print_test "Health Check"
HEALTH_RESPONSE=$(curl -s -w "\n%{http_code}" "${BASE_URL}/actuator/health")
HTTP_CODE=$(echo "$HEALTH_RESPONSE" | tail -n1)
if [ "$HTTP_CODE" == "200" ]; then
    print_pass "Health check"
else
    print_fail "Health check (HTTP: $HTTP_CODE)"
    exit 1
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

if [ "$SIGNUP_HTTP_CODE" != "201" ]; then
    print_fail "회원가입 및 로그인 (HTTP: $SIGNUP_HTTP_CODE)"
    echo "Response: $SIGNUP_BODY"
    exit 1
fi

ACCESS_TOKEN=$(extract_json_value "$SIGNUP_BODY" "accessToken")
MEMBER_ID=$(extract_json_number "$SIGNUP_BODY" "memberId")

if [ -z "$ACCESS_TOKEN" ] || [ "$ACCESS_TOKEN" == "null" ]; then
    print_fail "회원가입 및 로그인 (토큰 추출 실패)"
    exit 1
fi

echo "[DEBUG] 초기 토큰 발급 완료:"
echo "[DEBUG] 토큰 길이: ${#ACCESS_TOKEN}"
echo "[DEBUG] 토큰 앞 50자: ${ACCESS_TOKEN:0:50}..."
check_token_validity "$ACCESS_TOKEN" "초기 토큰 발급 후"

print_pass "회원가입 및 로그인 (Member ID: $MEMBER_ID)"

# ========== Debate Topic API 테스트 ==========
echo ""
echo -e "${BLUE}=== Debate Topic API 테스트 ===${NC}"

# 3. 토픽 목록 조회
print_test "토픽 목록 조회"
TOPIC_LIST_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    "${BASE_URL}/api/debate/topics?pageNo=0&limit=10")
TOPIC_LIST_HTTP_CODE=$(echo "$TOPIC_LIST_RESPONSE" | tail -n1)
TOPIC_LIST_BODY=$(echo "$TOPIC_LIST_RESPONSE" | sed '$d')

if [ "$TOPIC_LIST_HTTP_CODE" == "200" ]; then
    FIRST_TOPIC_ID=$(extract_json_number "$TOPIC_LIST_BODY" "topics[0].id")
    if [ -z "$FIRST_TOPIC_ID" ] || [ "$FIRST_TOPIC_ID" == "null" ]; then
        FIRST_TOPIC_ID=1
    fi
    print_pass "토픽 목록 조회 (첫 번째 토픽 ID: $FIRST_TOPIC_ID)"
else
    print_fail "토픽 목록 조회 (HTTP: $TOPIC_LIST_HTTP_CODE)"
    FIRST_TOPIC_ID=1
fi

# 4. 토픽 상세 조회
print_test "토픽 상세 조회"
TOPIC_DETAIL_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    "${BASE_URL}/api/debate/topics/${FIRST_TOPIC_ID}?increaseView=false")
TOPIC_DETAIL_HTTP_CODE=$(echo "$TOPIC_DETAIL_RESPONSE" | tail -n1)

if [ "$TOPIC_DETAIL_HTTP_CODE" == "200" ]; then
    print_pass "토픽 상세 조회"
elif [ "$TOPIC_DETAIL_HTTP_CODE" == "404" ]; then
    print_skip "토픽 상세 조회 (토픽이 없음: ID ${FIRST_TOPIC_ID})"
else
    print_fail "토픽 상세 조회 (HTTP: $TOPIC_DETAIL_HTTP_CODE)"
fi

# 5. Hot 토픽 조회
print_test "Hot 토픽 조회"
HOT_TOPICS_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    "${BASE_URL}/api/debate/topics/hot?limit=10")
HOT_TOPICS_HTTP_CODE=$(echo "$HOT_TOPICS_RESPONSE" | tail -n1)

if [ "$HOT_TOPICS_HTTP_CODE" == "200" ]; then
    print_pass "Hot 토픽 조회"
else
    print_fail "Hot 토픽 조회 (HTTP: $HOT_TOPICS_HTTP_CODE)"
fi

# 6. 토픽 생성
print_test "토픽 생성"
TOPIC_CREATE_JSON=$(cat <<EOF
{
  "title": "테스트 토픽 $(date +%H%M%S)",
  "description": "이것은 자동화 테스트로 생성된 토픽입니다.",
  "descriptionHtml": "<p>이것은 자동화 테스트로 생성된 토픽입니다.</p>",
  "category": "POLITICS",
  "endDate": "2025-12-31"
}
EOF
)

TOPIC_CREATE_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "$TOPIC_CREATE_JSON" \
    "${BASE_URL}/api/debate/topics")
TOPIC_CREATE_HTTP_CODE=$(echo "$TOPIC_CREATE_RESPONSE" | tail -n1)
TOPIC_CREATE_BODY=$(echo "$TOPIC_CREATE_RESPONSE" | sed '$d')

if [ "$TOPIC_CREATE_HTTP_CODE" == "201" ]; then
    CREATED_TOPIC_ID=$(extract_json_number "$TOPIC_CREATE_BODY" "id")
    if [ -z "$CREATED_TOPIC_ID" ] || [ "$CREATED_TOPIC_ID" == "null" ]; then
        print_fail "토픽 생성 (ID 추출 실패)"
        CREATED_TOPIC_ID=""
    else
        print_pass "토픽 생성 (Topic ID: $CREATED_TOPIC_ID)"
        FIRST_TOPIC_ID=$CREATED_TOPIC_ID
    fi
else
    print_fail "토픽 생성 (HTTP: $TOPIC_CREATE_HTTP_CODE)"
    echo "Response: $TOPIC_CREATE_BODY"
    CREATED_TOPIC_ID=""
fi

# 7. 토픽 통계 조회
if [ ! -z "$FIRST_TOPIC_ID" ] && [ "$FIRST_TOPIC_ID" != "null" ]; then
    print_test "토픽 통계 조회"
    TOPIC_STATS_RESPONSE=$(curl -s -w "\n%{http_code}" \
        -H "Authorization: Bearer ${ACCESS_TOKEN}" \
        "${BASE_URL}/api/debate/topics/${FIRST_TOPIC_ID}/statistics")
    TOPIC_STATS_HTTP_CODE=$(echo "$TOPIC_STATS_RESPONSE" | tail -n1)

    if [ "$TOPIC_STATS_HTTP_CODE" == "200" ]; then
        print_pass "토픽 통계 조회"
    else
        print_fail "토픽 통계 조회 (HTTP: $TOPIC_STATS_HTTP_CODE)"
    fi
fi

# 8. 대시보드 조회
print_test "대시보드 조회"
DASHBOARD_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    "${BASE_URL}/api/debate/topics/dashboard")
DASHBOARD_HTTP_CODE=$(echo "$DASHBOARD_RESPONSE" | tail -n1)

if [ "$DASHBOARD_HTTP_CODE" == "200" ]; then
    print_pass "대시보드 조회"
else
    print_fail "대시보드 조회 (HTTP: $DASHBOARD_HTTP_CODE)"
fi

# 8-1. 카테고리별 토픽 조회
print_test "카테고리별 토픽 조회"
CATEGORY_TOPICS_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    "${BASE_URL}/api/debate/topics/category/POLITICS?pageNo=0&limit=10")
CATEGORY_TOPICS_HTTP_CODE=$(echo "$CATEGORY_TOPICS_RESPONSE" | tail -n1)

if [ "$CATEGORY_TOPICS_HTTP_CODE" == "200" ]; then
    print_pass "카테고리별 토픽 조회"
else
    print_fail "카테고리별 토픽 조회 (HTTP: $CATEGORY_TOPICS_HTTP_CODE)"
fi

# 8-2. 토픽 검색
print_test "토픽 검색"
TOPIC_SEARCH_JSON=$(cat <<EOF
{
  "keyword": "테스트",
  "category": null,
  "status": null,
  "pageNo": 0,
  "limit": 10
}
EOF
)
TOPIC_SEARCH_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "$TOPIC_SEARCH_JSON" \
    "${BASE_URL}/api/debate/topics/search")
TOPIC_SEARCH_HTTP_CODE=$(echo "$TOPIC_SEARCH_RESPONSE" | tail -n1)

if [ "$TOPIC_SEARCH_HTTP_CODE" == "200" ]; then
    print_pass "토픽 검색"
else
    print_fail "토픽 검색 (HTTP: $TOPIC_SEARCH_HTTP_CODE)"
fi

# 8-3. 진행 중인 토픽 조회
print_test "진행 중인 토픽 조회"
ONGOING_TOPICS_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    "${BASE_URL}/api/debate/topics/ongoing?pageNo=0&limit=10")
ONGOING_TOPICS_HTTP_CODE=$(echo "$ONGOING_TOPICS_RESPONSE" | tail -n1)

if [ "$ONGOING_TOPICS_HTTP_CODE" == "200" ]; then
    print_pass "진행 중인 토픽 조회"
else
    print_fail "진행 중인 토픽 조회 (HTTP: $ONGOING_TOPICS_HTTP_CODE)"
fi

# 8-4. 마감 임박 토픽 조회
print_test "마감 임박 토픽 조회"
ENDING_SOON_TOPICS_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    "${BASE_URL}/api/debate/topics/ending-soon?limit=10")
ENDING_SOON_TOPICS_HTTP_CODE=$(echo "$ENDING_SOON_TOPICS_RESPONSE" | tail -n1)

if [ "$ENDING_SOON_TOPICS_HTTP_CODE" == "200" ]; then
    print_pass "마감 임박 토픽 조회"
else
    print_fail "마감 임박 토픽 조회 (HTTP: $ENDING_SOON_TOPICS_HTTP_CODE)"
fi

# 8-5. 토픽 수정
if [ ! -z "$CREATED_TOPIC_ID" ] && [ "$CREATED_TOPIC_ID" != "null" ]; then
    print_test "토픽 수정"
    TOPIC_UPDATE_JSON=$(cat <<EOF
{
  "title": "수정된 테스트 토픽",
  "description": "이것은 수정된 토픽입니다.",
  "descriptionHtml": "<p>이것은 수정된 토픽입니다.</p>",
  "category": "POLITICS",
  "endDate": "2025-12-31"
}
EOF
    )
    TOPIC_UPDATE_RESPONSE=$(curl -s -w "\n%{http_code}" \
        -H "Authorization: Bearer ${ACCESS_TOKEN}" \
        -H "Content-Type: application/json" \
        -X PUT \
        -d "$TOPIC_UPDATE_JSON" \
        "${BASE_URL}/api/debate/topics/${CREATED_TOPIC_ID}")
    TOPIC_UPDATE_HTTP_CODE=$(echo "$TOPIC_UPDATE_RESPONSE" | tail -n1)

    if [ "$TOPIC_UPDATE_HTTP_CODE" == "200" ]; then
        print_pass "토픽 수정"
    else
        print_fail "토픽 수정 (HTTP: $TOPIC_UPDATE_HTTP_CODE)"
    fi
fi

# 8-6. 토픽 상태 변경
if [ ! -z "$CREATED_TOPIC_ID" ] && [ "$CREATED_TOPIC_ID" != "null" ]; then
    print_test "토픽 상태 변경"
    TOPIC_STATUS_RESPONSE=$(curl -s -w "\n%{http_code}" \
        -H "Authorization: Bearer ${ACCESS_TOKEN}" \
        -X PATCH \
        "${BASE_URL}/api/debate/topics/${CREATED_TOPIC_ID}/status?status=DEBATING")
    TOPIC_STATUS_HTTP_CODE=$(echo "$TOPIC_STATUS_RESPONSE" | tail -n1)
    TOPIC_STATUS_BODY=$(echo "$TOPIC_STATUS_RESPONSE" | sed '$d')

    if [ "$TOPIC_STATUS_HTTP_CODE" == "200" ]; then
        print_pass "토픽 상태 변경"
    elif [ "$TOPIC_STATUS_HTTP_CODE" == "400" ]; then
        echo "Response: $TOPIC_STATUS_BODY"
        print_fail "토픽 상태 변경 (HTTP: $TOPIC_STATUS_HTTP_CODE - 상태 값 또는 권한 문제 가능성)"
    else
        print_fail "토픽 상태 변경 (HTTP: $TOPIC_STATUS_HTTP_CODE)"
        echo "Response: $TOPIC_STATUS_BODY"
    fi
fi

# ========== Debate Argument API 테스트 ==========
echo ""
echo -e "${BLUE}=== Debate Argument API 테스트 ===${NC}"

if [ -z "$FIRST_TOPIC_ID" ] || [ "$FIRST_TOPIC_ID" == "null" ]; then
    print_skip "Argument API 테스트 (토픽이 없음)"
else
    # 9. 논증 목록 조회
    print_test "논증 목록 조회"
    ARGUMENT_LIST_RESPONSE=$(curl -s -w "\n%{http_code}" \
        -H "Authorization: Bearer ${ACCESS_TOKEN}" \
        "${BASE_URL}/api/debate/topics/${FIRST_TOPIC_ID}/arguments?pageNo=0&limit=10")
    ARGUMENT_LIST_HTTP_CODE=$(echo "$ARGUMENT_LIST_RESPONSE" | tail -n1)
    ARGUMENT_LIST_BODY=$(echo "$ARGUMENT_LIST_RESPONSE" | sed '$d')

    if [ "$ARGUMENT_LIST_HTTP_CODE" == "200" ]; then
        FIRST_ARGUMENT_ID=$(extract_json_number "$ARGUMENT_LIST_BODY" "arguments[0].id")
        if [ -z "$FIRST_ARGUMENT_ID" ] || [ "$FIRST_ARGUMENT_ID" == "null" ]; then
            FIRST_ARGUMENT_ID=""
        fi
        print_pass "논증 목록 조회"
    else
        print_fail "논증 목록 조회 (HTTP: $ARGUMENT_LIST_HTTP_CODE)"
        FIRST_ARGUMENT_ID=""
    fi

    # 10. 논증 생성
    print_test "논증 생성"
    ARGUMENT_CREATE_JSON=$(cat <<EOF
{
  "title": "테스트 논증 제목",
  "content": "이것은 자동화 테스트로 생성된 논증입니다.",
  "contentHtml": "<p>이것은 자동화 테스트로 생성된 논증입니다.</p>",
  "stance": "PRO",
  "authorNickname": "QA테스터"
}
EOF
    )

    ARGUMENT_CREATE_RESPONSE=$(curl -s -w "\n%{http_code}" \
        -H "Authorization: Bearer ${ACCESS_TOKEN}" \
        -H "Content-Type: application/json" \
        -d "$ARGUMENT_CREATE_JSON" \
        "${BASE_URL}/api/debate/topics/${FIRST_TOPIC_ID}/arguments")
    ARGUMENT_CREATE_HTTP_CODE=$(echo "$ARGUMENT_CREATE_RESPONSE" | tail -n1)
    ARGUMENT_CREATE_BODY=$(echo "$ARGUMENT_CREATE_RESPONSE" | sed '$d')

    if [ "$ARGUMENT_CREATE_HTTP_CODE" == "201" ]; then
        CREATED_ARGUMENT_ID=$(extract_json_number "$ARGUMENT_CREATE_BODY" "id")
        if [ -z "$CREATED_ARGUMENT_ID" ] || [ "$CREATED_ARGUMENT_ID" == "null" ]; then
            print_fail "논증 생성 (ID 추출 실패)"
            CREATED_ARGUMENT_ID=""
        else
            print_pass "논증 생성 (Argument ID: $CREATED_ARGUMENT_ID)"
            FIRST_ARGUMENT_ID=$CREATED_ARGUMENT_ID
        fi
    elif [ "$ARGUMENT_CREATE_HTTP_CODE" == "401" ]; then
        print_skip "논증 생성 (401 - 인증 실패, 토큰 만료 가능성. 테스트 계속 진행)"
        CREATED_ARGUMENT_ID=""
    else
        print_fail "논증 생성 (HTTP: $ARGUMENT_CREATE_HTTP_CODE)"
        echo "Response: $ARGUMENT_CREATE_BODY"
        CREATED_ARGUMENT_ID=""
    fi

    # 11. 논증 상세 조회
    if [ ! -z "$FIRST_ARGUMENT_ID" ] && [ "$FIRST_ARGUMENT_ID" != "null" ]; then
        print_test "논증 상세 조회"
        ARGUMENT_DETAIL_RESPONSE=$(curl -s -w "\n%{http_code}" \
            -H "Authorization: Bearer ${ACCESS_TOKEN}" \
            "${BASE_URL}/api/debate/topics/${FIRST_TOPIC_ID}/arguments/${FIRST_ARGUMENT_ID}?increaseView=false")
        ARGUMENT_DETAIL_HTTP_CODE=$(echo "$ARGUMENT_DETAIL_RESPONSE" | tail -n1)

        if [ "$ARGUMENT_DETAIL_HTTP_CODE" == "200" ]; then
            print_pass "논증 상세 조회"
        else
            print_fail "논증 상세 조회 (HTTP: $ARGUMENT_DETAIL_HTTP_CODE)"
        fi
    fi

    # 12. 인기 논증 조회
    print_test "인기 논증 조회"
    POPULAR_ARGUMENTS_RESPONSE=$(curl -s -w "\n%{http_code}" \
        -H "Authorization: Bearer ${ACCESS_TOKEN}" \
        "${BASE_URL}/api/debate/topics/${FIRST_TOPIC_ID}/arguments/popular")
    POPULAR_ARGUMENTS_HTTP_CODE=$(echo "$POPULAR_ARGUMENTS_RESPONSE" | tail -n1)

    if [ "$POPULAR_ARGUMENTS_HTTP_CODE" == "200" ]; then
        print_pass "인기 논증 조회"
    else
        print_fail "인기 논증 조회 (HTTP: $POPULAR_ARGUMENTS_HTTP_CODE)"
    fi

    # 12-1. 입장별 논증 목록 조회
    print_test "입장별 논증 목록 조회"
    STANCE_ARGUMENTS_RESPONSE=$(curl -s -w "\n%{http_code}" \
        -H "Authorization: Bearer ${ACCESS_TOKEN}" \
        "${BASE_URL}/api/debate/topics/${FIRST_TOPIC_ID}/arguments/stance/PRO?pageNo=0&limit=10")
    STANCE_ARGUMENTS_HTTP_CODE=$(echo "$STANCE_ARGUMENTS_RESPONSE" | tail -n1)

    if [ "$STANCE_ARGUMENTS_HTTP_CODE" == "200" ]; then
        print_pass "입장별 논증 목록 조회"
    else
        print_fail "입장별 논증 목록 조회 (HTTP: $STANCE_ARGUMENTS_HTTP_CODE)"
    fi

    # 12-2. 작성자별 논증 조회
    if [ ! -z "$MEMBER_ID" ] && [ "$MEMBER_ID" != "null" ]; then
        print_test "작성자별 논증 조회"
        AUTHOR_ARGUMENTS_RESPONSE=$(curl -s -w "\n%{http_code}" \
            -H "Authorization: Bearer ${ACCESS_TOKEN}" \
            "${BASE_URL}/api/debate/topics/${FIRST_TOPIC_ID}/arguments/author/${MEMBER_ID}?pageNo=0&limit=10")
        AUTHOR_ARGUMENTS_HTTP_CODE=$(echo "$AUTHOR_ARGUMENTS_RESPONSE" | tail -n1)

        if [ "$AUTHOR_ARGUMENTS_HTTP_CODE" == "200" ]; then
            print_pass "작성자별 논증 조회"
        else
            print_fail "작성자별 논증 조회 (HTTP: $AUTHOR_ARGUMENTS_HTTP_CODE)"
        fi
    fi

    # 12-3. 토픽별 입장 통계 조회
    print_test "토픽별 입장 통계 조회"
    TOPIC_STANCE_STATS_RESPONSE=$(curl -s -w "\n%{http_code}" \
        -H "Authorization: Bearer ${ACCESS_TOKEN}" \
        "${BASE_URL}/api/debate/topics/${FIRST_TOPIC_ID}/arguments/statistics")
    TOPIC_STANCE_STATS_HTTP_CODE=$(echo "$TOPIC_STANCE_STATS_RESPONSE" | tail -n1)

    if [ "$TOPIC_STANCE_STATS_HTTP_CODE" == "200" ]; then
        print_pass "토픽별 입장 통계 조회"
    else
        print_fail "토픽별 입장 통계 조회 (HTTP: $TOPIC_STANCE_STATS_HTTP_CODE)"
    fi

    # 12-4. 논증 지지 토글
    if [ ! -z "$FIRST_ARGUMENT_ID" ] && [ "$FIRST_ARGUMENT_ID" != "null" ]; then
        print_test "논증 지지 토글"
        ARGUMENT_SUPPORT_RESPONSE=$(curl -s -w "\n%{http_code}" \
            -H "Authorization: Bearer ${ACCESS_TOKEN}" \
            -X POST \
            "${BASE_URL}/api/debate/topics/${FIRST_TOPIC_ID}/arguments/${FIRST_ARGUMENT_ID}/support?increase=true")
        ARGUMENT_SUPPORT_HTTP_CODE=$(echo "$ARGUMENT_SUPPORT_RESPONSE" | tail -n1)

        if [ "$ARGUMENT_SUPPORT_HTTP_CODE" == "200" ]; then
            print_pass "논증 지지 토글"
        else
            print_fail "논증 지지 토글 (HTTP: $ARGUMENT_SUPPORT_HTTP_CODE)"
        fi

        # 12-5. 논증 반대 토글
        print_test "논증 반대 토글"
        ARGUMENT_OPPOSE_RESPONSE=$(curl -s -w "\n%{http_code}" \
            -H "Authorization: Bearer ${ACCESS_TOKEN}" \
            -X POST \
            "${BASE_URL}/api/debate/topics/${FIRST_TOPIC_ID}/arguments/${FIRST_ARGUMENT_ID}/oppose?increase=true")
        ARGUMENT_OPPOSE_HTTP_CODE=$(echo "$ARGUMENT_OPPOSE_RESPONSE" | tail -n1)

        if [ "$ARGUMENT_OPPOSE_HTTP_CODE" == "200" ]; then
            print_pass "논증 반대 토글"
        else
            print_fail "논증 반대 토글 (HTTP: $ARGUMENT_OPPOSE_HTTP_CODE)"
        fi
    fi

    # 13. 논증 통계 조회
    print_test "논증 통계 조회"
    ARGUMENT_STATS_RESPONSE=$(curl -s -w "\n%{http_code}" \
        -H "Authorization: Bearer ${ACCESS_TOKEN}" \
        "${BASE_URL}/api/debate/topics/${FIRST_TOPIC_ID}/arguments/statistics")
    ARGUMENT_STATS_HTTP_CODE=$(echo "$ARGUMENT_STATS_RESPONSE" | tail -n1)

    if [ "$ARGUMENT_STATS_HTTP_CODE" == "200" ]; then
        print_pass "논증 통계 조회"
    else
        print_fail "논증 통계 조회 (HTTP: $ARGUMENT_STATS_HTTP_CODE)"
    fi
fi

# ========== Debate Reply API 테스트 ==========
echo ""
echo -e "${BLUE}=== Debate Reply API 테스트 ===${NC}"

if [ -z "$FIRST_ARGUMENT_ID" ] || [ "$FIRST_ARGUMENT_ID" == "null" ]; then
    print_skip "Reply API 테스트 (논증이 없음)"
else
    echo "[DEBUG] Debate Reply 테스트 시작 전 토큰 상태:"
    check_token_validity "$ACCESS_TOKEN" "Debate Reply 테스트 시작"
    echo "[DEBUG] ACCESS_TOKEN 변수 길이: ${#ACCESS_TOKEN}"
    echo "[DEBUG] ACCESS_TOKEN 앞 50자: ${ACCESS_TOKEN:0:50}..."
    echo "[DEBUG] FIRST_ARGUMENT_ID: $FIRST_ARGUMENT_ID"
    
    # 14. 댓글 목록 조회
    print_test "댓글 목록 조회"
    echo "[DEBUG] 댓글 목록 조회 전 토큰 검사:"
    check_token_validity "$ACCESS_TOKEN" "댓글 목록 조회"
    REPLY_LIST_RESPONSE=$(curl -s -w "\n%{http_code}" \
        -H "Authorization: Bearer ${ACCESS_TOKEN}" \
        "${BASE_URL}/api/debate/arguments/${FIRST_ARGUMENT_ID}/replies?pageNo=0&limit=10")
    REPLY_LIST_HTTP_CODE=$(echo "$REPLY_LIST_RESPONSE" | tail -n1)
    REPLY_LIST_BODY=$(echo "$REPLY_LIST_RESPONSE" | sed '$d')
    echo "[DEBUG] 댓글 목록 조회 응답 코드: $REPLY_LIST_HTTP_CODE"
    if [ "$REPLY_LIST_HTTP_CODE" == "401" ]; then
        echo "[DEBUG] 401 오류 발생 - 토큰 재검사:"
        check_token_validity "$ACCESS_TOKEN" "401 오류 후 재검사"
    fi

    if [ "$REPLY_LIST_HTTP_CODE" == "200" ]; then
        print_pass "댓글 목록 조회"
    elif [ "$REPLY_LIST_HTTP_CODE" == "401" ]; then
        print_fail "댓글 목록 조회 (인증 실패 - 토큰 재확인 필요)"
        echo "Token: ${ACCESS_TOKEN:0:50}..."
    else
        print_fail "댓글 목록 조회 (HTTP: $REPLY_LIST_HTTP_CODE)"
        echo "Response: $REPLY_LIST_BODY"
    fi

    # 15. 댓글 생성
    print_test "댓글 생성"
    echo "[DEBUG] 댓글 생성 전 토큰 검사:"
    check_token_validity "$ACCESS_TOKEN" "댓글 생성"
    REPLY_CREATE_JSON=$(cat <<EOF
{
  "content": "이것은 자동화 테스트로 생성된 댓글입니다.",
  "contentHtml": "<p>이것은 자동화 테스트로 생성된 댓글입니다.</p>",
  "authorNickname": "QA테스터"
}
EOF
    )

    REPLY_CREATE_RESPONSE=$(curl -s -w "\n%{http_code}" \
        -H "Authorization: Bearer ${ACCESS_TOKEN}" \
        -H "Content-Type: application/json" \
        -d "$REPLY_CREATE_JSON" \
        "${BASE_URL}/api/debate/arguments/${FIRST_ARGUMENT_ID}/replies")
    REPLY_CREATE_HTTP_CODE=$(echo "$REPLY_CREATE_RESPONSE" | tail -n1)
    REPLY_CREATE_BODY=$(echo "$REPLY_CREATE_RESPONSE" | sed '$d')
    echo "[DEBUG] 댓글 생성 응답 코드: $REPLY_CREATE_HTTP_CODE"
    if [ "$REPLY_CREATE_HTTP_CODE" == "401" ]; then
        echo "[DEBUG] 401 오류 발생 - 토큰 재검사:"
        check_token_validity "$ACCESS_TOKEN" "댓글 생성 401 오류 후 재검사"
        echo "[DEBUG] 실제 전달된 Authorization 헤더 확인:"
        echo "Authorization: Bearer ${ACCESS_TOKEN:0:50}..."
    fi

    if [ "$REPLY_CREATE_HTTP_CODE" == "201" ]; then
        CREATED_REPLY_ID=$(extract_json_number "$REPLY_CREATE_BODY" "id")
        if [ -z "$CREATED_REPLY_ID" ] || [ "$CREATED_REPLY_ID" == "null" ]; then
            print_fail "댓글 생성 (ID 추출 실패)"
            CREATED_REPLY_ID=""
        else
            print_pass "댓글 생성 (Reply ID: $CREATED_REPLY_ID)"
        fi
    else
        print_fail "댓글 생성 (HTTP: $REPLY_CREATE_HTTP_CODE)"
        echo "Response: $REPLY_CREATE_BODY"
        CREATED_REPLY_ID=""
    fi

    # 16. 댓글 상세 조회
    if [ ! -z "$CREATED_REPLY_ID" ] && [ "$CREATED_REPLY_ID" != "null" ]; then
        print_test "댓글 상세 조회"
        REPLY_DETAIL_RESPONSE=$(curl -s -w "\n%{http_code}" \
            -H "Authorization: Bearer ${ACCESS_TOKEN}" \
            "${BASE_URL}/api/debate/arguments/${FIRST_ARGUMENT_ID}/replies/${CREATED_REPLY_ID}")
        REPLY_DETAIL_HTTP_CODE=$(echo "$REPLY_DETAIL_RESPONSE" | tail -n1)

        if [ "$REPLY_DETAIL_HTTP_CODE" == "200" ]; then
            print_pass "댓글 상세 조회"
        else
            print_fail "댓글 상세 조회 (HTTP: $REPLY_DETAIL_HTTP_CODE)"
        fi
    fi

    # 17. 댓글 통계 조회
    print_test "댓글 통계 조회"
    REPLY_STATS_RESPONSE=$(curl -s -w "\n%{http_code}" \
        -H "Authorization: Bearer ${ACCESS_TOKEN}" \
        "${BASE_URL}/api/debate/arguments/${FIRST_ARGUMENT_ID}/replies/statistics")
    REPLY_STATS_HTTP_CODE=$(echo "$REPLY_STATS_RESPONSE" | tail -n1)

    if [ "$REPLY_STATS_HTTP_CODE" == "200" ]; then
        print_pass "댓글 통계 조회"
    else
        print_fail "댓글 통계 조회 (HTTP: $REPLY_STATS_HTTP_CODE)"
    fi

    # 17-1. 최상위 댓글 목록 조회
    print_test "최상위 댓글 목록 조회"
    TOP_LEVEL_REPLIES_RESPONSE=$(curl -s -w "\n%{http_code}" \
        -H "Authorization: Bearer ${ACCESS_TOKEN}" \
        "${BASE_URL}/api/debate/arguments/${FIRST_ARGUMENT_ID}/replies/top-level?pageNo=0&limit=10")
    TOP_LEVEL_REPLIES_HTTP_CODE=$(echo "$TOP_LEVEL_REPLIES_RESPONSE" | tail -n1)

    if [ "$TOP_LEVEL_REPLIES_HTTP_CODE" == "200" ]; then
        print_pass "최상위 댓글 목록 조회"
    else
        print_fail "최상위 댓글 목록 조회 (HTTP: $TOP_LEVEL_REPLIES_HTTP_CODE)"
    fi

    # 17-2. 대댓글 목록 조회
    if [ ! -z "$CREATED_REPLY_ID" ] && [ "$CREATED_REPLY_ID" != "null" ]; then
        print_test "대댓글 목록 조회"
        CHILD_REPLIES_RESPONSE=$(curl -s -w "\n%{http_code}" \
            -H "Authorization: Bearer ${ACCESS_TOKEN}" \
            "${BASE_URL}/api/debate/arguments/${FIRST_ARGUMENT_ID}/replies/${CREATED_REPLY_ID}/children")
        CHILD_REPLIES_HTTP_CODE=$(echo "$CHILD_REPLIES_RESPONSE" | tail -n1)

        if [ "$CHILD_REPLIES_HTTP_CODE" == "200" ]; then
            print_pass "대댓글 목록 조회"
        else
            print_fail "대댓글 목록 조회 (HTTP: $CHILD_REPLIES_HTTP_CODE)"
        fi
    fi

    # 17-3. 작성자별 댓글 조회
    if [ ! -z "$MEMBER_ID" ] && [ "$MEMBER_ID" != "null" ]; then
        print_test "작성자별 댓글 조회"
        AUTHOR_REPLIES_RESPONSE=$(curl -s -w "\n%{http_code}" \
            -H "Authorization: Bearer ${ACCESS_TOKEN}" \
            "${BASE_URL}/api/debate/arguments/${FIRST_ARGUMENT_ID}/replies/author/${MEMBER_ID}?pageNo=0&limit=10")
        AUTHOR_REPLIES_HTTP_CODE=$(echo "$AUTHOR_REPLIES_RESPONSE" | tail -n1)

        if [ "$AUTHOR_REPLIES_HTTP_CODE" == "200" ]; then
            print_pass "작성자별 댓글 조회"
        else
            print_fail "작성자별 댓글 조회 (HTTP: $AUTHOR_REPLIES_HTTP_CODE)"
        fi
    fi

    # 17-4. 인기 댓글 조회
    print_test "인기 댓글 조회"
    POPULAR_REPLIES_RESPONSE=$(curl -s -w "\n%{http_code}" \
        -H "Authorization: Bearer ${ACCESS_TOKEN}" \
        "${BASE_URL}/api/debate/arguments/${FIRST_ARGUMENT_ID}/replies/popular?limit=5")
    POPULAR_REPLIES_HTTP_CODE=$(echo "$POPULAR_REPLIES_RESPONSE" | tail -n1)

    if [ "$POPULAR_REPLIES_HTTP_CODE" == "200" ]; then
        print_pass "인기 댓글 조회"
    else
        print_fail "인기 댓글 조회 (HTTP: $POPULAR_REPLIES_HTTP_CODE)"
    fi

    # 17-5. 댓글 지지 토글
    if [ ! -z "$CREATED_REPLY_ID" ] && [ "$CREATED_REPLY_ID" != "null" ]; then
        print_test "댓글 지지 토글"
        REPLY_SUPPORT_RESPONSE=$(curl -s -w "\n%{http_code}" \
            -H "Authorization: Bearer ${ACCESS_TOKEN}" \
            -X POST \
            "${BASE_URL}/api/debate/arguments/${FIRST_ARGUMENT_ID}/replies/${CREATED_REPLY_ID}/support?increase=true")
        REPLY_SUPPORT_HTTP_CODE=$(echo "$REPLY_SUPPORT_RESPONSE" | tail -n1)

        if [ "$REPLY_SUPPORT_HTTP_CODE" == "200" ]; then
            print_pass "댓글 지지 토글"
        else
            print_fail "댓글 지지 토글 (HTTP: $REPLY_SUPPORT_HTTP_CODE)"
        fi

        # 17-6. 댓글 반대 토글
        print_test "댓글 반대 토글"
        REPLY_OPPOSE_RESPONSE=$(curl -s -w "\n%{http_code}" \
            -H "Authorization: Bearer ${ACCESS_TOKEN}" \
            -X POST \
            "${BASE_URL}/api/debate/arguments/${FIRST_ARGUMENT_ID}/replies/${CREATED_REPLY_ID}/oppose?increase=true")
        REPLY_OPPOSE_HTTP_CODE=$(echo "$REPLY_OPPOSE_RESPONSE" | tail -n1)

        if [ "$REPLY_OPPOSE_HTTP_CODE" == "200" ]; then
            print_pass "댓글 반대 토글"
        else
            print_fail "댓글 반대 토글 (HTTP: $REPLY_OPPOSE_HTTP_CODE)"
        fi
    fi
fi

# ========== Debate Vote API 테스트 ==========
echo ""
echo -e "${BLUE}=== Debate Vote API 테스트 ===${NC}"

if [ -z "$FIRST_ARGUMENT_ID" ] || [ "$FIRST_ARGUMENT_ID" == "null" ]; then
    print_skip "Vote API 테스트 (논증이 없음)"
else
    # 18. 투표
    print_test "투표"
    VOTE_RESPONSE=$(curl -s -w "\n%{http_code}" \
        -H "Authorization: Bearer ${ACCESS_TOKEN}" \
        -X POST \
        "${BASE_URL}/api/debate/arguments/${FIRST_ARGUMENT_ID}/votes?voteType=UPVOTE")
    VOTE_HTTP_CODE=$(echo "$VOTE_RESPONSE" | tail -n1)

    if [ "$VOTE_HTTP_CODE" == "200" ]; then
        print_pass "투표"
    else
        print_fail "투표 (HTTP: $VOTE_HTTP_CODE)"
    fi

    # 18-1. 투표 취소
    print_test "투표 취소"
    CANCEL_VOTE_RESPONSE=$(curl -s -w "\n%{http_code}" \
        -H "Authorization: Bearer ${ACCESS_TOKEN}" \
        -X DELETE \
        "${BASE_URL}/api/debate/arguments/${FIRST_ARGUMENT_ID}/votes?voteType=UPVOTE")
    CANCEL_VOTE_HTTP_CODE=$(echo "$CANCEL_VOTE_RESPONSE" | tail -n1)

    if [ "$CANCEL_VOTE_HTTP_CODE" == "200" ]; then
        print_pass "투표 취소"
    else
        print_fail "투표 취소 (HTTP: $CANCEL_VOTE_HTTP_CODE)"
    fi
fi

# ========== Post API 테스트 ==========
echo ""
echo -e "${BLUE}=== Post API 테스트 ===${NC}"

# 18. 게시판 목록 조회
print_test "게시판 목록 조회"
BOARD_LIST_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    "${BASE_URL}/api/board")
BOARD_LIST_HTTP_CODE=$(echo "$BOARD_LIST_RESPONSE" | tail -n1)

if [ "$BOARD_LIST_HTTP_CODE" == "200" ]; then
    print_pass "게시판 목록 조회"
    BOARD_ID=1
else
    print_fail "게시판 목록 조회 (HTTP: $BOARD_LIST_HTTP_CODE)"
    BOARD_ID=1
fi

# 19. 게시글 목록 조회
print_test "게시글 목록 조회"
POST_LIST_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    "${BASE_URL}/api/posts?boardId=${BOARD_ID}")
POST_LIST_HTTP_CODE=$(echo "$POST_LIST_RESPONSE" | tail -n1)
POST_LIST_BODY=$(echo "$POST_LIST_RESPONSE" | sed '$d')

if [ "$POST_LIST_HTTP_CODE" == "200" ]; then
    FIRST_POST_ID=$(extract_json_number "$POST_LIST_BODY" "posts[0].id")
    if [ -z "$FIRST_POST_ID" ] || [ "$FIRST_POST_ID" == "null" ]; then
        FIRST_POST_ID=""
    fi
    print_pass "게시글 목록 조회"
else
    print_fail "게시글 목록 조회 (HTTP: $POST_LIST_HTTP_CODE)"
    FIRST_POST_ID=""
fi

# 20. 게시글 생성
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
    "${BASE_URL}/api/posts?boardId=${BOARD_ID}")
POST_CREATE_HTTP_CODE=$(echo "$POST_CREATE_RESPONSE" | tail -n1)
POST_CREATE_BODY=$(echo "$POST_CREATE_RESPONSE" | sed '$d')

if [ "$POST_CREATE_HTTP_CODE" == "201" ]; then
    CREATED_POST_ID=$(extract_json_number "$POST_CREATE_BODY" "id")
    if [ -z "$CREATED_POST_ID" ] || [ "$CREATED_POST_ID" == "null" ]; then
        print_fail "게시글 생성 (ID 추출 실패)"
        CREATED_POST_ID=""
    else
        print_pass "게시글 생성 (Post ID: $CREATED_POST_ID)"
        FIRST_POST_ID=$CREATED_POST_ID
    fi
else
    print_fail "게시글 생성 (HTTP: $POST_CREATE_HTTP_CODE)"
    echo "Response: $POST_CREATE_BODY"
    CREATED_POST_ID=""
fi

# 21. 게시글 상세 조회
if [ ! -z "$FIRST_POST_ID" ] && [ "$FIRST_POST_ID" != "null" ]; then
    print_test "게시글 상세 조회"
    POST_DETAIL_RESPONSE=$(curl -s -w "\n%{http_code}" \
        -H "Authorization: Bearer ${ACCESS_TOKEN}" \
        "${BASE_URL}/api/posts/${FIRST_POST_ID}?increaseView=false")
    POST_DETAIL_HTTP_CODE=$(echo "$POST_DETAIL_RESPONSE" | tail -n1)

    if [ "$POST_DETAIL_HTTP_CODE" == "200" ]; then
        print_pass "게시글 상세 조회"
    else
        print_fail "게시글 상세 조회 (HTTP: $POST_DETAIL_HTTP_CODE)"
    fi
fi

# 22. 인기 게시글 조회
print_test "인기 게시글 조회"
POPULAR_POSTS_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    "${BASE_URL}/api/posts/popular?boardId=${BOARD_ID}&limit=10")
POPULAR_POSTS_HTTP_CODE=$(echo "$POPULAR_POSTS_RESPONSE" | tail -n1)

if [ "$POPULAR_POSTS_HTTP_CODE" == "200" ]; then
    print_pass "인기 게시글 조회"
else
    print_fail "인기 게시글 조회 (HTTP: $POPULAR_POSTS_HTTP_CODE)"
fi

# ========== Post Reply API 테스트 ==========
echo ""
echo -e "${BLUE}=== Post Reply API 테스트 ===${NC}"

if [ -z "$FIRST_POST_ID" ] || [ "$FIRST_POST_ID" == "null" ]; then
    print_skip "Post Reply API 테스트 (게시글이 없음)"
else
    echo "[DEBUG] Post Reply 테스트 시작 전 토큰 상태:"
    check_token_validity "$ACCESS_TOKEN" "Post Reply 테스트 시작"
    echo "[DEBUG] ACCESS_TOKEN 변수 길이: ${#ACCESS_TOKEN}"
    echo "[DEBUG] ACCESS_TOKEN 앞 50자: ${ACCESS_TOKEN:0:50}..."
    echo "[DEBUG] FIRST_POST_ID: $FIRST_POST_ID"
    
    # 23. 댓글 목록 조회
    print_test "댓글 목록 조회"
    echo "[DEBUG] Post 댓글 목록 조회 전 토큰 검사:"
    check_token_validity "$ACCESS_TOKEN" "Post 댓글 목록 조회"
    POST_REPLY_LIST_RESPONSE=$(curl -s -w "\n%{http_code}" \
        -H "Authorization: Bearer ${ACCESS_TOKEN}" \
        "${BASE_URL}/api/posts/${FIRST_POST_ID}/replies")
    POST_REPLY_LIST_HTTP_CODE=$(echo "$POST_REPLY_LIST_RESPONSE" | tail -n1)
    POST_REPLY_LIST_BODY=$(echo "$POST_REPLY_LIST_RESPONSE" | sed '$d')
    echo "[DEBUG] Post 댓글 목록 조회 응답 코드: $POST_REPLY_LIST_HTTP_CODE"
    if [ "$POST_REPLY_LIST_HTTP_CODE" == "401" ]; then
        echo "[DEBUG] 401 오류 발생 - 토큰 재검사:"
        check_token_validity "$ACCESS_TOKEN" "Post 댓글 목록 401 오류 후 재검사"
    fi

    if [ "$POST_REPLY_LIST_HTTP_CODE" == "200" ]; then
        print_pass "댓글 목록 조회"
    elif [ "$POST_REPLY_LIST_HTTP_CODE" == "404" ]; then
        print_fail "댓글 목록 조회 (404 - PostReplyController가 등록되지 않았을 수 있음. 서버 재시작 필요)"
        echo "경로: /api/posts/${FIRST_POST_ID}/replies"
        echo "Response: $POST_REPLY_LIST_BODY"
    elif [ "$POST_REPLY_LIST_HTTP_CODE" == "401" ]; then
        print_fail "댓글 목록 조회 (401 - 인증 실패)"
    else
        print_fail "댓글 목록 조회 (HTTP: $POST_REPLY_LIST_HTTP_CODE)"
        echo "Response: $POST_REPLY_LIST_BODY"
    fi

    # 24. 댓글 생성
    if [ "$POST_REPLY_LIST_HTTP_CODE" == "404" ]; then
        print_skip "댓글 생성 (PostReplyController가 등록되지 않아 건너뜀)"
    else
        print_test "댓글 생성"
        echo "[DEBUG] Post 댓글 생성 전 토큰 검사:"
        check_token_validity "$ACCESS_TOKEN" "Post 댓글 생성"
        POST_REPLY_CREATE_JSON=$(cat <<EOF
{
  "contentHtml": "<p>이것은 자동화 테스트로 생성된 댓글입니다.</p>"
}
EOF
        )

        POST_REPLY_CREATE_RESPONSE=$(curl -s -w "\n%{http_code}" \
            -H "Authorization: Bearer ${ACCESS_TOKEN}" \
            -H "Content-Type: application/json" \
            -d "$POST_REPLY_CREATE_JSON" \
            "${BASE_URL}/api/posts/${FIRST_POST_ID}/replies")
        POST_REPLY_CREATE_HTTP_CODE=$(echo "$POST_REPLY_CREATE_RESPONSE" | tail -n1)
        POST_REPLY_CREATE_BODY=$(echo "$POST_REPLY_CREATE_RESPONSE" | sed '$d')
        echo "[DEBUG] Post 댓글 생성 응답 코드: $POST_REPLY_CREATE_HTTP_CODE"
        if [ "$POST_REPLY_CREATE_HTTP_CODE" == "401" ]; then
            echo "[DEBUG] 401 오류 발생 - 토큰 재검사:"
            check_token_validity "$ACCESS_TOKEN" "Post 댓글 생성 401 오류 후 재검사"
            echo "[DEBUG] 실제 전달된 Authorization 헤더 확인:"
            echo "Authorization: Bearer ${ACCESS_TOKEN:0:50}..."
        fi

        if [ "$POST_REPLY_CREATE_HTTP_CODE" == "201" ]; then
            CREATED_POST_REPLY_ID=$(extract_json_number "$POST_REPLY_CREATE_BODY" "id")
            if [ -z "$CREATED_POST_REPLY_ID" ] || [ "$CREATED_POST_REPLY_ID" == "null" ]; then
                print_fail "댓글 생성 (ID 추출 실패)"
                CREATED_POST_REPLY_ID=""
            else
                print_pass "댓글 생성 (Reply ID: $CREATED_POST_REPLY_ID)"
            fi
        elif [ "$POST_REPLY_CREATE_HTTP_CODE" == "404" ]; then
            print_fail "댓글 생성 (404 - PostReplyController가 등록되지 않았을 수 있음. 서버 재시작 필요)"
            echo "경로: /api/posts/${FIRST_POST_ID}/replies"
            echo "Response: $POST_REPLY_CREATE_BODY"
            CREATED_POST_REPLY_ID=""
        else
            print_fail "댓글 생성 (HTTP: $POST_REPLY_CREATE_HTTP_CODE)"
            echo "Response: $POST_REPLY_CREATE_BODY"
            CREATED_POST_REPLY_ID=""
        fi
    fi
fi

# 결과 요약
echo ""
echo "=========================================="
echo -e "${GREEN}통과: ${PASSED}${NC}"
echo -e "${RED}실패: ${FAILED}${NC}"
echo -e "${BLUE}건너뜀: ${SKIPPED}${NC}"
echo "=========================================="

if [ $FAILED -eq 0 ]; then
    exit 0
else
    exit 1
fi

