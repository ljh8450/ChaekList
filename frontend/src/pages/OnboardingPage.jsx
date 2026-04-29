import { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../App";
import BookSearchPanel from "../components/BookSearchPanel";

function normalizeCategory(category) {
  return {
    id: category.id ?? category.categoryId ?? category.name ?? category.label,
    name: category.name ?? category.label ?? category.title ?? "분야",
    description: category.description ?? "관심 분야를 선택하면 추천 이유가 더 구체적으로 바뀝니다.",
  };
}

function normalizeBook(book) {
  return {
    id: book.id ?? book.bookId,
    title: book.title ?? "제목 없음",
    author: book.author ?? "저자 미상",
    category: book.category ?? book.categoryName ?? "교양",
    reason: book.reason ?? book.recommendationReason ?? "최근 읽은 책과 관심 분야를 함께 반영합니다.",
  };
}

function normalizeReadingPurpose(purpose) {
  return {
    code: purpose.code ?? purpose.purposeCode,
    label: purpose.label ?? purpose.name ?? "독서 목적",
    description: purpose.description ?? "선택한 독서 목적을 추천 이유에 반영합니다.",
  };
}

function getInitialCategoryIds(options, myPage) {
  const interestIds = new Set((myPage?.interests ?? []).map((interest) => String(interest.id)));
  return options.filter((category) => interestIds.has(String(category.id))).map((category) => category.id);
}

function getInitialReadingPurposeCodes(options, myPage) {
  const purposeCodes = new Set((myPage?.readingPurposes ?? []).map((purpose) => String(purpose.code)));
  return options.filter((purpose) => purposeCodes.has(String(purpose.code))).map((purpose) => purpose.code);
}

function getInitialBookIds(options, myPage) {
  const readBookIds = new Set((myPage?.readBooks ?? []).map((book) => String(book.id)));
  return options.filter((book) => readBookIds.has(String(book.id))).map((book) => book.id);
}

function mergeBooks(currentBooks, nextBooks) {
  const booksById = new Map(currentBooks.map((book) => [String(book.id), book]));
  nextBooks.forEach((book) => {
    if (!booksById.has(String(book.id))) {
      booksById.set(String(book.id), normalizeBook(book));
    }
  });
  return Array.from(booksById.values());
}

export default function OnboardingPage() {
  const { accessToken, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [availableCategories, setAvailableCategories] = useState([]);
  const [availableBooks, setAvailableBooks] = useState([]);
  const [availableReadingPurposes, setAvailableReadingPurposes] = useState([]);
  const [selectedCategoryIds, setSelectedCategoryIds] = useState([]);
  const [selectedBookIds, setSelectedBookIds] = useState([]);
  const [selectedReadingPurposeCodes, setSelectedReadingPurposeCodes] = useState([]);
  const [isEditMode, setIsEditMode] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [message, setMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const [canSave, setCanSave] = useState(false);

  const canSubmit =
    canSave &&
    selectedCategoryIds.length > 0 &&
    selectedReadingPurposeCodes.length > 0 &&
    selectedReadingPurposeCodes.length <= 3 &&
    selectedBookIds.length > 0 &&
    !isSubmitting;
  const focusedSection = new URLSearchParams(location.search).get("section");
  const isInterestFocused = focusedSection === "interests";
  const isPurposeFocused = focusedSection === "reading-purposes";
  const isReadBooksFocused = focusedSection === "read-books";

  const selectedSummary = useMemo(
    () => ({
      categories: availableCategories.filter((category) => selectedCategoryIds.includes(category.id)),
      readingPurposes: availableReadingPurposes.filter((purpose) => selectedReadingPurposeCodes.includes(purpose.code)),
      books: availableBooks.filter((book) => selectedBookIds.includes(book.id)),
    }),
    [availableBooks, availableCategories, availableReadingPurposes, selectedBookIds, selectedCategoryIds, selectedReadingPurposeCodes],
  );

  useEffect(() => {
    let ignore = false;

    async function loadOnboarding() {
      setIsLoading(true);
      setErrorMessage("");
      setMessage("");
      setCanSave(false);

      try {
        if (!accessToken) {
          return;
        }

        const headers = {
          Authorization: `Bearer ${accessToken}`,
        };

        const statusResponse = await fetch("/api/me/onboarding-status", { headers });

        if (statusResponse.status === 401) {
          logout();
          return;
        }

        if (statusResponse.ok) {
          await statusResponse.json();
        }

        const optionsResponse = await fetch("/api/me/onboarding-options", { headers });

        if (!optionsResponse.ok) {
          throw new Error("온보딩 선택지를 불러오지 못했습니다.");
        }

        const options = await optionsResponse.json();
        const nextCategories = (options.categories ?? options.interestCategories ?? []).map(normalizeCategory).filter((category) => category.id);
        const nextBooks = (options.books ?? options.readableBooks ?? []).map(normalizeBook).filter((book) => book.id);
        const nextReadingPurposes = (options.readingPurposes ?? []).map(normalizeReadingPurpose).filter((purpose) => purpose.code);
        const myPageResponse = await fetch("/api/me/mypage", { headers });
        const myPage = myPageResponse.ok ? await myPageResponse.json() : null;
        const initialCategoryIds = getInitialCategoryIds(nextCategories, myPage);
        const initialReadingPurposeCodes = getInitialReadingPurposeCodes(nextReadingPurposes, myPage);
        const initialBookIds = getInitialBookIds(nextBooks, myPage);

        if (!ignore) {
          setAvailableCategories(nextCategories);
          setAvailableBooks(nextBooks);
          setAvailableReadingPurposes(nextReadingPurposes);
          setSelectedCategoryIds(initialCategoryIds);
          setSelectedReadingPurposeCodes(initialReadingPurposeCodes);
          setSelectedBookIds(initialBookIds);
          setIsEditMode(initialCategoryIds.length > 0 || initialReadingPurposeCodes.length > 0 || initialBookIds.length > 0);
          setCanSave(nextCategories.length > 0 && nextReadingPurposes.length > 0);
          if (nextCategories.length === 0 || nextReadingPurposes.length === 0) {
            setMessage("온보딩 선택지가 아직 준비되지 않았습니다. 잠시 후 다시 시도해 주세요.");
          }
        }
      } catch {
        if (!ignore) {
          setAvailableCategories([]);
          setAvailableBooks([]);
          setAvailableReadingPurposes([]);
          setIsEditMode(false);
          setCanSave(false);
          setMessage("온보딩 선택지를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }
      } finally {
        if (!ignore) {
          setIsLoading(false);
        }
      }
    }

    loadOnboarding();

    return () => {
      ignore = true;
    };
  }, [accessToken, logout, navigate]);

  function toggleCategory(categoryId) {
    setSelectedCategoryIds((current) =>
      current.includes(categoryId) ? current.filter((id) => id !== categoryId) : [...current, categoryId],
    );
    setErrorMessage("");
  }

  function toggleBook(bookId) {
    setSelectedBookIds((current) => (current.includes(bookId) ? current.filter((id) => id !== bookId) : [...current, bookId]));
    setErrorMessage("");
  }

  function toggleReadingPurpose(purposeCode) {
    setSelectedReadingPurposeCodes((current) => {
      if (current.includes(purposeCode)) {
        return current.filter((code) => code !== purposeCode);
      }
      if (current.length >= 3) {
        setErrorMessage("독서 목적은 최대 3개까지 선택할 수 있습니다.");
        return current;
      }
      setErrorMessage("");
      return [...current, purposeCode];
    });
  }

  async function submitOnboarding(event) {
    event.preventDefault();

    if (selectedCategoryIds.length === 0) {
      setErrorMessage("관심 분야를 1개 이상 선택해 주세요.");
      return;
    }

    if (!canSave) {
      setErrorMessage("온보딩 선택지를 불러온 뒤 저장할 수 있습니다.");
      return;
    }

    if (selectedReadingPurposeCodes.length === 0) {
      setErrorMessage("독서 목적을 1개 이상 선택해 주세요.");
      return;
    }

    if (selectedBookIds.length === 0) {
      setErrorMessage("읽은 책을 1권 이상 선택해 주세요.");
      return;
    }

    setIsSubmitting(true);
    setErrorMessage("");

    try {
      if (accessToken) {
        const response = await fetch("/api/me/onboarding", {
          body: JSON.stringify({
            categoryIds: selectedCategoryIds,
            readBookIds: selectedBookIds,
            readingPurposeCodes: selectedReadingPurposeCodes,
          }),
          headers: {
            Authorization: `Bearer ${accessToken}`,
            "Content-Type": "application/json",
          },
          method: "PUT",
        });

        if (response.status === 401) {
          logout();
          return;
        }

        if (!response.ok) {
          const data = await response.json().catch(() => ({}));
          throw new Error(data.message ?? "온보딩 정보를 저장하지 못했습니다.");
        }
      }

      navigate("/mypage", { replace: true });
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "알 수 없는 오류가 발생했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className="mx-auto w-full max-w-7xl px-5 py-8">
      <form className="grid grid-cols-1 gap-6 lg:grid-cols-[1fr_340px]" onSubmit={submitOnboarding}>
        <div className="space-y-6">
          <div className="rounded-lg border border-[#E5E7EB] bg-white p-6 shadow-sm">
            <p className="text-sm font-semibold text-[#4CAF50]">{isEditMode ? "취향 수정" : "개인화 시작"}</p>
            <h1 className="mt-3 text-3xl font-bold leading-tight text-[#1E2A38]">
              {isEditMode ? "관심 분야, 독서 목적, 읽은 책을 다시 조정해 주세요" : "관심 분야, 독서 목적, 읽은 책을 선택해 주세요"}
            </h1>
            <p className="mt-4 max-w-2xl text-sm leading-6 text-[#6B7280]">
              {isEditMode
                ? "현재 마이페이지에 반영된 취향을 불러왔습니다. 수정한 내용은 저장 후 추천 이유에 다시 반영됩니다."
                : "선택한 정보는 마이페이지와 추천 이유에 반영됩니다. 지금 고른 내용은 이후 취향 수정에서 다시 바꿀 수 있습니다."}
            </p>
            {isLoading ? <p className="mt-4 text-sm text-[#6B7280]">온보딩 선택지를 불러오는 중입니다.</p> : null}
            {message ? <p className="mt-4 text-sm font-medium text-[#4CAF50]">{message}</p> : null}
          </div>

          <section
            className={`rounded-lg border bg-white p-5 shadow-sm ${
              isInterestFocused ? "border-[#4CAF50] ring-2 ring-[#4CAF50]/20" : "border-[#E5E7EB]"
            }`}
          >
            <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
              <div>
                <p className="text-sm font-semibold text-[#1E2A38]">관심 분야</p>
                <h2 className="mt-2 text-2xl font-bold text-[#1E2A38]">추천 기준이 되는 교양 필터</h2>
                {isInterestFocused ? <p className="mt-2 text-sm text-[#4CAF50]">마이페이지에서 관심 분야 수정을 선택했습니다.</p> : null}
              </div>
              <span className="rounded-full bg-[#4CAF50]/10 px-3 py-2 text-xs font-semibold text-[#4CAF50]">
                {selectedCategoryIds.length}개 선택
              </span>
            </div>

            <div className="mt-5 grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-3">
              {availableCategories.length > 0 ? availableCategories.map((category) => {
                const isSelected = selectedCategoryIds.includes(category.id);

                return (
                  <button
                    className={`rounded-lg border p-4 text-left transition ${
                      isSelected
                        ? "border-[#4CAF50] bg-[#4CAF50]/10"
                        : "border-[#E5E7EB] bg-white hover:border-[#1E2A38]"
                    }`}
                    key={category.id}
                    type="button"
                    onClick={() => toggleCategory(category.id)}
                  >
                    <span className="text-base font-bold text-[#1E2A38]">{category.name}</span>
                    <span className="mt-2 block text-sm leading-6 text-[#6B7280]">{category.description}</span>
                  </button>
                );
              }) : (
                <p className="rounded-lg border border-[#E5E7EB] bg-[#F5F3EF] p-4 text-sm text-[#6B7280] md:col-span-2 xl:col-span-3">
                  선택 가능한 관심 분야를 불러오지 못했습니다.
                </p>
              )}
            </div>
          </section>

          <section
            className={`rounded-lg border bg-white p-5 shadow-sm ${
              isPurposeFocused ? "border-[#2563EB] ring-2 ring-[#2563EB]/20" : "border-[#E5E7EB]"
            }`}
          >
            <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
              <div>
                <p className="text-sm font-semibold text-[#1E2A38]">독서 목적</p>
                <h2 className="mt-2 text-2xl font-bold text-[#1E2A38]">책을 찾는 이유</h2>
                {isPurposeFocused ? <p className="mt-2 text-sm text-[#2563EB]">마이페이지에서 독서 목적 수정을 선택했습니다.</p> : null}
              </div>
              <span className="rounded-full bg-[#2563EB]/10 px-3 py-2 text-xs font-semibold text-[#1D4ED8]">
                {selectedReadingPurposeCodes.length}/3개 선택
              </span>
            </div>

            <div className="mt-5 grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-3">
              {availableReadingPurposes.length > 0 ? availableReadingPurposes.map((purpose) => {
                const isSelected = selectedReadingPurposeCodes.includes(purpose.code);

                return (
                  <button
                    className={`rounded-lg border p-4 text-left transition ${
                      isSelected
                        ? "border-[#2563EB] bg-[#2563EB]/10"
                        : "border-[#E5E7EB] bg-white hover:border-[#1E2A38]"
                    }`}
                    key={purpose.code}
                    type="button"
                    onClick={() => toggleReadingPurpose(purpose.code)}
                  >
                    <span className="text-base font-bold text-[#1E2A38]">{purpose.label}</span>
                    <span className="mt-2 block text-sm leading-6 text-[#6B7280]">{purpose.description}</span>
                  </button>
                );
              }) : (
                <p className="rounded-lg border border-[#E5E7EB] bg-[#F5F3EF] p-4 text-sm text-[#6B7280] md:col-span-2 xl:col-span-3">
                  선택 가능한 독서 목적을 불러오지 못했습니다.
                </p>
              )}
            </div>
          </section>

          <section
            className={`rounded-lg border bg-white p-5 shadow-sm ${
              isReadBooksFocused ? "border-[#F59E0B] ring-2 ring-[#F59E0B]/20" : "border-[#E5E7EB]"
            }`}
          >
            <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
              <div>
                <p className="text-sm font-semibold text-[#1E2A38]">읽은 책</p>
                <h2 className="mt-2 text-2xl font-bold text-[#1E2A38]">취향 분석에 사용할 책</h2>
                {isReadBooksFocused ? <p className="mt-2 text-sm text-[#B45309]">마이페이지에서 읽은 책 수정을 선택했습니다.</p> : null}
              </div>
              <span className="rounded-full bg-[#F59E0B]/10 px-3 py-2 text-xs font-semibold text-[#B45309]">
                {selectedBookIds.length}권 선택
              </span>
            </div>

            <BookSearchPanel
              actionLabel="읽은 책 선택"
              emptyMessage="검색 결과가 없습니다."
              onResults={(books) => setAvailableBooks((current) => mergeBooks(current, books))}
              onSelect={(book) => {
                setAvailableBooks((current) => mergeBooks(current, [book]));
                setSelectedBookIds((current) => (current.includes(book.id) ? current : [...current, book.id]));
              }}
              selectedIds={selectedBookIds}
              selectedLabel="선택됨"
              title="읽은 책 검색"
            />

            <div className="mt-5 grid grid-cols-1 gap-4 md:grid-cols-2">
              {availableBooks.length > 0 ? availableBooks.map((book) => {
                const isSelected = selectedBookIds.includes(book.id);

                return (
                  <button
                    className={`rounded-lg border p-4 text-left transition ${
                      isSelected
                        ? "border-[#1E2A38] bg-[#F5F3EF]"
                        : "border-[#E5E7EB] bg-white hover:border-[#1E2A38]"
                    }`}
                    key={book.id}
                    type="button"
                    onClick={() => toggleBook(book.id)}
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <p className="font-bold text-[#1E2A38]">{book.title}</p>
                        <p className="mt-1 text-sm text-[#6B7280]">{book.author}</p>
                      </div>
                      <span className="shrink-0 rounded-full border border-[#E5E7EB] px-3 py-1 text-xs font-semibold text-[#6B7280]">
                        {book.category}
                      </span>
                    </div>
                    <p className="mt-3 text-sm leading-6 text-[#6B7280]">{book.reason}</p>
                  </button>
                );
              }) : (
                <p className="rounded-lg border border-[#E5E7EB] bg-[#F5F3EF] p-4 text-sm text-[#6B7280] md:col-span-2">
                  선택 가능한 책을 불러오지 못했습니다.
                </p>
              )}
            </div>
          </section>
        </div>

        <aside className="h-fit rounded-lg border border-[#E5E7EB] bg-white p-5 shadow-sm lg:sticky lg:top-28">
          <p className="text-sm font-semibold text-[#1E2A38]">선택 요약</p>
          <h2 className="mt-2 text-xl font-bold text-[#1E2A38]">마이페이지에 반영될 정보</h2>

          <div className="mt-5 space-y-5">
            <div>
              <p className="text-xs font-semibold text-[#6B7280]">관심 분야</p>
              <div className="mt-2 flex flex-wrap gap-2">
                {selectedSummary.categories.length > 0 ? (
                  selectedSummary.categories.map((category) => (
                    <span className="rounded-full bg-[#4CAF50]/10 px-3 py-1 text-xs font-semibold text-[#4CAF50]" key={category.id}>
                      {category.name}
                    </span>
                  ))
                ) : (
                  <span className="text-sm text-[#6B7280]">아직 선택하지 않았습니다.</span>
                )}
              </div>
            </div>

            <div>
              <p className="text-xs font-semibold text-[#6B7280]">독서 목적</p>
              <div className="mt-2 flex flex-wrap gap-2">
                {selectedSummary.readingPurposes.length > 0 ? (
                  selectedSummary.readingPurposes.map((purpose) => (
                    <span className="rounded-full bg-[#2563EB]/10 px-3 py-1 text-xs font-semibold text-[#1D4ED8]" key={purpose.code}>
                      {purpose.label}
                    </span>
                  ))
                ) : (
                  <span className="text-sm text-[#6B7280]">아직 선택하지 않았습니다.</span>
                )}
              </div>
            </div>

            <div>
              <p className="text-xs font-semibold text-[#6B7280]">읽은 책</p>
              <div className="mt-2 space-y-2">
                {selectedSummary.books.length > 0 ? (
                  selectedSummary.books.map((book) => (
                    <p className="rounded-md bg-[#F5F3EF] px-3 py-2 text-sm font-medium text-[#1E2A38]" key={book.id}>
                      {book.title}
                    </p>
                  ))
                ) : (
                  <span className="text-sm text-[#6B7280]">아직 선택하지 않았습니다.</span>
                )}
              </div>
            </div>
          </div>

          {errorMessage ? <p className="mt-5 text-sm text-red-600">{errorMessage}</p> : null}

          <button
            className="mt-5 w-full rounded-md bg-[#1E2A38] px-4 py-3 text-sm font-semibold text-white transition hover:bg-[#27384a] disabled:cursor-not-allowed disabled:bg-[#6B7280]"
            disabled={!canSubmit}
            type="submit"
          >
            {isSubmitting ? "저장 중..." : isEditMode ? "수정하고 마이페이지로" : "저장하고 마이페이지로"}
          </button>
        </aside>
      </form>
    </section>
  );
}
