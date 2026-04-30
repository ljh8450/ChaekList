import { useEffect, useState } from "react";
import { useAuth } from "../App";

const visibilityOptions = [
  ["PRIVATE", "비공개"],
  ["PUBLIC", "공개"],
];

export default function SettingsPage() {
  const { accessToken, logout } = useAuth();
  const [settings, setSettings] = useState(null);
  const [message, setMessage] = useState("");

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
    }
    if (accessToken) {
      loadSettings();
    }
    return () => {
      ignore = true;
    };
  }, [accessToken, logout]);

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
    const confirmed = window.confirm("탈퇴하면 계정은 비활성화되고 기존 공개 게시글 작성자는 익명화됩니다.");
    if (!confirmed) {
      return;
    }
    const response = await fetch("/api/me/withdraw", {
      headers: { Authorization: `Bearer ${accessToken}` },
      method: "POST",
    });
    if (response.ok) {
      logout();
    } else {
      setMessage("회원 탈퇴를 처리하지 못했습니다.");
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

      <section className="mt-5 rounded-lg border border-[#FCA5A5] bg-white p-5 shadow-sm">
        <h2 className="text-xl font-bold text-[#991B1B]">회원 탈퇴</h2>
        <p className="mt-3 text-sm leading-6 text-[#6B7280]">탈퇴 후 공개 게시글은 유지되며 작성자 정보는 익명화됩니다.</p>
        <button className="mt-4 rounded-md bg-[#991B1B] px-4 py-2 text-sm font-semibold text-white" type="button" onClick={withdraw}>
          회원 탈퇴
        </button>
      </section>
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
