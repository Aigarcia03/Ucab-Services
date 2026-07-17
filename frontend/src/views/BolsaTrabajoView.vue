<template>
  <div class="bolsa-wrapper">
    <div class="bolsa-header">
      <h2 class="page-title">Bolsa de trabajo</h2>
    </div>

    <div v-if="oportunidades.length === 0" class="empty-state">No hay ofertas publicadas.</div>

    <div v-for="(o, i) in oportunidades" :key="i" class="oferta-card">
      <div class="oferta-header">
        <div>
          <strong class="cargo">{{ o.cargo }}</strong>
          <span class="org">{{ o.razonsocial }}</span>
        </div>
        <span class="tag-status" :class="o.estatusvacante">{{ o.estatusvacante }}</span>
      </div>
      <div class="oferta-body">
        <div class="detail"><span class="label">Perfil buscado:</span> {{ o.perfilbuscado }}</div>
        <div class="detail"><span class="label">Responsabilidades:</span> {{ o.responsabilidades }}</div>
        <div class="detail"><span class="label">Beneficios:</span> {{ o.beneficios }}</div>
        <div class="detail"><span class="label">Publicado:</span> {{ formatFecha(o.fechahoraoferta) }}</div>
      </div>

      <div class="aplicar-section">
        <button @click="toggleAplicar(i)" class="btn-aplicar-toggle">
          {{ ofertaAbierta === i ? 'Cerrar' : 'Aplicar' }}
        </button>

        <div v-if="ofertaAbierta === i" class="aplicar-form">
          <div class="form-row">
            <div class="form-group">
              <label>Tu CI</label>
              <input v-model="aplicacion.ci" class="input-field" placeholder="12345678" />
            </div>
          </div>
          <div class="form-group">
            <label>Curriculum (PDF o imagen)</label>
            <input type="file" accept=".pdf,image/*" @change="onCurriculumChange" class="input-field" />
            <span v-if="aplicacion.curriculumName" class="file-name">{{ aplicacion.curriculumName }}</span>
          </div>
          <div class="form-actions">
            <button @click="aplicar(o)" :disabled="aplicando" class="btn-aplicar">
              {{ aplicando ? 'Enviando...' : 'Enviar postulación' }}
            </button>
          </div>
          <p v-if="msgApl && aplicandoOferta === i" class="form-msg" :class="msgAplError ? 'error' : 'exito'">{{ msgApl }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';

const oportunidades = ref([]);
const aplicando = ref(false);
const aplicandoOferta = ref(null);
const ofertaAbierta = ref(null);
const msgApl = ref('');
const msgAplError = ref(false);

const aplicacion = ref({ ci: '', curriculum: '', curriculumName: '' });

function onCurriculumChange(e) {
  const file = e.target.files[0];
  if (!file) return;
  aplicacion.value.curriculumName = file.name;
  const reader = new FileReader();
  reader.onload = () => {
    const base64 = reader.result.split(',')[1];
    aplicacion.value.curriculum = base64;
  };
  reader.readAsDataURL(file);
}

const cargarOfertas = async () => {
  try {
    const res = await fetch('http://localhost:8080/api/bolsa/oportunidades');
    if (res.ok) oportunidades.value = await res.json();
  } catch (e) {
    console.error('Error cargando ofertas', e);
  }
};

const toggleAplicar = (i) => {
  ofertaAbierta.value = ofertaAbierta.value === i ? null : i;
  aplicacion.value = { ci: '', curriculum: '', curriculumName: '' };
  msgApl.value = '';
};

const aplicar = async (o) => {
  if (!aplicacion.value.ci || !aplicacion.value.curriculum) {
    msgApl.value = 'Completa todos los campos.'; msgAplError.value = true; return;
  }
  aplicando.value = true; aplicandoOferta.value = oportunidades.value.indexOf(o);
  msgApl.value = '';
  try {
    const res = await fetch('http://localhost:8080/api/bolsa/postular', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        rif: o.rif,
        cargo: o.cargo,
        ci: aplicacion.value.ci,
        curriculum: aplicacion.value.curriculum
      })
    });
    const data = await res.json();
    if (res.ok) {
      msgApl.value = data.mensaje; msgAplError.value = false;
      aplicacion.value = { ci: '', curriculum: '', curriculumName: '' };
    } else { msgApl.value = data.error; msgAplError.value = true; }
  } catch (e) { msgApl.value = 'Error de conexión.'; msgAplError.value = true; }
  finally { aplicando.value = false; }
};

const formatFecha = (f) => {
  if (!f) return '—';
  return new Date(f).toLocaleString('es-ES', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
};

onMounted(cargarOfertas);
</script>

<style scoped>
.bolsa-wrapper { width: 100%; max-width: 900px; }
.bolsa-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px;
}
.page-title { color: #173a2e; font-size: 28px; margin: 0; }
.empty-state { background: white; border-radius: 16px; padding: 40px; text-align: center; color: #666; }
.oferta-card {
  background: white; border-radius: 14px; padding: 18px 22px;
  margin-bottom: 14px; box-shadow: 0 2px 6px rgba(0,0,0,0.04);
}
.oferta-header {
  display: flex; justify-content: space-between; align-items: center;
  padding-bottom: 10px; margin-bottom: 10px; border-bottom: 1px solid #eef2ee;
}
.cargo { font-size: 1.1rem; color: #173a2e; display: block; }
.org { font-size: 0.85rem; color: #666; }
.tag-status { padding: 3px 12px; border-radius: 12px; font-size: 0.8rem; font-weight: 700; }
.tag-status.disponible { background: #dcedc8; color: #33691e; }
.tag-status.ocupada { background: #fce4ec; color: #c62828; }
.oferta-body { margin-bottom: 12px; }
.oferta-body .detail { font-size: 0.9rem; margin-bottom: 4px; }
.oferta-body .label { font-weight: 600; color: #666; }
.aplicar-section { border-top: 1px solid #eef2ee; padding-top: 12px; }
.btn-aplicar-toggle {
  background: #43a047; color: white; border: none; border-radius: 12px;
  padding: 7px 18px; font-weight: 700; cursor: pointer; font-size: 0.85rem;
}
.btn-aplicar-toggle:hover { background: #388e3c; }
.aplicar-form {
  margin-top: 12px; padding: 14px; background: #f7f9f6;
  border-radius: 12px;
}
.btn-aplicar {
  background: linear-gradient(135deg, #43a047, #388e3c); color: white;
  border: none; border-radius: 14px; padding: 10px 28px; font-weight: 700; cursor: pointer;
}
.btn-aplicar:disabled { opacity: 0.5; cursor: not-allowed; }
.file-name { font-size: 0.8rem; color: #2e7d32; font-weight: 600; margin-top: 4px; }
</style>
