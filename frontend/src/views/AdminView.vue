<template>
  <div class="admin-wrapper">
    <h2 class="page-title">Panel de Administración</h2>

    <!-- Tabs -->
    <div class="admin-tabs">
      <button v-for="tab in tabs" :key="tab.key" :class="['tab-btn', { active: activeTab === tab.key }]" @click="activeTab = tab.key">
        {{ tab.label }}
      </button>
    </div>

    <!-- ─── TAB: MIEMBROS ───────────────────────────────────────────── -->
    <div v-if="activeTab === 'miembros'" class="admin-section">
      <div class="section-toolbar">
        <h3>Gestión de Miembros</h3>
      </div>
      <div v-if="miembrosError" class="msg-error">{{ miembrosError }}</div>
      <div v-if="miembrosOk" class="msg-ok">{{ miembrosOk }}</div>
      <div class="table-responsive">
        <table class="admin-table">
          <thead>
            <tr><th>CI</th><th>Nombre</th><th>Correo</th><th>Categoría</th><th>Estado</th><th>Acciones</th></tr>
          </thead>
          <tbody>
            <tr v-for="m in miembros" :key="m.ci">
              <td>{{ m.ci }}</td>
              <td>{{ m.primerNombre }} {{ m.primerApellido }}</td>
              <td>{{ m.correoinstitucional }}</td>
              <td><span class="rol-badge">{{ m.activerole }}</span></td>
              <td><span class="status-badge" :class="m.estadocuenta">{{ m.estadocuenta }}</span></td>
              <td class="actions-cell">
                <select v-model="m._newStatus" class="admin-select-sm">
                  <option value="">— Cambiar estatus —</option>
                  <option value="activa">Activa</option>
                  <option value="suspendida">Suspendida</option>
                  <option value="bloqueada">Bloqueada</option>
                </select>
                <button class="btn-sm" @click="cambiarEstatus(m)">Aplicar</button>
                <button class="btn-sm btn-primary" @click="abrirAsignarRol(m)">Asignar Rol</button>
                <button class="btn-sm" @click="abrirFinalizarRol(m)" :disabled="m.activerole === 'Miembro'">Finalizar Rol</button>
                <button class="btn-sm btn-danger" @click="inactivarMiembro(m)">Inactivar</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Asignar Rol Modal -->
      <div v-if="rolFormVisible" class="rol-modal-overlay" @click.self="rolFormVisible = false">
        <div class="rol-modal">
          <h4>Asignar rol a {{ rolTarget?.primerNombre }} {{ rolTarget?.primerApellido }}</h4>
          <p class="text-sm">CI: {{ rolTarget?.ci }} — Rol actual: <strong>{{ rolTarget?.activerole }}</strong></p>
          <div class="inline-form" style="flex-direction:column;align-items:stretch;gap:10px">
            <select v-model="rolForm.rol" class="admin-input">
              <option value="">— Seleccionar rol —</option>
              <option value="Estudiante">Estudiante</option>
              <option value="Estudiante (Becario)">Estudiante (Becario)</option>
              <option value="Estudiante (Preparador)">Estudiante (Preparador)</option>
              <option value="Profesor">Profesor</option>
              <option value="Personal Administrativo">Personal Administrativo</option>
            </select>
            <input v-model="rolForm.fechaInicio" type="date" class="admin-input" />

            <!-- Campos Estudiante / Becario / Preparador -->
            <template v-if="rolForm.rol?.startsWith('Estudiante')">
              <div class="form-row-custom">
                <input v-model="rolForm.escuela" class="admin-input" placeholder="Escuela" />
                <input v-model="rolForm.facultadAdscripcion" class="admin-input" placeholder="Facultad" />
              </div>
            </template>

            <!-- Campos solo Becario -->
            <template v-if="rolForm.rol === 'Estudiante (Becario)'">
              <select v-model="rolForm.tipoBeca" class="admin-input">
                <option value="ayuda económica">Ayuda económica</option>
                <option value="excelencia">Excelencia</option>
                <option value="comedor">Comedor</option>
              </select>
            </template>

            <!-- Campos solo Preparador -->
            <template v-if="rolForm.rol === 'Estudiante (Preparador)'">
              <input v-model="rolForm.asignatura" class="admin-input" placeholder="Asignatura" />
            </template>

            <!-- Campos Profesor -->
            <template v-if="rolForm.rol === 'Profesor'">
              <div class="form-row-custom">
                <input v-model="rolForm.escalafonDocente" class="admin-input" placeholder="Escalafón docente" />
                <input v-model="rolForm.codigoInvestigador" class="admin-input" placeholder="Código investigador" />
              </div>
            </template>

            <!-- Campos Personal Administrativo -->
            <template v-if="rolForm.rol === 'Personal Administrativo'">
              <div class="form-row-custom">
                <input v-model="rolForm.unidadAdscripcionPresupuestaria" class="admin-input" placeholder="Unidad adscripción presupuestaria" />
                <input v-model="rolForm.cargoAdministrativo" class="admin-input" placeholder="Cargo administrativo" />
              </div>
            </template>

            <div style="display:flex;gap:8px;margin-top:4px">
              <button class="btn-sm btn-primary" @click="asignarRol" :disabled="!rolForm.rol || !rolForm.fechaInicio">Guardar</button>
              <button class="btn-sm" @click="rolFormVisible = false">Cancelar</button>
            </div>
          </div>
          <div v-if="rolError" class="msg-error" style="margin-top:12px">{{ rolError }}</div>
          <div v-if="rolOk" class="msg-ok" style="margin-top:12px">{{ rolOk }}</div>
        </div>
      </div>
    </div>

    <!-- ─── TAB: SEDES ──────────────────────────────────────────────── -->
    <div v-if="activeTab === 'sedes'" class="admin-section">
      <div class="section-toolbar">
        <h3>Sedes</h3>
      </div>
      <div v-if="sedesError" class="msg-error">{{ sedesError }}</div>
      <div v-if="sedesOk" class="msg-ok">{{ sedesOk }}</div>
      <div class="table-responsive">
        <table class="admin-table">
          <thead><tr><th>Ubicación</th><th>Acciones</th></tr></thead>
          <tbody>
            <tr v-for="s in sedes" :key="s.ubicacion">
              <td>{{ s.ubicacion }}</td>
              <td><button class="btn-sm btn-danger" @click="eliminarSede(s)">Eliminar</button></td>
            </tr>
          </tbody>
        </table>
      </div>

      <h3 style="margin-top: 30px;">Edificaciones</h3>
      <div class="section-toolbar">
        <button class="btn-sm btn-primary" @click="showFormEdif = true">+ Nueva Edificación</button>
      </div>
      <div v-if="showFormEdif" class="inline-form">
        <input v-model="newEdif.direccion" class="admin-input" placeholder="Dirección" />
        <input v-model="newEdif.nombre" class="admin-input" placeholder="Nombre" />
        <select v-model="newEdif.ubicacion" class="admin-input">
          <option value="">Sede...</option>
          <option v-for="s in sedes" :key="s.ubicacion" :value="s.ubicacion">{{ s.ubicacion }}</option>
        </select>
        <button class="btn-sm btn-primary" @click="crearEdificacion">Guardar</button>
        <button class="btn-sm" @click="showFormEdif = false">Cancelar</button>
      </div>
      <div class="table-responsive">
        <table class="admin-table">
          <thead><tr><th>Dirección</th><th>Nombre</th><th>Sede</th><th>Acciones</th></tr></thead>
          <tbody>
            <tr v-for="e in edificaciones" :key="e.direccion + e.nombre">
              <td>{{ e.direccion }}</td><td>{{ e.nombre }}</td><td>{{ e.sedeubicacion }}</td>
              <td><button class="btn-sm btn-danger" @click="eliminarEdificacion(e)">Eliminar</button></td>
            </tr>
          </tbody>
        </table>
      </div>

      <h3 style="margin-top: 30px;">Espacios Físicos</h3>
      <div class="section-toolbar">
        <button class="btn-sm btn-primary" @click="showFormEsp = true">+ Nuevo Espacio</button>
      </div>
      <div v-if="showFormEsp" class="inline-form inline-form-esp">
        <input v-model="newEsp.nroIdentificador" class="admin-input" placeholder="Nro ID" type="number" />
        <select v-model="newEsp.edificacionKey" class="admin-input" @change="onEspEdifChange">
          <option value="">Seleccionar edificación...</option>
          <option v-for="e in edificaciones" :key="e.direccion + '|' + e.nombre" :value="e.direccion + '|' + e.nombre">
            {{ e.nombre }} ({{ e.direccion }}) — {{ e.sedeubicacion }}
          </option>
        </select>
        <input v-model="newEsp.capacidadMaxima" class="admin-input" placeholder="Capacidad" type="number" />
        <input v-model="newEsp.tipoDeMobiliario" class="admin-input" placeholder="Mobiliario" />
        <button class="btn-sm btn-primary" @click="crearEspacio">Guardar</button>
        <button class="btn-sm" @click="showFormEsp = false">Cancelar</button>
      </div>
      <div class="table-responsive">
        <table class="admin-table">
          <thead><tr><th>Nro ID</th><th>Edificación</th><th>Capacidad</th><th>Mobiliario</th><th>Acciones</th></tr></thead>
          <tbody>
            <tr v-for="e in espacios" :key="e.nroidentificador + e.direccion + e.nombre">
              <td>{{ e.nroidentificador }}</td>
              <td>{{ e.edificacionnombre }} ({{ e.sedeubicacion }})</td>
              <td>{{ e.capacidadmaxima }}</td>
              <td>{{ e.tipodemobiliario }}</td>
              <td>
                <button class="btn-sm" @click="editarEspacio(e)">Editar</button>
                <button class="btn-sm btn-danger" @click="eliminarEspacio(e)">Eliminar</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="editEsp" class="inline-form inline-form-esp">
        <input v-model="editEsp.capacidadMaxima" class="admin-input" placeholder="Capacidad" type="number" />
        <input v-model="editEsp.tipoDeMobiliario" class="admin-input" placeholder="Mobiliario" />
        <button class="btn-sm btn-primary" @click="guardarEditEspacio">Guardar</button>
        <button class="btn-sm" @click="editEsp = null">Cancelar</button>
      </div>
    </div>

    <!-- ─── TAB: CURSOS ─────────────────────────────────────────────── -->
    <div v-if="activeTab === 'cursos'" class="admin-section">
      <div class="section-toolbar">
        <h3>Cursos</h3>
        <button class="btn-sm btn-primary" @click="showFormCurso = true">+ Nuevo Curso</button>
      </div>
      <div v-if="cursosError" class="msg-error">{{ cursosError }}</div>
      <div v-if="cursosOk" class="msg-ok">{{ cursosOk }}</div>
      <div v-if="showFormCurso" class="inline-form">
        <input v-model="newCurso.materia" class="admin-input" placeholder="Materia" />
        <select v-model="newCurso.profesorCi" class="admin-input" @change="autoFillFechaInicio">
          <option value="">Profesor (opcional — auto-asigna fecha)</option>
          <option v-for="p in profesores" :key="p.ci" :value="p.ci">{{ p.primernombre }} {{ p.primerapellido }} (inicio rol: {{ p.rolfechainicio }})</option>
        </select>
        <input v-model="newCurso.fechaInicio" class="admin-input" type="date" placeholder="Fecha inicio" />
        <input v-model="newCurso.fechaFin" class="admin-input" type="date" placeholder="Fecha fin (opcional)" />
        <button class="btn-sm btn-primary" @click="crearCurso">Guardar</button>
        <button class="btn-sm" @click="showFormCurso = false">Cancelar</button>
      </div>
      <div class="table-responsive">
        <table class="admin-table">
          <thead><tr><th>Materia</th><th>Inicio</th><th>Fin</th><th>Profesor</th><th>Acciones</th></tr></thead>
          <tbody>
            <tr v-for="c in cursos" :key="c.materia + c.fechainicio">
              <td>{{ c.materia }}</td>
              <td>{{ c.fechainicio }}</td>
              <td>{{ c.fechafin || '—' }}</td>
              <td>{{ c.profesornombre || '—' }}</td>
              <td class="actions-cell">
                <button class="btn-sm" @click="abrirInscribir(c)">Inscribir Estudiante</button>
                <button class="btn-sm" @click="verEstudiantesCurso(c)">Ver estudiantes</button>
                <button class="btn-sm btn-danger" @click="eliminarCurso(c)">Eliminar</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Modal: Inscribir Estudiante (asigna profesor + estudiante) -->
      <div v-if="showInscribir" class="modal-overlay">
        <div class="modal-card">
          <h3>Inscribir Estudiante en {{ cursoActivo?.materia }}</h3>
          <p class="modal-hint">Solo se muestran estudiantes cuyo rol inicie en la misma fecha del curso ({{ cursoActivo?.fechainicio }}). El profesor debe tener la misma fecha de inicio de rol.</p>
          <select v-model="profesorInscribir" class="admin-input full-width">
            <option value="">Profesor del curso...</option>
            <option v-for="p in profesores" :key="p.ci" :value="p.ci">{{ p.primernombre }} {{ p.primerapellido }} (inicio rol: {{ p.rolfechainicio }})</option>
          </select>
          <select v-model="estudianteInscribir" class="admin-input full-width">
            <option value="">Estudiante a inscribir...</option>
            <option v-for="e in estudiantesDisponibles" :key="e.ci" :value="e.ci">{{ e.primernombre }} {{ e.primerapellido }} (CI: {{ e.ci }})</option>
          </select>
          <div class="modal-actions">
            <button class="btn-sm btn-primary" @click="inscribirEstudiante" :disabled="!profesorInscribir || !estudianteInscribir">Inscribir</button>
            <button class="btn-sm" @click="showInscribir = false">Cancelar</button>
          </div>
        </div>
      </div>

      <!-- Panel: Ver estudiantes del curso -->
      <div v-if="cursoEstudiantes.length > 0" class="curso-estudiantes-panel">
        <h4>Estudiantes de {{ cursoEstMateria }}</h4>
        <div class="table-responsive">
          <table class="admin-table">
            <thead><tr><th>CI</th><th>Nombre</th><th>Nota</th></tr></thead>
            <tbody>
              <tr v-for="e in cursoEstudiantes" :key="e.ciestudiante">
                <td>{{ e.ciestudiante }}</td>
                <td>{{ e.primernombre }} {{ e.primerapellido }}</td>
                <td>{{ e.nota }}</td>
              </tr>
            </tbody>
        </table>
        </div>
      </div>

      <!-- Finalizar Rol Modal -->
      <div v-if="finRolVisible" class="rol-modal-overlay" @click.self="finRolVisible = false">
        <div class="rol-modal">
          <h4>Finalizar rol de {{ finTarget?.primerNombre }} {{ finTarget?.primerApellido }}</h4>
          <p class="text-sm">CI: {{ finTarget?.ci }} — Rol actual: <strong>{{ finTarget?.activerole }}</strong></p>
          <div class="inline-form">
            <input v-model="finFechaFin" type="date" class="admin-input" />
            <button class="btn-sm btn-primary" @click="finalizarRol" :disabled="!finFechaFin">Finalizar</button>
            <button class="btn-sm" @click="finRolVisible = false">Cancelar</button>
          </div>
          <div v-if="finRolError" class="msg-error" style="margin-top:12px">{{ finRolError }}</div>
          <div v-if="finRolOk" class="msg-ok" style="margin-top:12px">{{ finRolOk }}</div>
        </div>
      </div>
    </div>

    <!-- ─── TAB: SERVICIOS ──────────────────────────────────────────── -->
    <div v-if="activeTab === 'servicios'" class="admin-section">
      <div class="section-toolbar">
        <h3>Servicios</h3>
        <button class="btn-sm btn-primary" @click="showFormServ = true">+ Nuevo Servicio</button>
      </div>
      <div v-if="serviciosError" class="msg-error">{{ serviciosError }}</div>
      <div v-if="serviciosOk" class="msg-ok">{{ serviciosOk }}</div>
      <div v-if="showFormServ" class="inline-form inline-form-esp">
        <select v-model="newServ.nombreCategoria" class="admin-input">
          <option value="">Categoría...</option>
          <option v-for="c in categorias" :key="c.nombre" :value="c.nombre">{{ c.nombre }}</option>
        </select>
        <select v-model="newServ.idPrestadora" class="admin-input">
          <option value="">Prestadora...</option>
          <option v-for="p in prestadoras" :key="p.idprestadora" :value="p.idprestadora">ID {{ p.idprestadora }}</option>
        </select>
        <input v-model="newServ.descripcion" class="admin-input" placeholder="Descripción" />
        <input v-model="newServ.precioBase" class="admin-input" placeholder="Precio base" type="number" step="0.01" />
        <input v-model="newServ.ajuste" class="admin-input" placeholder="Ajuste" type="number" step="0.01" />
        <select v-model="newServ.ubicacion" class="admin-input">
          <option value="">Sede...</option>
          <option v-for="s in sedes" :key="s.ubicacion" :value="s.ubicacion">{{ s.ubicacion }}</option>
        </select>
        <button class="btn-sm btn-primary" @click="crearServicio">Guardar</button>
        <button class="btn-sm" @click="showFormServ = false">Cancelar</button>
      </div>
      <div class="table-responsive">
        <table class="admin-table">
          <thead><tr><th>Categoría</th><th>Prestadora</th><th>Descripción</th><th>Precio Base</th><th>Ajuste</th><th>Sede</th><th>Acciones</th></tr></thead>
          <tbody>
            <tr v-for="s in servicios" :key="s.nombrecategoria + s.idprestadora + s.descripcion">
              <td>{{ s.categorianombre }}</td>
              <td>{{ s.idprestadora }}</td>
              <td>{{ s.descripcion }}</td>
              <td>{{ s.preciobase }}</td>
              <td>{{ s.ajuste }}</td>
              <td>{{ s.ubicacion }}</td>
              <td><button class="btn-sm btn-danger" @click="eliminarServicio(s)">Eliminar</button></td>
            </tr>
          </tbody>
        </table>
      </div>

      <h3 style="margin-top: 30px;">Tarifas</h3>
      <div class="section-toolbar">
        <button class="btn-sm btn-primary" @click="showFormTarifa = true">+ Nueva Tarifa</button>
      </div>
      <div v-if="showFormTarifa" class="inline-form inline-form-esp">
        <input v-model="newTarifa.nombreCategoria" class="admin-input" placeholder="Categoría" />
        <input v-model="newTarifa.idPrestadora" class="admin-input" placeholder="ID Prestadora" type="number" />
        <input v-model="newTarifa.descripcion" class="admin-input" placeholder="Descripción" />
        <input v-model="newTarifa.fechaInicio" class="admin-input" type="date" />
        <select v-model="newTarifa.perfil" class="admin-input">
          <option value="">Perfil...</option>
          <option value="miembro activo">Miembro activo</option>
          <option value="egresado">Egresado</option>
          <option value="público externo">Público externo</option>
        </select>
        <input v-model="newTarifa.costoFinal" class="admin-input" placeholder="Costo final" type="number" step="0.01" />
        <button class="btn-sm btn-primary" @click="crearTarifa">Guardar</button>
        <button class="btn-sm" @click="showFormTarifa = false">Cancelar</button>
      </div>
      <div class="table-responsive">
        <table class="admin-table">
          <thead><tr><th>Categoría</th><th>Prestadora</th><th>Descripción</th><th>Inicio</th><th>Perfil</th><th>Costo</th></tr></thead>
          <tbody>
            <tr v-for="t in tarifas" :key="t.nombrecategoria + t.fechainicio">
              <td>{{ t.nombrecategoria }}</td><td>{{ t.idprestadora }}</td><td>{{ t.descripcion }}</td>
              <td>{{ t.fechainicio }}</td><td>{{ t.perfil }}</td><td>{{ t.costofinal }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- ─── TAB: TRÁMITES ───────────────────────────────────────────── -->
    <div v-if="activeTab === 'tramites'" class="admin-section">
      <div class="section-toolbar"><h3>Trámites</h3></div>
      <div class="table-responsive">
        <table class="admin-table">
          <thead><tr><th>CI</th><th>Miembro</th><th>Servicio</th><th>Fecha Creación</th><th>Estado</th><th>Acciones</th></tr></thead>
          <tbody>
            <tr v-for="t in tramites" :key="t.ci + t.fechacreacion">
              <td>{{ t.ci }}</td>
              <td>{{ t.primerNombre }} {{ t.primerApellido }}</td>
              <td>{{ t.nombrecategoria }}</td>
              <td>{{ t.fechacreacion }}</td>
              <td><span class="status-badge" :class="t.estado">{{ t.estado }}</span></td>
              <td><button class="btn-sm" @click="verPasos(t)">Ver Pasos</button></td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="pasos.length > 0" class="pasos-section">
        <h4>Pasos del Trámite</h4>
        <div v-for="(p, i) in pasos" :key="i" class="paso-card">
          <div class="paso-header">
            <strong>Paso {{ p.ordenSecuencial }}</strong>
            <span class="status-badge" :class="p.estado">{{ p.estado }}</span>
          </div>
          <p>{{ p.descripcionInteraccion }}</p>
          <p v-if="p.responsableAsignado" class="paso-responsable">Responsable: {{ p.responsableAsignado }}</p>
          <div v-if="p.estado === 'en curso'" class="paso-actions">
            <select v-model="p._editEstado" class="admin-select-sm">
              <option value="completado">Completado</option>
              <option value="en curso">En curso</option>
            </select>
            <input v-model="p._editResponsable" class="admin-input-sm" placeholder="Responsable" />
            <button class="btn-sm" @click="actualizarPaso(p)">Actualizar</button>
          </div>
        </div>
        <div class="paso-add-form">
          <input v-model="nuevoPasoDesc" class="admin-input" placeholder="Nuevo paso..." @keyup.enter="crearPasoAdmin" />
          <button class="btn-sm btn-primary" @click="crearPasoAdmin" :disabled="!nuevoPasoDesc.trim()">+ Agregar paso</button>
        </div>
        <p v-if="pasoAdminMsg" class="paso-msg" :class="pasoAdminMsgError ? 'error' : 'exito'">{{ pasoAdminMsg }}</p>
      </div>
    </div>

    <!-- ─── TAB: FACTURAS ───────────────────────────────────────────── -->
    <div v-if="activeTab === 'facturas'" class="admin-section">
      <div class="section-toolbar"><h3>Facturas</h3></div>
      <div v-if="factError" class="msg-error">{{ factError }}</div>
      <div v-if="factOk" class="msg-ok">{{ factOk }}</div>
      <div class="table-responsive">
        <table class="admin-table">
          <thead><tr><th>Número</th><th>CI</th><th>Miembro</th><th>Deuda</th><th>Monto Acum.</th><th>Estatus</th><th>Emisión</th><th>Pago</th><th>Acción</th></tr></thead>
          <tbody>
            <tr v-for="f in facturas" :key="f.numero">
              <td>{{ f.numero }}</td>
              <td>{{ f.ci || '—' }}</td>
              <td>{{ f.primerNombre || '' }} {{ f.primerApellido || '' }}</td>
              <td>{{ f.deuda }}</td>
              <td>{{ f.montoacumulado }}</td>
              <td><span class="status-badge" :class="f.estatus">{{ f.estatus }}</span></td>
              <td>{{ f.fechahoraemision }}</td>
              <td>{{ f.fechahorapago || '—' }}</td>
              <td>
                <button v-if="f.estatus !== 'pagada'" class="btn-sm btn-primary" @click="abrirPagoFactura(f)">Pago</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Modal: Registrar pago presencial en factura -->
      <div v-if="showPagoFactura" class="modal-overlay" @click.self="showPagoFactura = false">
        <div class="modal-card">
          <h3>Registrar Pago — Factura #{{ facturaPago?.numero }}</h3>
          <p class="modal-hint">Deuda: {{ facturaPago?.deuda }} | Miembro: {{ facturaPago?.primerNombre }} {{ facturaPago?.primerApellido }}</p>

          <label class="form-label">Monto</label>
          <input v-model="pagoForm.monto" type="number" step="0.01" class="admin-input full-width" placeholder="0.00" />

          <label class="form-label">Método de pago</label>
          <select v-model="pagoForm.metodo" class="admin-input full-width">
            <option value="tarjeta">Tarjeta</option>
            <option value="movil">Pago móvil</option>
            <option value="efectivo">Efectivo</option>
            <option value="tai">TAI</option>
          </select>

          <template v-if="pagoForm.metodo === 'tarjeta'">
            <input v-model="pagoForm.nroTarjeta" class="admin-input full-width" placeholder="Nro. de tarjeta" />
            <select v-model="pagoForm.companiaEmisora" class="admin-input full-width">
              <option value="Visa">Visa</option>
              <option value="Mastercard">Mastercard</option>
              <option value="American Express">American Express</option>
              <option value="Cabal">Cabal</option>
              <option value="Maestro">Maestro</option>
            </select>
            <input v-model="pagoForm.monedaLiquidacion" class="admin-input full-width" placeholder="Moneda de liquidación (USD/EUR/VES)" />
            <select v-model="pagoForm.tipoRed" class="admin-input full-width">
              <option value="nacional">Nacional</option>
              <option value="internacional">Internacional</option>
            </select>
            <input v-model="pagoForm.fechaVencimiento" type="month" class="admin-input full-width" />
          </template>

          <template v-if="pagoForm.metodo === 'movil'">
            <input v-model="pagoForm.nroReferencia" class="admin-input full-width" placeholder="Nro. de referencia" />
            <input v-model="pagoForm.telefonoEmisor" class="admin-input full-width" placeholder="Teléfono del emisor" />
            <select v-model="pagoForm.banco" class="admin-input full-width">
              <option value="Bancamiga">Bancamiga</option>
              <option value="Banesco">Banesco</option>
              <option value="Mercantil">Mercantil</option>
              <option value="Provincial">Provincial</option>
              <option value="Venezuela">Banco de Venezuela</option>
            </select>
          </template>

          <template v-if="pagoForm.metodo === 'efectivo'">
            <select v-model="pagoForm.monedaDeCurso" class="admin-input full-width">
              <option value="bolivares">Bolívares</option>
              <option value="divisas">Divisas</option>
            </select>
          </template>

          <template v-if="pagoForm.metodo === 'tai'">
            <input v-model="pagoForm.pos" type="number" class="admin-input full-width" placeholder="POS" />
            <input v-model="pagoForm.uid" type="number" class="admin-input full-width" placeholder="UID (cédula)" />
          </template>

          <div v-if="factPagoError" class="msg-error" style="margin-top:10px">{{ factPagoError }}</div>
          <div v-if="factPagoOk" class="msg-ok" style="margin-top:10px">{{ factPagoOk }}</div>

          <div class="modal-actions">
            <button class="btn-sm btn-primary" @click="registrarPagoFactura" :disabled="pagoProcesando">
              {{ pagoProcesando ? 'Procesando...' : 'Registrar pago' }}
            </button>
            <button class="btn-sm" @click="showPagoFactura = false">Cancelar</button>
          </div>
        </div>
      </div>
    </div>

    <!-- ─── TAB: TAI ─────────────────────────────────────────────────── -->
    <div v-if="activeTab === 'tai'" class="admin-section">
      <div class="section-toolbar"><h3>Tarjetas TAI</h3></div>
      <div v-if="taiError" class="msg-error">{{ taiError }}</div>
      <div v-if="taiOk" class="msg-ok">{{ taiOk }}</div>
      <div class="table-responsive">
        <table class="admin-table">
          <thead><tr><th>POS</th><th>UID</th><th>Miembro</th><th>Estado</th><th>Acciones</th></tr></thead>
          <tbody>
            <tr v-for="t in taiList" :key="t.pos + t.fechahorapago">
              <td>{{ t.pos }}</td>
              <td>{{ t.uid }}</td>
              <td>{{ t.primerNombre || '' }} {{ t.primerApellido || '' }}</td>
              <td><span class="status-badge" :class="t.estado || 'activo'">{{ t.estado || 'activo' }}</span></td>
              <td>
                <button v-if="t.estado !== 'bloqueado'" class="btn-sm btn-danger" @click="cambiarEstadoTAI(t, 'bloqueado')">Bloquear</button>
                <button v-else class="btn-sm btn-primary" @click="cambiarEstadoTAI(t, 'activo')">Desbloquear</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- ─── TAB: ORGANIZACIONES ─────────────────────────────────────── -->
    <div v-if="activeTab === 'organizaciones'" class="admin-section">
      <div class="section-toolbar">
        <h3>Organizaciones Externas</h3>
        <button class="btn-sm btn-primary" @click="showFormOrg = true">+ Nueva Organización</button>
      </div>
      <div v-if="orgError" class="msg-error">{{ orgError }}</div>
      <div v-if="orgOk" class="msg-ok">{{ orgOk }}</div>

      <div v-if="showFormOrg" class="inline-form inline-form-esp">
        <input v-model="newOrg.rif" class="admin-input" placeholder="RIF" />
        <input v-model="newOrg.razonSocial" class="admin-input" placeholder="Razón Social" />
        <input v-model="newOrg.fechaVencimientoContrato" type="date" class="admin-input" />
        <input v-model="newOrg.servicioCategoria" class="admin-input" placeholder="Categoría servicio (Cultura/Deporte/Reservas)" />
        <input v-model="newOrg.servicioDescripcion" class="admin-input" placeholder="Descripción servicio" />
        <input v-model="newOrg.servicioPrecioBase" type="number" step="0.01" class="admin-input" placeholder="Precio base" />
        <input v-model="newOrg.servicioUbicacion" class="admin-input" placeholder="Sede (Guayana/Montalbán)" />
        <button class="btn-sm btn-primary" @click="crearOrganizacion">Guardar</button>
        <button class="btn-sm" @click="showFormOrg = false">Cancelar</button>
      </div>
      <div class="table-responsive">
        <table class="admin-table">
          <thead><tr><th>RIF</th><th>Razón Social</th><th>Venc. Contrato</th><th>ID Prestadora</th><th>Contactos</th></tr></thead>
          <tbody>
            <tr v-for="o in organizaciones" :key="o.rif">
              <td>{{ o.rif }}</td><td>{{ o.razonsocial }}</td><td>{{ o.fechavencimientocontrato }}</td>
              <td>{{ o.idprestadora }}</td>
              <td><span v-if="o.contactos?.length">{{ o.contactos.map(c => c.nombre + ' (' + c.telefono + ')').join(', ') }}</span><span v-else class="text-muted">—</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- ─── TAB: POSTULACIONES ──────────────────────────────────────── -->
    <div v-if="activeTab === 'postulaciones'" class="admin-section">
      <div class="section-toolbar">
        <h3>Oportunidades Laborales</h3>
        <button class="btn-sm btn-primary" @click="showFormPost = true">+ Nueva Postulación</button>
      </div>
      <div v-if="postError" class="msg-error">{{ postError }}</div>
      <div v-if="postOk" class="msg-ok">{{ postOk }}</div>

      <div v-if="showFormPost" class="inline-form" style="flex-direction:column;align-items:stretch">
        <div class="form-row-custom">
          <select v-model="newPost.rif" class="admin-input" style="flex:1">
            <option value="">Seleccionar organización...</option>
            <option v-for="o in organizaciones" :key="o.rif" :value="o.rif">{{ o.razonsocial }} ({{ o.rif }})</option>
          </select>
          <input v-model="newPost.cargo" class="admin-input" style="flex:1" placeholder="Cargo" />
        </div>
        <textarea v-model="newPost.perfilBuscado" class="admin-input" placeholder="Perfil buscado" rows="2"></textarea>
        <div class="form-row-custom">
          <textarea v-model="newPost.beneficios" class="admin-input" placeholder="Beneficios" rows="2" style="flex:1"></textarea>
          <textarea v-model="newPost.responsabilidades" class="admin-input" placeholder="Responsabilidades" rows="2" style="flex:1"></textarea>
        </div>
        <div>
          <button class="btn-sm btn-primary" @click="crearPostulacion">Publicar</button>
          <button class="btn-sm" @click="showFormPost = false">Cancelar</button>
        </div>
      </div>
      <div class="table-responsive">
        <table class="admin-table">
          <thead><tr><th>Organización</th><th>Cargo</th><th>Perfil</th><th>Fecha</th><th>Estatus</th><th>Acciones</th></tr></thead>
          <tbody>
            <tr v-for="p in postulaciones" :key="p.rif + p.fechahoraoferta">
              <td>{{ p.razonsocial }}</td><td>{{ p.cargo }}</td>
              <td style="max-width:200px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">{{ p.perfilbuscado }}</td>
              <td>{{ p.fechahoraoferta }}</td>
              <td><span class="status-badge" :class="p.estatusvacante">{{ p.estatusvacante }}</span></td>
              <td>
                <button class="btn-sm btn-danger" @click="eliminarPostulacion(p)">Eliminar</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- ─── TAB: TRANSPORTE ─────────────────────────────────────────── -->
    <div v-if="activeTab === 'transporte'" class="admin-section">
      <div class="section-toolbar">
        <h3>Medios de Transporte</h3>
        <button class="btn-sm btn-primary" @click="showFormTrans = true">+ Nuevo</button>
      </div>
      <div v-if="transporteError" class="msg-error">{{ transporteError }}</div>
      <div v-if="transporteOk" class="msg-ok">{{ transporteOk }}</div>
      <div v-if="showFormTrans" class="inline-form inline-form-esp">
        <input v-model="newTrans.placa" class="admin-input" placeholder="Placa" />
        <select v-model="newTrans.ubicacion" class="admin-input">
          <option value="">Sede...</option>
          <option v-for="s in sedes" :key="s.ubicacion" :value="s.ubicacion">{{ s.ubicacion }}</option>
        </select>
        <input type="file" accept=".pdf,image/*" @change="onCarnetChange" class="admin-input" />
        <span v-if="newTrans.carnetName" class="file-name">{{ newTrans.carnetName }}</span>
        <select v-model="newTrans.tipoVehiculo" class="admin-input">
          <option value="">Tipo...</option>
          <option value="carro">Carro</option>
          <option value="autobús">Autobús</option>
        </select>
        <input v-model="newTrans.capacidad" class="admin-input" placeholder="Capacidad" type="number" />
        <label class="admin-check-label"><input v-model="newTrans.disponibilidad" type="checkbox" /> Disponible</label>
        <button class="btn-sm btn-primary" @click="crearTransporte">Guardar</button>
        <button class="btn-sm" @click="showFormTrans = false">Cancelar</button>
      </div>
      <div class="table-responsive">
        <table class="admin-table">
          <thead><tr><th>Placa</th><th>Sede</th><th>Tipo</th><th>Capacidad</th><th>Disponible</th><th>Acciones</th></tr></thead>
          <tbody>
            <tr v-for="t in transportes" :key="t.placa + t.carnetdeconducir">
              <td>{{ t.placa }}</td><td>{{ t.ubicacion }}</td><td>{{ t.tipovehiculo }}</td>
              <td>{{ t.capacidad }}</td><td>{{ t.disponibilidad ? 'Sí' : 'No' }}</td>
              <td><button class="btn-sm btn-danger" @click="eliminarTransporte(t)">Eliminar</button></td>
            </tr>
          </tbody>
        </table>
      </div>

      <h3 style="margin-top: 30px;">Viajes</h3>
      <div class="section-toolbar">
        <button class="btn-sm btn-primary" @click="showFormViaje = true">+ Nuevo Viaje</button>
      </div>
      <div v-if="showFormViaje" class="inline-form">
        <input v-model="newViaje.placa" class="admin-input" placeholder="Placa" />
        <input type="file" accept=".pdf,image/*" @change="onViajeCarnetChange" class="admin-input" />
        <span v-if="newViaje.carnetName" class="file-name">{{ newViaje.carnetName }}</span>
        <input v-model="newViaje.destino" class="admin-input" placeholder="Destino" />
        <button class="btn-sm btn-primary" @click="crearViaje">Guardar</button>
        <button class="btn-sm" @click="showFormViaje = false">Cancelar</button>
      </div>
      <div class="table-responsive">
        <table class="admin-table">
          <thead><tr><th>Inicio</th><th>Fin</th><th>Placa</th><th>Carnet</th><th>Destino</th><th>Acciones</th></tr></thead>
          <tbody>
            <tr v-for="v in viajes" :key="v.fechahorainicio">
              <td>{{ v.fechahorainicio }}</td><td>{{ v.fechahorafin || '—' }}</td>
              <td>{{ v.placa }}</td><td>{{ v.carnetdeconducir }}</td><td>{{ v.destino }}</td>
              <td><button class="btn-sm btn-danger" @click="eliminarViaje(v)">Eliminar</button></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const props = defineProps({
  userCi: { type: Number, default: null }
});

const tabs = [
  { key: 'miembros', label: 'Miembros' },
  { key: 'sedes', label: 'Sedes' },
  { key: 'cursos', label: 'Cursos' },
  { key: 'servicios', label: 'Servicios' },
  { key: 'tramites', label: 'Trámites' },
  { key: 'facturas', label: 'Facturas' },
  { key: 'tai', label: 'TAI' },
  { key: 'organizaciones', label: 'Organizaciones' },
  { key: 'postulaciones', label: 'Postulaciones' },
  { key: 'transporte', label: 'Transporte' }
]
const activeTab = ref('miembros')

const api = (path, opts = {}) => fetch('/api/admin' + path, { headers: { 'Content-Type': 'application/json' }, ...opts })

// ── Miembros ──────────────────────────────────────────────────────
const miembros = ref([])
const miembrosError = ref('')
const miembrosOk = ref('')

async function cargarMiembros() {
  const res = await api('/miembros')
  if (res.ok) {
    miembros.value = (await res.json()).map(m => ({ ...m, _newStatus: '' }))
  }
}

async function cambiarEstatus(m) {
  if (!m._newStatus) return
  miembrosError.value = ''; miembrosOk.value = ''
  const res = await api(`/miembros/${m.ci}/estatus`, { method: 'PUT', body: JSON.stringify({ estatus: m._newStatus }) })
  const data = await res.json()
  if (res.ok) { miembrosOk.value = data.message; m.estadocuenta = m._newStatus; m._newStatus = '' }
  else miembrosError.value = data.error
}

// ── Rol (asignación académica) ──────────────────────────────────────
const rolFormVisible = ref(false)
const rolTarget = ref(null)
const rolForm = ref({ rol: '', fechaInicio: '', semestre: 1, escuela: '', facultadAdscripcion: '', unidadesCreditoAprobadas: 0, promedioPonderado: 0, tipoBeca: 'ayuda económica', asignatura: '', horasAyudantia: 0, cargaHorariaSemanal: 0, escalafonDocente: '', codigoInvestigador: '', unidadAdscripcionPresupuestaria: '', cargoAdministrativo: '' })
const rolError = ref('')
const rolOk = ref('')

function abrirAsignarRol(m) {
  rolTarget.value = m
  rolForm.value = { rol: '', fechaInicio: '', semestre: 1, escuela: '', facultadAdscripcion: '', unidadesCreditoAprobadas: 0, promedioPonderado: 0, tipoBeca: 'ayuda económica', asignatura: '', horasAyudantia: 0, cargaHorariaSemanal: 0, escalafonDocente: '', codigoInvestigador: '', unidadAdscripcionPresupuestaria: '', cargoAdministrativo: '' }
  rolError.value = ''
  rolOk.value = ''
  rolFormVisible.value = true
}

async function asignarRol() {
  if (!rolForm.value.rol || !rolForm.value.fechaInicio) return
  rolError.value = ''; rolOk.value = ''
  const res = await api(`/miembros/${rolTarget.value.ci}/rol`, { method: 'POST', body: JSON.stringify(rolForm.value) })
  const data = await res.json()
  if (res.ok) {
    rolOk.value = data.message
    setTimeout(() => { rolFormVisible.value = false; cargarMiembros() }, 1500)
  } else {
    rolError.value = data.error
  }
}

// ── Finalizar Rol ─────────────────────────────────────────────────
const finRolVisible = ref(false)
const finTarget = ref(null)
const finFechaFin = ref('')
const finRolError = ref('')
const finRolOk = ref('')

function abrirFinalizarRol(m) {
  finTarget.value = m
  finFechaFin.value = ''
  finRolError.value = ''
  finRolOk.value = ''
  finRolVisible.value = true
}

async function finalizarRol() {
  if (!finFechaFin.value) return
  finRolError.value = ''; finRolOk.value = ''
  const res = await api(`/miembros/${finTarget.value.ci}/rol/finalizar`, { method: 'PUT', body: JSON.stringify({ fechaFin: finFechaFin.value }) })
  const data = await res.json()
  if (res.ok) {
    finRolOk.value = data.message
    setTimeout(() => { finRolVisible.value = false; cargarMiembros() }, 1500)
  } else {
    finRolError.value = data.error
  }
}

async function inactivarMiembro(m) {
  if (!confirm(`¿Inactivar a ${m.primerNombre} ${m.primerApellido}?`)) return
  miembrosError.value = ''; miembrosOk.value = ''
  const res = await api(`/miembros/${m.ci}`, { method: 'DELETE' })
  const data = await res.json()
  if (res.ok) { miembrosOk.value = data.message; m.estadocuenta = 'suspendida' }
  else miembrosError.value = data.error
}

// ── Sedes ─────────────────────────────────────────────────────────
const sedes = ref([])
const edificaciones = ref([])
const espacios = ref([])
const sedesError = ref('')
const sedesOk = ref('')
const showFormSede = ref(false)
const showFormEdif = ref(false)
const showFormEsp = ref(false)
const newSede = ref({ ubicacion: '' })
const newEdif = ref({ direccion: '', nombre: '', ubicacion: '' })
const newEsp = ref({ nroIdentificador: '', edificacionKey: '', direccion: '', nombre: '', capacidadMaxima: '', tipoDeMobiliario: '' })

function onEspEdifChange() {
  if (newEsp.value.edificacionKey) {
    const [dir, nom] = newEsp.value.edificacionKey.split('|');
    newEsp.value.direccion = dir;
    newEsp.value.nombre = nom;
  } else {
    newEsp.value.direccion = '';
    newEsp.value.nombre = '';
  }
}

async function cargarSedes() {
  const [r1, r2, r3] = await Promise.all([
    api('/sedes'), api('/edificaciones'), api('/espacios')
  ])
  if (r1.ok) sedes.value = await r1.json()
  if (r2.ok) edificaciones.value = await r2.json()
  if (r3.ok) espacios.value = await r3.json()
}

async function crearSede() {
  sedesError.value = ''; sedesOk.value = ''
  const res = await api('/sedes', { method: 'POST', body: JSON.stringify({ ubicacion: newSede.value.ubicacion }) })
  const data = await res.json()
  if (res.ok) { sedesOk.value = data.message; showFormSede.value = false; newSede.value = { ubicacion: '' }; cargarSedes() }
  else sedesError.value = data.error
}

async function eliminarSede(s) {
  if (!confirm(`¿Eliminar sede ${s.ubicacion}?`)) return
  const res = await api(`/sedes/${encodeURIComponent(s.ubicacion)}`, { method: 'DELETE' })
  const data = await res.json()
  if (res.ok) { sedesOk.value = data.message; cargarSedes() }
  else sedesError.value = data.error
}

async function crearEdificacion() {
  const res = await api('/edificaciones', { method: 'POST', body: JSON.stringify(newEdif.value) })
  const data = await res.json()
  if (res.ok) {
    showFormEdif.value = false; newEdif.value = { direccion: '', nombre: '', ubicacion: '' }; cargarSedes()
    if (data.sinEspacios) sedesOk.value = data.message
  } else sedesError.value = data.error
}

async function eliminarEdificacion(e) {
  if (!confirm(`¿Eliminar edificación ${e.nombre}?`)) return
  const res = await api('/edificaciones', { method: 'DELETE', body: JSON.stringify({ direccion: e.direccion, nombre: e.nombre }) })
  if (res.ok) cargarSedes()
}

async function crearEspacio() {
  const body = { nroIdentificador: newEsp.value.nroIdentificador, direccion: newEsp.value.direccion, nombre: newEsp.value.nombre, capacidadMaxima: newEsp.value.capacidadMaxima, tipoDeMobiliario: newEsp.value.tipoDeMobiliario }
  const res = await api('/espacios', { method: 'POST', body: JSON.stringify(body) })
  const data = await res.json()
  if (res.ok) { showFormEsp.value = false; newEsp.value = { nroIdentificador: '', edificacionKey: '', direccion: '', nombre: '', capacidadMaxima: '', tipoDeMobiliario: '' }; cargarSedes() }
  else sedesError.value = data.error
}

const editEsp = ref(null)

function editarEspacio(e) {
  editEsp.value = { nroIdentificador: e.nroidentificador, direccion: e.direccion, nombre: e.nombre, capacidadMaxima: e.capacidadmaxima, tipoDeMobiliario: e.tipodemobiliario }
}

async function guardarEditEspacio() {
  const res = await api('/espacios', { method: 'PUT', body: JSON.stringify(editEsp.value) })
  const data = await res.json()
  if (res.ok) { editEsp.value = null; cargarSedes() }
  else sedesError.value = data.error
}

async function eliminarEspacio(e) {
  if (!confirm(`¿Eliminar espacio ${e.nroidentificador}?`)) return
  const res = await api('/espacios', { method: 'DELETE', body: JSON.stringify({ nroIdentificador: e.nroidentificador, direccion: e.direccion, nombre: e.nombre }) })
  if (res.ok) cargarSedes()
}

// ── Cursos ────────────────────────────────────────────────────────
const cursos = ref([])
const cursosError = ref('')
const cursosOk = ref('')
const showFormCurso = ref(false)
const newCurso = ref({ materia: '', profesorCi: '', fechaInicio: '', fechaFin: '' })

const cursoActivo = ref(null)
const profesores = ref([])
const estudiantes = ref([])

// Inscribir estudiante
const showInscribir = ref(false)
const profesorInscribir = ref('')
const estudianteInscribir = ref('')
const estudiantesDisponibles = ref([])

// Ver estudiantes del curso
const cursoEstudiantes = ref([])
const cursoEstMateria = ref('')

async function cargarCursos() {
  const res = await api('/cursos')
  if (res.ok) {
    const data = await res.json()
    cursos.value = data.value || data
  }
}

async function cargarProfesoresYEstudiantes() {
  const [r1, r2] = await Promise.all([api('/profesores'), api('/estudiantes')])
  if (r1.ok) { const d = await r1.json(); profesores.value = d.value || d }
  if (r2.ok) { const d = await r2.json(); estudiantes.value = d.value || d }
}

function autoFillFechaInicio() {
  if (newCurso.value.profesorCi) {
    const prof = profesores.value.find(p => String(p.ci) === String(newCurso.value.profesorCi))
    if (prof && prof.rolfechainicio) {
      newCurso.value.fechaInicio = prof.rolfechainicio
    }
  }
}

async function crearCurso() {
  cursosError.value = ''; cursosOk.value = ''
  const res = await api('/cursos', { method: 'POST', body: JSON.stringify(newCurso.value) })
  const data = await res.json()
  if (res.ok) {
    // Si se seleccionó profesor, inscribirlo junto con el primer estudiante disponible
    cursosOk.value = data.message
    showFormCurso.value = false
    newCurso.value = { materia: '', profesorCi: '', fechaInicio: '', fechaFin: '' }
    cargarCursos()
  } else cursosError.value = data.error
}

async function eliminarCurso(c) {
  if (!confirm(`¿Eliminar curso ${c.materia}?`)) return
  const res = await api('/cursos', { method: 'DELETE', body: JSON.stringify({ materia: c.materia, fechaInicio: c.fechainicio }) })
  if (res.ok) { cursosOk.value = 'Curso eliminado'; cargarCursos() }
}

function abrirInscribir(c) {
  cursoActivo.value = c
  profesorInscribir.value = ''
  estudianteInscribir.value = ''
  estudiantesDisponibles.value = []
  showInscribir.value = true
  // Cargar solo estudiantes cuyo FechaInicio coincida con el del curso
  api('/estudiantes-por-fecha?fechaInicio=' + c.fechainicio).then(r => r.json()).then(data => {
    estudiantesDisponibles.value = data.value || data
  }).catch(() => {})
}

async function inscribirEstudiante() {
  cursosError.value = ''; cursosOk.value = ''
  const res = await api('/cursos/inscribir', {
    method: 'POST',
    body: JSON.stringify({ ciEstudiante: estudianteInscribir.value, ciProfesor: profesorInscribir.value, materia: cursoActivo.value.materia, fechaInicio: cursoActivo.value.fechainicio })
  })
  const data = await res.json()
  if (res.ok) { cursosOk.value = data.message; showInscribir.value = false; cargarCursos() }
  else cursosError.value = data.error
}

async function verEstudiantesCurso(c) {
  try {
    const res = await api(`/cursos/${encodeURIComponent(c.materia)}/${c.fechainicio}/estudiantes-admin`)
    if (res.ok) { cursoEstudiantes.value = await res.json(); cursoEstMateria.value = c.materia }
  } catch (e) {
    cursosError.value = 'Error al cargar estudiantes'
  }
}

// ── Servicios ─────────────────────────────────────────────────────
const servicios = ref([])
const tarifas = ref([])
const categorias = ref([])
const prestadoras = ref([])
const serviciosError = ref('')
const serviciosOk = ref('')
const showFormServ = ref(false)
const showFormTarifa = ref(false)
const newServ = ref({ nombreCategoria: '', idPrestadora: '', descripcion: '', precioBase: '', ajuste: '', ubicacion: '' })
const newTarifa = ref({ nombreCategoria: '', idPrestadora: '', descripcion: '', fechaInicio: '', perfil: '', costoFinal: '' })

async function cargarServicios() {
  const [r1, r2, r3, r4] = await Promise.all([
    api('/servicios'), api('/tarifas'), api('/categorias'), api('/prestadoras')
  ])
  if (r1.ok) servicios.value = await r1.json()
  if (r2.ok) tarifas.value = await r2.json()
  if (r3.ok) categorias.value = await r3.json()
  if (r4.ok) prestadoras.value = await r4.json()
}

async function crearServicio() {
  serviciosError.value = ''; serviciosOk.value = ''
  const res = await api('/servicios', { method: 'POST', body: JSON.stringify(newServ.value) })
  const data = await res.json()
  if (res.ok) { serviciosOk.value = data.message; showFormServ.value = false; newServ.value = { nombreCategoria: '', idPrestadora: '', descripcion: '', precioBase: '', ajuste: '', ubicacion: '' }; cargarServicios() }
  else serviciosError.value = data.error
}

async function eliminarServicio(s) {
  if (!confirm(`¿Eliminar servicio ${s.descripcion}?`)) return
  const res = await api('/servicios', { method: 'DELETE', body: JSON.stringify({ nombreCategoria: s.nombrecategoria, idPrestadora: s.idprestadora, descripcion: s.descripcion }) })
  if (res.ok) { serviciosOk.value = 'Servicio eliminado'; cargarServicios() }
}

async function crearTarifa() {
  serviciosError.value = ''; serviciosOk.value = ''
  const res = await api('/tarifas', { method: 'POST', body: JSON.stringify(newTarifa.value) })
  const data = await res.json()
  if (res.ok) { serviciosOk.value = data.message; showFormTarifa.value = false; newTarifa.value = { nombreCategoria: '', idPrestadora: '', descripcion: '', fechaInicio: '', perfil: '', costoFinal: '' }; cargarServicios() }
  else serviciosError.value = data.error
}

// ── Trámites ──────────────────────────────────────────────────────
const tramites = ref([])
const pasos = ref([])
const tramiteActivo = ref(null)
const nuevoPasoDesc = ref('')
const pasoAdminMsg = ref('')
const pasoAdminMsgError = ref(false)

async function cargarTramites() {
  const res = await api('/tramites')
  if (res.ok) tramites.value = await res.json()
}

async function verPasos(t) {
  tramiteActivo.value = t
  nuevoPasoDesc.value = ''
  pasoAdminMsg.value = ''
  const params = new URLSearchParams({ ci: t.ci, idPrestadora: t.idprestadora, nombreCategoria: t.nombrecategoria, descripcion: t.descripcion, fechaCreacion: t.fechacreacion })
  const res = await api(`/pasos?${params}`)
  if (res.ok) {
    pasos.value = (await res.json()).map(p => ({ ...p, _editEstado: 'completado', _editResponsable: p.responsableAsignado || '' }))
  }
}

async function actualizarPaso(p) {
  const body = {
    ci: tramiteActivo.value.ci, idPrestadora: tramiteActivo.value.idprestadora,
    nombreCategoria: tramiteActivo.value.nombrecategoria, descripcion: tramiteActivo.value.descripcion,
    fechaCreacion: tramiteActivo.value.fechacreacion,
    estado: p._editEstado, responsable: p._editResponsable,
    adminCi: props.userCi
  }
  const res = await api(`/pasos/${p.ordenSecuencial}`, { method: 'PUT', body: JSON.stringify(body) })
  if (res.ok) { p.estado = p._editEstado; pasoAdminMsg = 'Paso actualizado'; pasoAdminMsgError = false }
  else { const d = await res.json(); pasoAdminMsg = d.error; pasoAdminMsgError = true }
}

async function crearPasoAdmin() {
  const t = tramiteActivo.value
  if (!t || !nuevoPasoDesc.value.trim()) return
  pasoAdminMsg.value = ''
  try {
    const res = await api('/pasos', {
      method: 'POST',
      body: JSON.stringify({
        ci: t.ci, idPrestadora: t.idprestadora,
        nombreCategoria: t.nombrecategoria, descripcion: t.descripcion,
        fechaCreacion: t.fechacreacion, descripcionInteraccion: nuevoPasoDesc.value.trim(),
        adminCi: props.userCi
      })
    })
    const data = await res.json()
    if (res.ok) {
      pasoAdminMsg.value = data.message
      pasoAdminMsgError.value = false
      nuevoPasoDesc.value = ''
      verPasos(t)
    } else {
      pasoAdminMsg.value = data.error || 'Error al crear paso'
      pasoAdminMsgError.value = true
    }
  } catch (e) {
    pasoAdminMsg.value = 'Error de conexión'
    pasoAdminMsgError.value = true
  }
}

// ── Facturas ──────────────────────────────────────────────────────
const facturas = ref([])
const factError = ref('')
const factOk = ref('')
const showPagoFactura = ref(false)
const facturaPago = ref(null)
const pagoProcesando = ref(false)
const factPagoError = ref('')
const factPagoOk = ref('')
const pagoForm = ref({
  monto: '',
  metodo: 'tarjeta',
  nroTarjeta: '',
  companiaEmisora: 'Visa',
  monedaLiquidacion: 'USD',
  tipoRed: 'nacional',
  fechaVencimiento: '',
  nroReferencia: '',
  telefonoEmisor: '',
  banco: 'Bancamiga',
  monedaDeCurso: 'bolivares',
  pos: '',
  uid: ''
})

async function cargarFacturas() {
  const res = await api('/facturas')
  if (res.ok) facturas.value = await res.json()
}

function abrirPagoFactura(f) {
  facturaPago.value = f
  pagoForm.value = {
    monto: '',
    metodo: 'tarjeta',
    nroTarjeta: '',
    companiaEmisora: 'Visa',
    monedaLiquidacion: 'USD',
    tipoRed: 'nacional',
    fechaVencimiento: '',
    nroReferencia: '',
    telefonoEmisor: '',
    banco: 'Bancamiga',
    monedaDeCurso: 'bolivares',
    pos: '',
    uid: ''
  }
  factPagoError.value = ''
  factPagoOk.value = ''
  showPagoFactura.value = true
}

async function registrarPagoFactura() {
  const f = facturaPago.value
  if (!f) return
  pagoProcesando.value = true
  factPagoError.value = ''
  factPagoOk.value = ''
  try {
    const body = { ...pagoForm.value, monto: parseFloat(pagoForm.value.monto) || 0, adminCi: props.userCi }
    const res = await fetch(`/api/admin/facturas/${f.numero}/pago`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    })
    const data = await res.json()
    if (res.ok) {
      factPagoOk.value = data.message || 'Pago registrado correctamente.'
      showPagoFactura.value = false
      cargarFacturas()
    } else {
      factPagoError.value = data.error || 'Error al registrar pago.'
    }
  } catch (e) {
    factPagoError.value = 'Error de conexión.'
  } finally {
    pagoProcesando.value = false
  }
}

