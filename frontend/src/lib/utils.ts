import { type ClassValue, clsx } from "clsx";
import { twMerge } from "tailwind-merge";
import type { Language } from "../i18n/LanguageProvider";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatDateTime(value: string, language?: Language) {
  const portuguese = language === "pt";
  const formatted = new Intl.DateTimeFormat(portuguese ? "pt-BR" : undefined, {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    timeZone: portuguese ? "America/Sao_Paulo" : undefined,
  }).format(new Date(value));
  return portuguese ? `${formatted} BRT` : formatted;
}
