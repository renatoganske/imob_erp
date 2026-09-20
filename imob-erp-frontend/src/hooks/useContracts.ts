"use client";

import { useAuth } from "@clerk/nextjs";
import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { PageResponse } from "@/types/common";
import type { Contract, ContractRequest, ContractStatus, ContractType, ExpiringSummary } from "@/types/contract";

export function useContracts(
  params: { status?: ContractStatus; type?: ContractType; expiringInDays?: number; size?: number } = {},
) {
  const { getToken } = useAuth();
  const [data, setData] = useState<PageResponse<Contract> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const query = new URLSearchParams(
    Object.entries(params).filter(([, v]) => v !== undefined) as [string, string][]
  ).toString();

  const reload = useCallback(() => {
    setLoading(true);
    api
      .get<PageResponse<Contract>>(`/api/v1/contracts?${query}`, { getToken })
      .then(setData)
      .catch(setError)
      .finally(() => setLoading(false));
  }, [query, getToken]);

  useEffect(() => {
    reload();
  }, [reload]);

  return { data, loading, error, reload };
}

// Só Admin e Financeiro podem ler o resumo (403 para o corretor): `enabled` evita a chamada para os demais papéis.
export function useExpiringContractsSummary(enabled: boolean) {
  const { getToken } = useAuth();
  const [data, setData] = useState<ExpiringSummary | null>(null);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    if (!enabled) return;
    let active = true;
    api
      .get<ExpiringSummary>("/api/v1/contracts/expiring-summary", { getToken })
      .then((summary) => active && setData(summary))
      .catch((err) => active && setError(err));
    return () => {
      active = false;
    };
  }, [enabled, getToken]);

  return { data, error };
}

export function useContractMutations() {
  const { getToken } = useAuth();

  return {
    create: (payload: ContractRequest) => api.post<Contract>("/api/v1/contracts", payload, { getToken }),
    update: (id: string, payload: ContractRequest) =>
      api.put<Contract>(`/api/v1/contracts/${id}`, payload, { getToken }),
    updateStatus: (id: string, status: ContractStatus) =>
      api.patch<Contract>(`/api/v1/contracts/${id}/status`, { status }, { getToken }),
  };
}