// ── TAI ───────────────────────────────────────────────────────────
const taiList = ref([])
const taiError = ref('')
const taiOk = ref('')
async function cargarTAI() {
  const res = await api('/tai')
  if (res.ok) taiList.value = await res.json()
}

async function cambiarEstadoTAI(t, estado) {
  taiError.value = ''; taiOk.value = ''
  const res = await api('/tai/bloquear', { method: 'PUT', body: JSON.stringify({ pos: t.pos, estado }) })
  const data = await res.json()
  if (res.ok) { taiOk.value = data.message; t.estado = estado }
  else taiError.value = data.error
}

// ── Transporte ────────────────────────────────────────────────────
const transportes = ref([])
const viajes = ref([])
const transporteError = ref('')
const transporteOk = ref('')
const showFormTrans = ref(false)
const showFormViaje = ref(false)
const newTrans = ref({ placa: '', ubicacion: '', carnetDeConducir: '', carnetName: '', tipoVehiculo: 'carro', capacidad: '', disponibilidad: true })
const newViaje = ref({ placa: '', carnetDeConducir: '', carnetName: '', destino: '' })

function onCarnetChange(e) {
  const file = e.target.files[0];
  if (!file) return;
  newTrans.value.carnetName = file.name;
  const reader = new FileReader();
  reader.onload = () => { newTrans.value.carnetDeConducir = reader.result.split(',')[1]; };
  reader.readAsDataURL(file);
}

