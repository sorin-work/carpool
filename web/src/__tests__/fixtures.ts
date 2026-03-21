import type { Member, Ride } from '@/api/types'

export function ride(overrides: Partial<Ride> = {}): Ride {
  return {
    id: 41,
    tripId: 3,
    tripName: 'Morning school run',
    origin: '12 Elm St',
    destination: 'Riverside Primary',
    date: '2099-03-03',
    departureAt: '2099-03-03T05:45:00Z',
    status: 'SCHEDULED',
    seatsOffered: 3,
    seatsTaken: 1,
    waitlisted: 0,
    distanceKm: 6.4,
    ...overrides,
  }
}

export function member(overrides: Partial<Member> = {}): Member {
  return {
    id: 7,
    userId: 70,
    displayName: 'Ben',
    role: 'MEMBER',
    joinedAt: '2026-03-01T00:00:00Z',
    canDrive: false,
    carSeats: 0,
    ...overrides,
  }
}

export const stubs = { RouterLink: { template: '<a><slot /></a>' } }
