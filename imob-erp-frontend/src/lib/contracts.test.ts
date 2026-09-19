import { describe, expect, it } from "vitest";
import type { Contract } from "@/types/contract";
import { filterByPeriod, installmentCount, newContractHref, parsePrefill, prefillFromLead } from "./contracts";

const contract = (startDate: string): Contract => ({
  id: startDate,
  propertyId: "p",
  agentId: "a",
  type: "LOCACAO",
  status: "ATIVO",
  value: 1000,
  startDate,
  buyerName: "b",
  buyerDocument: "1",
  ownerName: "o",
  ownerDocument: "2",
});

describe("installmentCount", () => {
  it("gera 12 parcelas na locação e 1 na venda", () => {
    expect(installmentCount("LOCACAO")).toBe(12);
    expect(installmentCount("COMPRA_VENDA")).toBe(1);
  });
});

describe("filterByPeriod", () => {
  const all = [contract("2026-01-10"), contract("2026-02-15"), contract("2026-03-20")];

  it("sem limites devolve tudo", () => {
    expect(filterByPeriod(all)).toHaveLength(3);
  });
  it("aplica limites inclusivos", () => {
    expect(filterByPeriod(all, "2026-02-15", "2026-03-20").map((c) => c.startDate)).toEqual(["2026-02-15", "2026-03-20"]);
  });
  it("aceita só início ou só fim", () => {
    expect(filterByPeriod(all, "2026-02-01")).toHaveLength(2);
    expect(filterByPeriod(all, undefined, "2026-02-01")).toHaveLength(1);
  });
});

describe("pré-preenchimento a partir de lead", () => {
  it("usa id, nome e o primeiro imóvel de interesse", () => {
    expect(prefillFromLead({ id: "l1", name: "Ana", propertiesOfInterest: ["p1", "p2"] })).toEqual({
      leadId: "l1",
      buyerName: "Ana",
      propertyId: "p1",
    });
  });
  it("monta e lê a URL de volta, omitindo campos vazios", () => {
    const href = newContractHref(prefillFromLead({ id: "l1", name: "Ana Souza", propertiesOfInterest: [] }));
    expect(href).toBe("/contratos/novo?leadId=l1&buyerName=Ana+Souza");
    const query = Object.fromEntries(new URL(href, "http://x").searchParams);
    expect(parsePrefill(query)).toEqual({ leadId: "l1", buyerName: "Ana Souza", propertyId: undefined });
  });
  it("sem dados a URL fica limpa", () => {
    expect(newContractHref({})).toBe("/contratos/novo");
  });
});
