import { isValidEntityId } from '@/utils/id-utils'

export function requireValidEntityIdParam(paramName = 'id', fallback = { name: 'app-explore' }) {
  return (to) => (isValidEntityId(to.params[paramName]) ? true : fallback)
}
