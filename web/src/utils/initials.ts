/** Up to two letters for an avatar: "Ana Popescu" -> "AP", "ben" -> "B". */
export function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean)
  if (!parts.length) {
    return '?'
  }
  const picked = parts.length === 1 ? parts : [parts[0]!, parts[parts.length - 1]!]
  return picked.map(firstCharacter).join('').toUpperCase()
}

// Intl.Segmenter is not in the ES2020 lib typings this project compiles against.
type Segmenter = { segment(input: string): Iterable<{ segment: string }> }
const SegmenterCtor = (
  Intl as unknown as { Segmenter?: new (locale?: string, options?: object) => Segmenter }
).Segmenter
const graphemes = SegmenterCtor ? new SegmenterCtor(undefined, { granularity: 'grapheme' }) : null

/**
 * The first user-perceived character. charAt(0) splits emoji and other characters outside the
 * Basic Multilingual Plane in half, and drops the accent from letters typed as letter + accent.
 */
function firstCharacter(part: string): string {
  if (graphemes) {
    for (const { segment } of graphemes.segment(part)) {
      return segment
    }
  }
  return Array.from(part)[0] ?? ''
}

/** A stable hue per name so the same person always gets the same avatar colour. */
export function avatarHue(name: string): number {
  let hash = 0
  for (const char of name) {
    hash = (hash * 31 + (char.codePointAt(0) ?? 0)) % 360
  }
  return hash
}
