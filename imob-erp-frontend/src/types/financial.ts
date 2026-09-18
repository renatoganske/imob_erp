export type FinancialType = "RECEITA" | "DESPESA";
export type FinancialCategory =
  | "ALUGUEL"
  | "PARCELA_VENDA"
  | "TAXA_ADMINISTRACAO"
  | "REPASSE_PROPRIETARIO"
  | "COMISSAO"
  | "DESPESA_OPERACIONAL"
  | "OUTRO";
export type FinancialStatus = "PENDENTE" | "PAGO" | "ATRASADO" | "CANCELADO";

export interface FinancialEntry {
  id: string;
  contractId?: string;
  type: FinancialType;
  category: FinancialCategory;
  description: string;
  value: number;
  dueDate: string;
  paidAt?: string;
  status: FinancialStatus;
  recurrent: boolean;
}

export type FinancialEntryRequest = Pick<FinancialEntry, "type" | "category" | "description" | "value" | "dueDate">;

export interface FinancialDashboard {
  saldoDoMes: number;
  totalAReceber: number;
  totalAPagar: number;
  inadimplencia: number;
}
