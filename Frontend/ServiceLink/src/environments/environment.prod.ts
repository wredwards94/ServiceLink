/**
 * Deployed configuration. Swapped in for environment.ts by the `production`
 * build via fileReplacements in angular.json.
 *
 * apiUrl is deliberately EMPTY. The Ingress serves this app and the API from
 * one host — `/` here, `/api` to the backend — so every request the services
 * build (`${environment.apiUrl}/api/tickets`) resolves to a same-origin
 * `/api/tickets`.
 *
 * Two problems disappear as a result:
 *   1. CORS. Same origin means the browser never sends a cross-origin request,
 *      so there is nothing to allow and no preflight to misconfigure.
 *   2. Baked-in hostnames. The image has no environment-specific URL compiled
 *      into it, so the same image runs in any cluster without a rebuild or a
 *      runtime config-injection step.
 */
export const environment = {
  production: true,
  apiUrl: '',
};
