/** Up to two letters for an avatar: "Ana Popescu" -> "AP", "ben" -> "B". */
export function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean)
  if (!parts.length) {
    return '?'
  }
  const picked = parts.length === 1 ? parts : [parts[0]!, parts[parts.length - 1]!]
  return picked
    .map((part) => part.charAt(0))
    .join('')
    .toUpperCase()
}

/** A stable hue per name so the same person always gets the same avatar colour. */
export function avatarHue(name: string): number {
  let hash = 0
  for (const char of name) {
    hash = (hash * 31 + (char.codePointAt(0) ?? 0)) % 360
  }
  return hash
}
