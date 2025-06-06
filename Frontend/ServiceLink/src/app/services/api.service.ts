const API_ROOT = 'http://localhost:8080/'
// const API_ROOT = 'http://192.168.1.213:8080/'

export default async function fetchFromAPI(method: string, endpoint: string, body?: any) {
  let url = `${API_ROOT}${endpoint}`
  method = method.toUpperCase()
  let options: any = {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  }
  try {
    const response = await fetch(url, options)
    const data = await response.json()
    if (response.ok) {
      return data
    } else console.error('API Error:', data)
  } catch (error) {
    console.error(error)
    return error
  }
}
