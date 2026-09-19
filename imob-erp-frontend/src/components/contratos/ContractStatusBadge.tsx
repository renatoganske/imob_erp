import { Badge } from "@/components/ui/badge";
import { CONTRACT_STATUS_LABEL } from "@/lib/labels";
import type { ContractStatus } from "@/types/contract";

const VARIANT: Record<ContractStatus, "success" | "warning" | "secondary" | "destructive"> = {
  RASCUNHO: "warning",
  ATIVO: "success",
  ENCERRADO: "secondary",
  CANCELADO: "destructive",
};

export function ContractStatusBadge({ status }: { status: ContractStatus }) {
  return <Badge variant={VARIANT[status]}>{CONTRACT_STATUS_LABEL[status]}</Badge>;
}
