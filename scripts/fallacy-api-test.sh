#!/bin/bash

BASE_URL="http://localhost:8080"
PYTHON_SERVICE_URL="http://localhost:8000/api/v1"

PASS_COUNT=0
FAIL_COUNT=0

print_test() {
    echo -e "\033[1;33m[TEST]\033[0m $1"
}

print_pass() {
    echo -e "\033[0;32m[PASS]\033[0m $1"
    ((PASS_COUNT++))
}

print_fail() {
    echo -e "\033[0;31m[FAIL]\033[0m $1"
    ((FAIL_COUNT++))
}

echo "=========================================="
echo "Fallacy 관련 API 테스트 시작"
echo "=========================================="

# 1. Python 서비스 헬스 체크
print_test "Python Fallacy Detection Service Health Check"
PYTHON_HEALTH=$(curl -s -w "\n%{http_code}" "${PYTHON_SERVICE_URL}/health")
PYTHON_HTTP_CODE=$(echo "$PYTHON_HEALTH" | tail -n1)
PYTHON_BODY=$(echo "$PYTHON_HEALTH" | sed '$d')

if [ "$PYTHON_HTTP_CODE" == "200" ]; then
    print_pass "Python 서비스 헬스 체크"
    echo "Response: $PYTHON_BODY"
else
    print_fail "Python 서비스 헬스 체크 (HTTP: $PYTHON_HTTP_CODE)"
    echo "Response: $PYTHON_BODY"
    echo "⚠️ Python 서비스가 실행되지 않았습니다. fallacy-detection-service를 시작해주세요."
fi

# 2. 회원가입 및 로그인
print_test "회원가입 및 자동 로그인"
TEST_USER_LOGIN_ID="fallacy_test_$(date +%s)"
TEST_USER_EMAIL="fallacy_test_$(date +%s)@test.com"

SIGNUP_JSON=$(cat <<EOF
{
  "loginId": "${TEST_USER_LOGIN_ID}",
  "password": "Passw0rd!",
  "name": "Fallacy테스터",
  "phoneNumber": "01012345678",
  "email": "${TEST_USER_EMAIL}",
  "birth": "1990-01-01",
  "nickname": "Fallacy테스터$(date +%H%M%S)",
  "sex": "M",
  "ideology": "M",
  "profile": "https://example.com/profile.png"
}
EOF
)

SIGNUP_RESPONSE=$(curl -s -w "\n%{http_code}" -H "Content-Type: application/json" -d "$SIGNUP_JSON" "${BASE_URL}/api/signup-and-login")
SIGNUP_HTTP_CODE=$(echo "$SIGNUP_RESPONSE" | tail -n1)
SIGNUP_BODY=$(echo "$SIGNUP_RESPONSE" | sed '$d')

if [ "$SIGNUP_HTTP_CODE" == "200" ] || [ "$SIGNUP_HTTP_CODE" == "201" ]; then
    ACCESS_TOKEN=$(echo "$SIGNUP_BODY" | python3 -c "import sys, json; print(json.load(sys.stdin).get('accessToken', ''))" 2>/dev/null)
    if [ ! -z "$ACCESS_TOKEN" ] && [ "$ACCESS_TOKEN" != "null" ]; then
        print_pass "회원가입 및 자동 로그인"
    else
        print_fail "회원가입 및 자동 로그인 (토큰 없음)"
        exit 1
    fi
else
    print_fail "회원가입 및 자동 로그인 (HTTP: $SIGNUP_HTTP_CODE)"
    echo "Response: $SIGNUP_BODY"
    exit 1
fi

# 3. 토픽 생성
print_test "토픽 생성"
TOPIC_JSON=$(cat <<EOF
{
  "title": "논리 오류 탐지 테스트 토픽",
  "description": "이것은 논리 오류 탐지 기능을 테스트하기 위한 토픽입니다.",
  "descriptionHtml": "<p>테스트 토픽</p>",
  "category": "POLITICS",
  "endDate": "2025-12-31"
}
EOF
)

TOPIC_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "$TOPIC_JSON" \
    "${BASE_URL}/api/debate/topics")
TOPIC_HTTP_CODE=$(echo "$TOPIC_RESPONSE" | tail -n1)
TOPIC_BODY=$(echo "$TOPIC_RESPONSE" | sed '$d')

if [ "$TOPIC_HTTP_CODE" == "201" ]; then
    CREATED_TOPIC_ID=$(echo "$TOPIC_BODY" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null)
    if [ ! -z "$CREATED_TOPIC_ID" ] && [ "$CREATED_TOPIC_ID" != "null" ]; then
        print_pass "토픽 생성 (ID: $CREATED_TOPIC_ID)"
    else
        print_fail "토픽 생성 (ID 추출 실패)"
        exit 1
    fi
