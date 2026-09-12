export type Env = {
  DB: D1Database;
  BACKUPS: R2Bucket;
  SALT_EDGE_APP_ID: string;
  SALT_EDGE_SECRET: string;
  AUTH_JWT_SECRET: string;
  SALTEDGE_CALLBACK_PUBLIC_KEY?: string;
  SALT_EDGE_BASE_URL?: string;
  ENVIRONMENT?: string;
  ALLOWED_ORIGINS?: string;
};

export type AuthUser = {
  id: string;
  email: string;
};

export type QuadrantIndex = 0 | 1 | 2 | 3;

export type SaltEdgeProvider = {
  code: string;
  name: string;
  country_code?: string;
  mode?: string;
  regulated?: boolean;
  logo_url?: string;
  supported_iframe_embedding?: boolean;
};

export type SaltEdgeConnection = {
  id: string;
  customer_id: string;
  provider_code: string;
  provider_name: string;
  partner_consent_id?: string;
  status?: string;
  last_success_at?: string;
  last_attempt?: string;
  created_at?: string;
  updated_at?: string;
};

export type SaltEdgePartnerConsent = {
  id: string;
  connection_id: string;
  customer_id: string;
  status: 'active' | 'revoked' | string;
  revoked_at?: string;
  created_at?: string;
  updated_at?: string;
};

export type SaltEdgeAccount = {
  id: string;
  connection_id: string;
  name: string;
  nature?: string;
  balance?: number;
  currency_code: string;
  updated_at?: string;
};

export type SaltEdgeTransaction = {
  id: string;
  account_id: string;
  amount: number | string;
  currency_code: string;
  description?: string;
  category?: string;
  status?: string;
  made_on: string;
  updated_at?: string;
  created_at?: string;
  extra?: {
    merchant_name?: string;
    original_amount?: number | string;
    original_currency_code?: string;
    posting_date?: string;
    time?: string;
  };
};
