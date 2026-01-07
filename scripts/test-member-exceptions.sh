#!/bin/bash

# Member 관련 API 예외 처리 테스트 스크립트
BASE_URL="http://localhost:8080"

echo "=========================================="
echo "Member API 예외 처리 테스트"
echo "=========================================="
echo ""

# 색상 코드
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

test_exception() {
  local method=$1
  local url=$2
  local token=$3
  local data=$4
  local test_name=$5
  local expected_status=$6
  local expected_keyword=$7
  
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
    if [ -n "$expected_keyword" ] && echo "$BODY" | grep -q "$expected_keyword"; then
      echo "  └─ 에러 메시지 포함: ✓"
    elif [ -n "$expected_keyword" ]; then
      echo -e "  └─ ${YELLOW}에러 메시지 확인 필요${NC}"
    fi
    return 0
  else
    echo -e "${RED}✗ 실패 (HTTP $HTTP_CODE, 예상: $expected_status)${NC}"
    echo "  └─ Response: $BODY"
    return 1
  fi
}

FAILED=0

echo "1. 회원가입 예외 처리 테스트"
echo "=========================================="

# 이미 존재하는 이메일로 회원가입 시도
test_exception "POST" "$BASE_URL/api/signup" "" '{
  "loginId": "test_duplicate_email",
  "password": "Test1234!",
  "name": "Test User",
  "phoneNumber": "01099999999",
  "email": "auth_owner@test.com",
  "birth": "1990-01-01",
  "nickname": "test_dup_email",
  "sex": "M",
  "ideology": "M",
  "profile": ""
}' "이메일 중복 회원가입" "400" "이메일\|EMAIL" || FAILED=$((FAILED+1))

# 이미 존재하는 로그인 ID로 회원가입 시도
test_exception "POST" "$BASE_URL/api/signup" "" '{
  "loginId": "auth_test_owner",
  "password": "Test1234!",
  "name": "Test User",
  "phoneNumber": "01088888888",
  "email": "new_email@test.com",
  "birth": "1990-01-01",
  "nickname": "test_dup_loginid",
  "sex": "M",
  "ideology": "M",
  "profile": ""
}' "로그인 ID 중복 회원가입" "400" "로그인\|LOGIN" || FAILED=$((FAILED+1))

# 이미 존재하는 닉네임으로 회원가입 시도
test_exception "POST" "$BASE_URL/api/signup" "" '{
  "loginId": "test_duplicate_nickname",
  "password": "Test1234!",
  "name": "Test User",
  "phoneNumber": "01077777777",
  "email": "new_email2@test.com",
  "birth": "1990-01-01",
  "nickname": "auth_owner",
  "sex": "M",
  "ideology": "M",
  "profile": ""
}' "닉네임 중복 회원가입" "400" "닉네임\|NICKNAME" || FAILED=$((FAILED+1))

echo ""
echo "2. 로그인 예외 처리 테스트"
echo "=========================================="

# 잘못된 로그인 ID로 로그인 시도
test_exception "POST" "$BASE_URL/api/login" "" '{
  "loginId": "nonexistent_user",
  "password": "Test1234!"
}' "존재하지 않는 사용자 로그인" "401" "로그인\|LOGIN" || FAILED=$((FAILED+1))

# 잘못된 비밀번호로 로그인 시도
test_exception "POST" "$BASE_URL/api/login" "" '{
  "loginId": "auth_test_owner",
  "password": "WrongPassword123!"
}' "잘못된 비밀번호 로그인" "401" "로그인\|비밀번호\|PASSWORD" || FAILED=$((FAILED+1))

echo ""
echo "3. 비밀번호 변경 예외 처리 테스트"
echo "=========================================="

# 정상 사용자 로그인
echo "정상 사용자 로그인 중..."
NORMAL_USER_LOGIN=$(curl -s -X POST "$BASE_URL/api/login" \
  -H "Content-Type: application/json" \
  -d '{
    "loginId": "auth_test_owner",
    "password": "Test1234!"
  }')

NORMAL_USER_TOKEN=$(echo "$NORMAL_USER_LOGIN" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -z "$NORMAL_USER_TOKEN" ]; then
  echo -e "${RED}정상 사용자 로그인 실패${NC}"
  exit 1
fi

# 새 비밀번호 불일치
test_exception "PATCH" "$BASE_URL/api/password" "$NORMAL_USER_TOKEN" '{
  "password": "Test1234!",
  "newPassword1": "NewPass123!",
  "newPassword2": "DifferentPass123!"
}' "비밀번호 변경 - 새 비밀번호 불일치" "400" "비밀번호\|PASSWORD" || FAILED=$((FAILED+1))

# 현재 비밀번호 불일치
test_exception "PATCH" "$BASE_URL/api/password" "$NORMAL_USER_TOKEN" '{
  "password": "WrongPassword123!",
  "newPassword1": "NewPass123!",
  "newPassword2": "NewPass123!"
}' "비밀번호 변경 - 현재 비밀번호 불일치" "400" "현재\|CURRENT" || FAILED=$((FAILED+1))

echo ""
echo "4. 정치성향 변경 예외 처리 테스트"
echo "=========================================="
echo "정치성향 변경 API 엔드포인트가 없습니다. (스킵)"

echo ""
echo "=========================================="
if [ $FAILED -eq 0 ]; then
  echo -e "${GREEN}모든 테스트 통과! ✓${NC}"
else
  echo -e "${RED}$FAILED 개 테스트 실패${NC}"
fi
echo "=========================================="

