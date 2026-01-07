#!/bin/bash

# API 권한 테스트 스크립트
# 작성자가 아닌 사용자가 수정/삭제/의의제기를 시도할 때 예외 처리가 잘 되는지 확인

BASE_URL="http://localhost:8080"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=========================================="
echo "API 권한 예외 처리 테스트"
echo "=========================================="
echo ""

# 1. 두 명의 사용자 생성 및 로그인
echo "1. 테스트 사용자 생성 및 로그인..."
echo ""

# Owner 사용자 생성
echo "Owner 사용자 생성 중..."
OWNER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/signup" \
  -H "Content-Type: application/json" \
  -d '{
    "loginId": "owner_test",
    "password": "Test1234!",
    "name": "Owner User",
    "phoneNumber": "01011111111",
    "email": "owner@test.com",
    "birth": "1990-01-01",
    "nickname": "owner123",
    "sex": "M",
    "ideology": "M"
  }')

if echo "$OWNER_RESPONSE" | grep -q "error\|Error"; then
  echo "Owner 사용자가 이미 존재하거나 생성 실패. 로그인 시도..."
fi

# Owner 로그인
OWNER_LOGIN=$(curl -s -X POST "$BASE_URL/api/login" \
  -H "Content-Type: application/json" \
  -d '{
    "loginId": "owner_test",
    "password": "Test1234!"
  }')

OWNER_TOKEN=$(echo "$OWNER_LOGIN" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
if [ -z "$OWNER_TOKEN" ]; then
  echo -e "${RED}Owner 로그인 실패${NC}"
  echo "Response: $OWNER_LOGIN"
  exit 1
fi
echo -e "${GREEN}Owner 로그인 성공${NC}"
echo ""

# Other 사용자 생성
echo "Other 사용자 생성 중..."
OTHER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/signup" \
  -H "Content-Type: application/json" \
  -d '{
    "loginId": "other_test",
    "password": "Test1234!",
    "name": "Other User",
    "phoneNumber": "01022222222",
    "email": "other@test.com",
    "birth": "1990-01-01",
    "nickname": "other123",
    "sex": "M",
    "ideology": "M"
  }')

if echo "$OTHER_RESPONSE" | grep -q "error\|Error"; then
  echo "Other 사용자가 이미 존재하거나 생성 실패. 로그인 시도..."
fi

# Other 로그인
OTHER_LOGIN=$(curl -s -X POST "$BASE_URL/api/login" \
  -H "Content-Type: application/json" \
  -d '{
    "loginId": "other_test",
    "password": "Test1234!"
  }')

