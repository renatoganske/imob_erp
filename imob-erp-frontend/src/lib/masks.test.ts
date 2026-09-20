import { describe, expect, it } from "vitest";
import {
  centsFromDigits,
  documentError,
  formatBRL,
  formatCpfCnpj,
  formatPhone,
  parseBRL,
  validateCnpj,
  validateCpf,
} from "./masks";

describe("BRL", () => {
  it("formata com R$, milhar e vírgula", () => {
    expect(formatBRL(1234.56)).toBe("R$ 1.234,56");
    expect(formatBRL(0.5)).toBe("R$ 0,50");
  });
  it("digitação em centavos", () => {
    expect(centsFromDigits("R$ 12,3")).toBe(1.23);
    expect(centsFromDigits("")).toBe(0);
  });
  it("interpreta texto colado", () => {
    expect(parseBRL("R$ 1.234,56")).toBe(1234.56);
    expect(parseBRL("1234,5")).toBe(1234.5);
    expect(parseBRL("1234.56")).toBe(1234.56);
    expect(parseBRL("1.234")).toBe(1234);
    expect(parseBRL("abc")).toBeNull();
  });
});

describe("CPF/CNPJ", () => {
  it("alterna a máscara pelo tamanho", () => {
    expect(formatCpfCnpj("12345678909")).toBe("123.456.789-09");
    expect(formatCpfCnpj("12345678000195")).toBe("12.345.678/0001-95");
    expect(formatCpfCnpj("123")).toBe("123");
    expect(formatCpfCnpj("123.456.789-09")).toBe("123.456.789-09");
    expect(formatCpfCnpj("")).toBe("");
  });
  it("valida dígitos verificadores", () => {
    expect(validateCpf("529.982.247-25")).toBe(true);
    expect(validateCpf("52998224726")).toBe(false);
    expect(validateCpf("11111111111")).toBe(false);
    expect(validateCnpj("11.222.333/0001-81")).toBe(true);
    expect(validateCnpj("11222333000180")).toBe(false);
    expect(validateCnpj("00000000000000")).toBe(false);
  });
  it("documentError: vazio e válido não têm erro", () => {
    expect(documentError("")).toBeNull();
    expect(documentError("52998224725")).toBeNull();
    expect(documentError("11222333000181")).toBeNull();
    expect(documentError("5299")).toBe("CPF/CNPJ inválido");
    expect(documentError("52998224726")).toBe("CPF/CNPJ inválido");
  });
});

describe("telefone", () => {
  it("celular e fixo", () => {
    expect(formatPhone("41999991234")).toBe("(41) 99999-1234");
    expect(formatPhone("4133331234")).toBe("(41) 3333-1234");
    expect(formatPhone("(41) 99999-1234")).toBe("(41) 99999-1234");
    expect(formatPhone("41")).toBe("(41");
    expect(formatPhone("")).toBe("");
  });
});
