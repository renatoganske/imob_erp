import type { Config } from "tailwindcss";

const token = (name: string) => `hsl(var(--${name}))`;

const config: Config = {
  darkMode: ["class", '[data-theme="dark"]'],
  content: ["./src/**/*.{ts,tsx}"],
  theme: {
    container: {
      center: true,
      padding: "1rem",
      screens: { "2xl": "1400px" },
    },
    extend: {
      fontFamily: {
        sans: ["var(--font-inter)", "system-ui", "sans-serif"],
      },
      colors: {
        border: token("border"),
        ring: token("ring"),
        background: token("background"),
        foreground: token("foreground"),
        card: { DEFAULT: token("card"), foreground: token("card-foreground") },
        primary: { DEFAULT: token("primary"), foreground: token("primary-foreground"), soft: token("primary-soft") },
        secondary: { DEFAULT: token("secondary"), foreground: token("secondary-foreground") },
        muted: { DEFAULT: token("muted"), foreground: token("muted-foreground") },
        destructive: { DEFAULT: token("destructive"), foreground: token("destructive-foreground") },
        success: { DEFAULT: token("success"), soft: token("success-soft") },
        warning: { DEFAULT: token("warning"), soft: token("warning-soft") },
        info: { DEFAULT: token("info"), soft: token("info-soft") },
        danger: { DEFAULT: token("danger"), soft: token("danger-soft") },
      },
      borderRadius: {
        lg: "var(--radius)",
        md: "calc(var(--radius) - 2px)",
        sm: "calc(var(--radius) - 4px)",
      },
      boxShadow: {
        card: "0 1px 2px 0 hsl(222 30% 10% / 0.04)",
        pop: "0 8px 24px -8px hsl(222 30% 10% / 0.18)",
      },
    },
  },
  plugins: [],
};

export default config;
