"use client";

import { useAuth } from "@clerk/nextjs";
import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { AppUser } from "@/types/user";

export function useUsers() {
  const { getToken } = useAuth();
  const [data, setData] = useState<AppUser[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api
      .get<AppUser[]>("/api/v1/users", { getToken })
      .then(setData)
      .catch(() => setData([]))
      .finally(() => setLoading(false));
  }, [getToken]);

  return { data, loading };
}
