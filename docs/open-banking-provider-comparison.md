# Open Banking provider decision

## Decision

The first adapter targets **Salt Edge Partners API**. The backend keeps a provider boundary so Tink or Enable Banking can be added later without changing the Android finance model.

## Why Salt Edge first

- The Partners API documents PSD2/Open Banking integrations in the EU, including the Polish API.
- It provides a hosted connection flow, account and transaction endpoints, daily synchronization and transaction enrichment.
- The API is server-to-server; Salt Edge credentials stay in the Cloudflare Worker.
- The test mode supports fake/sandbox providers before production onboarding.

References:

- [Salt Edge Partners API overview](https://docs.saltedge.com/partners/v1/)
- [Salt Edge Partners quick start](https://docs.saltedge.com/partners/v1/#quick-start)
- [Tink account aggregation](https://tink.com/account-aggregation/)
- [Enable Banking provider landscape](https://enablebanking.com/blog/the-open-banking-provider-landscape-in-europe-what-to-know-before-you-choose?rq=yapily)

## Scope

The first real integration is read-only:

- multiple EU bank connections,
- account balances and account metadata,
- transactions and pending/posted status where exposed,
- up to the provider's allowed history window,
- manual refresh plus daily scheduled refresh,
- transparent quadrant suggestion with user confirmation in Fokus,
- disconnect/revoke consent support before production launch.

No payment initiation, transfers or bank credentials are stored by Fokus.

## Production checklist

- Create Salt Edge Partner account and obtain Service API credentials.
- Confirm country/bank coverage for the first countries and banks.
- Configure Google/e-mail authentication and issue JWTs to the Worker.
- Configure D1 and R2 in Cloudflare.
- Configure callback URL and signature verification.
- Add privacy policy, consent copy, retention/deletion flow and disconnect/revoke UX.
- Complete Salt Edge test/production review.
