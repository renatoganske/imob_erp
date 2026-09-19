import { UserButton } from "@clerk/nextjs";
import { MobileNav } from "./MobileNav";
import { ThemeToggle } from "./ThemeToggle";

export function Header() {
  return (
    <header className="sticky top-0 z-30 flex h-14 items-center justify-between gap-3 border-b border-border bg-background/85 px-4 backdrop-blur sm:px-6">
      <div className="flex min-w-0 items-center gap-3">
        <MobileNav />
      </div>
      <div className="flex items-center gap-2">
        <ThemeToggle />
        <UserButton afterSignOutUrl="/sign-in" />
      </div>
    </header>
  );
}
