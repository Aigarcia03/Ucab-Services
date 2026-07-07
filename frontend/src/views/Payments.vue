<template>
  <div class="pago-container">
    <h2 class="pago-title">Pago de servicio</h2>

    <div class="pago-form">
      <div class="form-row">
        <div class="form-group half">
          <label class="form-label">Descripción del servicio</label>
          <div class="form-input custom-select" style="background-color: #d9e8f5;">{{ descripcion }}</div>
        </div>

        <div class="form-group half">
          <label class="form-label">Monto a pagar</label>
          <div class="monto-box">
            <span class="subtotal">Subtotal: ${{ monto }} USD.</span>
          </div>
        </div>
      </div>

      <div class="form-row">
        <div class="form-group full">
          <label class="form-label">Categoría de pago</label>
          <div class="radio-group">
            <label class="radio-option">
              <input type="radio" value="online" v-model="categoria">
              <span class="radio-dot"></span> En línea
            </label>
            <label class="radio-option">
              <input type="radio" value="presencial" v-model="categoria">
              <span class="radio-dot"></span> Presencial
            </label>
          </div>
        </div>
      </div>

      <div class="form-row">
        <div class="form-group full">
          <label class="form-label">Método de pago</label>
          <div class="radio-group">
            <template v-if="categoria === 'online'">
              <label class="radio-option">
                <input type="radio" value="zelle" v-model="metodo">
                <span class="radio-dot"></span> Zelle
              </label>
              <label class="radio-option">
                <input type="radio" value="crypto" v-model="metodo">
                <span class="radio-dot"></span> Criptomoneda
              </label>
            </template>
            <template v-else>
              <label class="radio-option">
                <input type="radio" value="movil" v-model="metodo">
                <span class="radio-dot"></span> Pago móvil
              </label>
              <label class="radio-option">
                <input type="radio" value="tarjeta" v-model="metodo">
                <span class="radio-dot"></span> Tarjeta
              </label>
              <label class="radio-option">
                <input type="radio" value="efectivo" v-model="metodo">
                <span class="radio-dot"></span> Efectivo
              </label>
              <label class="radio-option">
                <input type="radio" value="tai" v-model="metodo">
                <span class="radio-dot"></span> TAI
              </label>
            </template>
          </div>
        </div>
      </div>

      <!-- Zelle -->
      <template v-if="metodo === 'zelle'">
        <div class="form-row">
          <div class="form-group half">
            <label class="form-label">Código de confirmación</label>
            <input type="text" v-model="form.codigoConfirmacion" class="form-input" placeholder="123456">
          </div>
          <div class="form-group half">
            <label class="form-label">Correo electrónico</label>
            <input type="email" v-model="form.correoElectronico" class="form-input" placeholder="usuario@ejemplo.com">
          </div>
        </div>
        <div class="form-row">
          <div class="form-group half">
            <label class="form-label">Primer nombre</label>
            <input type="text" v-model="form.primerNombre" class="form-input" placeholder="Juan">
          </div>
          <div class="form-group half">
            <label class="form-label">Primer apellido</label>
            <input type="text" v-model="form.primerApellido" class="form-input" placeholder="Pérez">
          </div>
        </div>
      </template>

      <!-- Criptomoneda -->
      <template v-if="metodo === 'crypto'">
        <div class="form-row">
          <div class="form-group half">
            <label class="form-label">TXID (Hash de transacción)</label>
            <input type="text" v-model="form.txid" class="form-input" placeholder="0xabc123...">
          </div>
          <div class="form-group half">
            <label class="form-label">Red</label>
            <select v-model="form.red" class="form-input custom-select">
              <option value="BTC">Bitcoin</option>
              <option value="ETH">Ethereum</option>
              <option value="USDT_TRC20">USDT (TRC-20)</option>
              <option value="USDT_ERC20">USDT (ERC-20)</option>
              <option value="BNB">BNB Chain</option>
            </select>
          </div>
        </div>
        <div class="form-row">
          <div class="form-group half">
            <label class="form-label">Dirección de billetera</label>
            <input type="text" v-model="form.direccionBilletera" class="form-input" placeholder="1A1zP1eP5QGefi2D...">
          </div>
          <div class="form-group half">
            <label class="form-label">Tasa de conversión (USD / crypto)</label>
            <input type="number" step="0.01" v-model="form.tasaConversion" class="form-input" placeholder="0.00005">
          </div>
        </div>
      </template>

      <!-- Pago móvil -->
      <template v-if="metodo === 'movil'">
        <div class="form-row">
          <div class="form-group half">
            <label class="form-label">Nro. de referencia</label>
            <input type="text" v-model="form.nroReferencia" class="form-input" placeholder="01293849390891">
          </div>
          <div class="form-group half">
            <label class="form-label">Teléfono del emisor</label>
            <input type="text" v-model="form.telefonoEmisor" class="form-input" placeholder="04121234567">
          </div>
        </div>
        <div class="form-row">
          <div class="form-group half">
            <label class="form-label">Banco</label>
            <select v-model="form.banco" class="form-input custom-select">
              <option value="Bancamiga">Bancamiga</option>
              <option value="Banesco">Banesco</option>
              <option value="Mercantil">Mercantil</option>
              <option value="Provincial">Provincial</option>
              <option value="Venezuela">Banco de Venezuela</option>
            </select>
          </div>
          <div class="form-group half">
            <label class="form-label">Cédula</label>
            <input type="text" v-model="form.cedula" class="form-input" placeholder="27033930">
          </div>
        </div>
      </template>

      <!-- Tarjeta -->
      <template v-if="metodo === 'tarjeta'">
        <div class="form-row">
          <div class="form-group half">
            <label class="form-label">Nro. de tarjeta</label>
            <input type="text" v-model="form.nroTarjeta" class="form-input" placeholder="1234567890123456">
          </div>
          <div class="form-group half">
            <label class="form-label">Compañía emisora</label>
            <select v-model="form.companiaEmisora" class="form-input custom-select">
              <option value="Visa">Visa</option>
              <option value="Mastercard">Mastercard</option>
              <option value="American Express">American Express</option>
              <option value="Cabal">Cabal</option>
              <option value="Maestro">Maestro</option>
            </select>
          </div>
        </div>
        <div class="form-row">
          <div class="form-group half">
            <label class="form-label">Moneda de liquidación</label>
            <input type="text" v-model="form.monedaLiquidacion" class="form-input" placeholder="USD / EUR / VES">
          </div>
          <div class="form-group half">
            <label class="form-label">Tipo de red</label>
            <select v-model="form.tipoRed" class="form-input custom-select">
              <option value="nacional">Nacional</option>
              <option value="internacional">Internacional</option>
            </select>
          </div>
        </div>
        <div class="form-row">
          <div class="form-group half">
            <label class="form-label">Fecha de vencimiento</label>
            <input type="month" v-model="form.fechaVencimiento" class="form-input">
          </div>
        </div>
      </template>

      <!-- Efectivo -->
      <template v-if="metodo === 'efectivo'">
        <div class="form-row">
          <div class="form-group half">
            <label class="form-label">Moneda de curso</label>
            <select v-model="form.monedaDeCurso" class="form-input custom-select">
              <option value="bolivares">Bolívares</option>
              <option value="divisas">Divisas</option>
            </select>
          </div>
        </div>
        <div class="form-row">
          <div class="form-group full">
            <label class="form-label">Desglose de denominaciones</label>
            <div v-for="(d, i) in form.denominaciones" :key="i" class="den-row">
              <input type="number" v-model="d.cantidad" class="form-input den-input" placeholder="Cantidad" min="1">
              <input type="number" v-model="d.valor" class="form-input den-input" placeholder="Valor unitario" min="1">
              <button @click="quitarDenominacion(i)" class="btn-den-remove" v-if="form.denominaciones.length > 1">✕</button>
            </div>
            <button @click="agregarDenominacion" class="btn-den-add">+ Agregar denominación</button>
          </div>
        </div>
      </template>

      <!-- TAI -->
      <template v-if="metodo === 'tai'">
        <div class="form-row">
          <div class="form-group half">
            <label class="form-label">POS (Punto de operación)</label>
            <input type="number" v-model="form.pos" class="form-input" placeholder="1">
          </div>
          <div class="form-group half">
            <label class="form-label">UID (Cédula del estudiante)</label>
            <input type="number" v-model="form.uid" class="form-input" placeholder="12345678">
          </div>
        </div>
      </template>

      <div class="form-actions">
        <button @click="registrarPago" class="btn-registrar" :disabled="procesando">
          {{ procesando ? 'Procesando...' : 'Registrar pago' }}
        </button>
      </div>
      <p v-if="mensaje" class="form-msg" :class="mensajeError ? 'error' : 'exito'">{{ mensaje }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  transaction: {
    type: Object,
    default: () => ({ descripcion: 'Reserva de servicio', monto: 12.5 })
  }
})
const emit = defineEmits(['paymentConfirmed'])

