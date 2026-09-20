import { render, screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { Visit } from "@/types/visit";
import { VisitList } from "./VisitList";

const visit = (id: string, scheduledAt: string, status: Visit["status"] = "AGENDADA"): Visit => ({
  id,
  leadId: "lead",
  propertyId: `prop-${id}`,
  agentId: "agent",
  scheduledAt,
  status,
});

describe("VisitList", () => {
  it("agrupa as visitas por data, com o horário e o badge de status em cada uma", () => {
    render(
      <VisitList
        visits={[
          visit("a", "2026-10-05T09:00:00-03:00"),
          visit("b", "2026-10-05T15:30:00-03:00", "REALIZADA"),
          visit("c", "2026-10-06T10:00:00-03:00", "CANCELADA"),
        ]}
        propertyTitles={{ "prop-a": "Casa A", "prop-b": "Casa B", "prop-c": "Casa C" }}
        leadNames={{ lead: "Maria" }}
        onSaveResult={vi.fn()}
      />
    );

    const headings = screen.getAllByRole("heading", { level: 2 }).map((h) => h.textContent);
    expect(headings).toEqual(["segunda-feira, 05 de outubro de 2026", "terça-feira, 06 de outubro de 2026"]);

    const monday = screen.getByRole("region", { name: "segunda-feira, 05 de outubro de 2026" });
    expect(within(monday).getByText(/Casa A/)).toBeInTheDocument();
    expect(within(monday).getByText(/Casa B/)).toBeInTheDocument();
    expect(within(monday).getByText(/09:00/)).toBeInTheDocument();
    expect(within(monday).getByText("Realizada")).toBeInTheDocument();
    expect(within(monday).queryByText(/Casa C/)).not.toBeInTheDocument();

    const tuesday = screen.getByRole("region", { name: "terça-feira, 06 de outubro de 2026" });
    expect(within(tuesday).getByText("Cancelada")).toBeInTheDocument();
  });
});
