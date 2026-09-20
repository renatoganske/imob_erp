import { describe, expect, it } from "vitest";
import {
  daysUntil,
  expiringContractsHref,
  expiryAlerts,
  expiryLabel,
  expiryLevel,
  parseExpiringInDays,
} from "./contracts";

const TODAY = "2026-09-20";
const rental = (endDate?: string, status: "ATIVO" | "RASCUNHO" | "ENCERRADO" = "ATIVO") =>
  ({ status, type: "LOCACAO", endDate }) as const;

describe("daysUntil", () => {
  it("conta dias inteiros entre datas puras, inclusive na virada de mês e ano", () => {
    expect(daysUntil("2026-09-20", TODAY)).toBe(0);
    expect(daysUntil("2026-10-20", TODAY)).toBe(30);
    expect(daysUntil("2026-12-19", TODAY)).toBe(90);
    expect(daysUntil("2027-01-02", "2026-12-31")).toBe(2);
  });

  it("é negativo quando a data já passou", () => {
    expect(daysUntil("2026-09-19", TODAY)).toBe(-1);
  });

  it("não se confunde com a mudança de horário de verão", () => {
    expect(daysUntil("2026-11-10", "2026-10-10")).toBe(31);
    expect(daysUntil("2026-03-30", "2026-02-28")).toBe(30);
  });
});

describe("expiryLevel", () => {
  it("usa vermelho até 30 dias, amarelo até 60 e info até 90, com limites inclusivos", () => {
    expect(expiryLevel(rental("2026-09-20"), TODAY)).toBe("danger");
    expect(expiryLevel(rental("2026-10-20"), TODAY)).toBe("danger");
    expect(expiryLevel(rental("2026-10-21"), TODAY)).toBe("warning");
    expect(expiryLevel(rental("2026-11-19"), TODAY)).toBe("warning");
    expect(expiryLevel(rental("2026-11-20"), TODAY)).toBe("info");
    expect(expiryLevel(rental("2026-12-19"), TODAY)).toBe("info");
  });

  it("não alerta além de 90 dias, depois do fim ou sem data de fim", () => {
    expect(expiryLevel(rental("2026-12-20"), TODAY)).toBeNull();
    expect(expiryLevel(rental("2026-09-19"), TODAY)).toBeNull();
    expect(expiryLevel(rental(undefined), TODAY)).toBeNull();
  });

  it("só vale para locação ATIVA", () => {
    expect(expiryLevel(rental("2026-10-01", "RASCUNHO"), TODAY)).toBeNull();
    expect(expiryLevel(rental("2026-10-01", "ENCERRADO"), TODAY)).toBeNull();
    expect(expiryLevel({ status: "ATIVO", type: "COMPRA_VENDA", endDate: "2026-10-01" }, TODAY)).toBeNull();
  });
});

describe("expiryLabel", () => {
  it("descreve hoje, amanhã e os demais dias", () => {
    expect(expiryLabel(0)).toBe("Vence hoje");
    expect(expiryLabel(1)).toBe("Vence amanhã");
    expect(expiryLabel(12)).toBe("Vence em 12 dias");
  });
});

describe("expiryAlerts", () => {
  it("mapeia o resumo para 30/60/90 com o nível de cada janela", () => {
    expect(expiryAlerts({ within30Days: 1, within60Days: 3, within90Days: 5 })).toEqual([
      { window: 30, count: 1, level: "danger" },
      { window: 60, count: 3, level: "warning" },
      { window: 90, count: 5, level: "info" },
    ]);
  });
});

describe("parseExpiringInDays / expiringContractsHref", () => {
  it("aceita só 30, 60 e 90", () => {
    expect(parseExpiringInDays("30")).toBe(30);
    expect(parseExpiringInDays("90")).toBe(90);
    expect(parseExpiringInDays("45")).toBeUndefined();
    expect(parseExpiringInDays("abc")).toBeUndefined();
    expect(parseExpiringInDays(null)).toBeUndefined();
  });

  it("monta o link da lista filtrada", () => {
    expect(expiringContractsHref(60)).toBe("/contratos?expiringInDays=60");
  });
});
