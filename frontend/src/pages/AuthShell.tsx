import type { ReactNode } from "react";
import { Trophy } from "lucide-react";
import { useLanguage } from "../i18n/LanguageProvider";

export function AuthShell({ title, subtitle, children }: { title: string; subtitle: string; children: ReactNode }) {
  const { language, setLanguage } = useLanguage();

  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-4 py-10">
      <section className="w-full max-w-md rounded-lg border bg-white p-6 shadow-sm">
        <div className="mb-6 flex items-start justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-md bg-primary text-primary-foreground">
              <Trophy className="h-5 w-5" />
            </div>
            <div>
              <h1 className="text-xl font-semibold">{title}</h1>
              <p className="text-sm text-muted-foreground">{subtitle}</p>
            </div>
          </div>
          <div className="flex rounded-md border bg-white p-1">
            <button
              type="button"
              className={language === "pt" ? "h-7 rounded-sm bg-secondary px-2 text-xs font-medium" : "h-7 rounded-sm px-2 text-xs font-medium"}
              onClick={() => setLanguage("pt")}
            >
              PT
            </button>
            <button
              type="button"
              className={language === "en" ? "h-7 rounded-sm bg-secondary px-2 text-xs font-medium" : "h-7 rounded-sm px-2 text-xs font-medium"}
              onClick={() => setLanguage("en")}
            >
              EN
            </button>
          </div>
        </div>
        {children}
      </section>
    </main>
  );
}
