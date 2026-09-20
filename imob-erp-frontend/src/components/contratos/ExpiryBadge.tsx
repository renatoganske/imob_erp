import { Badge } from "@/components/ui/badge";
import { daysUntil, expiryLabel, expiryLevel, type ExpiryLevel } from "@/lib/contracts";
import type { Contract } from "@/types/contract";

const VARIANT: Record<ExpiryLevel, "destructive" | "warning" | "info"> = {
  danger: "destructive",
  warning: "warning",
  info: "info",
};

// Selo de vencimento na lista: só para locações ativas que terminam nos próximos 90 dias.
export function ExpiryBadge({
  contract,
  today,
}: {
  contract: Pick<Contract, "status" | "type" | "endDate">;
  today: string;
}) {
  const level = expiryLevel(contract, today);
  if (!level || !contract.endDate) return null;
  return <Badge variant={VARIANT[level]}>{expiryLabel(daysUntil(contract.endDate, today))}</Badge>;
}
