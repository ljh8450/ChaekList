import { Link, Navigate, useParams } from "react-router-dom";
import BookCard from "../components/BookCard";
import { books } from "../data/books";

export default function BookDetailPage() {
  const { bookId } = useParams();
  const book = books.find((item) => item.id === bookId);

  if (!book) {
    return <Navigate replace to="/" />;
  }

  const similarBooks = books.filter((item) => item.id !== book.id && item.category === book.category).slice(0, 3);

  return (
    <section className="mx-auto w-full max-w-6xl px-5 py-8">
      <Link className="text-sm font-semibold text-[#6B7280] hover:text-[#1E2A38]" to="/">
        홈으로 돌아가기
      </Link>
      <div className="mt-5 grid grid-cols-1 gap-6 rounded-lg border border-[#E5E7EB] bg-white p-6 shadow-sm md:grid-cols-[240px_1fr]">
        <div className={`flex aspect-[3/4] items-end rounded-lg ${book.cover} p-5 text-lg font-bold text-white shadow-sm`}>
          {book.category}
        </div>
        <div>
          <p className="text-sm font-semibold text-[#4CAF50]">{book.tag}</p>
          <h1 className="mt-3 text-3xl font-bold text-[#1E2A38]">{book.title}</h1>
          <p className="mt-2 text-[#6B7280]">{book.author}</p>
          <p className="mt-6 max-w-2xl leading-7 text-[#111827]">{book.summary}</p>
          <div className="mt-6 rounded-lg border border-[#4CAF50]/30 bg-[#4CAF50]/10 p-4">
            <p className="text-sm font-bold text-[#1E2A38]">추천 이유</p>
            <p className="mt-2 text-sm leading-6 text-[#6B7280]">{book.reason}</p>
          </div>
          <div className="mt-5 flex flex-wrap gap-2">
            {book.keywords.map((keyword) => (
              <span className="rounded-full border border-[#E5E7EB] px-3 py-2 text-sm text-[#6B7280]" key={keyword}>
                {keyword}
              </span>
            ))}
          </div>
        </div>
      </div>

      {similarBooks.length ? (
        <section className="mt-6">
          <h2 className="text-xl font-bold text-[#1E2A38]">비슷한 책</h2>
          <div className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2">
            {similarBooks.map((item) => (
              <BookCard book={item} key={item.id} />
            ))}
          </div>
        </section>
      ) : null}
    </section>
  );
}
