import type { FieldError } from "react-hook-form";
import { Label } from "./ui/label";

export function FormField({
  label,
  error,
  children,
}: {
  label: string;
  error?: FieldError;
  children: React.ReactNode;
}) {
  return (
    <div className="space-y-2">
      <Label>{label}</Label>
      {children}
      {error ? <p className="text-xs font-medium text-destructive">{error.message}</p> : null}
    </div>
  );
}
