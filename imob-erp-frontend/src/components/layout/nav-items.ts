import {
  Building2,
  Users,
  CalendarCheck,
  FileText,
  Wallet,
  Percent,
  Settings,
  type LucideIcon,
} from "lucide-react";

export interface NavItem {
  href: string;
  label: string;
  icon: LucideIcon;
}

export const NAV_ITEMS: NavItem[] = [
  { href: "/imoveis", label: "Imóveis", icon: Building2 },
  { href: "/leads", label: "Leads", icon: Users },
  { href: "/visitas", label: "Visitas", icon: CalendarCheck },
  { href: "/contratos", label: "Contratos", icon: FileText },
  { href: "/financeiro", label: "Financeiro", icon: Wallet },
  { href: "/comissoes", label: "Comissões", icon: Percent },
  { href: "/configuracoes/usuarios", label: "Usuários", icon: Settings },
];