function onViajeCarnetChange(e) {
  const file = e.target.files[0];
  if (!file) return;
  newViaje.value.carnetName = file.name;
  const reader = new FileReader();
  reader.onload = () => { newViaje.value.carnetDeConducir = reader.result.split(',')[1]; };
  reader.readAsDataURL(file);
}

async function cargarTransporte() {
  const [r1, r2] = await Promise.all([api('/transportes'), api('/viajes')])
  if (r1.ok) transportes.value = await r1.json()
  if (r2.ok) viajes.value = await r2.json()
}

async function crearTransporte() {
  transporteError.value = ''; transporteOk.value = ''
  const res = await api('/transportes', { method: 'POST', body: JSON.stringify(newTrans.value) })
  const data = await res.json()
  if (res.ok) { transporteOk.value = data.message; showFormTrans.value = false; newTrans.value = { placa: '', ubicacion: '', carnetDeConducir: '', carnetName: '', tipoVehiculo: 'carro', capacidad: '', disponibilidad: true }; cargarTransporte() }
  else transporteError.value = data.error
}

async function eliminarTransporte(t) {
  if (!confirm(`¿Eliminar transporte ${t.placa}?`)) return
  const res = await api('/transportes', { method: 'DELETE', body: JSON.stringify({ placa: t.placa, carnetDeConducir: t.carnetdeconducir }) })
  if (res.ok) { transporteOk.value = 'Transporte eliminado'; cargarTransporte() }
}

