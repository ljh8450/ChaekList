import { useAuth } from "../App";

export default function MyPage() {
  const { currentUser } = useAuth();

  return (
    <section className="mx-auto w-full max-w-5xl px-5 py-8">
      <div className="rounded-lg border border-[#E5E7EB] bg-white p-6 shadow-sm">
        <p className="text-sm font-semibold text-[#4CAF50]">마이페이지</p>
        <h1 className="mt-2 text-3xl font-bold text-[#1E2A38]">{currentUser.nickname}님의 독서 취향</h1>
        <p className="mt-4 text-[#6B7280]">관심 분야, 읽은 책, 추천 히스토리는 이후 API 연동 단계에서 확장합니다.</p>
        <div className="mt-6 grid grid-cols-1 gap-4 md:grid-cols-3">
          {["관심 분야", "읽은 책", "추천 히스토리"].map((label) => (
            <div className="rounded-lg border border-[#E5E7EB] p-4" key={label}>
              <p className="font-semibold text-[#1E2A38]">{label}</p>
              <p className="mt-2 text-sm text-[#6B7280]">준비 중</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
