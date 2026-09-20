import type { Contract, ContractType, ExpiringSummary } from "@/types/contract";
import type { Lead } from "@/types/lead";

export interface ContractFilterValues {
  status?: Contract["status"];
  type?: ContractType;
  from?: string; // "YYYY-MM-DD", inclusive
  to?: string; // "YYYY-MM-DD", inclusive
}

// A API não filtra por período: o recorte é feito no cliente sobre a página carregada.
export const CONTRACTS_PAGE_SIZE = 200;

// Regra do backend (ContractService.activate): locação gera 12 parcelas de aluguel, venda gera 1 lançamento.
export function installmentCount(type: ContractType): number {
  return type === "LOCACAO" ? 12 : 1;
}

// Datas puras "YYYY-MM-DD" comparam corretamente como texto.
export function filterByPeriod(contracts: Contract[], from?: string, to?: string): Contract[] {
  return contracts.filter((c) => (!from || c.startDate >= from) && (!to || c.startDate <= to));
}

// ---- Alertas de vencimento (IMOB-28): 90 dias = info, 60 = amarelo, 30 = vermelho ----

export type ExpiryWindow = 30 | 60 | 90;
export type ExpiryLevel = "danger" | "warning" | "info";

export const EXPIRY_WINDOWS: readonly ExpiryWindow[] = [30, 60, 90];

const LEVEL_BY_WINDOW: Record<ExpiryWindow, ExpiryLevel> = { 30: "danger", 60: "warning", 90: "info" };

const DATE_ONLY = /^(\d{4})-(\d{2})-(\d{2})$/;

const utcDay = (date: string): number => {
  const [, year, month, day] = DATE_ONLY.exec(date) ?? [];
  return Date.UTC(Number(year), Number(month) - 1, Number(day));
};

// Dias inteiros de `today` até `endDate` (datas "YYYY-MM-DD"); negativo se já passou.
export function daysUntil(endDate: string, today: string): number {
  return Math.round((utcDay(endDate) - utcDay(today)) / 86_400_000);
}

// Nível do alerta de um contrato. Mesmas regras do backend: só locação ATIVA com data de fim, entre hoje e +90 dias.
export function expiryLevel(
  contract: Pick<Contract, "status" | "type" | "endDate">,
  today: string,
): ExpiryLevel | null {
  if (contract.status !== "ATIVO" || contract.type !== "LOCACAO" || !contract.endDate) return null;
  const days = daysUntil(contract.endDate, today);
  if (days < 0) return null;
  const window = EXPIRY_WINDOWS.find((w) => days <= w);
  return window ? LEVEL_BY_WINDOW[window] : null;
}

export function expiryLabel(days: number): string {
  return days === 0 ? "Vence hoje" : days === 1 ? "Vence amanhã" : `Vence em ${days} dias`;
}

export interface ExpiryAlert {
  window: ExpiryWindow;
  count: number;
  level: ExpiryLevel;
}

export function expiryAlerts(summary: ExpiringSummary): ExpiryAlert[] {
  const counts: Record<ExpiryWindow, number> = {
    30: summary.within30Days,
    60: summary.within60Days,
    90: summary.within90Days,
  };
  return EXPIRY_WINDOWS.map((window) => ({ window, count: counts[window], level: LEVEL_BY_WINDOW[window] }));
}

// Valor de ?expiringInDays= na URL da tela de contratos; qualquer coisa fora de 30/60/90 vira "sem filtro".
export function parseExpiringInDays(value: string | null | undefined): ExpiryWindow | undefined {
  return EXPIRY_WINDOWS.find((window) => String(window) === value);
}

export function expiringContractsHref(window: ExpiryWindow): string {
  return `/contratos?expiringInDays=${window}`;
}

export interface ContractPrefill {
  leadId?: string;
  buyerName?: string;
  propertyId?: string;
}

export function prefillFromLead(lead: Pick<Lead, "id" | "name" | "propertiesOfInterest">): ContractPrefill {
  return { leadId: lead.id, buyerName: lead.name, propertyId: lead.propertiesOfInterest[0] };
}

export function newContractHref(prefill: ContractPrefill): string {
  const params = new URLSearchParams();
  Object.entries(prefill).forEach(([key, value]) => value && params.set(key, value));
  const query = params.toString();
  return query ? `/contratos/novo?${query}` : "/contratos/novo";
}

export function parsePrefill(searchParams: Record<string, string | string[] | undefined>): ContractPrefill {
  const first = (v: string | string[] | undefined) => (Array.isArray(v) ? v[0] : v) || undefined;
  return {
    leadId: first(searchParams.leadId),
    buyerName: first(searchParams.buyerName),
    propertyId: first(searchParams.propertyId),
  };
}
