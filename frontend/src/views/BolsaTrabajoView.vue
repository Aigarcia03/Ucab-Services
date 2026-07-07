<template>
  <div class="bolsa-wrapper">
    <div class="bolsa-header">
      <h2 class="page-title">Bolsa de trabajo</h2>
      <button @click="mostrarPublicar = !mostrarPublicar" class="btn-toggle-pub">
        {{ mostrarPublicar ? 'Cancelar' : 'Publicar oferta' }}
      </button>
    </div>

    <div v-if="mostrarPublicar" class="pub-section">
      <div class="pub-form">
        <div class="form-row">
          <div class="form-group">
            <label>RIF</label>
            <input v-model="nuevaOferta.rif" class="input-field" placeholder="123456789" />
          </div>
          <div class="form-group">
            <label>Cargo</label>
            <input v-model="nuevaOferta.cargo" class="input-field" placeholder="Analista de datos" />
          </div>
        </div>
        <div class="form-group">
          <label>Perfil buscado</label>
          <textarea v-model="nuevaOferta.perfilBuscado" class="input-field area" rows="2" placeholder="Descripción del perfil..."></textarea>
        </div>
        <div class="form-group">
          <label>Responsabilidades</label>
          <textarea v-model="nuevaOferta.responsabilidades" class="input-field area" rows="2" placeholder="Responsabilidades del cargo..."></textarea>
        </div>
        <div class="form-group">
          <label>Beneficios</label>
          <textarea v-model="nuevaOferta.beneficios" class="input-field area" rows="2" placeholder="Beneficios ofrecidos..."></textarea>
        </div>
        <div class="form-actions">
          <button @click="publicar" :disabled="publicando" class="btn-publicar">
            {{ publicando ? 'Publicando...' : 'Publicar oferta' }}
          </button>
        </div>
        <p v-if="msgPub" class="form-msg" :class="msgPubError ? 'error' : 'exito'">{{ msgPub }}</p>
      </div>
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
            <label>Curriculum (texto)</label>
            <textarea v-model="aplicacion.curriculum" class="input-field area" rows="3" placeholder="Tu formación, experiencia, etc..."></textarea>
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
const publicando = ref(false);
const aplicando = ref(false);
const aplicandoOferta = ref(null);
const mostrarPublicar = ref(false);
const ofertaAbierta = ref(null);
const msgPub = ref('');
const msgPubError = ref(false);
const msgApl = ref('');
const msgAplError = ref(false);

const nuevaOferta = ref({ rif: '', cargo: '', perfilBuscado: '', responsabilidades: '', beneficios: '' });
const aplicacion = ref({ ci: '', curriculum: '' });

const cargarOfertas = async () => {
  try {
    const res = await fetch('http://localhost:8080/api/bolsa/oportunidades');
    if (res.ok) oportunidades.value = await res.json();
  } catch (e) {
    console.error('Error cargando ofertas', e);
  }
};

const publicar = async () => {
  const o = nuevaOferta.value;
  if (!o.rif || !o.cargo || !o.perfilBuscado || !o.responsabilidades || !o.beneficios) {
    msgPub.value = 'Completa todos los campos.'; msgPubError.value = true; return;
  }
  publicando.value = true; msgPub.value = '';
  try {
    const res = await fetch('http://localhost:8080/api/bolsa/oportunidades', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(o)
    });
    const data = await res.json();
    if (res.ok) {
      msgPub.value = data.mensaje; msgPubError.value = false;
      nuevaOferta.value = { rif: '', cargo: '', perfilBuscado: '', responsabilidades: '', beneficios: '' };
      await cargarOfertas();
    } else { msgPub.value = data.error; msgPubError.value = true; }
  } catch (e) { msgPub.value = 'Error de conexión.'; msgPubError.value = true; }
  finally { publicando.value = false; }
};

const toggleAplicar = (i) => {
  ofertaAbierta.value = ofertaAbierta.value === i ? null : i;
  aplicacion.value = { ci: '', curriculum: '' };
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
        fechaHoraOferta: o.fechahoraoferta,
        ci: aplicacion.value.ci,
        curriculum: aplicacion.value.curriculum
      })
    });
    const data = await res.json();
    if (res.ok) {
      msgApl.value = data.mensaje; msgAplError.value = false;
      aplicacion.value = { ci: '', curriculum: '' };
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
.btn-toggle-pub {
  background: #2cb5e8; color: white; border: none; border-radius: 14px;
  padding: 8px 20px; font-weight: 700; cursor: pointer; font-size: 0.9rem;
}
.btn-toggle-pub:hover { background: #1ba3d4; }
.pub-section {
  background: white; border-radius: 16px; padding: 24px;
  margin-bottom: 18px; box-shadow: 0 4px 8px rgba(0,0,0,0.04);
}
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.form-group { display: flex; flex-direction: column; gap: 4px; margin-bottom: 10px; }
.form-group label { font-size: 0.85rem; font-weight: 600; color: #173a2e; }
.input-field {
  padding: 10px 14px; border-radius: 12px; border: 1px solid #dcedc8;
  outline: none; font-size: 0.9rem; width: 100%; box-sizing: border-box;
}
.input-field.area { resize: vertical; font-family: inherit; }
.form-actions { display: flex; justify-content: flex-end; margin-top: 6px; }
.btn-publicar {
  background: linear-gradient(135deg, #2cb5e8, #1ba3d4); color: white;
  border: none; border-radius: 14px; padding: 10px 28px; font-weight: 700; cursor: pointer;
}
.btn-publicar:disabled { opacity: 0.5; cursor: not-allowed; }
.form-msg { margin-top: 8px; font-weight: 600; font-size: 0.9rem; }
.form-msg.exito { color: #2e7d32; }
.form-msg.error { color: #c62828; }
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
</style>