async function crearViaje() {
  transporteError.value = ''; transporteOk.value = ''
  const res = await api('/viajes', { method: 'POST', body: JSON.stringify(newViaje.value) })
  const data = await res.json()
  if (res.ok) { transporteOk.value = data.message; showFormViaje.value = false; newViaje.value = { placa: '', carnetDeConducir: '', carnetName: '', destino: '' }; cargarTransporte() }
  else transporteError.value = data.error
}

async function eliminarViaje(v) {
  if (!confirm(`¿Eliminar viaje?`)) return
  const res = await api('/viajes', { method: 'DELETE', body: JSON.stringify({ fechaHoraInicio: v.fechahorainicio, placa: v.placa, carnetDeConducir: v.carnetdeconducir }) })
  if (res.ok) { transporteOk.value = 'Viaje eliminado'; cargarTransporte() }
}

// ── Organizaciones ────────────────────────────────────────────────
const organizaciones = ref([])
const orgError = ref('')
const orgOk = ref('')
const showFormOrg = ref(false)
const newOrg = ref({ rif: '', razonSocial: '', fechaVencimientoContrato: '', servicioCategoria: 'Reservas', servicioDescripcion: '', servicioPrecioBase: '10', servicioAjuste: '0', servicioUbicacion: 'Guayana' })

