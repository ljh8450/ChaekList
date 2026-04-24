import { useMemo, useState } from "react";

const initialForm = {
  email: "",
  nickname: "",
  password: "",
  passwordConfirm: "",
};

const demoUser = {
  id: 1,
  email: "reader@readpick.kr",
  nickname: "quiet-reader",
  status: "ACTIVE",
};

function validateForm(mode, form) {
  if (!form.email.trim()) {
    return "이메일을 입력해 주세요.";
  }

  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    return "올바른 이메일 형식이 아닙니다.";
  }

  if (mode === "signup" && !form.nickname.trim()) {
    return "닉네임을 입력해 주세요.";
  }

  if (mode === "signup" && form.nickname.trim().length > 50) {
    return "닉네임은 50자 이하로 입력해 주세요.";
  }

  if (form.password.length < 8) {
    return "비밀번호는 8자 이상이어야 합니다.";
  }

  if (mode === "signup" && form.password !== form.passwordConfirm) {
    return "비밀번호 확인이 일치하지 않습니다.";
  }

  return "";
}

export default function App() {
  const [mode, setMode] = useState("login");
  const [form, setForm] = useState(initialForm);
  const [fieldError, setFieldError] = useState("");
  const [serverMessage, setServerMessage] = useState("");
  const [serverError, setServerError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [currentUser, setCurrentUser] = useState(null);

  const isSignup = mode === "signup";
  const title = isSignup ? "회원가입" : "로그인";
  const endpoint = isSignup ? "/api/auth/signup" : "/api/auth/login";

  const payload = useMemo(() => {
    if (isSignup) {
      return {
        email: form.email.trim(),
        nickname: form.nickname.trim(),
        password: form.password,
      };
    }

    return {
      email: form.email.trim(),
      password: form.password,
    };
  }, [form, isSignup]);

  function updateField(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
    setFieldError("");
    setServerError("");
    setServerMessage("");
  }

  function changeMode(nextMode) {
    setMode(nextMode);
    setForm(initialForm);
    setFieldError("");
    setServerError("");
    setServerMessage("");
    setCurrentUser(null);
  }

  async function submitAuth(event) {
    event.preventDefault();

    const validationMessage = validateForm(mode, form);
    if (validationMessage) {
      setFieldError(validationMessage);
      return;
    }

    setIsSubmitting(true);
    setServerError("");
    setServerMessage("");

    try {
      const response = await fetch(endpoint, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });

      const data = await response.json().catch(() => ({}));

      if (!response.ok) {
        throw new Error(data.message ?? "요청을 처리하지 못했습니다.");
      }

      setCurrentUser({
        id: data.id ?? demoUser.id,
        email: data.email ?? payload.email,
        nickname: data.nickname ?? form.nickname.trim() ?? demoUser.nickname,
        status: data.status ?? "ACTIVE",
      });
      setServerMessage(isSignup ? "가입이 완료되어 바로 로그인되었습니다." : "로그인되었습니다.");
    } catch (error) {
      setServerError(error instanceof Error ? error.message : "알 수 없는 오류가 발생했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  }

  function fillDemo() {
    setForm({
      email: demoUser.email,
      nickname: demoUser.nickname,
      password: "readpick123",
      passwordConfirm: "readpick123",
    });
    setFieldError("");
    setServerError("");
    setServerMessage("데모 계정 정보가 입력되었습니다.");
  }

  return (
    <main className="min-h-screen bg-[#F5F3EF] text-[#111827]">
      <div className="mx-auto grid min-h-screen w-full max-w-6xl grid-cols-1 lg:grid-cols-[1fr_420px]">
        <section className="flex flex-col justify-between px-6 py-8 sm:px-10 lg:px-12">
          <header className="flex items-center justify-between">
            <div>
              <p className="text-sm font-semibold text-[#1E2A38]">ReadPick</p>
              <p className="mt-1 text-xs text-[#6B7280]">교양 독서를 위한 조용한 추천 서비스</p>
            </div>
            <button
              className="rounded-md border border-[#E5E7EB] bg-white px-3 py-2 text-sm font-medium text-[#1E2A38] shadow-sm transition hover:border-[#1E2A38]"
              type="button"
              onClick={fillDemo}
            >
              데모 입력
            </button>
          </header>

          <div className="my-14 max-w-2xl lg:my-0">
            <p className="text-sm font-semibold text-[#4CAF50]">개인화 추천 준비</p>
            <h1 className="mt-4 text-4xl font-bold leading-tight text-[#1E2A38] sm:text-5xl">
              읽을 만한 책을 차분하게 고르는 시간
            </h1>
            <p className="mt-5 max-w-xl text-base leading-7 text-[#6B7280]">
              로그인하면 관심 카테고리, 저장한 책, 최근 읽은 기록을 바탕으로 더 구체적인 추천 이유를
              보여줍니다.
            </p>

            <div className="mt-10 grid max-w-2xl grid-cols-1 gap-3 sm:grid-cols-3">
              {["추천 이유", "교양 필터", "랭킹 변화"].map((label) => (
                <div key={label} className="rounded-lg border border-[#E5E7EB] bg-white/70 p-4">
                  <p className="text-sm font-semibold text-[#1E2A38]">{label}</p>
                  <p className="mt-2 text-xs leading-5 text-[#6B7280]">
                    과장 없이 필요한 정보만 정리합니다.
                  </p>
                </div>
              ))}
            </div>
          </div>

          <p className="text-xs text-[#6B7280]">
            DDL 기준 사용자 필드: email, nickname, password_hash, status
          </p>
        </section>

        <section className="flex items-center px-6 pb-8 sm:px-10 lg:px-0 lg:pr-12">
          <div className="w-full rounded-lg border border-[#E5E7EB] bg-white p-6 shadow-sm sm:p-8">
            <div className="grid grid-cols-2 rounded-md bg-[#F5F3EF] p-1">
              <button
                className={`rounded px-3 py-2 text-sm font-semibold transition ${
                  !isSignup ? "bg-[#1E2A38] text-white" : "text-[#6B7280] hover:text-[#1E2A38]"
                }`}
                type="button"
                onClick={() => changeMode("login")}
              >
                로그인
              </button>
              <button
                className={`rounded px-3 py-2 text-sm font-semibold transition ${
                  isSignup ? "bg-[#1E2A38] text-white" : "text-[#6B7280] hover:text-[#1E2A38]"
                }`}
                type="button"
                onClick={() => changeMode("signup")}
              >
                회원가입
              </button>
            </div>

            <div className="mt-7">
              <p className="text-sm font-medium text-[#6B7280]">ReadPick 계정</p>
              <h2 className="mt-2 text-2xl font-bold text-[#1E2A38]">{title}</h2>
            </div>

            <form className="mt-6 space-y-4" onSubmit={submitAuth}>
              <label className="block">
                <span className="text-sm font-medium text-[#1E2A38]">이메일</span>
                <input
                  autoComplete="email"
                  className="mt-2 w-full rounded-md border border-[#E5E7EB] px-3 py-3 text-sm outline-none transition focus:border-[#1E2A38] focus:ring-2 focus:ring-[#1E2A38]/10"
                  name="email"
                  placeholder="reader@example.com"
                  type="email"
                  value={form.email}
                  onChange={updateField}
                />
              </label>

              {isSignup ? (
                <label className="block">
                  <span className="text-sm font-medium text-[#1E2A38]">닉네임</span>
                  <input
                    autoComplete="nickname"
                    className="mt-2 w-full rounded-md border border-[#E5E7EB] px-3 py-3 text-sm outline-none transition focus:border-[#1E2A38] focus:ring-2 focus:ring-[#1E2A38]/10"
                    maxLength={50}
                    name="nickname"
                    placeholder="quiet-reader"
                    type="text"
                    value={form.nickname}
                    onChange={updateField}
                  />
                </label>
              ) : null}

              <label className="block">
                <span className="text-sm font-medium text-[#1E2A38]">비밀번호</span>
                <input
                  autoComplete={isSignup ? "new-password" : "current-password"}
                  className="mt-2 w-full rounded-md border border-[#E5E7EB] px-3 py-3 text-sm outline-none transition focus:border-[#1E2A38] focus:ring-2 focus:ring-[#1E2A38]/10"
                  name="password"
                  placeholder="8자 이상"
                  type="password"
                  value={form.password}
                  onChange={updateField}
                />
              </label>

              {isSignup ? (
                <label className="block">
                  <span className="text-sm font-medium text-[#1E2A38]">비밀번호 확인</span>
                  <input
                    autoComplete="new-password"
                    className="mt-2 w-full rounded-md border border-[#E5E7EB] px-3 py-3 text-sm outline-none transition focus:border-[#1E2A38] focus:ring-2 focus:ring-[#1E2A38]/10"
                    name="passwordConfirm"
                    placeholder="비밀번호를 한 번 더 입력"
                    type="password"
                    value={form.passwordConfirm}
                    onChange={updateField}
                  />
                </label>
              ) : null}

              {fieldError ? <p className="text-sm text-[#B45309]">{fieldError}</p> : null}
              {serverError ? <p className="text-sm text-red-600">{serverError}</p> : null}
              {serverMessage ? <p className="text-sm text-[#4CAF50]">{serverMessage}</p> : null}

              <button
                className="w-full rounded-md bg-[#1E2A38] px-4 py-3 text-sm font-semibold text-white transition hover:bg-[#27384a] disabled:cursor-not-allowed disabled:bg-[#6B7280]"
                disabled={isSubmitting}
                type="submit"
              >
                {isSubmitting ? "처리 중..." : title}
              </button>
            </form>

            {currentUser ? (
              <div className="mt-6 rounded-md border border-[#4CAF50]/30 bg-[#4CAF50]/10 p-4">
                <p className="text-sm font-semibold text-[#1E2A38]">{currentUser.nickname}</p>
                <p className="mt-1 text-xs text-[#6B7280]">{currentUser.email}</p>
                <p className="mt-2 text-xs font-medium text-[#4CAF50]">상태: {currentUser.status}</p>
              </div>
            ) : null}
          </div>
        </section>
      </div>
    </main>
  );
}
