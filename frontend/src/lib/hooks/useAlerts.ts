// @path frontend/src/lib/hooks/useAlerts.ts
// @owner frontend
// @responsibility Hook de alertas — lista REST + WebSocket STOMP em tempo real
// @see docs/API_CONTRACTS.md#alerts-list | docs/API_CONTRACTS.md#ws-alerts
'use client';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import { alertsApi } from '@/lib/api/alerts';

export function useAlerts(params?: { acknowledged?: boolean }) {
  return useQuery({
    queryKey: ['alerts', params],
    queryFn: () => alertsApi.list(params).then((r) => r.data),
    staleTime: 15_000,
  });
}

export function useAcknowledgeAlert() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => alertsApi.acknowledge(id).then((r) => r.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['alerts'] });
    },
  });
}

// Hook para receber alertas em tempo real via WebSocket STOMP
// @see docs/API_CONTRACTS.md#ws-alerts
export function useAlertWebSocket(tenantId: string, onAlert: (alert: unknown) => void) {
  const wsUrl = process.env.NEXT_PUBLIC_WS_URL ?? 'ws://localhost:8000/ws';
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    if (!tenantId) return;

    const token = sessionStorage.getItem('access_token');
    const client = new Client({
      brokerURL: `${wsUrl}/alerts?token=${token}`,
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(`/topic/tenant/${tenantId}/alerts`, (message) => {
          onAlert(JSON.parse(message.body));
        });
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [tenantId, onAlert, wsUrl]);
}