OTHER_TOKEN=$(echo "$OTHER_LOGIN" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
if [ -z "$OTHER_TOKEN" ]; then
  echo -e "${RED}Other 로그인 실패${NC}"
  echo "Response: $OTHER_LOGIN"
  exit 1
fi
echo -e "${GREEN}Other 로그인 성공${NC}"
echo ""

# 2. 테스트 데이터 생성 (Owner가 생성)
echo "2. 테스트 데이터 생성 중..."
echo ""

# 토픽 생성
echo "토픽 생성 중..."
TOPIC_RESPONSE=$(curl -s -X POST "$BASE_URL/api/debate/topics" \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "테스트 토픽",
    "description": "테스트 설명",
    "descriptionHtml": "<p>테스트 설명</p>",
    "category": "POLITICS",
    "startDate": "2025-01-01",
    "endDate": "2025-12-31"
  }')

TOPIC_ID=$(echo "$TOPIC_RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
if [ -z "$TOPIC_ID" ]; then
  echo -e "${RED}토픽 생성 실패${NC}"
  echo "Response: $TOPIC_RESPONSE"
  exit 1
fi
echo -e "${GREEN}토픽 생성 성공 (ID: $TOPIC_ID)${NC}"

# 논증 생성
echo "논증 생성 중..."
ARGUMENT_RESPONSE=$(curl -s -X POST "$BASE_URL/api/debate/topics/$TOPIC_ID/arguments" \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "테스트 논증",
    "content": "테스트 내용",
    "contentHtml": "<p>테스트 내용</p>",
    "stance": "PRO",
    "fileIds": null
  }')

ARGUMENT_ID=$(echo "$ARGUMENT_RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
if [ -z "$ARGUMENT_ID" ]; then
  echo -e "${RED}논증 생성 실패${NC}"
  echo "Response: $ARGUMENT_RESPONSE"
  exit 1
fi
echo -e "${GREEN}논증 생성 성공 (ID: $ARGUMENT_ID)${NC}"

# 댓글 생성
echo "댓글 생성 중..."
REPLY_RESPONSE=$(curl -s -X POST "$BASE_URL/api/debate/arguments/$ARGUMENT_ID/replies" \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "테스트 댓글",
    "contentHtml": "<p>테스트 댓글</p>",
    "parentId": null,
    "fileIds": null
  }')

REPLY_ID=$(echo "$REPLY_RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
if [ -z "$REPLY_ID" ]; then
  echo -e "${RED}댓글 생성 실패${NC}"
  echo "Response: $REPLY_RESPONSE"
  exit 1
fi
echo -e "${GREEN}댓글 생성 성공 (ID: $REPLY_ID)${NC}"

# 게시글 생성
echo "게시글 생성 중..."
POST_RESPONSE=$(curl -s -X POST "$BASE_URL/api/posts?boardId=2" \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "테스트 게시글",
    "content": "테스트 내용",
    "fileIds": null
  }')

POST_ID=$(echo "$POST_RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
if [ -z "$POST_ID" ]; then
  echo -e "${RED}게시글 생성 실패${NC}"
  echo "Response: $POST_RESPONSE"
  exit 1
fi
echo -e "${GREEN}게시글 생성 성공 (ID: $POST_ID)${NC}"

# 게시글 댓글 생성
echo "게시글 댓글 생성 중..."
POST_REPLY_RESPONSE=$(curl -s -X POST "$BASE_URL/api/posts/$POST_ID/replies" \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "테스트 댓글",
    "contentHtml": "<p>테스트 댓글</p>",
    "fileIds": null
  }')

POST_REPLY_ID=$(echo "$POST_REPLY_RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
if [ -z "$POST_REPLY_ID" ]; then
  echo -e "${RED}게시글 댓글 생성 실패${NC}"
  echo "Response: $POST_REPLY_RESPONSE"
  exit 1
fi
echo -e "${GREEN}게시글 댓글 생성 성공 (ID: $POST_REPLY_ID)${NC}"
echo ""

# 3. 권한 테스트
echo "=========================================="
echo "3. 권한 예외 처리 테스트"
echo "=========================================="
echo ""

test_unauthorized() {
  local method=$1
  local url=$2
  local token=$3
  local data=$4
  local test_name=$5
  
  echo -n "테스트: $test_name ... "
  
  if [ -z "$data" ]; then
    RESPONSE=$(curl -s -w "\n%{http_code}" -X "$method" "$url" \
      -H "Authorization: Bearer $token" \
      -H "Content-Type: application/json")
  else
    RESPONSE=$(curl -s -w "\n%{http_code}" -X "$method" "$url" \
      -H "Authorization: Bearer $token" \
      -H "Content-Type: application/json" \
      -d "$data")
  fi
  
  HTTP_CODE=$(echo "$RESPONSE" | tail -1)
  BODY=$(echo "$RESPONSE" | sed '$d')
  
  if [ "$HTTP_CODE" = "403" ] || [ "$HTTP_CODE" = "400" ]; then
    echo -e "${GREEN}✓ 통과 (HTTP $HTTP_CODE)${NC}"
    if echo "$BODY" | grep -q "권한\|작성자\|접근\|토픽\|논증\|댓글"; then
      echo -e "  ${GREEN}  에러 메시지 포함: ✓${NC}"
    else
      echo -e "  ${YELLOW}  에러 메시지 확인 필요${NC}"
    fi
    return 0
  else
    echo -e "${RED}✗ 실패 (HTTP $HTTP_CODE)${NC}"
    echo "  Response: $BODY"
    return 1
  fi
}

# 토픽 수정 권한 테스트
test_unauthorized "PUT" "$BASE_URL/api/debate/topics/$TOPIC_ID" "$OTHER_TOKEN" '{"title":"수정된 제목"}' "토픽 수정 (작성자 아님)"

# 토픽 삭제 권한 테스트
test_unauthorized "DELETE" "$BASE_URL/api/debate/topics/$TOPIC_ID" "$OTHER_TOKEN" "" "토픽 삭제 (작성자 아님)"

# 논증 삭제 권한 테스트
test_unauthorized "DELETE" "$BASE_URL/api/debate/topics/$TOPIC_ID/arguments/$ARGUMENT_ID" "$OTHER_TOKEN" "" "논증 삭제 (작성자 아님)"

# 댓글 삭제 권한 테스트
test_unauthorized "DELETE" "$BASE_URL/api/debate/arguments/$ARGUMENT_ID/replies/$REPLY_ID" "$OTHER_TOKEN" "" "댓글 삭제 (작성자 아님)"

# 게시글 수정 권한 테스트
test_unauthorized "PUT" "$BASE_URL/api/posts/$POST_ID" "$OTHER_TOKEN" '{"title":"수정된 제목"}' "게시글 수정 (작성자 아님)"

# 게시글 삭제 권한 테스트
test_unauthorized "DELETE" "$BASE_URL/api/posts/$POST_ID" "$OTHER_TOKEN" "" "게시글 삭제 (작성자 아님)"

# 게시글 댓글 수정 권한 테스트
test_unauthorized "PUT" "$BASE_URL/api/posts/$POST_ID/replies/$POST_REPLY_ID" "$OTHER_TOKEN" '{"content":"수정된 댓글","contentHtml":"<p>수정된 댓글</p>"}' "게시글 댓글 수정 (작성자 아님)"

# 게시글 댓글 삭제 권한 테스트
test_unauthorized "DELETE" "$BASE_URL/api/posts/$POST_ID/replies/$POST_REPLY_ID" "$OTHER_TOKEN" "" "게시글 댓글 삭제 (작성자 아님)"

# 의의제기 권한 테스트
test_unauthorized "POST" "$BASE_URL/api/debate/arguments/$ARGUMENT_ID/fallacy/appeal" "$OTHER_TOKEN" '{"appealReason":"의의 제기 사유"}' "의의제기 (논증 작성자 아님)"

echo ""
echo "=========================================="
echo "테스트 완료"
echo "=========================================="