const descripcion = computed(() => props.transaction?.descripcion ?? 'Reserva de servicio')
const monto = computed(() => (props.transaction?.monto ?? 12.5).toFixed(2))

const categoria = ref('online')
const metodo = ref('zelle')
const procesando = ref(false)
const mensaje = ref('')
const mensajeError = ref(false)

const form = ref({
  // Zelle
  codigoConfirmacion: '',
  correoElectronico: '',
  primerNombre: '',
  primerApellido: '',
  // Crypto
  txid: '',
  red: 'BTC',
  direccionBilletera: '',
  tasaConversion: '',
  // Pago móvil
  nroReferencia: '',
  telefonoEmisor: '',
  banco: 'Bancamiga',
  cedula: '',
  // Tarjeta
  nroTarjeta: '',
  companiaEmisora: 'Visa',
  monedaLiquidacion: 'USD',
  tipoRed: 'nacional',
  fechaVencimiento: '',
  // Efectivo
  monedaDeCurso: 'bolivares',
  denominaciones: [{ cantidad: 1, valor: 10 }],
  // TAI
  pos: '',
  uid: ''
})

watch(categoria, () => {
  metodo.value = categoria.value === 'online' ? 'zelle' : 'movil'
})

const agregarDenominacion = () => {
  form.value.denominaciones.push({ cantidad: 1, valor: 10 })
}

