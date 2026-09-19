import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatCurrency(value: number): string {
  return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(value);
}

// Data de hoje no fuso local ("YYYY-MM-DD"). `toISOString()` usa UTC e, à noite no Brasil, já devolve o dia seguinte.
export function todayLocal(): string {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${now.getFullYear()}-${month}-${day}`;
}

const DATE_ONLY = /^(\d{4})-(\d{2})-(\d{2})$/;

// Datas puras ("2026-09-18") vindas da API nao tem fuso: `new Date(str)` as le como UTC e, no Brasil,
// exibe o dia anterior. Aqui sao montadas no fuso local.
export function formatDate(value: string | Date): string {
  let date: Date;
  if (typeof value === "string") {
    const match = DATE_ONLY.exec(value);
    date = match ? new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3])) : new Date(value);
  } else {
    date = value;
  }
  return new Intl.DateTimeFormat("pt-BR").format(date);
}
