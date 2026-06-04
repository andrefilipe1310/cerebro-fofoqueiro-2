// @path frontend/src/lib/utils/cn.ts
// @owner frontend
// @responsibility Helper para combinar class names — usado pelos componentes Shadcn
import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
