import type { ApiError } from "@/types/common";

const API_URL = process.env.NEXT_PUBLIC_API_URL;

export class ApiRequestError extends Error {
  code: string;
  status: number;

  constructor(status: number, body: ApiError) {
    super(body.error);
    this.code = body.code;
    this.status = status;
  }
}

interface RequestOptions extends RequestInit {
  getToken?: () => Promise<string | null>;
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { getToken, headers, ...rest } = options;
  const token = getToken ? await getToken() : null;

  const response = await fetch(`${API_URL}${path}`, {
    ...rest,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...headers,
    },
  });

  if (!response.ok) {
    const body = (await response.json().catch(() => ({ error: response.statusText, code: "UNKNOWN" }))) as ApiError;
    throw new ApiRequestError(response.status, body);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export const api = {
  get: <T>(path: string, options?: RequestOptions) => request<T>(path, { ...options, method: "GET" }),
  post: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: "POST", body: body ? JSON.stringify(body) : undefined }),
  put: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: "PUT", body: body ? JSON.stringify(body) : undefined }),
  patch: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: "PATCH", body: body ? JSON.stringify(body) : undefined }),
  delete: <T>(path: string, options?: RequestOptions) => request<T>(path, { ...options, method: "DELETE" }),
};
