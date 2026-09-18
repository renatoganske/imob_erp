"use client";

import { useAuth } from "@clerk/nextjs";
import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { PageResponse } from "@/types/common";
import type {
  FinancialDashboard,
  FinancialEntry,
  FinancialEntryRequest,
  FinancialStatus,
  FinancialType,
} from "@/types/financial";

export function useFinancialEntries(params: { type?: FinancialType; status?: FinancialStatus } = {}) {
  const { getToken } = useAuth();
  const [data, setData] = useState<PageResponse<FinancialEntry> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const query = new URLSearchParams(
    Object.entries(params).filter(([, v]) => v !== undefined) as [string, string][]
  ).toString();

  const reload = useCallback(() => {
    setLoading(true);
    api
      .get<PageResponse<FinancialEntry>>(`/api/v1/financial/entries?${query}`, { getToken })
      .then(setData)
      .catch(setError)
      .finally(() => setLoading(false));
  }, [query, getToken]);

  useEffect(() => {
    reload();
  }, [reload]);

  return { data, loading, error, reload };
}

export function useFinancialDashboard() {
  const { getToken } = useAuth();
  const [data, setData] = useState<FinancialDashboard | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api
      .get<FinancialDashboard>("/api/v1/financial/dashboard", { getToken })
      .then(setData)
      .finally(() => setLoading(false));
  }, [getToken]);

  return { data, loading };
}

export function useFinancialMutations() {
  const { getToken } = useAuth();

  return {
    create: (payload: FinancialEntryRequest) =>
      api.post<FinancialEntry>("/api/v1/financial/entries", payload, { getToken }),
    pay: (id: string) => api.patch<FinancialEntry>(`/api/v1/financial/entries/${id}/pay`, undefined, { getToken }),
    cancel: (id: string) =>
      api.patch<FinancialEntry>(`/api/v1/financial/entries/${id}/cancel`, undefined, { getToken }),
  };
}
