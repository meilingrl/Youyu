import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { logUserConsent } from '@/api/modules/user'
import { getAuthToken } from '@/utils/auth'

const CONSENT_KEY = 'youyu-cookie-consent'
const SEARCH_HISTORY_KEY = 'youyu-search-history'

function readStoredConsent() {
  try {
    const raw = localStorage.getItem(CONSENT_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export const useConsentStore = defineStore('consent', () => {
  const cookieConsent = ref(readStoredConsent())
  const saving = ref(false)

  const hasCookieChoice = computed(() => Boolean(cookieConsent.value?.timestamp))
  const functionalEnabled = computed(() => cookieConsent.value?.functional === true)
  const analyticsEnabled = computed(() => cookieConsent.value?.analytics === true)

  function persistConsent(payload) {
    cookieConsent.value = {
      necessary: true,
      functional: payload.functional === true,
      analytics: payload.analytics === true,
      timestamp: new Date().toISOString()
    }
    localStorage.setItem(CONSENT_KEY, JSON.stringify(cookieConsent.value))
    if (!cookieConsent.value.functional) {
      localStorage.removeItem(SEARCH_HISTORY_KEY)
    }
    return cookieConsent.value
  }

  async function saveCookieConsent(payload) {
    const consent = persistConsent(payload)
    if (!getAuthToken()) {
      return consent
    }
    saving.value = true
    try {
      await Promise.all([
        logUserConsent({
          consentType: 'cookie_functional',
          consented: consent.functional,
          source: 'cookie_banner'
        }),
        logUserConsent({
          consentType: 'cookie_analytics',
          consented: consent.analytics,
          source: 'cookie_banner'
        })
      ])
    } finally {
      saving.value = false
    }
    return consent
  }

  return {
    cookieConsent,
    saving,
    hasCookieChoice,
    functionalEnabled,
    analyticsEnabled,
    saveCookieConsent
  }
})
