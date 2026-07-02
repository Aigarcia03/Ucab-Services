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

export async function closeLatestSession(ci) {
  if (!ci) {
    return;
  }

  try {
    await fetch('/api/auth/session/close-latest', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ci })
    });
  } catch (error) {
    console.error('Error cerrando la ultima sesion:', error);
  }
}

export function closeLatestSessionOnUnload(ci) {
  if (!ci) {
    return;
  }

  const payload = JSON.stringify({ ci });

  if (navigator.sendBeacon) {
    const blob = new Blob([payload], { type: 'application/json' });
    navigator.sendBeacon('/api/auth/session/close-latest', blob);
    return;
  }

  fetch('/api/auth/session/close-latest', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: payload,
    keepalive: true
  }).catch(() => {
    // Ignorar errores en cierre de navegador.
  });
}
