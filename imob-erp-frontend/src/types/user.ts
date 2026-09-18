import type { Role } from "./common";

export interface AppUser {
  id: string;
  name: string;
  email: string;
  role: Role;
  commissionRate?: number;
  active: boolean;
}

export type UserInviteRequest = Pick<AppUser, "name" | "email" | "role">;
