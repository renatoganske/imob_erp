"use client";

import { useUser } from "@clerk/nextjs";
import { parseRole } from "@/lib/permissions";
import type { Role } from "@/types/common";

// Papel do usuário logado (publicMetadata.role). Indefinido enquanto carrega ou se ausente/inválido.
export function useRole(): Role | undefined {
  const { user } = useUser();
  return parseRole(user?.publicMetadata?.role);
}
