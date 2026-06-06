'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, Pencil, Trash2, Wifi, WifiOff } from 'lucide-react';
import { apiClient } from '@/lib/api/client';
import type { Camera } from '@/types';

interface Page<T> { content: T[]; totalElements: number; }

interface CreateCameraForm {
  name: string; rtspUrl: string; locationId: string; lat: string; lng: string; ptzEnabled: boolean;
}

export default function CamerasPage() {
  const qc = useQueryClient();
  const [showModal, setShowModal] = useState(false);
  const [editingCamera, setEditingCamera] = useState<Camera | null>(null);
  const [form, setForm] = useState<CreateCameraForm>({ name: '', rtspUrl: '', locationId: '', lat: '', lng: '', ptzEnabled: false });

  const { data, isLoading } = useQuery<Page<Camera>>({
    queryKey: ['cameras'],
    queryFn: () => apiClient.get('/cameras').then(r => r.data),
  });

  const createCamera = useMutation({
    mutationFn: (payload: object) => apiClient.post('/cameras', payload).then(r => r.data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['cameras'] }); setShowModal(false); resetForm(); },
  });

  const updateCamera = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: object }) =>
      apiClient.put(`/cameras/${id}`, payload).then(r => r.data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['cameras'] }); setShowModal(false); setEditingCamera(null); resetForm(); },
  });

  const deleteCamera = useMutation({
    mutationFn: (id: string) => apiClient.delete(`/cameras/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['cameras'] }),
  });

  const resetForm = () => setForm({ name: '', rtspUrl: '', locationId: '', lat: '', lng: '', ptzEnabled: false });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const payload = { name: form.name, rtsp_url: form.rtspUrl, lat: form.lat ? parseFloat(form.lat) : null, lng: form.lng ? parseFloat(form.lng) : null, ptz_enabled: form.ptzEnabled };
    if (editingCamera) {
      updateCamera.mutate({ id: editingCamera.id, payload });
    } else {
      createCamera.mutate(payload);
    }
  };

  const openEdit = (camera: Camera) => {
    setEditingCamera(camera);
    setForm({ name: camera.name, rtspUrl: '', locationId: '', lat: camera.lat?.toString() ?? '', lng: camera.lng?.toString() ?? '', ptzEnabled: camera.ptz_enabled });
    setShowModal(true);
  };

  const cameras = data?.content ?? [];

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-semibold">Câmeras</h1>
        <button onClick={() => { resetForm(); setEditingCamera(null); setShowModal(true); }}
          className="flex items-center gap-2 px-4 py-2 rounded-md bg-primary text-primary-foreground text-sm font-medium hover:bg-primary/90">
          <Plus className="h-4 w-4" /> Adicionar Câmera
        </button>
      </div>

      {isLoading ? (
        <div className="text-muted-foreground text-sm">Carregando câmeras...</div>
      ) : cameras.length === 0 ? (
        <div className="text-center py-16 text-muted-foreground">
          <p>Nenhuma câmera cadastrada.</p>
          <p className="text-sm mt-1">Clique em "Adicionar Câmera" para começar.</p>
        </div>
      ) : (
        <div className="rounded-md border overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-muted/50">
              <tr>
                <th className="px-4 py-3 text-left font-medium">Nome</th>
                <th className="px-4 py-3 text-left font-medium">Status</th>
                <th className="px-4 py-3 text-left font-medium">PTZ</th>
                <th className="px-4 py-3 text-left font-medium">Localização</th>
                <th className="px-4 py-3 text-right font-medium">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {cameras.map((cam) => (
                <tr key={cam.id} className="hover:bg-muted/25">
                  <td className="px-4 py-3 font-medium">{cam.name}</td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex items-center gap-1.5 text-xs font-medium px-2 py-1 rounded-full ${
                      cam.status === 'ONLINE' ? 'bg-green-100 text-green-700' :
                      cam.status === 'OFFLINE' ? 'bg-red-100 text-red-700' :
                      'bg-gray-100 text-gray-700'
                    }`}>
                      {cam.status === 'ONLINE' ? <Wifi className="h-3 w-3" /> : <WifiOff className="h-3 w-3" />}
                      {cam.status}
                    </span>
                  </td>
                  <td className="px-4 py-3">{cam.ptz_enabled ? 'Sim' : 'Não'}</td>
                  <td className="px-4 py-3 text-muted-foreground">{cam.location?.name ?? '—'}</td>
                  <td className="px-4 py-3 text-right">
                    <div className="flex items-center gap-2 justify-end">
                      <button onClick={() => openEdit(cam)} className="p-1.5 rounded hover:bg-accent"><Pencil className="h-3.5 w-3.5" /></button>
                      <button onClick={() => { if (confirm('Deletar câmera?')) deleteCamera.mutate(cam.id); }}
                        className="p-1.5 rounded hover:bg-accent text-destructive"><Trash2 className="h-3.5 w-3.5" /></button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="bg-card rounded-xl border shadow-lg w-full max-w-md p-6">
            <h2 className="text-lg font-semibold mb-4">{editingCamera ? 'Editar Câmera' : 'Nova Câmera'}</h2>
            <form onSubmit={handleSubmit} className="space-y-3">
              <div>
                <label className="block text-sm font-medium mb-1">Nome</label>
                <input value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                  className="w-full px-3 py-2 rounded-md border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring" required />
              </div>
              {!editingCamera && (
                <div>
                  <label className="block text-sm font-medium mb-1">URL RTSP</label>
                  <input value={form.rtspUrl} onChange={e => setForm(f => ({ ...f, rtspUrl: e.target.value }))}
                    placeholder="rtsp://user:pass@192.168.1.100:554/stream"
                    className="w-full px-3 py-2 rounded-md border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring" required />
                </div>
              )}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-sm font-medium mb-1">Latitude</label>
                  <input value={form.lat} onChange={e => setForm(f => ({ ...f, lat: e.target.value }))}
                    type="number" step="any" className="w-full px-3 py-2 rounded-md border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring" />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">Longitude</label>
                  <input value={form.lng} onChange={e => setForm(f => ({ ...f, lng: e.target.value }))}
                    type="number" step="any" className="w-full px-3 py-2 rounded-md border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring" />
                </div>
              </div>
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" checked={form.ptzEnabled} onChange={e => setForm(f => ({ ...f, ptzEnabled: e.target.checked }))} />
                PTZ habilitado
              </label>
              <div className="flex gap-2 pt-2">
                <button type="button" onClick={() => { setShowModal(false); setEditingCamera(null); resetForm(); }}
                  className="flex-1 py-2 rounded-md border text-sm hover:bg-accent">Cancelar</button>
                <button type="submit" disabled={createCamera.isPending || updateCamera.isPending}
                  className="flex-1 py-2 rounded-md bg-primary text-primary-foreground text-sm font-medium hover:bg-primary/90 disabled:opacity-50">
                  {createCamera.isPending || updateCamera.isPending ? 'Salvando...' : 'Salvar'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
