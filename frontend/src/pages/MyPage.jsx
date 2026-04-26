import BookCard from "../components/BookCard";
import { useAuth } from "../App";
import { books } from "../data/books";

const interestProfiles = [
  { label: "인문", description: "사유와 독서법에 가까운 책을 우선 추천합니다.", score: 92 },
  { label: "경제", description: "투자보다 습관과 관점 중심의 경제서를 선호합니다.", score: 78 },
  { label: "자기계발", description: "집중, 루틴, 실행 전략 키워드에 반응이 높습니다.", score: 74 },
];

const recommendationHistory = [
  {
    id: "history-1",
    title: "느리게 읽는 법",
    reason: "최근 인문 분야 저장과 상세 조회가 함께 증가했습니다.",
    source: "인문 관심 기반 추천",
    date: "2026.04.26",
  },
  {
    id: "history-2",
    title: "주의의 설계",
    reason: "집중과 루틴 키워드가 읽은 책 목록과 잘 맞습니다.",
    source: "읽은 책 기반 추천",
    date: "2026.04.25",
  },
  {
    id: "history-3",
    title: "조용한 투자 습관",
    reason: "경제 입문서 중 저장 수 상승률이 높은 책입니다.",
    source: "랭킹 변화 기반 추천",
    date: "2026.04.24",
  },
];

export default function MyPage() {
  const { currentUser } = useAuth();
  const readBooks = [books[0], books[2], books[4]];
  const savedBooks = [books[1], books[3]];

  return (
    <section className="mx-auto w-full max-w-7xl px-5 py-8">
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-[320px_1fr]">
        <aside className="rounded-lg border border-[#E5E7EB] bg-white p-6 shadow-sm">
          <p className="text-sm font-semibold text-[#4CAF50]">마이페이지</p>
          <h1 className="mt-2 text-3xl font-bold leading-tight text-[#1E2A38]">{currentUser.nickname}님의 독서 취향</h1>
          <p className="mt-4 text-sm leading-6 text-[#6B7280]">
            관심 분야, 읽은 책, 추천 히스토리를 바탕으로 지금 읽을 만한 교양서를 정리합니다.
          </p>

          <div className="mt-6 space-y-3 rounded-lg bg-[#F5F3EF] p-4">
            <div>
              <p className="text-xs font-medium text-[#6B7280]">이메일</p>
              <p className="mt-1 text-sm font-semibold text-[#1E2A38]">{currentUser.email}</p>
            </div>
            <div>
              <p className="text-xs font-medium text-[#6B7280]">계정 상태</p>
              <p className="mt-1 text-sm font-semibold text-[#4CAF50]">{currentUser.status}</p>
            </div>
          </div>

          <div className="mt-6 grid grid-cols-3 gap-2">
            {[
              ["관심", interestProfiles.length],
              ["읽은 책", readBooks.length],
              ["추천", recommendationHistory.length],
            ].map(([label, value]) => (
              <div className="rounded-lg border border-[#E5E7EB] p-3 text-center" key={label}>
                <p className="text-xl font-bold text-[#1E2A38]">{value}</p>
                <p className="mt-1 text-xs text-[#6B7280]">{label}</p>
              </div>
            ))}
          </div>
        </aside>

        <div className="space-y-6">
          <section className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
            <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
              <div>
                <p className="text-sm font-semibold text-[#1E2A38]">관심 분야</p>
                <h2 className="mt-2 text-2xl font-bold text-[#1E2A38]">추천에 반영되는 독서 취향</h2>
              </div>
              <span className="rounded-full bg-[#4CAF50]/10 px-3 py-2 text-xs font-semibold text-[#4CAF50]">Mock 데이터</span>
            </div>

            <div className="mt-5 grid grid-cols-1 gap-4 md:grid-cols-3">
              {interestProfiles.map((interest) => (
                <article className="rounded-lg border border-[#E5E7EB] p-4" key={interest.label}>
                  <div className="flex items-center justify-between gap-3">
                    <h3 className="font-bold text-[#1E2A38]">{interest.label}</h3>
                    <span className="text-sm font-bold text-[#F59E0B]">{interest.score}%</span>
                  </div>
                  <div className="mt-3 h-2 rounded-full bg-[#F5F3EF]">
                    <div className="h-2 rounded-full bg-[#4CAF50]" style={{ width: `${interest.score}%` }} />
                  </div>
                  <p className="mt-3 text-sm leading-6 text-[#6B7280]">{interest.description}</p>
                </article>
              ))}
            </div>
          </section>

          <section className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
            <div>
              <p className="text-sm font-semibold text-[#1E2A38]">읽은 책</p>
              <h2 className="mt-2 text-2xl font-bold text-[#1E2A38]">취향 분석에 사용된 책</h2>
            </div>
            <div className="mt-5 grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
              {readBooks.map((book) => (
                <BookCard book={book} key={book.id} />
              ))}
            </div>
          </section>

          <section className="grid grid-cols-1 gap-6 xl:grid-cols-[1fr_360px]">
            <div className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
              <p className="text-sm font-semibold text-[#1E2A38]">추천 히스토리</p>
              <h2 className="mt-2 text-2xl font-bold text-[#1E2A38]">왜 추천됐는지 남기는 기록</h2>
              <ol className="mt-5 divide-y divide-[#E5E7EB]">
                {recommendationHistory.map((history) => (
                  <li className="py-4 first:pt-0 last:pb-0" key={history.id}>
                    <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                      <div>
                        <p className="font-bold text-[#1E2A38]">{history.title}</p>
                        <p className="mt-1 text-sm leading-6 text-[#6B7280]">{history.reason}</p>
                      </div>
                      <span className="shrink-0 rounded-full border border-[#E5E7EB] px-3 py-1 text-xs text-[#6B7280]">{history.date}</span>
                    </div>
                    <p className="mt-2 text-xs font-semibold text-[#4CAF50]">{history.source}</p>
                  </li>
                ))}
              </ol>
            </div>

            <aside className="rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
              <p className="text-sm font-semibold text-[#F59E0B]">저장한 책</p>
              <h2 className="mt-2 text-xl font-bold text-[#1E2A38]">다음에 읽을 후보</h2>
              <div className="mt-5 space-y-4">
                {savedBooks.map((book) => (
                  <div className="border-b border-[#E5E7EB] pb-4 last:border-b-0 last:pb-0" key={book.id}>
                    <p className="font-semibold text-[#1E2A38]">{book.title}</p>
                    <p className="mt-1 text-sm text-[#6B7280]">{book.author}</p>
                    <p className="mt-2 text-xs font-semibold text-[#4CAF50]">{book.reason}</p>
                  </div>
                ))}
              </div>
            </aside>
          </section>
        </div>
      </div>
    </section>
  );
}
