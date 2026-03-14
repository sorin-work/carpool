/** Notification text uses **double asterisks** around names; show those in bold. */
export function renderMessage(message: string): string {
  return message.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
}
