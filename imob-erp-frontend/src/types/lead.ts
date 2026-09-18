export type LeadSource = "WHATSAPP" | "SITE" | "INDICACAO" | "PORTAL_ZAP" | "PORTAL_VIVAREAL" | "OUTRO";
export type LeadStage = "NOVO" | "EM_ATENDIMENTO" | "VISITA_AGENDADA" | "PROPOSTA" | "FECHADO" | "PERDIDO";

export const LEAD_STAGES: LeadStage[] = [
  "NOVO",
  "EM_ATENDIMENTO",
  "VISITA_AGENDADA",
  "PROPOSTA",
  "FECHADO",
  "PERDIDO",
];

export interface Lead {
  id: string;
  name: string;
  phone: string;
  email?: string;
  source: LeadSource;
  stage: LeadStage;
  assignedTo: string;
  notes?: string;
  propertiesOfInterest: string[];
}

export type LeadRequest = Omit<Lead, "id" | "stage" | "propertiesOfInterest">;
