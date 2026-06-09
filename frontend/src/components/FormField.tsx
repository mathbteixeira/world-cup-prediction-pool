import React, { useId } from "react";
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
  const generatedId = useId();
  const fieldId = fieldIdFor(children) ?? generatedId;
  const labelledChildren = React.isValidElement(children)
    ? React.cloneElement(children, { id: fieldId } as { id: string })
    : children;

  return (
    <div className="space-y-2">
      <Label htmlFor={fieldId}>{label}</Label>
      {labelledChildren}
      {error ? <p className="text-xs font-medium text-destructive">{error.message}</p> : null}
    </div>
  );
}

function fieldIdFor(children: React.ReactNode) {
  return React.isValidElement<{ id?: string; name?: string }>(children)
    ? children.props.id ?? children.props.name
    : undefined;
}
