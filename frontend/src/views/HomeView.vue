<script setup>
import { ref } from 'vue'

// Variables para capturar los datos de los formularios
const loginData = ref({ usuario: '', contrasena: '' })
const registerData = ref({
  nombres: '', apellidos: '', correo: '', contrasena: '', genero: '', fechaNacimiento: ''
})

const handleLogin = () => {
  console.log('Intentando login con:', loginData.value)
  // Aquí luego agregarás la lógica para ir al backend
}

const handleRegister = () => {
  console.log('Intentando registro con:', registerData.value)
  // Aquí luego agregarás la lógica para ir al backend
}
</script>

<template>
  <div class="home-container">
    
    <header class="navbar">
      <div class="logo">UCAB-Services</div>
      
      <div class="login-form">
        <div class="input-group">
          <label>Usuario:</label>
          <input type="text" v-model="loginData.usuario" />
        </div>
        <div class="input-group">
          <label>Contraseña:</label>
          <input type="password" v-model="loginData.contrasena" />
          <a href="#" class="forgot-link">¿Olvidó su contraseña?</a>
        </div>
        <button class="btn-blue" @click="handleLogin">Ingresar</button>
      </div>
    </header>

    <main class="main-content">
      
      <section class="hero-text">
        <h1>Conecta con tu<br>vida universitaria<br>en un clic</h1>
      </section>

      <section class="register-section">
        <div class="register-card">
          <p class="subtitle">¿No te has registrado todavía?</p>
          <h2>Únete ya a la familia UCAB</h2>
          
          <form @submit.prevent="handleRegister" class="form-grid">
            <div class="form-group">
              <label>Nombres:</label>
              <input type="text" v-model="registerData.nombres" />
            </div>
            
            <div class="form-group">
              <label>Apellidos:</label>
              <input type="text" v-model="registerData.apellidos" />
            </div>
            
            <div class="form-group">
              <label>Correo electrónico:</label>
              <input type="email" v-model="registerData.correo" />
            </div>
            
            <div class="form-group">
              <label>Contraseña:</label>
              <input type="password" v-model="registerData.contrasena" />
            </div>
            
            <div class="form-row">
              <div class="form-group gender-group">
                <label>Género:</label>
                <div class="radio-options">
                  <label><input type="radio" value="Mujer" v-model="registerData.genero"> Mujer.</label>
                  <label><input type="radio" value="Hombre" v-model="registerData.genero"> Hombre.</label>
                  <label><input type="radio" value="Otro" v-model="registerData.genero"> Otro.</label>
                </div>
              </div>
              
              <div class="form-group date-group">
                <label>Fecha de nacimiento:</label>
                <input type="date" v-model="registerData.fechaNacimiento" />
              </div>
            </div>
            
            <div class="submit-row">
              <button type="submit" class="btn-blue register-btn">Registrarme</button>
            </div>
          </form>
        </div>
      </section>

    </main>
  </div>
</template>

<style scoped>
/* Contenedor principal con imagen de fondo */
.home-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-image: url('../assets/fondo-ucab.png');
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
}

/* NAVBAR (Verde) */
.navbar {
  background-color: #0b7c3e; /* Verde UCAB */
  padding: 15px 40px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  color: white;
}

.logo {
  font-family: 'Times New Roman', serif;
  font-size: 2.2rem;
  margin-top: 5px;
}

.login-form {
  display: flex;
  gap: 15px;
  align-items: flex-start;
}

.input-group {
  display: flex;
  flex-direction: column;
}

.input-group label {
  font-size: 0.85rem;
  margin-bottom: 4px;
}

.input-group input {
  padding: 8px 12px;
  border-radius: 20px;
  border: none;
  outline: none;
  background-color: #c4d6c4; /* Tono gris/verdoso del input */
  width: 180px;
}

.forgot-link {
  color: white;
  font-size: 0.75rem;
  margin-top: 5px;
  text-decoration: none;
}

.forgot-link:hover {
  text-decoration: underline;
}

/* BOTONES AZULES */
.btn-blue {
  background-color: #5bb3e6;
  color: white;
  border: none;
  border-radius: 20px;
  padding: 8px 25px;
  cursor: pointer;
  font-weight: bold;
  margin-top: 20px; /* Alineado con los inputs */
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
  transition: background 0.3s;
}

.btn-blue:hover {
  background-color: #4aa3d6;
}

/* CONTENIDO PRINCIPAL */
.main-content {
  flex: 1;
  display: flex;
  justify-content: space-between;
  padding: 40px 80px;
  align-items: center;
}

/* TEXTO IZQUIERDO */
.hero-text h1 {
  font-family: 'Times New Roman', serif;
  font-size: 4rem;
  color: white;
  font-style: italic;
  text-shadow: 2px 2px 4px rgba(0,0,0,0.5);
  line-height: 1.2;
}

/* TARJETA DE REGISTRO */
.register-card {
  background-color: rgba(226, 214, 150, 0.85); /* Amarillo/Beige translúcido */
  padding: 30px 40px;
  border-radius: 15px;
  width: 450px;
  box-shadow: 0 10px 20px rgba(0,0,0,0.2);
  color: white;
}

.subtitle {
  text-align: center;
  margin: 0;
  font-size: 1.1rem;
}

.register-card h2 {
  text-align: center;
  margin-top: 5px;
  margin-bottom: 20px;
  font-size: 1.6rem;
  font-weight: bold;
}

.form-group {
  display: flex;
  flex-direction: column;
  margin-bottom: 15px;
}

.form-group label {
  font-weight: bold;
  font-size: 0.9rem;
  margin-bottom: 5px;
}

.form-group input[type="text"],
.form-group input[type="email"],
.form-group input[type="password"] {
  padding: 10px;
  border-radius: 20px;
  border: none;
  background-color: #e8e8e8;
  outline: none;
}

.form-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 15px;
}

.radio-options {
  display: flex;
  flex-direction: column;
  gap: 5px;
  font-size: 0.9rem;
}

.date-group input {
  padding: 8px;
  border-radius: 20px;
  border: none;
  background-color: #e8e8e8;
}

.submit-row {
  display: flex;
  justify-content: flex-end;
}

.register-btn {
  padding: 10px 30px;
  font-size: 1rem;
  margin-top: 0;
}
</style>