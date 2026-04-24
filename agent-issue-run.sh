#!/usr/bin/env bash
# 사용 예시: bash agent-issue-run.sh feature "검색 API 구현" "도서 검색어를 받아 관련 도서 목록을 반환하는 API를 구현한다."

set -e

TYPE="$1"
TITLE="$2"
BODY="$3"

if [ -z "$TYPE" ] || [ -z "$TITLE" ] || [ -z "$BODY" ]; then
	echo "사용법: bash agent-issue-run.sh feature \"이슈 제목\" \"이슈 내용\""
	exit 1
fi

echo "1. main 최신화"
git checkout main
git pull origin main

echo "2. GitHub 이슈 생성"
ISSUE_URL=$(gh issue create \
	--title "$TITLE" \
	--body "$BODY" \
	--label "$TYPE")

ISSUE_NUMBER=$(echo "$ISSUE_URL" | grep -o '[0-9]*$')

echo "생성된 이슈: #$ISSUE_NUMBER"
echo "$ISSUE_URL"

BRANCH_NAME="agent/${TYPE}/issue-${ISSUE_NUMBER}"

echo "3. 브랜치 생성: $BRANCH_NAME"
git checkout -b "$BRANCH_NAME"

echo "4. Codex 실행"
codex "Issue #$ISSUE_NUMBER 작업을 수행해줘.

제목:
$TITLE

내용:
$BODY

규칙:
- AGENTS.md를 반드시 따른다.
- 관련 없는 파일은 수정하지 않는다.
- 작업 완료 후 변경 내용을 요약한다."

echo "5. 검증 실행"

if [ -d "frontend" ]; then
	echo "frontend 검증"
	cd frontend
	npm run lint
	npm run build
	cd ..
fi

if [ -d "backend" ]; then
	echo "backend 검증"
	cd backend
	./gradlew build
	cd ..
fi

echo "6. 변경사항 확인"
git status

echo "7. 커밋"
git add .
git commit -m "${TYPE}: #${ISSUE_NUMBER} ${TITLE}"

echo "8. push"
git push -u origin "$BRANCH_NAME"

echo "9. PR 생성"
gh pr create \
	--base main \
	--head "$BRANCH_NAME" \
	--title "${TYPE}: #${ISSUE_NUMBER} ${TITLE}" \
	--body "## 작업 내용

Closes #${ISSUE_NUMBER}

$BODY

## 검증
- frontend: npm run lint && npm run build
- backend: ./gradlew build

## 참고
- Codex를 통해 작업된 PR입니다.
- 반드시 리뷰 후 merge 해주세요."

echo "완료"