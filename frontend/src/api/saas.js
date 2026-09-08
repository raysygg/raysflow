import http from './http'

export const fetchIdentityConfigs = () => http.get('/saas/identity/configs')
export const saveIdentityDraft = payload => http.post('/saas/identity/drafts', payload)
export const validateIdentityConfig = id => http.post(`/saas/identity/${id}/validate`)
export const activateIdentityConfig = id => http.post(`/saas/identity/${id}/activate`)
export const restoreIdentityConfig = id => http.post(`/saas/identity/${id}/restore`)
export const saveMfaPolicy = payload => http.post('/saas/identity/mfa/policies', payload)
export const syncScimUser = payload => http.put('/saas/identity/scim/users', payload)

export const fetchEntitlements = () => http.get('/saas/entitlements')
export const checkAdmission = payload => http.post('/saas/entitlements/admission', payload)
export const fetchUsageSummary = params => http.get('/saas/usage/summary', { params })

export const fetchBillingOverview = () => http.get('/saas/billing/overview')
export const createBillingPeriod = payload => http.post('/saas/billing/periods', payload)
export const generateInvoice = id => http.post(`/saas/billing/periods/${id}/invoice`)
export const finalizeInvoice = id => http.post(`/saas/billing/invoices/${id}/finalize`)
export const syncInvoice = (id, payload) => http.post(`/saas/billing/invoices/${id}/sync`, payload)
export const exportInvoice = id => http.get(`/saas/billing/invoices/${id}/export`)

export const fetchRetentionPolicies = () => http.get('/saas/governance/retention')
export const fetchGovernanceRequests = () => http.get('/saas/governance/requests')
export const fetchGovernanceEvidence = id => http.get(`/saas/governance/requests/${id}/evidence`)
export const createGovernanceRequest = (type, payload) => http.post(`/saas/governance/requests/${type}`, payload)
export const approveGovernanceRequest = id => http.post(`/saas/governance/requests/${id}/approve`)
export const executeGovernanceRequest = id => http.post(`/saas/governance/requests/${id}/execute`)
export const retryGovernanceRequest = id => http.post(`/saas/governance/requests/${id}/retry`)
export const createLegalHold = payload => http.post('/saas/governance/holds', payload)

export const fetchAdoptionDefinitions = () => http.get('/saas/adoption/definitions')
export const fetchAdoptionReport = params => http.get('/saas/adoption/report', { params })
export const fetchAdoptionQuality = params => http.get('/saas/adoption/quality', { params })
export const fetchAdoptionBenchmark = params => http.get('/saas/adoption/benchmark', { params })
