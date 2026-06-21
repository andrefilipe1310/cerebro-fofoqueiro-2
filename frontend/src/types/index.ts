// @path frontend/src/types/index.ts
// @owner frontend
// @responsibility Tipos TypeScript globais — baseados nos contratos da API

// ─── ORGANIZATION ─────────────────────────────────────────────────────────────

export type OrgPlan = 'FREE' | 'STARTER' | 'PRO' | 'ENTERPRISE';
export type OrgStatus = 'ACTIVE' | 'SUSPENDED' | 'CANCELLED';

export interface Org {
  id: string;
  name: string;
  slug: string;
  plan: OrgPlan;
  logo_url: string | null;
  limits: {
    cameras_max: number;
    cameras_current: number;
    users_max: number;
    retention_days: number;
  };
}

/** Opção de organização retornada no login quando usuário tem múltiplas orgs */
export interface OrgOption {
  id: string;
  slug: string;
  name: string;
  logo_url: string | null;
  role: string;
}

// ─── USER ─────────────────────────────────────────────────────────────────────

export type UserRole = 'ADMIN' | 'OPERATOR' | 'VIEWER';

export interface User {
  id: string;
  email: string;
  role: UserRole;
  totp_enabled: boolean;
}

// ─── CAMERA ───────────────────────────────────────────────────────────────────

export type CameraStatus = 'ONLINE' | 'OFFLINE' | 'UNKNOWN';

export interface Camera {
  id: string;
  name: string;
  status: CameraStatus;
  ptz_enabled: boolean;
  lat: number | null;
  lng: number | null;
  location: { id: string; name: string } | null;
  thumbnail_url: string | null;
  created_at: string;
}

export interface CameraDetail extends Camera {
  privacy_zones_count: number;
  health: {
    last_seen_at: string | null;
    recording_confidence: number;
  };
}

export interface StreamUrlResponse {
  type: 'webrtc' | 'hls';
  url: string;
  expires_at: string;
  fallback_hls?: string;
}

// ─── ALERT ────────────────────────────────────────────────────────────────────

export type AlertType = 'CAMERA_OFFLINE' | 'CAMERA_ONLINE' | 'MOTION_DETECTED' | 'LOW_CONFIDENCE';
export type AlertSeverity = 'INFO' | 'WARNING' | 'CRITICAL';
export type AlertStatus = 'TRIGGERED' | 'ACKNOWLEDGED' | 'RESOLVED';

export interface Alert {
  id: string;
  type: AlertType;
  message: string;
  camera_id: string;
  org_id: string;
  severity: AlertSeverity;
  status: AlertStatus;
  triggered_at: string;
  acknowledged_at: string | null;
  acknowledged_by: string | null;
}

// ─── RECORDING ────────────────────────────────────────────────────────────────

export interface Recording {
  id: string;
  started_at: string;
  ended_at: string | null;
  duration_seconds: number | null;
  size_bytes: number | null;
  thumbnail_url: string | null;
}

export interface TimelineSegment {
  started_at: string;
  ended_at: string;
  recording_id: string;
  has_motion: boolean;
}

export interface TimelineGap {
  from: string;
  to: string;
  reason?: string;
}

export interface Timeline {
  camera_id: string;
  from: string;
  to: string;
  segments: TimelineSegment[];
  gaps: TimelineGap[];
}

// ─── API ──────────────────────────────────────────────────────────────────────

export interface PaginatedResponse<T> {
  data: T[];
  pagination: {
    next_cursor: string | null;
    has_more: boolean;
    limit: number;
  };
}

export interface ApiError {
  error: {
    code: string;
    message: string;
    status: number;
    timestamp: string;
    request_id?: string;
  };
}

// ─── AUTH ─────────────────────────────────────────────────────────────────────

export interface LoginResponse {
  access_token: string;
  refresh_token?: string;
  expires_in: number;
  requires_2fa: boolean;
  requires_org_selection: boolean;
  temp_token?: string;
  user_id?: string;
  org_id?: string;
  role?: string;
  orgs?: OrgOption[];
}

export interface AuthState {
  user: User | null;
  org: Org | null;
  isAuthenticated: boolean;
}