async function cargarOrganizaciones() {
  const res = await api('/organizaciones')
  if (res.ok) organizaciones.value = await res.json()
}

async function crearOrganizacion() {
  orgError.value = ''; orgOk.value = ''
  const res = await api('/organizaciones', { method: 'POST', body: JSON.stringify(newOrg.value) })
  const data = await res.json()
  if (res.ok) {
    orgOk.value = data.mensaje
    showFormOrg.value = false
    newOrg.value = { rif: '', razonSocial: '', fechaVencimientoContrato: '', servicioCategoria: 'Reservas', servicioDescripcion: '', servicioPrecioBase: '10', servicioAjuste: '0', servicioUbicacion: 'Guayana' }
    cargarOrganizaciones()
  } else {
    orgError.value = data.error || 'Error al crear organización'
  }
}

// ── Postulaciones ─────────────────────────────────────────────────
const postulaciones = ref([])
const postError = ref('')
const postOk = ref('')
const showFormPost = ref(false)
const newPost = ref({ rif: '', cargo: '', perfilBuscado: '', beneficios: '', responsabilidades: '' })

async function cargarPostulaciones() {
  const res = await api('/postulaciones')
  if (res.ok) postulaciones.value = await res.json()
}

async function crearPostulacion() {
  postError.value = ''; postOk.value = ''
  const res = await api('/postulaciones', { method: 'POST', body: JSON.stringify(newPost.value) })
  const data = await res.json()
  if (res.ok) {
    postOk.value = data.mensaje
    showFormPost.value = false
    newPost.value = { rif: '', cargo: '', perfilBuscado: '', beneficios: '', responsabilidades: '' }
    cargarPostulaciones()
  } else {
    postError.value = data.error || 'Error al publicar postulación'
  }
}

