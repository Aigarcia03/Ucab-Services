<template>
  <div class="estado-cuenta-wrapper">
    <h2 class="page-title">Estado de cuenta</h2>

    <!-- Summary Card -->
    <div class="summary-card">
      <div class="saldo-row">
        <strong>Saldo Total Pendiente: ${{ estadoCuentaStore.saldoTotal.toFixed(2) }}</strong>
      </div>
      <hr class="summary-divider" />
      <div class="summary-details">
        <div class="detail-item">
          <span class="detail-label">Tasa de cambio:</span>
          <span class="detail-value">36.65 Bs./USD</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">Estatus de solvencia:</span>
          <span class="detail-value" :class="estadoCuentaStore.saldoTotal > 0 ? 'no-solvente' : 'solvente'">
            {{ estadoCuentaStore.saldoTotal > 0 ? '● No solvente' : '● Solvente' }}
          </span>
        </div>
      </div>
    </div>

    <!-- Items List -->
    <div class="items-list">
      <div v-for="item in estadoCuentaStore.pendientes" :key="item.id" class="item-card">
        <div class="item-left">
          <span class="item-title">{{ item.titulo }}</span>
          <span class="item-code">SERV-{{ item.id.toString().slice(-4) }}</span>
        </div>
        <div class="item-right">
          <span class="item-date">{{ item.fecha }}</span>
          <strong class="item-amount">Pendiente  ${{ item.precioBase.toFixed(2) }}</strong>
        </div>
      </div>

      <div v-if="estadoCuentaStore.pendientes.length === 0" class="no-items">
        <p>No tienes pagos pendientes. ¡Estás solvente!</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue';
import { estadoCuentaStore } from '../services/estadoCuentaStore';

const props = defineProps({ userCi: { type: Number, default: null } });

onMounted(() => {
  estadoCuentaStore.cargarDesdeDB(props.userCi);
});
</script>

<style scoped>
.estado-cuenta-wrapper {
  width: 100%;
  max-width: 860px;
}

.page-title {
  font-size: 26px;
  font-weight: bold;
  color: #1e2b38;
  margin-bottom: 24px;
}

/* Summary card — matches the profile-card golden color */
.summary-card {
  background-color: #dcedc8;
  border-radius: 16px;
  padding: 22px 28px;
  box-shadow: 0 4px 6px rgba(0,0,0,0.06);
  margin-bottom: 24px;
}

.saldo-row {
  font-size: 18px;
  color: #1e2b38;
  margin-bottom: 4px;
}

.summary-divider {
  border: none;
  border-top: 1px solid rgba(0,0,0,0.15);
  margin: 14px 0;
}

.summary-details {
  display: flex;
  gap: 60px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
  font-size: 14px;
}

.detail-label {
  font-weight: 600;
  color: #333;
}

.detail-value {
  color: #444;
}

.solvente {
  color: #2e7d32;
  font-weight: 600;
}

.no-solvente {
  color: #c62828;
  font-weight: 600;
}

/* Item cards — same green as nav-item active in Dashboard.css */
.items-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.item-card {
  background-color: #c5e1a5;
  border-radius: 12px;
  padding: 16px 22px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 5px rgba(0,0,0,0.05);
}

.item-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.item-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e2b38;
}

.item-code {
  font-size: 13px;
  color: #444;
}

.item-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.item-date {
  font-size: 13px;
  font-weight: 600;
  color: #1e2b38;
}

.item-amount {
  font-size: 15px;
  color: #1e2b38;
}

.no-items {
  text-align: center;
  color: #666;
  padding: 40px;
  background-color: #f4f1e6;
  border-radius: 12px;
  font-size: 15px;
}
</style>
