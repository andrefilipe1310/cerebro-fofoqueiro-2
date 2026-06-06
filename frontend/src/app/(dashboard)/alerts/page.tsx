'use client';

import { useEffect, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Client } from '@stomp/stompjs';
import { Bell, CheckCircle, AlertTriangle, Info } from 'lucide-react';
import { apiClient } from '@/lib/api/client';
import { useAuthStore } from '@/stores/auth.store';
import type { Alert } from '@/types';

const severityIcon = (s: string) => {
  if (s === 'CRITICAL') return <AlertTriangle className="h-4 w-4 text-red-500" />;
  if (s === 'WARNING') return <Bell className="h-4 w-4 text-yellow-500" />;
  return <Info className="h-4 w-4 text-blue-500" />;
};

const severityClass = (s: string) => {
  if (s === 'CRITICAL') return 'border-red-200 bg-red-50';
  if (s === 'WARNING') return 'border-yellow-200 bg-yellow-50';
  return 'border-blue-200 bg-blue-50';
};

interface AlertsPage { content: Alert[]; totalElements: number; }

export default function AlertsPage() {
  const qc = useQueryClient();
  const { tenant } = useAuthStore();
  const [statusFilter, setStatusFilter] = useState<'TRIGGERED' | 'ACKNOWLEDGED' | ''>('TRIGGERED');
  const [toast, setToast] = useState<string | null>(null);

  const { data, isLoading } = useQuery<AlertsPage>({
    queryKey: ['alerts', statusFilter],
    queryFn: () => apiClient.get(`/alerts${statusFilter ? `?status=${statusFilter}` : ''}`).then(r => r.data),
    refetchInterval: 30_000,
  });

  const acknowledge = useMutation({
    mutationFn: (id: string) => apiClient.patch(`/alerts/${id}/acknowledge`).then(r => r.data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['alerts'] }),
  });

  useEffect(() => {
    if (!tenant?.id) return;
    const wsUrl = (process.env.NEXT_PUBLIC_WS_URL ?? 'http://localhost:8086') + '/ws';
    const token = sessionStorage.getItem('access_token') ?? '';

    const client = new Client({
      webSocketFactory: () => {
        const SockJS = require('sockjs-client');
        return new SockJS(`${wsUrl}?token=${token}`);
      },
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(`/topic/tenant/${tenant.id}/alerts`, (msg) => {
          const alert = JSON.parse(msg.body);
          setToast(`Novo alerta: ${alert.message}`);
          qc.invalidateQueries({ queryKey: ['alerts'] });
          setTimeout(() => setToast(null), 5000);
        });
      },
    });
    client.activate();
    return () => { client.deactivate(); };
  }, [tenant?.id, qc]);

  const alerts = data?.content ?? [];

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-semibold">Alertas</h1>
        <div className="flex gap-1 border rounded-md p-1">
          {[['', 'Todos'], ['TRIGGERED', 'Ativos'], ['ACKNOWLEDGED', 'Reconhecidos']].map(([v, l]) => (
            <button key={v} onClick={() => setStatusFilter(v as '' | 'TRIGGERED' | 'ACKNOWLEDGED')}
              className={`px-3 py-1 rounded text-sm font-medium transition-colors ${statusFilter === v ? 'bg-primary text-primary-foreground' : 'hover:bg-accent'}`}>
              {l}
            </button>
          ))}
        </div>
      </div>

      {isLoading ? (
        <div className="text-muted-foreground text-sm">Carregando alertas...</div>
      ) : alerts.length === 0 ? (
        <div className="text-center py-16 text-muted-foreground">Nenhum alerta encontrado.</div>
      ) : (
        <div className="space-y-3">
          {alerts.map((alert) => (
            <div key={alert.id} className={`rounded-lg border p-4 ${severityClass(alert.severity)}`}>
              <div className="flex items-start gap-3">
                <div className="mt-0.5">{severityIcon(alert.severity)}</div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-sm font-semibold">{alert.type.replace('_', ' ')}</span>
                    <span className="text-xs text-muted-foreground">{new Date(alert.triggered_at).toLocaleString('pt-BR')}</span>
                  </div>
                  <p className="text-sm">{alert.message}</p>
                </div>
                {!alert.acknowledged_at && (
                  <button onClick={() => acknowledge.mutate(alert.id)}
                    className="shrink-0 flex items-center gap-1.5 px-3 py-1.5 rounded-md bg-white border text-sm font-medium hover:bg-gray-50">
                    <CheckCircle className="h-3.5 w-3.5" /> Reconhecer
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {toast && (
        <div className="fixed bottom-4 right-4 z-50 bg-foreground text-background px-4 py-3 rounded-lg shadow-lg text-sm max-w-sm">
          {toast}
        </div>
      )}
    </div>
  );
}
