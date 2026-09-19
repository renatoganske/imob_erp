import type { Role } from "@/types/common";

const ROLES: Role[] = ["ADMIN", "CORRETOR", "FINANCEIRO"];

// Mesmo contrato do backend: o papel vem em publicMetadata.role. Qualquer valor desconhecido vira undefined.
export function parseRole(value: unknown): Role | undefined {
  return ROLES.find((role) => role === value);
}

// Espelham o @PreAuthorize do ContractController. Sem papel reconhecido, nega.
// Escrita (criar, editar, ativar, enviar PDF): Admin e Financeiro.
export function canManageContracts(role: Role | undefined): boolean {
  return role === "ADMIN" || role === "FINANCEIRO";
}

// Leitura: o corretor também lê, mas o backend só devolve os contratos dele e sem CPF/CNPJ nem PDF (IMOB-35).
export function canReadContracts(role: Role | undefined): boolean {
  return canManageContracts(role) || role === "CORRETOR";
}

const CONTRACTS_PATH = /^\/contratos(\/|$)/;
const CONTRACT_CREATE_PATH = /^\/contratos\/novo(\/|$)/;

export function canAccessPath(role: Role | undefined, pathname: string): boolean {
  if (CONTRACT_CREATE_PATH.test(pathname)) return canManageContracts(role);
  if (CONTRACTS_PATH.test(pathname)) return canReadContracts(role);
  return true;
}
