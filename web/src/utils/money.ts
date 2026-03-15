/** 1234 cents in EUR -> "€12.34" */
export function formatCents(cents: number, currency: string): string {
  return new Intl.NumberFormat('en-GB', { style: 'currency', currency }).format(cents / 100)
}

/** Distances from the API arrive as numbers with up to two decimals. */
export function formatKm(km: number): string {
  return `${new Intl.NumberFormat('en-GB', { maximumFractionDigits: 1 }).format(km)} km`
}
