export type Role = 'OWNER' | 'ADMIN' | 'MEMBER'
export type Weekday =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY'
export type RideStatus = 'SCHEDULED' | 'CANCELLED'
export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'WAITLISTED' | 'CANCELLED'
export type SwapStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'CANCELLED'

export interface User {
  id: number
  email: string
  displayName: string
  createdAt: string
}

export interface AuthResponse {
  token: string
  expiresAt: string
  user: User
}

export interface GroupSummary {
  id: number
  name: string
  description?: string
  myRole: Role
  memberCount: number
}

export interface Member {
  id: number
  userId: number
  displayName: string
  role: Role
  joinedAt: string
  canDrive: boolean
  carSeats: number
  pickupNote?: string
}

export interface GroupDetail {
  id: number
  name: string
  description?: string
  timezone: string
  inviteCode: string
  costPerKm: number
  currency: string
  myRole: Role
  myMemberId: number
  members: Member[]
}

export interface Trip {
  id: number
  groupId: number
  name: string
  origin: string
  destination: string
  departureTime: string
  days: Weekday[]
  distanceKm: number
  startsOn: string
  endsOn?: string
  active: boolean
}

export interface TripInput {
  name: string
  origin: string
  destination: string
  departureTime: string
  days: Weekday[]
  distanceKm: number
  startsOn: string
  endsOn?: string | null
}

export interface Ride {
  id: number
  tripId: number
  tripName: string
  origin: string
  destination: string
  date: string
  departureAt: string
  status: RideStatus
  cancelReason?: string
  driver?: { memberId: number; displayName: string }
  seatsOffered: number
  seatsTaken: number
  waitlisted: number
  myBooking?: BookingStatus
  distanceKm: number
}

export interface Booking {
  id: number
  rideId: number
  memberId: number
  displayName: string
  status: BookingStatus
  pickupNote?: string
  createdAt: string
}

export interface RotationProposal {
  rideId: number
  tripName: string
  date: string
  departureAt: string
  memberId?: number
  displayName?: string
}

export interface Absence {
  id: number
  memberId: number
  displayName: string
  startsOn: string
  endsOn: string
  note?: string
}

export interface Notification {
  id: number
  type: string
  message: string
  link?: string
  createdAt: string
  read: boolean
}

export interface Page<T> {
  items: T[]
  page: number
  size: number
  totalItems: number
  totalPages: number
  hasNext: boolean
}

export interface RideRef {
  id: number
  tripName: string
  date: string
  departureAt: string
}

export interface Swap {
  id: number
  status: SwapStatus
  message?: string
  createdAt: string
  ride: RideRef
  offeredRide?: RideRef
  requesterId: number
  requesterName: string
  targetId: number
  targetName: string
}

export interface MemberStats {
  memberId: number
  displayName: string
  drives: number
  kmDriven: number
  ridesTaken: number
  kmRidden: number
  passengersCarried: number
  lastDrove?: string
  driveShare: number
}

export interface GroupStats {
  from: string
  to: string
  rides: number
  cancelled: number
  members: MemberStats[]
}

export interface MemberBalance {
  memberId: number
  displayName: string
  earnedCents: number
  spentCents: number
  balanceCents: number
}

export interface Transfer {
  fromMemberId: number
  fromName: string
  toMemberId: number
  toName: string
  amountCents: number
}

export interface CostReport {
  from: string
  to: string
  currency: string
  costPerKm: number
  totalCents: number
  balances: MemberBalance[]
  settlements: Transfer[]
}
