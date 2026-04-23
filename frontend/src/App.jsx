import { useEffect, useState } from "react";

export default function App() {
  const [status, setStatus] = useState("Loading...");
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;

    async function loadHealth() {
      try {
        const response = await fetch("/api/health");

        if (!response.ok) {
          throw new Error(`Request failed with ${response.status}`);
        }

        const data = await response.json();

        if (!cancelled) {
          setStatus(data.status ?? "unknown");
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "Unknown error");
          setStatus("Unavailable");
        }
      }
    }

    loadHealth();

    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <main className="flex min-h-screen items-center justify-center bg-zinc-50 px-6">
      <section className="w-full max-w-sm rounded-2xl border border-zinc-200 bg-white p-8 shadow-sm">
        <p className="text-sm font-medium uppercase tracking-[0.2em] text-zinc-500">
          Backend Health
        </p>
        <h1 className="mt-3 text-3xl font-semibold text-zinc-950">{status}</h1>
        <p className="mt-3 text-sm text-zinc-600">
          Response from <code className="rounded bg-zinc-100 px-1 py-0.5">/api/health</code>
        </p>
        {error ? <p className="mt-4 text-sm text-red-600">{error}</p> : null}
      </section>
    </main>
  );
}
