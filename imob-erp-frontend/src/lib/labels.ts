import type { CommissionStatus } from "@/types/commission";
import type { ContractStatus, ContractType } from "@/types/contract";
import type { FinancialCategory, FinancialStatus } from "@/types/financial";
import type { LeadSource, LeadStage } from "@/types/lead";
import type { PropertyPurpose, PropertyStatus, PropertyType } from "@/types/property";
import type { VisitStatus } from "@/types/visit";

export const CONTRACT_TYPE_LABEL: Record<ContractType, string> = {
  COMPRA_VENDA: "Compra e venda",
  LOCACAO: "Locação",
};

export const CONTRACT_STATUS_LABEL: Record<ContractStatus, string> = {
  RASCUNHO: "Rascunho",
  ATIVO: "Ativo",
  ENCERRADO: "Encerrado",
  CANCELADO: "Cancelado",
};

export const VISIT_STATUS_LABEL: Record<VisitStatus, string> = {
  AGENDADA: "Agendada",
  REALIZADA: "Realizada",
  CANCELADA: "Cancelada",
};

export const ENTRY_STATUS_LABEL: Record<FinancialStatus, string> = {
  PENDENTE: "Pendente",
  PAGO: "Pago",
  ATRASADO: "Atrasado",
  CANCELADO: "Cancelado",
};

export const ENTRY_CATEGORY_LABEL: Record<FinancialCategory, string> = {
  ALUGUEL: "Aluguel",
  PARCELA_VENDA: "Parcela de venda",
  TAXA_ADMINISTRACAO: "Taxa de administração",
  REPASSE_PROPRIETARIO: "Repasse ao proprietário",
  COMISSAO: "Comissão",
  DESPESA_OPERACIONAL: "Despesa operacional",
  OUTRO: "Outro",
};

export const COMMISSION_STATUS_LABEL: Record<CommissionStatus, string> = {
  PENDENTE: "Pendente",
  PAGO: "Pago",
  PARCELADO: "Parcelado",
};

export const PROPERTY_TYPE_LABEL: Record<PropertyType, string> = {
  CASA: "Casa",
  APARTAMENTO: "Apartamento",
  COMERCIAL: "Comercial",
  TERRENO: "Terreno",
};

export const PROPERTY_STATUS_LABEL: Record<PropertyStatus, string> = {
  DISPONIVEL: "Disponível",
  RESERVADO: "Reservado",
  VENDIDO: "Vendido",
  ALUGADO: "Alugado",
};

export const PROPERTY_PURPOSE_LABEL: Record<PropertyPurpose, string> = {
  VENDA: "Venda",
  ALUGUEL: "Aluguel",
  AMBOS: "Venda e aluguel",
};

export const LEAD_STAGE_LABEL: Record<LeadStage, string> = {
  NOVO: "Novo",
  EM_ATENDIMENTO: "Em atendimento",
  VISITA_AGENDADA: "Visita agendada",
  PROPOSTA: "Proposta",
  FECHADO: "Fechado",
  PERDIDO: "Perdido",
};

export const LEAD_SOURCE_LABEL: Record<LeadSource, string> = {
  WHATSAPP: "WhatsApp",
  SITE: "Site",
  INDICACAO: "Indicação",
  PORTAL_ZAP: "Portal Zap",
  PORTAL_VIVAREAL: "Portal VivaReal",
  OUTRO: "Outro",
};
