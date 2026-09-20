import { http } from './client'
import type {
  Absence,
  AuthResponse,
  Booking,
  CostReport,
  GroupDetail,
  GroupStats,
  GroupSummary,
  Member,
  MemberRide,
  MyRide,
  Notification,
  Page,
  Payment,
  Ride,
  RotationProposal,
  Swap,
  Trip,
  TripInput,
  User,
} from './types'

export const authApi = {
  login: (email: string, password: string) =>
    http.post<AuthResponse>('/auth/login', { email, password }),
  register: (email: string, displayName: string, password: string) =>
    http.post<AuthResponse>('/auth/register', { email, displayName, password }),
  me: () => http.get<User>('/me'),
  updateMe: (displayName: string) => http.put<User>('/me', { displayName }),
  changePassword: (currentPassword: string, newPassword: string) =>
    http.put<void>('/me/password', { currentPassword, newPassword }),
}

export const groupApi = {
  list: () => http.get<GroupSummary[]>('/groups'),
  get: (id: number) => http.get<GroupDetail>(`/groups/${id}`),
  create: (name: string, description?: string, timezone?: string) =>
    http.post<GroupDetail>('/groups', { name, description, timezone }),
  preview: (inviteCode: string) =>
    http.get<{ name: string; description?: string; memberCount: number; alreadyMember: boolean }>(
      `/groups/invite/${encodeURIComponent(inviteCode)}`,
    ),
  join: (inviteCode: string) => http.post<GroupDetail>('/groups/join', { inviteCode }),
  update: (id: number, changes: Partial<Omit<GroupDetail, 'members'>>) =>
    http.patch<GroupDetail>(`/groups/${id}`, changes),
  regenerateInvite: (id: number) => http.post<GroupDetail>(`/groups/${id}/invite-code`),
  leave: (id: number) => http.post<void>(`/groups/${id}/leave`),
  updateMyMembership: (
    id: number,
    body: { canDrive: boolean; carSeats: number; pickupNote?: string },
  ) => http.put<Member>(`/groups/${id}/members/me`, body),
  changeRole: (id: number, memberId: number, role: 'ADMIN' | 'MEMBER') =>
    http.put<Member>(`/groups/${id}/members/${memberId}/role`, { role }),
  removeMember: (id: number, memberId: number) =>
    http.delete<void>(`/groups/${id}/members/${memberId}`),
}

export const tripApi = {
  list: (groupId: number) => http.get<Trip[]>(`/groups/${groupId}/trips`),
  create: (groupId: number, trip: TripInput) => http.post<Trip>(`/groups/${groupId}/trips`, trip),
  update: (id: number, trip: TripInput) => http.put<Trip>(`/trips/${id}`, trip),
  pause: (id: number) => http.post<Trip>(`/trips/${id}/pause`),
  resume: (id: number) => http.post<Trip>(`/trips/${id}/resume`),
  remove: (id: number) => http.delete<void>(`/trips/${id}`),
  addReturn: (id: number, departureTime: string) =>
    http.post<Trip>(`/trips/${id}/return`, { departureTime }),
  cancelDays: (id: number, from: string, to: string, reason?: string) =>
    http.post<{ cancelled: number }>(`/trips/${id}/cancel-days`, { from, to, reason }),
}

export const rideApi = {
  mine: (days = 7) => http.get<MyRide[]>('/me/rides', { days }),
  schedule: (groupId: number, from?: string, to?: string) =>
    http.get<Ride[]>(`/groups/${groupId}/rides`, { from, to }),
  get: (id: number) => http.get<Ride>(`/rides/${id}`),
  drive: (id: number, seats?: number) =>
    http.post<Ride>(`/rides/${id}/driver`, seats ? { seats } : undefined),
  stopDriving: (id: number) => http.delete<Ride>(`/rides/${id}/driver`),
  assignDriver: (id: number, memberId: number) => http.put<Ride>(`/rides/${id}/driver/${memberId}`),
  cancel: (id: number, reason?: string) => http.post<Ride>(`/rides/${id}/cancel`, { reason }),
  reinstate: (id: number) => http.post<Ride>(`/rides/${id}/reinstate`),
  setNote: (id: number, note: string) => http.put<Ride>(`/rides/${id}/note`, { note }),
  passengers: (id: number) => http.get<Booking[]>(`/rides/${id}/passengers`),
  requestSeat: (id: number) => http.post<Booking>(`/rides/${id}/seat`),
  cancelSeat: (id: number) => http.delete<void>(`/rides/${id}/seat`),
}

export const rotationApi = {
  preview: (groupId: number, from?: string, to?: string) =>
    http.get<RotationProposal[]>(`/groups/${groupId}/rotation`, { from, to }),
  apply: (groupId: number, from?: string, to?: string) =>
    http.post<RotationProposal[]>(`/groups/${groupId}/rotation/apply`, undefined, { from, to }),
}

export const absenceApi = {
  list: (groupId: number) => http.get<Absence[]>(`/groups/${groupId}/absences`),
  add: (groupId: number, startsOn: string, endsOn: string, note?: string) =>
    http.post<Absence>(`/groups/${groupId}/absences`, { startsOn, endsOn, note }),
  remove: (id: number) => http.delete<void>(`/absences/${id}`),
}

export const notificationApi = {
  list: (page = 0, size = 20) => http.get<Page<Notification>>('/notifications', { page, size }),
  unreadCount: () => http.get<{ count: number }>('/notifications/unread-count'),
  markRead: (id: number) => http.post<Notification>(`/notifications/${id}/read`),
  markAllRead: () => http.post<{ updated: number }>('/notifications/read-all'),
}

export const swapApi = {
  list: (groupId: number) => http.get<Swap[]>(`/groups/${groupId}/swaps`),
  create: (rideId: number, targetMemberId: number, offeredRideId?: number, message?: string) =>
    http.post<Swap>(`/rides/${rideId}/swaps`, { targetMemberId, offeredRideId, message }),
  accept: (id: number) => http.post<Swap>(`/swaps/${id}/accept`),
  decline: (id: number) => http.post<Swap>(`/swaps/${id}/decline`),
  cancel: (id: number) => http.post<Swap>(`/swaps/${id}/cancel`),
}

export const statsApi = {
  get: (groupId: number, from?: string, to?: string) =>
    http.get<GroupStats>(`/groups/${groupId}/stats`, { from, to }),
  memberRides: (groupId: number, memberId: number, from?: string, to?: string) =>
    http.get<MemberRide[]>(`/groups/${groupId}/members/${memberId}/rides`, { from, to }),
}

export const costApi = {
  get: (groupId: number, from?: string, to?: string) =>
    http.get<CostReport>(`/groups/${groupId}/costs`, { from, to }),
  recordPayment: (groupId: number, fromMemberId: number, toMemberId: number, amountCents: number) =>
    http.post<Payment>(`/groups/${groupId}/payments`, { fromMemberId, toMemberId, amountCents }),
}
