import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'

import App from '../App.vue'

describe('App', () => {
  it('renders the header and the router outlet', () => {
    const wrapper = mount(App, {
      global: {
        plugins: [createPinia()],
        stubs: {
          RouterView: { template: '<p>page</p>' },
          RouterLink: { template: '<a><slot /></a>' },
        },
      },
    })
    expect(wrapper.text()).toContain('Carpool')
    expect(wrapper.text()).toContain('page')
  })
})
