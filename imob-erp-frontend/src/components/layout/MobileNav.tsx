"use client";

import * as DialogPrimitive from "@radix-ui/react-dialog";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { Menu, X } from "lucide-react";
import { Brand, NavLinks } from "./NavLinks";

// O menu vive num portal (Radix Dialog): o header usa backdrop-blur, que prende `position: fixed`
// ao próprio header e cortaria o menu. O Dialog também cuida de foco, Esc e bloqueio de rolagem.
export function MobileNav() {
  const pathname = usePathname();
  const [open, setOpen] = useState(false);

  useEffect(() => {
    setOpen(false);
  }, [pathname]);

  return (
    <div className="md:hidden">
      <DialogPrimitive.Root open={open} onOpenChange={setOpen}>
        <DialogPrimitive.Trigger
          aria-label="Abrir menu"
          className="flex h-9 w-9 items-center justify-center rounded-md text-muted-foreground hover:bg-secondary"
        >
          <Menu className="h-5 w-5" />
        </DialogPrimitive.Trigger>

        <DialogPrimitive.Portal>
          <DialogPrimitive.Overlay className="fixed inset-0 z-50 bg-black/40" />
          <DialogPrimitive.Content
            aria-describedby={undefined}
            className="fixed inset-y-0 left-0 z-50 flex w-64 max-w-[80vw] flex-col bg-card p-4 shadow-pop focus:outline-none"
          >
            <DialogPrimitive.Title className="sr-only">Menu principal</DialogPrimitive.Title>
            <DialogPrimitive.Close
              aria-label="Fechar menu"
              className="absolute right-3 top-3 flex h-8 w-8 items-center justify-center rounded-md text-muted-foreground hover:bg-secondary"
            >
              <X className="h-4 w-4" />
            </DialogPrimitive.Close>
            <Brand />
            <NavLinks />
          </DialogPrimitive.Content>
        </DialogPrimitive.Portal>
      </DialogPrimitive.Root>
    </div>
  );
}
