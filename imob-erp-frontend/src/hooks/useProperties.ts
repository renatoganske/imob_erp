"use client";

import { useAuth } from "@clerk/nextjs";
import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { PageResponse } from "@/types/common";
import type { Property, PropertyFilters, PropertyRequest } from "@/types/property";

function toQueryString(filters: PropertyFilters): string {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== "") params.set(key, String(value));
  });
  return params.toString();
}

export function useProperties(filters: PropertyFilters = {}) {
  const { getToken } = useAuth();
  const [data, setData] = useState<PageResponse<Property> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const query = toQueryString(filters);

  const reload = useCallback(() => {
    setLoading(true);
    api
      .get<PageResponse<Property>>(`/api/v1/properties?${query}`, { getToken })
      .then(setData)
      .catch(setError)
      .finally(() => setLoading(false));
  }, [query, getToken]);

  useEffect(() => {
    reload();
  }, [reload]);

  return { data, loading, error, reload };
}

export function usePropertyMutations() {
  const { getToken } = useAuth();

  return {
    create: (payload: PropertyRequest) => api.post<Property>("/api/v1/properties", payload, { getToken }),
    update: (id: string, payload: PropertyRequest) =>
      api.put<Property>(`/api/v1/properties/${id}`, payload, { getToken }),
    updateStatus: (id: string, status: Property["status"]) =>
      api.patch<Property>(`/api/v1/properties/${id}/status`, { status }, { getToken }),
    remove: (id: string) => api.delete<void>(`/api/v1/properties/${id}`, { getToken }),
  };
}