else
    print_fail "토픽 생성 (HTTP: $TOPIC_HTTP_CODE)"
    echo "Response: $TOPIC_BODY"
    exit 1
fi

# 4. 논증 생성 (논리 오류 탐지 자동 실행)
print_test "논증 생성 (논리 오류 탐지 포함)"
ARGUMENT_JSON=$(cat <<EOF
{
  "title": "논리 오류가 있는 논증",
  "content": "모든 사람이 이렇게 생각하므로 이것이 맞습니다. 이것은 명백한 사실입니다.",
  "contentHtml": "<p>테스트 논증</p>",
  "stance": "PRO",
  "authorNickname": "테스터"
}
EOF
)

ARGUMENT_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "$ARGUMENT_JSON" \
    "${BASE_URL}/api/debate/topics/${CREATED_TOPIC_ID}/arguments")
ARGUMENT_HTTP_CODE=$(echo "$ARGUMENT_RESPONSE" | tail -n1)
ARGUMENT_BODY=$(echo "$ARGUMENT_RESPONSE" | sed '$d')

if [ "$ARGUMENT_HTTP_CODE" == "201" ]; then
    CREATED_ARGUMENT_ID=$(echo "$ARGUMENT_BODY" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null)
    FALLACY_DETECTED=$(echo "$ARGUMENT_BODY" | python3 -c "import sys, json; print(json.load(sys.stdin).get('fallacyHasFallacy', False))" 2>/dev/null)
    FALLACY_TYPE=$(echo "$ARGUMENT_BODY" | python3 -c "import sys, json; print(json.load(sys.stdin).get('fallacyType', 'None'))" 2>/dev/null)
    
    if [ ! -z "$CREATED_ARGUMENT_ID" ] && [ "$CREATED_ARGUMENT_ID" != "null" ]; then
        print_pass "논증 생성 (ID: $CREATED_ARGUMENT_ID)"
        echo "  논리 오류 탐지 결과: hasFallacy=$FALLACY_DETECTED, type=$FALLACY_TYPE"
    else
        print_fail "논증 생성 (ID 추출 실패)"
        exit 1
    fi
else
    print_fail "논증 생성 (HTTP: $ARGUMENT_HTTP_CODE)"
    echo "Response: $ARGUMENT_BODY"
    exit 1
fi

# 5. 논증 상세 조회 (논리 오류 정보 포함)
print_test "논증 상세 조회"
ARGUMENT_DETAIL_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    "${BASE_URL}/api/debate/topics/${CREATED_TOPIC_ID}/arguments/${CREATED_ARGUMENT_ID}")
ARGUMENT_DETAIL_HTTP_CODE=$(echo "$ARGUMENT_DETAIL_RESPONSE" | tail -n1)
ARGUMENT_DETAIL_BODY=$(echo "$ARGUMENT_DETAIL_RESPONSE" | sed '$d')

if [ "$ARGUMENT_DETAIL_HTTP_CODE" == "200" ]; then
    FALLACY_INFO=$(echo "$ARGUMENT_DETAIL_BODY" | python3 -c "import sys, json; data = json.load(sys.stdin); print(f\"hasFallacy: {data.get('fallacyHasFallacy', False)}, type: {data.get('fallacyType', 'None')}, confidence: {data.get('fallacyConfidence', 0.0)}\")" 2>/dev/null)
    print_pass "논증 상세 조회"
    echo "  논리 오류 정보: $FALLACY_INFO"
elif [ "$ARGUMENT_DETAIL_HTTP_CODE" == "401" ]; then
    echo "Response: $ARGUMENT_DETAIL_BODY"
    echo "인증 없이 재시도..."
    ARGUMENT_DETAIL_RESPONSE_NO_AUTH=$(curl -s -w "\n%{http_code}" \
        "${BASE_URL}/api/debate/topics/${CREATED_TOPIC_ID}/arguments/${CREATED_ARGUMENT_ID}")
    ARGUMENT_DETAIL_HTTP_CODE_NO_AUTH=$(echo "$ARGUMENT_DETAIL_RESPONSE_NO_AUTH" | tail -n1)
    if [ "$ARGUMENT_DETAIL_HTTP_CODE_NO_AUTH" == "200" ]; then
        print_pass "논증 상세 조회 (인증 없이)"
        FALLACY_INFO=$(echo "$ARGUMENT_DETAIL_RESPONSE_NO_AUTH" | sed '$d' | python3 -c "import sys, json; data = json.load(sys.stdin); print(f\"hasFallacy: {data.get('fallacyHasFallacy', False)}, type: {data.get('fallacyType', 'None')}, confidence: {data.get('fallacyConfidence', 0.0)}\")" 2>/dev/null)
        echo "  논리 오류 정보: $FALLACY_INFO"
    else
        print_fail "논증 상세 조회 (HTTP: $ARGUMENT_DETAIL_HTTP_CODE_NO_AUTH)"
    fi
