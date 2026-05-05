import { useEffect, useState } from "react";
import { useAuth } from "../App";
import SocialPostCard from "../components/SocialPostCard";

const visibilityOptions = [
  ["PRIVATE", "비공개"],
  ["PUBLIC", "공개"],
];

export default function SettingsPage() {
  const { accessToken, logout } = useAuth();
  const [settings, setSettings] = useState(null);
  const [myPosts, setMyPosts] = useState([]);
  const [likedPosts, setLikedPosts] = useState([]);
  const [message, setMessage] = useState("");
  const [withdrawConfirmOpen, setWithdrawConfirmOpen] = useState(false);
  const [withdrawPending, setWithdrawPending] = useState(false);

  useEffect(() => {
    let ignore = false;
    async function loadSettings() {
      const response = await fetch("/api/me/settings", {
        headers: { Authorization: `Bearer ${accessToken}` },
      });
      if (response.status === 401) {
        logout();
        return;
      }
      if (!response.ok) {
        setMessage("설정을 불러오지 못했습니다.");
        return;
      }
      const data = await response.json();
      if (!ignore) {
        setSettings(data);
      }
      loadSocialPosts(() => ignore);
    }
    if (accessToken) {
      loadSettings();
    }
    return () => {
      ignore = true;
    };
  }, [accessToken, logout]);

  async function loadSocialPosts(shouldIgnore = () => false) {
    try {
      const headers = { Authorization: `Bearer ${accessToken}` };
      const [myPostsResponse, likedPostsResponse] = await Promise.all([
        fetch("/api/me/social/posts?limit=10", { headers }),
        fetch("/api/me/social/liked-posts?limit=10", { headers }),
      ]);

      if (myPostsResponse.status === 401 || likedPostsResponse.status === 401) {
        logout();
        return;
      }
      if (!myPostsResponse.ok || !likedPostsResponse.ok) {
        if (!shouldIgnore()) {
          setMessage("작성 글 또는 좋아요한 글을 불러오지 못했습니다.");
        }
        return;
      }

      const [nextMyPosts, nextLikedPosts] = await Promise.all([
        myPostsResponse.json(),
        likedPostsResponse.json(),
      ]);
      if (!shouldIgnore()) {
        setMyPosts(Array.isArray(nextMyPosts) ? nextMyPosts : []);
        setLikedPosts(Array.isArray(nextLikedPosts) ? nextLikedPosts : []);
      }
    } catch {
      if (!shouldIgnore()) {
        setMessage("작성 글 또는 좋아요한 글을 불러오지 못했습니다.");
      }
    }
  }

  async function updatePrivacy(field, value) {
    const nextPrivacy = { ...settings.privacy, [field]: value };
    const response = await fetch("/api/me/privacy-settings", {
      body: JSON.stringify(nextPrivacy),
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      method: "PATCH",
    });
    if (response.ok) {
      setSettings((current) => ({ ...current, privacy: nextPrivacy }));
      setMessage("공개 설정을 저장했습니다.");
    }
  }

  async function updateNotification(field, value) {
    const nextNotifications = { ...settings.notifications, [field]: value };
    const response = await fetch("/api/me/notification-settings", {
      body: JSON.stringify(nextNotifications),
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      method: "PATCH",
    });
    if (response.ok) {
      setSettings((current) => ({ ...current, notifications: nextNotifications }));
      setMessage("알림 설정을 저장했습니다.");
    }
  }

  async function withdraw() {
    if (withdrawPending) {
      return;
    }
    setWithdrawPending(true);
    setMessage("");
    try {
      const response = await fetch("/api/me/withdraw", {
        headers: { Authorization: `Bearer ${accessToken}` },
        method: "POST",
      });
      if (response.ok || response.status === 401) {
        logout();
        return;
      }
      const data = await response.json().catch(() => ({}));
      setMessage(data.message ?? "회원 탈퇴를 처리하지 못했습니다.");
      setWithdrawConfirmOpen(false);
    } catch {
      setMessage("회원 탈퇴를 처리하지 못했습니다.");
      setWithdrawConfirmOpen(false);
    } finally {
      setWithdrawPending(false);
    }
  }

  if (!settings) {
    return (
      <section className="mx-auto w-full max-w-4xl px-5 py-8">
        <div className="rounded-lg border border-[#E5E7EB] bg-white p-5 text-sm text-[#6B7280] shadow-sm">설정을 불러오는 중입니다.</div>
      </section>
    );
  }

  return (
    <section className="mx-auto w-full max-w-4xl px-5 py-8">
      <div className="rounded-lg border border-[#E5E7EB] bg-white p-6 shadow-sm">
        <p className="text-sm font-semibold text-[#4CAF50]">개인정보 설정</p>
        <h1 className="mt-2 text-3xl font-bold text-[#1E2A38]">설정</h1>
        <p className="mt-3 text-sm leading-6 text-[#6B7280]">공개 범위, 알림, 회원 탈퇴를 관리합니다.</p>
      </div>

      {message ? <div className="mt-5 rounded-lg border border-[#E5E7EB] bg-white p-4 text-sm text-[#6B7280]">{message}</div> : null}

      <section className="mt-5 rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
        <h2 className="text-xl font-bold text-[#1E2A38]">공개 설정</h2>
        <div className="mt-5 space-y-4">
          <VisibilityRow label="읽은 책 목록" value={settings.privacy.readBooksVisibility} onChange={(value) => updatePrivacy("readBooksVisibility", value)} />
          <VisibilityRow label="저장한 책 목록" value={settings.privacy.savedBooksVisibility} onChange={(value) => updatePrivacy("savedBooksVisibility", value)} />
          <VisibilityRow label="독서 성장 카드" value={settings.privacy.readingGrowthVisibility} onChange={(value) => updatePrivacy("readingGrowthVisibility", value)} />
          <VisibilityRow label="배지" value={settings.privacy.badgesVisibility} onChange={(value) => updatePrivacy("badgesVisibility", value)} />
          <div className="flex flex-col gap-2 border-t border-[#E5E7EB] pt-4 sm:flex-row sm:items-center sm:justify-between">
            <span className="font-semibold text-[#1E2A38]">관심 분야</span>
            <select
              className="rounded-md border border-[#E5E7EB] px-3 py-2 text-sm"
              value={settings.privacy.interestCategoriesVisibility}
              onChange={(event) => updatePrivacy("interestCategoriesVisibility", event.target.value)}
            >
              <option value="PRIVATE">비공개</option>
              <option value="PARTIAL">일부 공개</option>
              <option value="PUBLIC">공개</option>
            </select>
          </div>
        </div>
      </section>

      <section className="mt-5 rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
        <h2 className="text-xl font-bold text-[#1E2A38]">알림 설정</h2>
        <div className="mt-5 space-y-4">
          <ToggleRow label="좋아요 알림" checked={settings.notifications.likeNotificationsEnabled} onChange={(value) => updateNotification("likeNotificationsEnabled", value)} />
          <ToggleRow label="신고 처리 상태 알림" checked={settings.notifications.reportStatusNotificationsEnabled} onChange={(value) => updateNotification("reportStatusNotificationsEnabled", value)} />
          <ToggleRow label="서비스 알림" checked={settings.notifications.serviceNotificationsEnabled} onChange={(value) => updateNotification("serviceNotificationsEnabled", value)} />
        </div>
      </section>

      <section className="mt-5 rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="text-sm font-semibold text-[#4CAF50]">내 활동</p>
            <h2 className="mt-2 text-xl font-bold text-[#1E2A38]">내가 작성한 글</h2>
          </div>
          <span className="text-sm font-semibold text-[#6B7280]">{myPosts.length}개</span>
        </div>
        <div className="mt-4 space-y-3">
          {myPosts.length === 0 ? (
            <p className="rounded-md border border-[#E5E7EB] bg-[#F9FAFB] p-4 text-sm text-[#6B7280]">작성한 글이 없습니다.</p>
          ) : (
            myPosts.map((post) => (
              <SocialPostCard key={post.id} post={post} compact onChanged={() => loadSocialPosts()} />
            ))
          )}
        </div>
      </section>

      <section className="mt-5 rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="text-sm font-semibold text-[#F59E0B]">내 활동</p>
            <h2 className="mt-2 text-xl font-bold text-[#1E2A38]">좋아요 누른 글</h2>
          </div>
          <span className="text-sm font-semibold text-[#6B7280]">{likedPosts.length}개</span>
        </div>
        <div className="mt-4 space-y-3">
          {likedPosts.length === 0 ? (
            <p className="rounded-md border border-[#E5E7EB] bg-[#F9FAFB] p-4 text-sm text-[#6B7280]">좋아요 누른 글이 없습니다.</p>
          ) : (
            likedPosts.map((post) => (
              <SocialPostCard key={post.id} post={post} compact onChanged={() => loadSocialPosts()} />
            ))
          )}
        </div>
      </section>

      <section className="mt-5 rounded-lg border border-[#FCA5A5] bg-white p-5 shadow-sm">
        <h2 className="text-xl font-bold text-[#991B1B]">회원 탈퇴</h2>
        <p className="mt-3 text-sm leading-6 text-[#6B7280]">
          탈퇴하면 계정은 사용할 수 없고, 기존 공개 게시글은 유지되며 작성자 정보는 익명화됩니다.
        </p>
        <button
          className="mt-4 rounded-md bg-[#991B1B] px-4 py-2 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:bg-[#FCA5A5]"
          type="button"
          disabled={withdrawPending}
          onClick={() => setWithdrawConfirmOpen(true)}
        >
          회원 탈퇴
        </button>
      </section>

      {withdrawConfirmOpen ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-5">
          <div className="w-full max-w-md rounded-lg border border-[#FCA5A5] bg-white p-5 shadow-xl">
            <p className="text-sm font-semibold text-[#991B1B]">회원 탈퇴 확인</p>
            <h2 className="mt-2 text-xl font-bold text-[#1E2A38]">정말 탈퇴하시겠습니까?</h2>
            <div className="mt-4 space-y-2 text-sm leading-6 text-[#6B7280]">
              <p>탈퇴 즉시 이 계정으로 다시 로그인할 수 없습니다.</p>
              <p>기존 공개 게시글은 삭제되지 않고 작성자가 탈퇴한 사용자로 표시됩니다.</p>
            </div>
            <div className="mt-5 flex flex-col gap-2 sm:flex-row sm:justify-end">
              <button
                className="rounded-md border border-[#E5E7EB] px-4 py-2 text-sm font-semibold text-[#1E2A38] disabled:cursor-not-allowed disabled:text-[#9CA3AF]"
                type="button"
                disabled={withdrawPending}
                onClick={() => setWithdrawConfirmOpen(false)}
              >
                취소
              </button>
              <button
                className="rounded-md bg-[#991B1B] px-4 py-2 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:bg-[#FCA5A5]"
                type="button"
                disabled={withdrawPending}
                onClick={withdraw}
              >
                {withdrawPending ? "처리 중" : "탈퇴하기"}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </section>
  );
}

function VisibilityRow({ label, value, onChange }) {
  return (
    <div className="flex flex-col gap-2 border-t border-[#E5E7EB] pt-4 first:border-t-0 first:pt-0 sm:flex-row sm:items-center sm:justify-between">
      <span className="font-semibold text-[#1E2A38]">{label}</span>
      <div className="flex gap-2">
        {visibilityOptions.map(([optionValue, optionLabel]) => (
          <button
            className={`rounded-md px-3 py-2 text-sm font-semibold ${
              value === optionValue ? "bg-[#1E2A38] text-white" : "border border-[#E5E7EB] text-[#6B7280]"
            }`}
            key={optionValue}
            type="button"
            onClick={() => onChange(optionValue)}
          >
            {optionLabel}
          </button>
        ))}
      </div>
    </div>
  );
}

function ToggleRow({ label, checked, onChange }) {
  return (
    <label className="flex items-center justify-between gap-3 border-t border-[#E5E7EB] pt-4 first:border-t-0 first:pt-0">
      <span className="font-semibold text-[#1E2A38]">{label}</span>
      <input className="h-5 w-5 accent-[#1E2A38]" type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} />
    </label>
  );
}
