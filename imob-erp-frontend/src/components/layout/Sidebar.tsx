import { Brand, NavLinks } from "./NavLinks";

export function Sidebar() {
  return (
    <aside className="sticky top-0 hidden h-screen w-60 shrink-0 flex-col border-r border-border bg-card p-4 md:flex">
      <Brand />
      <NavLinks />
    </aside>
  );
}
