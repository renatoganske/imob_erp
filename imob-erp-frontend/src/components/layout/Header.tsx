import { UserButton } from "@clerk/nextjs";
import { MobileNav } from "./MobileNav";

export function Header() {
  return (
    <header className="flex h-16 items-center justify-between gap-3 border-b border-border px-4 sm:px-6">
      <div className="flex min-w-0 items-center gap-3">
        <MobileNav />
        <div className="truncate text-sm text-muted-foreground">Do lead ao recebimento, sem sair do sistema.</div>
      </div>
      <UserButton afterSignOutUrl="/sign-in" />
    </header>
  );
}
