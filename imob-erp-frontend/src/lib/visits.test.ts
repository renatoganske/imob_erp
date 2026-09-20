import { describe, expect, it } from "vitest";
import type { Visit } from "@/types/visit";
import { formatGroupDate, groupVisitsByDate } from "./visits";

const visit = (id: string, scheduledAt: string): Visit => ({
  id,
  leadId: "l",
  propertyId: "p",
  agentId: "a",
  scheduledAt,
  status: "AGENDADA",
});

describe("groupVisitsByDate", () => {
  it("agrupa por dia e ordena dias e visitas cronologicamente", () => {
    const groups = groupVisitsByDate([
      visit("c", "2026-10-06T13:00:00-03:00"),
      visit("b", "2026-10-05T15:00:00-03:00"),
      visit("a", "2026-10-05T09:00:00-03:00"),
    ]);

    expect(groups.map((g) => g.date)).toEqual(["2026-10-05", "2026-10-06"]);
    expect(groups[0].visits.map((v) => v.id)).toEqual(["a", "b"]);
    expect(groups[1].visits.map((v) => v.id)).toEqual(["c"]);
  });

  it("usa o dia de Brasília: 23h30 de 05/10 (02h30Z do dia 06) continua no dia 05", () => {
    const groups = groupVisitsByDate([visit("late", "2026-10-06T02:30:00Z")]);
    expect(groups.map((g) => g.date)).toEqual(["2026-10-05"]);
  });

  it("nao altera a lista recebida e devolve vazio sem visitas", () => {
    const input = [visit("b", "2026-10-06T10:00:00-03:00"), visit("a", "2026-10-05T10:00:00-03:00")];
    groupVisitsByDate(input);
    expect(input.map((v) => v.id)).toEqual(["b", "a"]);
    expect(groupVisitsByDate([])).toEqual([]);
  });
});

describe("formatGroupDate", () => {
  it("escreve o dia da semana e a data por extenso em pt-BR", () => {
    expect(formatGroupDate("2026-10-05")).toBe("segunda-feira, 05 de outubro de 2026");
  });
});
