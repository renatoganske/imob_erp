"use client";

import { useAuth } from "@clerk/nextjs";
import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { PageResponse } from "@/types/common";
import type { Lead, LeadRequest, LeadStage } from "@/types/lead";

export function useLeads(params: { assignedTo?: string; stage?: LeadStage } = {}) {
  const { getToken } = useAuth();
  const [data, setData] = useState<PageResponse<Lead> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const query = new URLSearchParams(
    Object.entries(params).filter(([, v]) => v !== undefined) as [string, string][]
  ).toString();

  const reload = useCallback(() => {
    setLoading(true);
    api
      .get<PageResponse<Lead>>(`/api/v1/leads?${query}`, { getToken })
      .then(setData)
      .catch(setError)
      .finally(() => setLoading(false));
  }, [query, getToken]);

  useEffect(() => {
    reload();
  }, [reload]);

  return { data, loading, error, reload };
}

export function useLeadMutations() {
  const { getToken } = useAuth();

  return {
    create: (payload: LeadRequest) => api.post<Lead>("/api/v1/leads", payload, { getToken }),
    update: (id: string, payload: LeadRequest) => api.put<Lead>(`/api/v1/leads/${id}`, payload, { getToken }),
    updateStage: (id: string, stage: LeadStage) =>
      api.patch<Lead>(`/api/v1/leads/${id}/stage`, { stage }, { getToken }),
    assign: (id: string, agentId: string) =>
      api.patch<Lead>(`/api/v1/leads/${id}/assign`, { agentId }, { getToken }),
  };
}
