#!/bin/bash

BASE_URL="http://localhost:8080"
TIMESTAMP=$(date +%s)

echo "=========================================="
echo "권한 체크 테스트 시작"
echo "=========================================="
echo ""

echo "=== 1. 사용자 생성 ==="
OWNER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/signup-and-login" \
  -H "Content-Type: application/json" \
  -d "{
    \"loginId\": \"owner$TIMESTAMP\",
    \"password\": \"Passw0rd!\",
    \"name\": \"Owner User\",
    \"phoneNumber\": \"01011111111\",
    \"email\": \"owner$TIMESTAMP@test.com\",
    \"birth\": \"1990-01-01\",
    \"nickname\": \"owner$TIMESTAMP\",
    \"sex\": \"M\",
    \"ideology\": \"M\",
    \"profile\": \"https://example.com/profile.png\"
  }")

OWNER_TOKEN=$(echo "$OWNER_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('accessToken', ''))" 2>/dev/null)
OWNER_ID=$(echo "$OWNER_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('userId', ''))" 2>/dev/null)

if [ -z "$OWNER_TOKEN" ] || [ "$OWNER_TOKEN" == "null" ]; then
  echo "❌ Owner 사용자 생성 실패"
  exit 1
fi
echo "✅ Owner 사용자 생성 성공 (ID: $OWNER_ID)"

OTHER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/signup-and-login" \
  -H "Content-Type: application/json" \
  -d "{
    \"loginId\": \"other$TIMESTAMP\",
    \"password\": \"Passw0rd!\",
    \"name\": \"Other User\",
    \"phoneNumber\": \"01022222222\",
    \"email\": \"other$TIMESTAMP@test.com\",
    \"birth\": \"1990-01-01\",
    \"nickname\": \"other$TIMESTAMP\",
    \"sex\": \"M\",
    \"ideology\": \"M\",
    \"profile\": \"https://example.com/profile.png\"
  }")

OTHER_TOKEN=$(echo "$OTHER_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('accessToken', ''))" 2>/dev/null)
OTHER_ID=$(echo "$OTHER_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('userId', ''))" 2>/dev/null)

if [ -z "$OTHER_TOKEN" ] || [ "$OTHER_TOKEN" == "null" ]; then
  echo "❌ Other 사용자 생성 실패"
  exit 1
fi
echo "✅ Other 사용자 생성 성공 (ID: $OTHER_ID)"
echo ""

echo "=== 2. 토픽 생성 (Owner) ==="
TOPIC_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST "$BASE_URL/api/debate/topics" \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"권한 테스트 토픽\",
    \"description\": \"테스트 설명\",
    \"descriptionHtml\": \"<p>테스트 설명</p>\",
    \"category\": \"POLITICS\",
    \"endDate\": \"2025-12-31\"
  }")

TOPIC_HTTP=$(echo "$TOPIC_RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)
TOPIC_BODY=$(echo "$TOPIC_RESPONSE" | grep -v "HTTP_CODE")
TOPIC_ID=$(echo "$TOPIC_BODY" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null)

if [ "$TOPIC_HTTP" == "201" ] && [ ! -z "$TOPIC_ID" ]; then
  echo "✅ 토픽 생성 성공 (ID: $TOPIC_ID)"
else
  echo "❌ 토픽 생성 실패 (HTTP: $TOPIC_HTTP)"
  exit 1
fi
echo ""

echo "=== 3. 토픽 수정 테스트 ==="
echo "3-1. Owner가 자신의 토픽 수정 (성공 예상)"
UPDATE_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X PUT "$BASE_URL/api/debate/topics/$TOPIC_ID" \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"title\": \"수정된 제목\"}")

UPDATE_HTTP=$(echo "$UPDATE_RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)
if [ "$UPDATE_HTTP" == "200" ]; then
  echo "✅ Owner 토픽 수정 성공"
else
  echo "❌ Owner 토픽 수정 실패 (HTTP: $UPDATE_HTTP)"
fi

echo "3-2. Other가 Owner의 토픽 수정 시도 (실패 예상)"
UPDATE_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X PUT "$BASE_URL/api/debate/topics/$TOPIC_ID" \
  -H "Authorization: Bearer $OTHER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"title\": \"해킹 시도\"}")

