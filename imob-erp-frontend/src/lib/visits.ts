import type { Visit, VisitStatus } from "@/types/visit";

export const VISITS_PAGE_SIZE = 100;

export interface VisitFilterValues {
  status?: VisitStatus;
  from?: string;
  to?: string;
}

export interface VisitGroup {
  /** Dia no formato yyyy-MM-dd, no fuso de Brasília. */
  date: string;
  visits: Visit[];
}

// en-CA formata a data como yyyy-MM-dd; o fuso fixo evita que uma visita às 23h caia no dia seguinte.
const dayKey = new Intl.DateTimeFormat("en-CA", { timeZone: "America/Sao_Paulo" });

const byScheduledAt = (a: Visit, b: Visit) => a.scheduledAt.localeCompare(b.scheduledAt);

/** Agrupa as visitas por dia (fuso de Brasília), dias e visitas em ordem cronológica. */
export function groupVisitsByDate(visits: Visit[]): VisitGroup[] {
  const byDay = [...visits].sort(byScheduledAt).reduce<Map<string, Visit[]>>((groups, visit) => {
    const date = dayKey.format(new Date(visit.scheduledAt));
    return groups.set(date, [...(groups.get(date) ?? []), visit]);
  }, new Map());

  return [...byDay.entries()].map(([date, dayVisits]) => ({ date, visits: dayVisits }));
}

const dayHeading = new Intl.DateTimeFormat("pt-BR", {
  weekday: "long",
  day: "2-digit",
  month: "long",
  year: "numeric",
  timeZone: "UTC",
});

/** Rótulo do dia ("segunda-feira, 05 de outubro de 2026") a partir de yyyy-MM-dd, sem depender do fuso local. */
export const formatGroupDate = (date: string): string => dayHeading.format(new Date(`${date}T00:00:00Z`));