async function eliminarPostulacion(p) {
  if (!confirm(`¿Eliminar postulación para "${p.cargo}"?`)) return
  const rifEnc = encodeURIComponent(p.rif)
  const fechaEnc = encodeURIComponent(p.fechahoraoferta)
  const res = await api(`/postulaciones/${rifEnc}/${fechaEnc}`, { method: 'DELETE' })
  const data = await res.json()
  if (res.ok) { postOk.value = data.mensaje; cargarPostulaciones() }
  else postError.value = data.error
}

onMounted(() => {
  cargarMiembros(); cargarSedes(); cargarCursos(); cargarServicios(); cargarTramites(); cargarFacturas(); cargarTAI(); cargarTransporte(); cargarProfesoresYEstudiantes(); cargarOrganizaciones(); cargarPostulaciones()
})
</script>

<style scoped>
.admin-wrapper { width: 100%; }
.page-title { color: #173a2e; font-size: 28px; margin-bottom: 20px; }

.admin-tabs { display: flex; gap: 4px; margin-bottom: 24px; flex-wrap: wrap; border-bottom: 2px solid #e5e7eb; padding-bottom: 0; }
.tab-btn { padding: 10px 20px; background: transparent; border: none; cursor: pointer; font-weight: 600; font-size: 14px; color: #6b7280; border-radius: 10px 10px 0 0; transition: all 0.2s; }
.tab-btn:hover { background: #f3f4f6; color: #374151; }
.tab-btn.active { background: #dcedc8; color: #173a2e; }

.admin-section { background: #fff; border-radius: 20px; padding: 24px; box-shadow: 0 4px 12px rgba(0,0,0,0.06); }
.section-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 8px; }
.section-toolbar h3 { color: #173a2e; font-size: 18px; }

.table-responsive { overflow-x: auto; }
.admin-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.admin-table th { background: #f1f5f2; color: #173a2e; font-weight: 700; padding: 12px 10px; text-align: left; border-bottom: 2px solid #dcedc8; }
.admin-table td { padding: 10px; border-bottom: 1px solid #eef2ee; color: #334155; }
.admin-table tr:hover td { background: #fafcf8; }

.actions-cell { display: flex; gap: 6px; align-items: center; flex-wrap: wrap; }
.admin-select-sm { padding: 5px 8px; border: 1px solid #d1d5db; border-radius: 8px; font-size: 12px; background: white; }
.admin-input { padding: 8px 12px; border: 1px solid #d1d5db; border-radius: 10px; font-size: 13px; }
.admin-input-sm { padding: 5px 8px; border: 1px solid #d1d5db; border-radius: 8px; font-size: 12px; }
.admin-check-label { display: flex; align-items: center; gap: 6px; font-size: 13px; white-space: nowrap; }

.inline-form { display: flex; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; align-items: center; background: #f7f9f6; padding: 16px; border-radius: 16px; }
.inline-form-esp { display: flex; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; align-items: center; background: #f7f9f6; padding: 16px; border-radius: 16px; }

.btn-sm { padding: 6px 14px; border: none; border-radius: 999px; font-size: 12px; font-weight: 700; cursor: pointer; background: #e5e7eb; color: #374151; transition: all 0.2s; white-space: nowrap; }
.btn-sm:hover { transform: translateY(-1px); box-shadow: 0 4px 8px rgba(0,0,0,0.08); }
.btn-primary { background: linear-gradient(180deg, #39aaf2 0%, #2f9de6 100%); color: white; box-shadow: 0 4px 8px rgba(41,146,213,0.22); }
.btn-danger { background: #ef4444; color: white; }

.status-badge { padding: 3px 12px; border-radius: 12px; font-size: 11px; font-weight: 700; display: inline-block; }
.status-badge.activa { background: #dcedc8; color: #33691e; }
.status-badge.activo { background: #dcedc8; color: #33691e; }
.status-badge.suspendida { background: #fef3c7; color: #92400e; }
.status-badge.bloqueada { background: #fce4ec; color: #c62828; }
.status-badge.pagada { background: #dcedc8; color: #33691e; }
.status-badge.pendiente { background: #fef3c7; color: #92400e; }
.status-badge.finalizado { background: #dbeafe; color: #1e40af; }
.status-badge.en\ curso { background: #fef3c7; color: #92400e; }
.status-badge.en\ revisión { background: #fef3c7; color: #92400e; }
.status-badge.completado { background: #dcedc8; color: #33691e; }

.msg-error { background: #fef2f2; color: #991b1b; padding: 10px 16px; border-radius: 12px; margin-bottom: 12px; font-size: 13px; }
.msg-ok { background: #f0fdf4; color: #166534; padding: 10px 16px; border-radius: 12px; margin-bottom: 12px; font-size: 13px; }

.pasos-section { margin-top: 20px; }
.pasos-section h4 { color: #173a2e; margin-bottom: 12px; }
.paso-card { background: #f7f9f6; border-radius: 14px; padding: 16px; margin-bottom: 10px; }
.paso-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.paso-responsable { font-size: 12px; color: #6b7280; margin-top: 4px; }
.paso-actions { display: flex; gap: 8px; margin-top: 10px; flex-wrap: wrap; }
.empty-state { padding: 20px; text-align: center; color: #9ca3af; }

/* Modal overlay */
.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.modal-card {
  background: white;
  border-radius: 24px;
  padding: 28px;
  min-width: 400px;
  max-width: 500px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15);
}
.modal-card h3 { color: #173a2e; margin-bottom: 16px; }
.modal-card .full-width { width: 100%; margin-bottom: 12px; }
.modal-hint { font-size: 0.85rem; color: #6b7280; margin-bottom: 12px; }
.modal-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 16px; }

.curso-estudiantes-panel {
  margin-top: 20px;
  background: #f7f9f6;
  border-radius: 16px;
  padding: 20px;
}
.curso-estudiantes-panel h4 { color: #173a2e; margin-bottom: 12px; }
.rol-badge { padding: 3px 12px; border-radius: 12px; font-size: 11px; font-weight: 700; display: inline-block; background: #e0e7ff; color: #3730a3; }
.rol-modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.rol-modal { background: white; border-radius: 24px; padding: 28px; min-width: 400px; max-width: 500px; box-shadow: 0 20px 60px rgba(0,0,0,0.15); }
.rol-modal h4 { color: #173a2e; margin-bottom: 8px; }
.rol-modal .text-sm { font-size: 13px; color: #6b7280; margin-bottom: 16px; }
.form-row-custom { display: flex; gap: 8px; }
.form-row-custom > input { flex: 1; }
.paso-add-form { display: flex; gap: 8px; margin-top: 16px; align-items: center; }
.paso-msg { font-size: 0.8rem; font-weight: 600; margin-top: 6px; }
.paso-msg.exito { color: #2e7d32; }
.paso-msg.error { color: #c62828; }
</style>
