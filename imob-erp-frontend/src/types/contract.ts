export type ContractType = "COMPRA_VENDA" | "LOCACAO";
export type ContractStatus = "RASCUNHO" | "ATIVO" | "ENCERRADO" | "CANCELADO";
export type AdjustmentIndex = "IGPM" | "IPCA" | "FIXO";

export interface Contract {
  id: string;
  leadId?: string;
  propertyId: string;
  agentId: string;
  type: ContractType;
  status: ContractStatus;
  value: number;
  signedAt?: string;
  startDate: string;
  endDate?: string;
  adjustmentIndex?: AdjustmentIndex;
  buyerName: string;
  buyerDocument: string;
  ownerName: string;
  ownerDocument: string;
  documentUrl?: string;
  commissionRateOverride?: number;
  notes?: string;
}

export type ContractRequest = Omit<Contract, "id" | "status" | "documentUrl">;
