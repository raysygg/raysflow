import http from './http'

export const fetchProjectOverview = () => http.get('/project/overview')
export const fetchWorkbench = () => http.get('/project/workbench')
