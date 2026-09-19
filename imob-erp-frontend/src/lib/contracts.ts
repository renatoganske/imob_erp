import type { Contract, ContractType } from "@/types/contract";
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
