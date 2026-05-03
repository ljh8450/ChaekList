import { useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../App";

const initialForm = {
  email: "",
  nickname: "",
  password: "",
  passwordConfirm: "",
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

function createAuthSession(data, fallbackUser) {
  return {
    accessToken: data.accessToken ?? "",
    refreshToken: data.refreshToken ?? "",
    tokenType: data.tokenType ?? "Bearer",
    user: {
      id: data.id ?? fallbackUser.id,
      email: data.email ?? fallbackUser.email,
      nickname: data.nickname ?? fallbackUser.nickname,
      status: data.status ?? "ACTIVE",
      role: data.role ?? fallbackUser.role ?? "USER",
    },
  };
}

async function resolvePostAuthPath(accessToken, nextPath) {
  if (!accessToken) {
    return nextPath;
  }

  try {
    const response = await fetch("/api/me/onboarding-status", {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });

    if (!response.ok) {
      return nextPath;
    }

    const status = await response.json();
    if (!status.completed) {
      return "/onboarding";
    }
  } catch {
    return nextPath;
  }

  return nextPath;
}

export default function AuthForm({ mode }) {
  const { demoUser, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState(initialForm);
  const [fieldError, setFieldError] = useState("");
  const [serverMessage, setServerMessage] = useState("");
  const [serverError, setServerError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const isSignup = mode === "signup";
  const title = isSignup ? "회원가입" : "로그인";
  const endpoint = isSignup ? "/api/auth/signup" : "/api/auth/login";
  const nextPath = location.state?.from?.pathname ?? "/";

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

  function fillDemo() {
    setForm({
      email: demoUser.email,
      nickname: demoUser.nickname,
      password: "chaeklist123",
      passwordConfirm: "chaeklist123",
    });
    setFieldError("");
    setServerError("");
    setServerMessage("데모 계정 정보가 입력되었습니다.");
  }

  function canUseDemoLogin() {
    return form.email.trim().toLowerCase() === demoUser.email && form.password === "chaeklist123";
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
        body: JSON.stringify(payload),
        headers: {
          "Content-Type": "application/json",
        },
        method: "POST",
      });

      const data = await response.json().catch(() => ({}));

      if (!response.ok) {
        throw new Error(data.message ?? "요청을 처리하지 못했습니다.");
      }

      const session = createAuthSession(data, {
        ...demoUser,
        email: payload.email,
        nickname: form.nickname.trim() || demoUser.nickname,
      });
      login(session);
      navigate(await resolvePostAuthPath(session.accessToken, nextPath), { replace: true });
    } catch (error) {
      if (!isSignup && canUseDemoLogin()) {
        login({
          accessToken: "",
          refreshToken: "",
          tokenType: "Bearer",
          user: demoUser,
        });
        navigate(nextPath, { replace: true });
        return;
      }

      setServerError(error instanceof Error ? error.message : "알 수 없는 오류가 발생했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className="mx-auto grid w-full max-w-6xl grid-cols-1 gap-8 px-5 py-10 lg:grid-cols-[1fr_420px] lg:py-16">
      <div className="flex flex-col justify-center">
        <p className="text-sm font-semibold text-[#4CAF50]">개인화 경험</p>
        <h1 className="mt-4 max-w-2xl text-3xl font-bold leading-tight text-[#1E2A38] sm:text-4xl">
          로그인하면 오늘의 추천이 더 구체적으로 바뀝니다
        </h1>
        <p className="mt-5 max-w-xl text-base leading-7 text-[#6B7280]">
          계정이 있으면 관심 분야와 저장한 책을 바탕으로 추천 이유를 함께 제공합니다.
        </p>
      </div>

      <div className="rounded-lg border border-[#E5E7EB] bg-white p-6 shadow-sm sm:p-8">
        <div className="grid grid-cols-2 rounded-md bg-[#F5F3EF] p-1">
          <Link
            className={`rounded px-3 py-2 text-center text-sm font-semibold transition ${
              !isSignup ? "bg-[#1E2A38] text-white" : "text-[#6B7280] hover:text-[#1E2A38]"
            }`}
            to="/login"
          >
            로그인
          </Link>
          <Link
            className={`rounded px-3 py-2 text-center text-sm font-semibold transition ${
              isSignup ? "bg-[#1E2A38] text-white" : "text-[#6B7280] hover:text-[#1E2A38]"
            }`}
            to="/signup"
          >
            회원가입
          </Link>
        </div>

        <div className="mt-7">
          <p className="text-sm font-medium text-[#6B7280]">ChaekList 계정</p>
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
          <button
            className="w-full rounded-md border border-[#E5E7EB] px-4 py-3 text-sm font-semibold text-[#1E2A38] transition hover:border-[#1E2A38]"
            type="button"
            onClick={fillDemo}
          >
            데모 계정 입력
          </button>
        </form>
      </div>
    </section>
  );
}
