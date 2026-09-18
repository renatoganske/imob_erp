"use client";

import { useAuth } from "@clerk/nextjs";
import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { PageResponse } from "@/types/common";
import type { Commission, CommissionReportItem, CommissionStatus } from "@/types/commission";

export function useCommissions(params: { agentId?: string; status?: CommissionStatus } = {}) {
  const { getToken } = useAuth();
  const [data, setData] = useState<PageResponse<Commission> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const query = new URLSearchParams(
    Object.entries(params).filter(([, v]) => v !== undefined) as [string, string][]
  ).toString();

  const reload = useCallback(() => {
    setLoading(true);
    api
      .get<PageResponse<Commission>>(`/api/v1/commissions?${query}`, { getToken })
      .then(setData)
      .catch(setError)
      .finally(() => setLoading(false));
  }, [query, getToken]);

  useEffect(() => {
    reload();
  }, [reload]);

  return { data, loading, error, reload };
}

export function useCommissionReport(params: { from?: string; to?: string } = {}) {
  const { getToken } = useAuth();
  const [data, setData] = useState<CommissionReportItem[]>([]);
  const [loading, setLoading] = useState(true);

  const query = new URLSearchParams(
    Object.entries(params).filter(([, v]) => v !== undefined) as [string, string][]
  ).toString();

  useEffect(() => {
    api
      .get<CommissionReportItem[]>(`/api/v1/commissions/report?${query}`, { getToken })
      .then(setData)
      .finally(() => setLoading(false));
  }, [query, getToken]);

  return { data, loading };
}

export function useCommissionMutations() {
  const { getToken } = useAuth();

  return {
    pay: (id: string) => api.patch<Commission>(`/api/v1/commissions/${id}/pay`, undefined, { getToken }),
  };
}
