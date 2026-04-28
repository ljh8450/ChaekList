import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

export default function BookCard({ book, rank, variant = "default" }) {
  const displayRank = rank ?? book.rankPosition;
  const cover = book.cover ?? "bg-[#1E2A38]";
  const growth = book.growth ?? book.growthRate;
  const views = book.views ?? "0";
  const saves = book.saves ?? 0;
  const imageUrl = book.imageUrl;
  const [imageFailed, setImageFailed] = useState(false);
  const showImage = imageUrl && !imageFailed;

  useEffect(() => {
    setImageFailed(false);
  }, [imageUrl]);

  return (
    <Link
      className="group block rounded-lg border border-[#E5E7EB] bg-white p-4 shadow-sm transition hover:-translate-y-0.5 hover:border-[#1E2A38]"
      to={`/books/${book.id}`}
    >
      <div className="flex gap-4">
        {showImage ? (
          <img
            alt={`${book.title} 표지`}
            className="h-32 w-24 shrink-0 rounded-md object-cover shadow-sm"
            loading="lazy"
            onError={() => setImageFailed(true)}
            src={imageUrl}
          />
        ) : (
          <div className={`flex h-32 w-24 shrink-0 items-end rounded-md ${cover} p-3 text-xs font-semibold text-white shadow-sm`}>
            {displayRank ? `TOP ${displayRank}` : book.category}
          </div>
        )}
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            {displayRank ? <span className="text-sm font-bold text-[#F59E0B]">{displayRank}</span> : null}
            <span className="rounded-full border border-[#E5E7EB] px-2 py-1 text-xs text-[#6B7280]">{book.category}</span>
          </div>
          <h3 className="mt-3 line-clamp-2 text-base font-bold text-[#1E2A38] group-hover:underline">{book.title}</h3>
          <p className="mt-1 text-sm text-[#6B7280]">{book.author}</p>
          <p className="mt-3 text-xs font-medium text-[#4CAF50]">{book.tag}</p>
          <div className="mt-4 flex flex-wrap gap-2 text-xs text-[#6B7280]">
            <span>조회 {views}</span>
            <span>저장 {saves}</span>
            {variant === "trending" && growth ? <span className="font-bold text-[#F59E0B]">{growth}</span> : null}
          </div>
        </div>
      </div>
    </Link>
  );
}
