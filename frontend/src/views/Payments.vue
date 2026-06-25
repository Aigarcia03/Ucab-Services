<template>
  <div class="pago-container">
    <h2 class="pago-title">Pago de servicio</h2>
    
    <div class="pago-form">
      <div class="form-row">
        <div class="form-group half">
          <label class="form-label">Descripción del servicio</label>
          <div class="form-input custom-select" style="background-color: #d9e8f5;">{{ props.transaction?.descripcion ?? form.descripcion }}</div>
        </div>
        
            <div class="form-group half">
              <label class="form-label">Monto a pagar:</label>
              <div class="monto-box">
                <span class="subtotal">Subtotal: ${{ props.transaction?.monto?.toFixed(2) ?? '12.50' }} USD.</span>
                <span class="tasa">Tasa de Cambio (BCV): 36.50 Bs./USD</span>
              </div>
            </div>
      </div>

      <div class="form-row">
        <div class="form-group full">
          <label class="form-label">Método de pago</label>
          <div class="radio-group">
            <label class="radio-option">
              <input type="radio" value="movil" v-model="form.metodo">
              <span class="radio-dot"></span> Pago móvil
            </label>
            <label class="radio-option">
              <input type="radio" value="zelle" v-model="form.metodo">
              <span class="radio-dot"></span> Zelle
            </label>
            <label class="radio-option">
              <input type="radio" value="crypto" v-model="form.metodo">
              <span class="radio-dot"></span> Criptomoneda
            </label>
          </div>
        </div>
      </div>

      <div class="form-row">
        <div class="form-group half">
          <label class="form-label">Banco</label>
          <select v-model="form.banco" class="form-input custom-select">
            <option value="bancamiga">Bancamiga</option>
            <option value="banesco">Banesco</option>
            <option value="mercantil">Mercantil</option>
          </select>
        </div>
        
        <div class="form-group half">
          <label class="form-label">Referencia</label>
          <input type="text" v-model="form.referencia" class="form-input" placeholder="01293849390891">
        </div>
      </div>

      <div class="form-row">
        <div class="form-group half">
          <label class="form-label">Cédula</label>
          <input type="text" v-model="form.cedula" class="form-input" placeholder="27033930">
        </div>
        
        <div class="form-group half">
          <label class="form-label">Teléfono</label>
          <input type="text" v-model="form.telefono" class="form-input" placeholder="0123-4567890">
        </div>
      </div>

      <div class="form-actions">
        <button @click="registrarPago" class="btn-registrar">Registrar pago</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  transaction: {
    type: Object,
    default: () => ({ descripcion: 'Reserva de servicio', monto: 12.5 })
  }
})
const emit = defineEmits(['paymentConfirmed'])

const form = ref({
  descripcion: props.transaction.descripcion ?? 'Reserva de servicio',
  metodo: 'movil',
  banco: 'bancamiga',
  referencia: '',
  cedula: '',
  telefono: ''
})

watch(
  () => props.transaction,
  (newValue) => {
    if (newValue) {
      form.value.descripcion = newValue.descripcion || form.value.descripcion
    }
  },
  { immediate: true }
)

const registrarPago = () => {
  console.log('Registrando pago...', {
    ...form.value,
    monto: props.transaction.monto
  })
  emit('paymentConfirmed')
}
</script>

<style scoped>
.pago-container {
  background-color: #a2c8ec; /* Fondo azul claro del contenedor principal */
  border-radius: 30px;
  padding: 40px;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  color: #2c3e50;
  box-shadow: 0 4px 15px rgba(0,0,0,0.05);
  max-width: 900px;
  margin: 20px auto;
}

.pago-title {
  font-size: 2.2rem;
  font-weight: bold;
  margin-bottom: 30px;
  color: #263238;
}

.form-row {
  display: flex;
  gap: 30px;
  margin-bottom: 25px;
  flex-wrap: wrap;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-group.half {
  flex: 1;
  min-width: 250px;
}

.form-group.full {
  flex: 1;
  width: 100%;
}

.form-label {
  font-weight: 600;
  margin-bottom: 10px;
  font-size: 1.1rem;
}

.form-input {
  background-color: #d9e8f5; /* Tono grisáceo/azul claro para los inputs */
  border: none;
  border-radius: 25px;
  padding: 12px 20px;
  font-size: 1rem;
  color: #333;
  outline: none;
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.03);
}

.custom-select {
  appearance: none;
  background-image: url("data:image/svg+xml;utf8,<svg fill='%23555' height='24' viewBox='0 0 24 24' width='24' xmlns='http://www.w3.org/2000/svg'><path d='M7 10l5 5 5-5z'/><path d='M0 0h24v24H0z' fill='none'/></svg>");
  background-repeat: no-repeat;
  background-position: right 15px center;
  padding-right: 40px;
}

.monto-box {
  background-color: #d9e8f5;
  border-radius: 25px;
  padding: 12px 20px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.subtotal {
  font-weight: bold;
  font-size: 1.05rem;
}

.tasa {
  font-size: 0.9rem;
  color: #555;
  margin-top: 2px;
}

/* Radio buttons estilizados */
.radio-group {
  display: flex;
  gap: 30px;
  margin-top: 5px;
}

.radio-option {
  display: flex;
  align-items: center;
  cursor: pointer;
  font-weight: 500;
}

.radio-option input {
  display: none;
}

.radio-dot {
  width: 18px;
  height: 18px;
  border: 2px solid #555;
  border-radius: 50%;
  margin-right: 8px;
  display: inline-block;
  position: relative;
  background: white;
}

.radio-option input:checked + .radio-dot {
  border-color: #27ae60;
  background-color: #27ae60;
}

.radio-option input:checked + .radio-dot::after {
  content: '';
  width: 6px;
  height: 6px;
  background: white;
  border-radius: 50%;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

/* Botón Registrar */
.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.btn-registrar {
  background: linear-gradient(135deg, #44a4e1, #2980b9);
  color: white;
  border: none;
  border-radius: 25px;
  padding: 14px 40px;
  font-size: 1.1rem;
  font-weight: bold;
  cursor: pointer;
  box-shadow: 0 4px 10px rgba(41, 128, 185, 0.3);
  transition: all 0.2s ease;
}

.btn-registrar:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 14px rgba(41, 128, 185, 0.4);
}
</style>