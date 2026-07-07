<template>
  <div class="familiares-wrapper">
    <h2 class="page-title">Mis familiares</h2>

    <div class="form-card">
      <h3>Agregar familiar</h3>
      <div class="form-grid">
        <div class="form-group">
          <label>Tipo</label>
          <select v-model="nuevo.parentesco" class="input-field">
            <option value="hijo">Hijo</option>
            <option value="conyugue">Cónyugue</option>
          </select>
        </div>
        <div class="form-group">
          <label>Documento de identidad</label>
          <input v-model="nuevo.documentoIdentidad" class="input-field" placeholder="V-12345678" />
        </div>
        <div class="form-group">
          <label>Primer nombre</label>
          <input v-model="nuevo.primerNombre" class="input-field" />
        </div>
        <div class="form-group">
          <label>Primer apellido</label>
          <input v-model="nuevo.primerApellido" class="input-field" />
        </div>
        <div class="form-group">
          <label>Segundo nombre</label>
          <input v-model="nuevo.segundoNombre" class="input-field" />
        </div>
        <div class="form-group">
          <label>Segundo apellido</label>
          <input v-model="nuevo.segundoApellido" class="input-field" />
        </div>
        <div class="form-group">
          <label>Fecha de nacimiento</label>
          <input type="date" v-model="nuevo.fechaNacimiento" class="input-field" />
        </div>

        <template v-if="nuevo.parentesco === 'hijo'">
          <div class="form-group">
            <label>Centro educativo inicial</label>
            <input v-model="nuevo.centroEducativoInicial" class="input-field" />
          </div>
          <div class="form-group">
            <label>Esquema de vacunación</label>
            <input v-model="nuevo.esquemaVacunacion" class="input-field" />
          </div>
        </template>

        <template v-if="nuevo.parentesco === 'conyugue'">
          <div class="form-group">
            <label>Constancia estudios universitarios</label>
            <input v-model="nuevo.constanciaEstudiosUniversitarios" class="input-field" />
          </div>
          <div class="form-group">
            <label>Certificado de soltería</label>
            <input v-model="nuevo.certificadoSolteria" class="input-field" />
          </div>
        </template>
      </div>
      <div class="form-actions">
        <button @click="guardar" :disabled="guardando" class="btn-guardar">
          {{ guardando ? 'Guardando...' : 'Guardar familiar' }}
        </button>
      </div>
      <p v-if="msgForm" class="form-msg" :class="msgError ? 'error' : 'exito'">{{ msgForm }}</p>
    </div>

    <div class="lista">
      <h3>Familiares registrados</h3>
      <div v-if="familiares.length === 0" class="empty-state">No hay familiares registrados.</div>
      <div v-for="f in familiares" :key="f.documentoidentidad" class="familiar-card">
        <div class="familiar-header">
          <strong>{{ f.primernombre }} {{ f.primerapellido }}</strong>
          <span class="tag" :class="f.parentesco">{{ f.parentesco }}</span>
        </div>
        <div class="familiar-body">
          <div class="row"><span class="label">Documento:</span><span>{{ f.documentoidentidad }}</span></div>
          <div class="row"><span class="label">Nacimiento:</span><span>{{ f.fechanacimiento }}</span></div>
          <div class="row" v-if="f.segundonombre"><span class="label">Segundo nombre:</span><span>{{ f.segundonombre }}</span></div>
          <div class="row" v-if="f.segundoapellido"><span class="label">Segundo apellido:</span><span>{{ f.segundoapellido }}</span></div>
          <div class="row" v-if="f.centroeducativoinicial"><span class="label">Centro educativo:</span><span>{{ f.centroeducativoinicial }}</span></div>
          <div class="row" v-if="f.esquemavacunacion"><span class="label">Vacunación:</span><span>{{ f.esquemavacunacion }}</span></div>
          <div class="row" v-if="f.constanciaestudiosuniversitarios"><span class="label">Constancia estudios:</span><span>{{ f.constanciaestudiosuniversitarios }}</span></div>
          <div class="row" v-if="f.certificadosolteria"><span class="label">Cert. soltería:</span><span>{{ f.certificadosolteria }}</span></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';