else
    print_fail "논증 상세 조회 (HTTP: $ARGUMENT_DETAIL_HTTP_CODE)"
    echo "Response: $ARGUMENT_DETAIL_BODY"
fi

# 6. 의의 제기
print_test "의의 제기"
APPEAL_JSON=$(cat <<EOF
{
  "appealReason": "AI의 판단이 잘못되었습니다. 이 논증에는 논리 오류가 없습니다."
}
EOF
)

APPEAL_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "$APPEAL_JSON" \
    "${BASE_URL}/api/debate/arguments/${CREATED_ARGUMENT_ID}/fallacy/appeal")
APPEAL_HTTP_CODE=$(echo "$APPEAL_RESPONSE" | tail -n1)
APPEAL_BODY=$(echo "$APPEAL_RESPONSE" | sed '$d')

if [ "$APPEAL_HTTP_CODE" == "201" ]; then
    APPEAL_ID=$(echo "$APPEAL_BODY" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null)
    if [ ! -z "$APPEAL_ID" ] && [ "$APPEAL_ID" != "null" ]; then
        print_pass "의의 제기 (ID: $APPEAL_ID)"
    else
        print_fail "의의 제기 (ID 추출 실패)"
    fi
else
    print_fail "의의 제기 (HTTP: $APPEAL_HTTP_CODE)"
    echo "Response: $APPEAL_BODY"
fi

# 7. 의의 목록 조회
print_test "의의 목록 조회"
APPEALS_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    "${BASE_URL}/api/debate/arguments/${CREATED_ARGUMENT_ID}/fallacy/appeals")
APPEALS_HTTP_CODE=$(echo "$APPEALS_RESPONSE" | tail -n1)
APPEALS_BODY=$(echo "$APPEALS_RESPONSE" | sed '$d')

if [ "$APPEALS_HTTP_CODE" == "200" ]; then
    APPEAL_COUNT=$(echo "$APPEALS_BODY" | python3 -c "import sys, json; print(len(json.load(sys.stdin)))" 2>/dev/null)
    print_pass "의의 목록 조회 (개수: $APPEAL_COUNT)"
else
    print_fail "의의 목록 조회 (HTTP: $APPEALS_HTTP_CODE)"
fi

# 8. 의의 개수 조회
print_test "의의 개수 조회"
APPEAL_COUNT_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    "${BASE_URL}/api/debate/arguments/${CREATED_ARGUMENT_ID}/fallacy/appeals/count")
APPEAL_COUNT_HTTP_CODE=$(echo "$APPEAL_COUNT_RESPONSE" | tail -n1)
APPEAL_COUNT_BODY=$(echo "$APPEAL_COUNT_RESPONSE" | sed '$d')

if [ "$APPEAL_COUNT_HTTP_CODE" == "200" ]; then
    TOTAL_COUNT=$(echo "$APPEAL_COUNT_BODY" | python3 -c "import sys, json; print(json.load(sys.stdin).get('total', 0))" 2>/dev/null)
    PENDING_COUNT=$(echo "$APPEAL_COUNT_BODY" | python3 -c "import sys, json; print(json.load(sys.stdin).get('pending', 0))" 2>/dev/null)
    print_pass "의의 개수 조회 (전체: $TOTAL_COUNT, 대기: $PENDING_COUNT)"
else
    print_fail "의의 개수 조회 (HTTP: $APPEAL_COUNT_HTTP_CODE)"
fi

# 9. 미사용 재학습 데이터 개수 조회
print_test "미사용 재학습 데이터 개수 조회"
TRAINING_DATA_COUNT_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    "${BASE_URL}/api/debate/arguments/${CREATED_ARGUMENT_ID}/fallacy/training-data/count")
TRAINING_DATA_COUNT_HTTP_CODE=$(echo "$TRAINING_DATA_COUNT_RESPONSE" | tail -n1)
TRAINING_DATA_COUNT_BODY=$(echo "$TRAINING_DATA_COUNT_RESPONSE" | sed '$d')

if [ "$TRAINING_DATA_COUNT_HTTP_CODE" == "200" ]; then
    UNUSED_COUNT=$(echo "$TRAINING_DATA_COUNT_BODY" | python3 -c "import sys, json; print(json.load(sys.stdin).get('unused_count', 0))" 2>/dev/null)
    print_pass "미사용 재학습 데이터 개수 조회 (개수: $UNUSED_COUNT)"
else
    print_fail "미사용 재학습 데이터 개수 조회 (HTTP: $TRAINING_DATA_COUNT_HTTP_CODE)"
fi

echo ""
echo "=========================================="
echo "테스트 완료"
echo -e "\033[0;32m통과: $PASS_COUNT\033[0m"
echo -e "\033[0;31m실패: $FAIL_COUNT\033[0m"
echo "=========================================="

