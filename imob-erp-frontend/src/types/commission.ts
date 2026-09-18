export type CommissionStatus = "PENDENTE" | "PAGO" | "PARCELADO";

export interface Commission {
  id: string;
  contractId: string;
  agentId: string;
  rate: number;
  baseValue: number;
  value: number;
  status: CommissionStatus;
  paidAt?: string;
}

export interface CommissionReportItem {
  agentId: string;
  total: number;
}
