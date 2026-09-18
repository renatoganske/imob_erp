export type VisitStatus = "AGENDADA" | "REALIZADA" | "CANCELADA";

export interface Visit {
  id: string;
  leadId: string;
  propertyId: string;
  agentId: string;
  scheduledAt: string;
  status: VisitStatus;
  result?: string;
}

export type VisitRequest = Omit<Visit, "id" | "status" | "result">;
