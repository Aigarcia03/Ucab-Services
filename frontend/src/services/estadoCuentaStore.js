import { reactive } from 'vue';

function getField(obj, key) {
  if (obj == null) return undefined;
  if (Object.prototype.hasOwnProperty.call(obj, key)) return obj[key];
  const lower = key.toLowerCase();
  for (const k of Object.keys(obj)) {
    if (k.toLowerCase() === lower) return obj[k];
  }
  return undefined;
}

const state = reactive({
  pendientes: []
});

export const estadoCuentaStore = {
  get pendientes() {
    return state.pendientes.filter(item => item.estado === 'activo');
  },
  get saldoTotal() {
    return state.pendientes.filter(item => item.estado === 'activo')
      .reduce((total, item) => total + (item.precioBase || 0), 0);
  },

  // Called on mount for Estado de Cuenta view — loads from DB
  async cargarDesdeDB(ci) {
    if (!ci) return;
    try {
      const res = await fetch(`http://localhost:8080/api/tramites?ci=${ci}`);
      if (res.ok) {
        const tramites = await res.json();
        // Replace local state with DB data (avoid duplicates on re-open)
        state.pendientes = tramites.map(t => ({
          id: `${getField(t, 'ci')}-${getField(t, 'fechaCreacion')}-${getField(t, 'idPrestadora')}`,
          titulo: getField(t, 'descripcion') ? getField(t, 'descripcion').split('.')[0].trim() : getField(t, 'nombreCategoria'),
          descripcion: getField(t, 'descripcion'),
          precioBase: getField(t, 'precioBase') ?? 0,
          fecha: getField(t, 'fechaCreacion'),
          estado: getField(t, 'estado'),
          ci: getField(t, 'ci'),
          idPrestadora: getField(t, 'idPrestadora'),
          nombreCategoria: getField(t, 'nombreCategoria'),
          descripcionTramite: getField(t, 'descripcion'),
          fechaCreacion: getField(t, 'fechaCreacion')
        }));
      }
    } catch (e) {
      console.error('Error cargando tramites desde BD:', e);
    }
  },

  // Called when clicking "Agregar a Estado de Cuenta" in CatalogView
  async agregarAlEstadoDeCuenta(servicio, ci) {
    // 1. Persist to DB
    try {
      const res = await fetch('http://localhost:8080/api/tramites', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          ci: ci,
          idPrestadora: servicio.idPrestadora,
          nombreCategoria: servicio.nombreCategoria,
          descripcion: servicio.descripcion
        })
      });

      if (!res.ok) {
        const err = await res.json();
        console.error('Error en BD al agregar trámite:', err);
        return false;
      }
    } catch (e) {
      console.error('Error de red al guardar trámite:', e);
      return false;
    }

    // 2. Also update local store for immediate UI update
    const newItem = {
      id: Date.now(),
      titulo: servicio.titulo || servicio.nombreCategoria,
      descripcion: servicio.descripcion,
      precioBase: servicio.precioBase,
      fecha: new Date().toLocaleDateString('es-ES', { day: '2-digit', month: '2-digit', year: 'numeric' }),
      estado: 'activo'
    };
    state.pendientes.push(newItem);
    return true;
  }
};
