const ESCAPES: Record<string, string> = {
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;',
  "'": '&#39;',
}

export function escapeHtml(text: string): string {
  return text.replace(/[&<>"']/g, (c) => ESCAPES[c]!)
}

/**
 * Notification text uses **double asterisks** around names; show those in bold. Everything else
 * is user input (trip names, cancel reasons, display names) and is escaped first.
 */
export function renderMessage(message: string): string {
  return escapeHtml(message).replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
}