UPDATE_HTTP=$(echo "$UPDATE_RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)
if [ "$UPDATE_HTTP" == "403" ]; then
  echo "✅ Other 토픽 수정 차단 성공 (HTTP: $UPDATE_HTTP)"
else
  echo "⚠️ Other 토픽 수정 차단 실패 (HTTP: $UPDATE_HTTP) - 예상: 403"
fi
echo ""

echo "=== 4. 토픽 삭제 테스트 ==="
NEW_TOPIC_RESPONSE=$(curl -s -X POST "$BASE_URL/api/debate/topics" \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"삭제 테스트 토픽\",
    \"description\": \"테스트\",
    \"descriptionHtml\": \"<p>테스트</p>\",
    \"category\": \"POLITICS\",
    \"endDate\": \"2025-12-31\"
  }")

NEW_TOPIC_ID=$(echo "$NEW_TOPIC_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null)

echo "4-1. Owner가 자신의 토픽 삭제 (성공 예상)"
DELETE_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X DELETE "$BASE_URL/api/debate/topics/$NEW_TOPIC_ID" \
  -H "Authorization: Bearer $OWNER_TOKEN")

DELETE_HTTP=$(echo "$DELETE_RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)
if [ "$DELETE_HTTP" == "204" ]; then
  echo "✅ Owner 토픽 삭제 성공"
else
  echo "❌ Owner 토픽 삭제 실패 (HTTP: $DELETE_HTTP)"
fi

ANOTHER_TOPIC_RESPONSE=$(curl -s -X POST "$BASE_URL/api/debate/topics" \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"삭제 차단 테스트 토픽\",
    \"description\": \"테스트\",
    \"descriptionHtml\": \"<p>테스트</p>\",
    \"category\": \"POLITICS\",
    \"endDate\": \"2025-12-31\"
  }")

ANOTHER_TOPIC_ID=$(echo "$ANOTHER_TOPIC_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null)

echo "4-2. Other가 Owner의 토픽 삭제 시도 (실패 예상)"
DELETE_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X DELETE "$BASE_URL/api/debate/topics/$ANOTHER_TOPIC_ID" \
  -H "Authorization: Bearer $OTHER_TOKEN")

DELETE_HTTP=$(echo "$DELETE_RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)
if [ "$DELETE_HTTP" == "403" ]; then
  echo "✅ Other 토픽 삭제 차단 성공 (HTTP: $DELETE_HTTP)"
else
  echo "⚠️ Other 토픽 삭제 차단 실패 (HTTP: $DELETE_HTTP) - 예상: 403"
fi
echo ""

echo "=== 5. 논증 삭제 테스트 ==="
ARGUMENT_RESPONSE=$(curl -s -X POST "$BASE_URL/api/debate/topics/$TOPIC_ID/arguments" \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"테스트 논증\",
    \"content\": \"테스트 내용\",
    \"contentHtml\": \"<p>테스트 내용</p>\",
    \"stance\": \"PRO\",
    \"authorNickname\": \"테스터\"
  }")

ARGUMENT_ID=$(echo "$ARGUMENT_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null)

echo "5-1. Owner가 자신의 논증 삭제 (성공 예상)"
DELETE_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X DELETE "$BASE_URL/api/debate/topics/$TOPIC_ID/arguments/$ARGUMENT_ID" \
  -H "Authorization: Bearer $OWNER_TOKEN")

DELETE_HTTP=$(echo "$DELETE_RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)
if [ "$DELETE_HTTP" == "204" ]; then
  echo "✅ Owner 논증 삭제 성공"
else
  echo "❌ Owner 논증 삭제 실패 (HTTP: $DELETE_HTTP)"
fi

ANOTHER_ARGUMENT_RESPONSE=$(curl -s -X POST "$BASE_URL/api/debate/topics/$TOPIC_ID/arguments" \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"차단 테스트 논증\",
    \"content\": \"테스트 내용\",
    \"contentHtml\": \"<p>테스트 내용</p>\",
    \"stance\": \"PRO\",
    \"authorNickname\": \"테스터\"
  }")

ANOTHER_ARGUMENT_ID=$(echo "$ANOTHER_ARGUMENT_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null)

echo "5-2. Other가 Owner의 논증 삭제 시도 (실패 예상)"
DELETE_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X DELETE "$BASE_URL/api/debate/topics/$TOPIC_ID/arguments/$ANOTHER_ARGUMENT_ID" \
  -H "Authorization: Bearer $OTHER_TOKEN")

DELETE_HTTP=$(echo "$DELETE_RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)
if [ "$DELETE_HTTP" == "403" ]; then
  echo "✅ Other 논증 삭제 차단 성공 (HTTP: $DELETE_HTTP)"
else
  echo "⚠️ Other 논증 삭제 차단 실패 (HTTP: $DELETE_HTTP) - 예상: 403"
fi
echo ""

echo "=== 6. 게시글 수정 테스트 ==="
POST_RESPONSE=$(curl -s -X POST "$BASE_URL/api/posts?boardId=1" \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"권한 테스트 게시글\",
    \"content\": \"테스트 내용\",
    \"contentHtml\": \"<p>테스트 내용</p>\"
  }")

POST_ID=$(echo "$POST_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null)

echo "6-1. Owner가 자신의 게시글 수정 (성공 예상)"
UPDATE_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X PUT "$BASE_URL/api/posts/$POST_ID" \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"title\": \"수정된 제목\"}")

UPDATE_HTTP=$(echo "$UPDATE_RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)
if [ "$UPDATE_HTTP" == "200" ]; then
  echo "✅ Owner 게시글 수정 성공"
else
  echo "❌ Owner 게시글 수정 실패 (HTTP: $UPDATE_HTTP)"
fi

echo "6-2. Other가 Owner의 게시글 수정 시도 (실패 예상)"
UPDATE_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X PUT "$BASE_URL/api/posts/$POST_ID" \
  -H "Authorization: Bearer $OTHER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"title\": \"해킹 시도\"}")

UPDATE_HTTP=$(echo "$UPDATE_RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)
if [ "$UPDATE_HTTP" == "403" ]; then
  echo "✅ Other 게시글 수정 차단 성공 (HTTP: $UPDATE_HTTP)"
else
  echo "⚠️ Other 게시글 수정 차단 실패 (HTTP: $UPDATE_HTTP) - 예상: 403"
fi
echo ""

echo "=== 7. 의의 제기 테스트 ==="
APPEAL_ARGUMENT_RESPONSE=$(curl -s -X POST "$BASE_URL/api/debate/topics/$TOPIC_ID/arguments" \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"의의 제기 테스트 논증\",
    \"content\": \"테스트 내용\",
    \"contentHtml\": \"<p>테스트 내용</p>\",
    \"stance\": \"PRO\",
    \"authorNickname\": \"테스터\"
  }")

APPEAL_ARGUMENT_ID=$(echo "$APPEAL_ARGUMENT_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null)

sleep 3

echo "7-1. Owner가 자신의 논증에 의의 제기 (성공 예상)"
APPEAL_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST "$BASE_URL/api/debate/arguments/$APPEAL_ARGUMENT_ID/fallacy/appeal" \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"appealReason\": \"의의 제기 사유\"}")

APPEAL_HTTP=$(echo "$APPEAL_RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)
if [ "$APPEAL_HTTP" == "201" ]; then
  echo "✅ Owner 의의 제기 성공"
else
  echo "❌ Owner 의의 제기 실패 (HTTP: $APPEAL_HTTP)"
fi

echo "7-2. Other가 Owner의 논증에 의의 제기 시도 (실패 예상)"
APPEAL_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST "$BASE_URL/api/debate/arguments/$APPEAL_ARGUMENT_ID/fallacy/appeal" \
  -H "Authorization: Bearer $OTHER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"appealReason\": \"의의 제기 사유\"}")

APPEAL_HTTP=$(echo "$APPEAL_RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)
if [ "$APPEAL_HTTP" == "403" ]; then
  echo "✅ Other 의의 제기 차단 성공 (HTTP: $APPEAL_HTTP)"
else
  echo "⚠️ Other 의의 제기 차단 실패 (HTTP: $APPEAL_HTTP) - 예상: 403"
fi
echo ""

echo "=========================================="
echo "권한 체크 테스트 완료"
echo "=========================================="