const props = defineProps({ userCi: { type: Number, default: null } });

const familiares = ref([]);
const guardando = ref(false);
const msgForm = ref('');
const msgError = ref(false);

const nuevo = reactive({
  parentesco: 'hijo',
  documentoIdentidad: '',
  primerNombre: '',
  primerApellido: '',
  segundoNombre: '',
  segundoApellido: '',
  fechaNacimiento: '',
  centroEducativoInicial: '',
  esquemaVacunacion: '',
  constanciaEstudiosUniversitarios: '',
  certificadoSolteria: ''
});

const cargarFamiliares = async () => {
  if (!props.userCi) return;
  try {
    const res = await fetch(`http://localhost:8080/api/beneficiarios?ci=${props.userCi}`);
    if (res.ok) familiares.value = await res.json();
  } catch (e) {
    console.error('Error cargando familiares', e);
  }
};

const guardar = async () => {
  if (!nuevo.documentoIdentidad || !nuevo.primerNombre || !nuevo.primerApellido || !nuevo.fechaNacimiento) {
    msgForm.value = 'Completa los campos obligatorios.';
    msgError.value = true;
    return;
  }
  guardando.value = true;
  msgForm.value = '';
  try {
    const res = await fetch('http://localhost:8080/api/beneficiarios', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ...nuevo, ci: props.userCi })
    });
    const data = await res.json();
    if (res.ok) {
      msgForm.value = data.mensaje || 'Familiar registrado.';
      msgError.value = false;
      Object.assign(nuevo, {
        parentesco: 'hijo', documentoIdentidad: '', primerNombre: '', primerApellido: '',
        segundoNombre: '', segundoApellido: '', fechaNacimiento: '',
        centroEducativoInicial: '', esquemaVacunacion: '',
        constanciaEstudiosUniversitarios: '', certificadoSolteria: ''
      });
      await cargarFamiliares();
    } else {
      msgForm.value = data.error || 'Error al registrar.';
      msgError.value = true;
    }
  } catch (e) {
    msgForm.value = 'Error de conexión.';
    msgError.value = true;
  } finally {
    guardando.value = false;
  }
};

onMounted(cargarFamiliares);
</script>

<style scoped>
.familiares-wrapper {
  width: 100%;
  max-width: 800px;
}
.page-title {
  color: #173a2e;
  font-size: 28px;
  margin-bottom: 18px;
}
.form-card, .lista {
  background: white;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 4px 8px rgba(0,0,0,0.04);
}
.form-card h3, .lista h3 {
  margin: 0 0 16px;
  color: #173a2e;
}
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.form-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.form-group label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #173a2e;
}
.input-field {
  padding: 10px 14px;
  border-radius: 12px;
  border: 1px solid #dcedc8;
  outline: none;
  font-size: 0.9rem;
}
.form-actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.btn-guardar {
  background: linear-gradient(135deg, #43a047, #388e3c);
  color: white;
  border: none;
  border-radius: 14px;
  padding: 10px 28px;
  font-weight: 700;
  cursor: pointer;
}
.btn-guardar:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.form-msg {
  margin-top: 10px;
  font-weight: 600;
  font-size: 0.9rem;
}
.form-msg.exito { color: #2e7d32; }
.form-msg.error { color: #c62828; }
.empty-state {
  padding: 30px;
  text-align: center;
  color: #666;
}
.familiar-card {
  border: 1px solid #eef2ee;
  border-radius: 12px;
  padding: 14px 18px;
  margin-bottom: 10px;
}
.familiar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid #eef2ee;
}
.familiar-header strong {
  font-size: 1.05rem;
  color: #173a2e;
}
.tag {
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 0.8rem;
  font-weight: 700;
}
.tag.hijo { background: #dcedc8; color: #33691e; }
.tag.conyugue { background: #c8e6f6; color: #01579b; }
.familiar-body {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px 16px;
  font-size: 0.85rem;
}
.familiar-body .row {
  display: flex;
  gap: 4px;
}
.familiar-body .label {
  color: #666;
  font-weight: 500;
  min-width: 100px;
}
</style>
