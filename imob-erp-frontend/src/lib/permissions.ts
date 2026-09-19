import type { Role } from "@/types/common";

const ROLES: Role[] = ["ADMIN", "CORRETOR", "FINANCEIRO"];

// Mesmo contrato do backend: o papel vem em publicMetadata.role. Qualquer valor desconhecido vira undefined.
export function parseRole(value: unknown): Role | undefined {
  return ROLES.find((role) => role === value);
}

// Espelha o @PreAuthorize do ContractController. Sem papel reconhecido, nega.
export function canManageContracts(role: Role | undefined): boolean {
  return role === "ADMIN" || role === "FINANCEIRO";
}

const CONTRACTS_PATH = /^\/contratos(\/|$)/;

export function canAccessPath(role: Role | undefined, pathname: string): boolean {
  return CONTRACTS_PATH.test(pathname) ? canManageContracts(role) : true;
}
