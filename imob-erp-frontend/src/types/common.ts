export type Role = "ADMIN" | "CORRETOR" | "FINANCEIRO";

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ApiError {
  error: string;
  code: string;
}
