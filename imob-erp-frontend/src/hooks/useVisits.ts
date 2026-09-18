"use client";

import { useAuth } from "@clerk/nextjs";
import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { PageResponse } from "@/types/common";
import type { Visit, VisitRequest, VisitStatus } from "@/types/visit";

export function useVisits(params: { agentId?: string } = {}) {
  const { getToken } = useAuth();
  const [data, setData] = useState<PageResponse<Visit> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const query = new URLSearchParams(
    Object.entries(params).filter(([, v]) => v !== undefined) as [string, string][]
  ).toString();

  const reload = useCallback(() => {
    setLoading(true);
    api
      .get<PageResponse<Visit>>(`/api/v1/visits?${query}`, { getToken })
      .then(setData)
      .catch(setError)
      .finally(() => setLoading(false));
  }, [query, getToken]);

  useEffect(() => {
    reload();
  }, [reload]);

  return { data, loading, error, reload };
}

export function useVisitMutations() {
  const { getToken } = useAuth();

  return {
    create: (payload: VisitRequest) => api.post<Visit>("/api/v1/visits", payload, { getToken }),
    updateStatus: (id: string, status: VisitStatus) =>
      api.patch<Visit>(`/api/v1/visits/${id}/status`, { status }, { getToken }),
    updateResult: (id: string, result: string) =>
      api.patch<Visit>(`/api/v1/visits/${id}/result`, { result }, { getToken }),
  };
}