const quitarDenominacion = (i) => {
  form.value.denominaciones.splice(i, 1)
}

const registrarPago = async () => {
  const payload = {
    categoria: categoria.value,
    metodo: metodo.value,
    descripcion: descripcion.value,
    monto: parseFloat(monto.value),
    ...form.value
  }

  procesando.value = true
  mensaje.value = ''

  try {
    const res = await fetch('/api/pagos/procesar', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    })
    const data = await res.json()
    if (res.ok) {
      mensaje.value = data.mensaje || 'Pago registrado correctamente.'
      mensajeError.value = false
      emit('paymentConfirmed')
    } else {
      mensaje.value = data.error || 'Error al procesar el pago.'
      mensajeError.value = true
    }
  } catch (e) {
    mensaje.value = 'Error de conexión con el servidor.'
    mensajeError.value = true
  } finally {
    procesando.value = false
  }
}
</script>

<style scoped>
.pago-container {
  background-color: #a2c8ec;
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
  background-color: #d9e8f5;
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

.radio-group {
  display: flex;
  gap: 30px;
  margin-top: 5px;
  flex-wrap: wrap;
}

.radio-option {
  display: flex;
  align-items: center;
  cursor: pointer;
  font-weight: 500;
}

.radio-option input { display: none; }

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

.den-row {
  display: flex;
  gap: 12px;
  margin-bottom: 10px;
  align-items: center;
}

.den-input {
  flex: 1;
  min-width: 120px;
}

.btn-den-remove {
  background: #e74c3c;
  color: white;
  border: none;
  border-radius: 50%;
  width: 30px;
  height: 30px;
  cursor: pointer;
  font-weight: bold;
}

.btn-den-add {
  background: #2ecc71;
  color: white;
  border: none;
  border-radius: 20px;
  padding: 8px 18px;
  cursor: pointer;
  font-weight: 600;
  font-size: 0.9rem;
}

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

.btn-registrar:hover { transform: translateY(-2px); box-shadow: 0 6px 14px rgba(41, 128, 185, 0.4); }
.btn-registrar:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }

.form-msg { margin-top: 12px; font-weight: 600; font-size: 0.95rem; text-align: right; }
.form-msg.exito { color: #1b7a3d; }
.form-msg.error { color: #c62828; }
</style>
