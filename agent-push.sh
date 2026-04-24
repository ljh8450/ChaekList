#!/usr/bin/env bash
set -e

echo "현재 브랜치 확인"
CURRENT_BRANCH=$(git branch --show-current)
echo "👉 브랜치: $CURRENT_BRANCH"

# main 보호 (중요)
if [ "$CURRENT_BRANCH" = "main" ]; then
	echo "❌ main 브랜치에서는 실행할 수 없습니다."
	exit 1
fi

echo "🔍 검증 시작"

# frontend 검증
if [ -d "frontend" ]; then
	echo "👉 frontend 검사"
	cd frontend
	npm run lint
	npm run build
	cd ..
fi

# backend 검증
if [ -d "backend" ]; then
	echo "👉 backend 검사"
	cd backend
	./gradlew build
	cd ..
fi

echo "✅ 검증 통과"

echo "📦 변경사항 확인"
git status

echo "💾 커밋"
git add .
if [ -n "$ISSUE_NUMBER" ]; then
	git commit -m "${TYPE}: #${ISSUE_NUMBER} ${TITLE}"
else
	git commit -m "${TYPE}: ${TITLE}"
fi

echo "🚀 push"
git push

echo "🎯 완료"