import { Badge } from "@/components/ui/badge";
import { formatDate } from "@/lib/utils";
import type { Visit } from "@/types/visit";

export function VisitCard({ visit, onOpen }: { visit: Visit; onOpen: () => void }) {
  return (
    <button
      onClick={onOpen}
      className="flex w-full items-center justify-between rounded-md border border-border p-3 text-left text-sm hover:bg-secondary"
    >
      <div>
        <p className="font-medium">Visita — {formatDate(visit.scheduledAt)}</p>
        <p className="text-muted-foreground">Imóvel {visit.propertyId.slice(0, 8)}</p>
      </div>
      <Badge variant={visit.status === "REALIZADA" ? "success" : visit.status === "CANCELADA" ? "destructive" : "secondary"}>
        {visit.status}
      </Badge>
    </button>
  );
}
