export function getAuthUser() {
  try {
    const stored = localStorage.getItem('authUser');
    return stored ? JSON.parse(stored) : null;
  } catch (error) {
    console.error('Error leyendo usuario autenticado:', error);
    return null;
  }
}

export function setAuthUser(user) {
  try {
    localStorage.setItem('authUser', JSON.stringify(user));
  } catch (error) {
    console.error('Error guardando usuario autenticado:', error);
  }
}

export function clearAuthUser() {
  try {
    localStorage.removeItem('authUser');
  } catch (error) {
    console.error('Error borrando usuario autenticado:', error);
  }
}
