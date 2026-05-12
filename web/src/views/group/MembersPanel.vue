<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { ref } from 'vue'

import { groupApi } from '@/api'
import { ApiError } from '@/api/client'
import type { Member } from '@/api/types'
import MemberAvatar from '@/components/MemberAvatar.vue'
import MembershipForm from '@/components/MembershipForm.vue'
import { useGroupStore } from '@/stores/group'

const store = useGroupStore()
const { group, isManager, me } = storeToRefs(store)
const error = ref('')
const copied = ref<'code' | 'link' | null>(null)

const roleLabel: Record<string, string> = { OWNER: 'Owner', ADMIN: 'Admin', MEMBER: 'Member' }

async function copy(what: 'code' | 'link') {
  if (!group.value) return
  const text =
    what === 'code' ? group.value.inviteCode : `${location.origin}/join/${group.value.inviteCode}`
  await navigator.clipboard?.writeText(text)
  copied.value = what
  setTimeout(() => (copied.value = null), 1500)
}

async function newCode() {
  if (!group.value) return
  store.replace(await groupApi.regenerateInvite(group.value.id))
}

async function toggleAdmin(member: Member) {
  if (!group.value) return
  error.value = ''
  try {
    store.updateMember(
      await groupApi.changeRole(
        group.value.id,
        member.id,
        member.role === 'ADMIN' ? 'MEMBER' : 'ADMIN',
      ),
    )
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : 'Could not change the role'
  }
}

async function remove(member: Member) {
  if (!group.value || !confirm(`Remove ${member.displayName} from the group?`)) return
  error.value = ''
  try {
    await groupApi.removeMember(group.value.id, member.id)
    await store.load(group.value.id)
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : 'Could not remove the member'
  }
}
</script>

<template>
  <div v-if="group" class="stack">
    <div class="card invite row">
      <div>
        <div class="muted">Invite code</div>
        <code class="code">{{ group.inviteCode }}</code>
      </div>
      <span class="spacer" />
      <button type="button" @click="copy('code')">
        {{ copied === 'code' ? 'Copied' : 'Copy code' }}
      </button>
      <button type="button" @click="copy('link')">
        {{ copied === 'link' ? 'Copied' : 'Copy link' }}
      </button>
      <button v-if="isManager" type="button" @click="newCode">New code</button>
    </div>

    <p v-if="error" class="error">{{ error }}</p>

    <ul class="members card">
      <li v-for="member in group.members" :key="member.id" class="row">
        <MemberAvatar :name="member.displayName" />
        <div class="who">
          <div>
            {{ member.displayName }}
            <span v-if="member.id === group.myMemberId" class="muted">(you)</span>
          </div>
          <div class="muted small">
            <template v-if="member.canDrive">
              Drives · {{ member.carSeats }} {{ member.carSeats === 1 ? 'seat' : 'seats' }}
            </template>
            <template v-else>Rides only</template>
            <template v-if="member.pickupNote"> · {{ member.pickupNote }}</template>
          </div>
        </div>
        <span class="spacer" />
        <span class="badge neutral">{{ roleLabel[member.role] }}</span>
        <template v-if="isManager && member.role !== 'OWNER' && member.id !== group.myMemberId">
          <button type="button" class="link" @click="toggleAdmin(member)">
            {{ member.role === 'ADMIN' ? 'Make member' : 'Make admin' }}
          </button>
          <button type="button" class="link danger-link" @click="remove(member)">Remove</button>
        </template>
      </li>
    </ul>

    <MembershipForm
      v-if="me"
      :group-id="group.id"
      :member="me"
      @saved="store.updateMember($event)"
    />
  </div>
</template>

<style scoped>
.code {
  font-size: 1.3rem;
  letter-spacing: 0.15em;
  font-weight: 700;
}

.members {
  list-style: none;
  margin: 0;
  padding: 0.25rem 1.25rem;
}

.members li {
  padding: 0.7rem 0;
  border-bottom: 1px solid var(--border);
  gap: 0.75rem;
}

.members li:last-child {
  border-bottom: none;
}

.small {
  font-size: 0.85rem;
}

.danger-link {
  color: var(--danger);
}
</style>
