import { Badge } from "@/components/ui/badge";
import { VISIT_STATUS_LABEL } from "@/lib/labels";
import type { Visit } from "@/types/visit";

const dateTime = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" });

export function VisitCard({
  visit,
  propertyTitle,
  leadName,
  onOpen,
}: {
  visit: Visit;
  propertyTitle?: string;
  leadName?: string;
  onOpen: () => void;
}) {
  return (
    <button
      onClick={onOpen}
      className="flex w-full items-center justify-between gap-3 rounded-lg border border-border bg-card p-4 text-left text-sm shadow-card transition-colors hover:border-primary/40"
    >
      <div className="min-w-0">
        <p className="font-medium">{propertyTitle ?? "Imóvel"}</p>
        <p className="text-muted-foreground">
          {leadName ? `${leadName} · ` : ""}
          {dateTime.format(new Date(visit.scheduledAt))}
        </p>
      </div>
      <Badge variant={visit.status === "REALIZADA" ? "success" : visit.status === "CANCELADA" ? "destructive" : "info"}>
        {VISIT_STATUS_LABEL[visit.status]}
      </Badge>
    </button>
  );
}
