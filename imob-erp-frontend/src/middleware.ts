import { clerkMiddleware, createRouteMatcher } from "@clerk/nextjs/server";
import { NextResponse } from "next/server";
import { canAccessPath, parseRole } from "@/lib/permissions";

const isPublicRoute = createRouteMatcher(["/sign-in(.*)", "/sign-up(.*)"]);

export default clerkMiddleware(async (auth, req) => {
  if (isPublicRoute(req)) return;

  const { sessionClaims } = await auth.protect();
  // O token de sessão carrega publicMetadata (mesmo claim que o backend valida). Sem papel reconhecido, nega.
  const role = parseRole((sessionClaims?.publicMetadata as { role?: unknown } | undefined)?.role);
  if (!canAccessPath(role, req.nextUrl.pathname)) {
    return NextResponse.redirect(new URL("/", req.url));
  }
});

export const config = {
  matcher: [
    "/((?!_next|[^?]*\\.(?:html?|css|js(?!on)|jpe?g|webp|png|gif|svg|ttf|woff2?|ico|csv|docx?|xlsx?|zip|webmanifest)).*)",
    "/(api|trpc)(.*)",
    "/__clerk/:path*",
  ],
};
