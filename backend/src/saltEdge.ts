import type {
  Env,
  SaltEdgeAccount,
  SaltEdgeConnection,
  SaltEdgeProvider,
  SaltEdgePartnerConsent,
  SaltEdgeTransaction,
} from './types';

type SaltEdgeEnvelope<T> = {
  data: T;
  meta?: {
    next_page?: string;
    next_id?: string;
  };
};

export class SaltEdgeError extends Error {
  constructor(public readonly status: number, message: string) {
    super(message);
    this.name = 'SaltEdgeError';
  }
}

function dateDaysAgo(days: number): string {
  const date = new Date();
  date.setUTCDate(date.getUTCDate() - days);
  return date.toISOString().slice(0, 10);
}

export class SaltEdgeClient {
  private readonly baseUrl: string;

  constructor(private readonly env: Env) {
    this.baseUrl = (env.SALT_EDGE_BASE_URL || 'https://www.saltedge.com/api/partners/v1').replace(/\/$/, '');
  }

  private async request<T>(path: string, init: RequestInit = {}): Promise<SaltEdgeEnvelope<T>> {
    if (!this.env.SALT_EDGE_APP_ID || !this.env.SALT_EDGE_SECRET) {
      throw new Error('SALT_EDGE_APP_ID and SALT_EDGE_SECRET are not configured');
    }
    const response = await fetch(`${this.baseUrl}${path.startsWith('/') ? path : `/${path}`}`, {
      ...init,
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        'App-id': this.env.SALT_EDGE_APP_ID,
        Secret: this.env.SALT_EDGE_SECRET,
        ...(init.headers || {}),
      },
    });
    const body = await response.text();
    let parsed: Partial<SaltEdgeEnvelope<T>> & { error?: { message?: string } };
    try {
      parsed = JSON.parse(body) as Partial<SaltEdgeEnvelope<T>> & { error?: { message?: string } };
    } catch {
      throw new SaltEdgeError(response.status, `Salt Edge returned non-JSON response (${response.status})`);
    }
    if (!response.ok) {
      const message = parsed.error?.message;
      throw new SaltEdgeError(response.status, message || `Salt Edge request failed (${response.status})`);
    }
    return parsed as SaltEdgeEnvelope<T>;
  }

  private async paginate<T>(path: string): Promise<T[]> {
    const result: T[] = [];
    let next: string | undefined = path;
    for (let page = 0; next && page < 50; page += 1) {
      const currentPath: string = String(next);
      const pageResponse: SaltEdgeEnvelope<T[]> = await this.request<T[]>(currentPath);
      result.push(...(pageResponse.data || []));
      next = pageResponse.meta?.next_page;
      if (!next && pageResponse.meta?.next_id) {
        next = `${currentPath}${currentPath.includes('?') ? '&' : '?'}from_id=${encodeURIComponent(pageResponse.meta.next_id)}`;
      }
      if (next?.startsWith('http')) {
        const nextUrl: URL = new URL(next);
        next = `${nextUrl.pathname}${nextUrl.search}`;
      }
    }
    return result;
  }

  async listProviders(countryCode?: string): Promise<SaltEdgeProvider[]> {
    const query = countryCode ? `?country_code=${encodeURIComponent(countryCode)}` : '';
    return this.paginate<SaltEdgeProvider>(`/providers${query}`);
  }

  async createLead(email: string): Promise<{ customerId: string }> {
    const response = await this.request<{ customer_id: string }>('/leads', {
      method: 'POST',
      body: JSON.stringify({ data: { email } }),
    });
    return { customerId: response.data.customer_id };
  }

  async createLeadSession(
    customerId: string,
    historyDays: number,
  ): Promise<{ redirectUrl: string; expiresAt?: string }> {
    const response = await this.request<{ redirect_url: string; expires_at?: string }>('/lead_sessions/create', {
      method: 'POST',
      body: JSON.stringify({
        data: {
          customer_id: customerId,
          consent: {
            from_date: dateDaysAgo(historyDays),
            period_days: historyDays,
            scopes: ['account_details', 'transactions_details'],
          },
          attempt: {
            from_date: dateDaysAgo(historyDays),
            fetch_scopes: ['accounts', 'transactions'],
          },
        },
      }),
    });
    return { redirectUrl: response.data.redirect_url, expiresAt: response.data.expires_at };
  }

  async listConnections(customerId: string): Promise<SaltEdgeConnection[]> {
    return this.paginate<SaltEdgeConnection>(`/connections?per_page=1000&customer_id=${encodeURIComponent(customerId)}`);
  }

  async listPartnerConsents(connectionId: string): Promise<SaltEdgePartnerConsent[]> {
    return this.paginate<SaltEdgePartnerConsent>(
      `/partner_consents?per_page=1000&connection_id=${encodeURIComponent(connectionId)}`,
    );
  }

  async listAccounts(connectionId: string): Promise<SaltEdgeAccount[]> {
    return this.paginate<SaltEdgeAccount>(`/accounts?per_page=1000&connection_id=${encodeURIComponent(connectionId)}`);
  }

  async listTransactions(connectionId: string, accountId: string): Promise<SaltEdgeTransaction[]> {
    return this.paginate<SaltEdgeTransaction>(
      `/transactions?per_page=1000&connection_id=${encodeURIComponent(connectionId)}&account_id=${encodeURIComponent(accountId)}`,
    );
  }

  async revokePartnerConsent(consentId: string, connectionId: string): Promise<void> {
    await this.request(`/partner_consents/${encodeURIComponent(consentId)}/revoke?connection_id=${encodeURIComponent(connectionId)}`, {
      method: 'PUT',
    });
  }
}
