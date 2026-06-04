// @path frontend/src/lib/utils/date.ts
// @owner frontend
// @responsibility Formatadores de data/hora com date-fns em pt-BR
import { format, formatDistanceToNow, parseISO } from 'date-fns';
import { ptBR } from 'date-fns/locale';

export const formatDateTime = (iso: string): string =>
  format(parseISO(iso), "dd/MM/yyyy HH:mm:ss", { locale: ptBR });

export const formatDate = (iso: string): string =>
  format(parseISO(iso), "dd/MM/yyyy", { locale: ptBR });

export const formatRelative = (iso: string): string =>
  formatDistanceToNow(parseISO(iso), { addSuffix: true, locale: ptBR });

export const formatDuration = (seconds: number): string => {
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  if (h > 0) return `${h}h ${m}m ${s}s`;
  if (m > 0) return `${m}m ${s}s`;
  return `${s}s`;
};
