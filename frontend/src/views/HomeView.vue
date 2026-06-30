<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { setAuthUser } from '../services/authService'

const router = useRouter()

// Variables para capturar los datos de los formularios
const loginData = ref({ usuario: '', contrasena: '' })
const registerData = ref({
  ci: '',
  primerNombre: '',
  primerApellido: '',
  sexo: 'F',
  correo: '',
  contrasena: '',
  direccion: '',
  fechaNacimiento: '',
  telefono: '',
  categoria: 'frecuente'
})
const loginError = ref('')
const registerError = ref('')
const registerSuccess = ref('')

const handleLogin = async () => {
  loginError.value = ''

  if (!loginData.value.usuario || !loginData.value.contrasena) {
    loginError.value = 'Por favor ingresa usuario y contraseña.'
    return
  }

  try {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(loginData.value)
    })

    if (!response.ok) {
      const errorBody = await response.text()
      try {
        const parsedError = JSON.parse(errorBody)
        loginError.value = parsedError.error || 'Credenciales inválidas.'
      } catch {
        loginError.value = errorBody || 'Credenciales inválidas.'
      }
      return
    }

    const user = await response.json()
    setAuthUser(user)
    router.push({ name: 'main' })
  } catch (error) {
    console.error('Error al autenticar:', error)
    loginError.value = 'No se pudo conectar con el servidor. Intenta de nuevo.'
  }
}

const handleRegister = async () => {
  registerError.value = ''
  registerSuccess.value = ''

  const requiredFields = [
    { value: registerData.value.ci, label: 'CI' },
    { value: registerData.value.primerNombre, label: 'Primer nombre' },
    { value: registerData.value.primerApellido, label: 'Primer apellido' },
    { value: registerData.value.correo, label: 'Correo institucional' },
    { value: registerData.value.contrasena, label: 'Contraseña' },
    { value: registerData.value.direccion, label: 'Dirección' },
    { value: registerData.value.fechaNacimiento, label: 'Fecha de nacimiento' },
    { value: registerData.value.telefono, label: 'Teléfono' }
  ]

  const missing = requiredFields.filter(field => !field.value || String(field.value).trim() === '')
  if (missing.length > 0) {
    registerError.value = `Faltan campos obligatorios: ${missing.map(f => f.label).join(', ')}`
    return
  }

  try {
    const payload = {
      CI: parseInt(registerData.value.ci, 10),
      PrimerNombre: registerData.value.primerNombre,
      PrimerApellido: registerData.value.primerApellido,
      Sexo: registerData.value.sexo,
      CorreoInstitucional: registerData.value.correo,
      DireccionHabitacion: registerData.value.direccion,
      FechaNacimiento: registerData.value.fechaNacimiento,
      Telefono: registerData.value.telefono,
      Categoria: registerData.value.categoria,
      contrasena: registerData.value.contrasena
    }

    const response = await fetch('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    })

    if (!response.ok) {
      const errorBody = await response.text()
      try {
        const parsedError = JSON.parse(errorBody)
        registerError.value = parsedError.error || 'No se pudo registrar el usuario.'
      } catch {
        registerError.value = errorBody || 'No se pudo registrar el usuario.'
      }
      return
    }

    const user = await response.json()
    setAuthUser(user)
    registerSuccess.value = 'Registro exitoso. Redirigiendo...'
    setTimeout(() => router.push({ name: 'main' }), 700)
  } catch (error) {
    console.error('Error al registrar:', error)
    registerError.value = 'No se pudo conectar con el servidor. Intenta de nuevo.'
  }
}

const goToMain = () => {
  router.push({ name: 'main' })
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
        <button class="btn-blue" @click="handleLogin" type="button">Ingresar</button>
      </div>
      <div v-if="loginError" class="login-error">{{ loginError }}</div>
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
            <div class="form-row">
              <div class="form-group">
                <label>CI:</label>
                <input
                  type="text"
                  v-model="registerData.ci"
                  inputmode="numeric"
                  pattern="[0-9]*"
                  maxlength="12"
                />
              </div>
              <div class="form-group">
                <label>Teléfono:</label>
                <input
                  type="text"
                  v-model="registerData.telefono"
                  inputmode="numeric"
                  pattern="[0-9+\s-]*"
                />
              </div>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label>Primer nombre:</label>
                <input type="text" v-model="registerData.primerNombre" />
              </div>
              <div class="form-group">
                <label>Primer apellido:</label>
                <input type="text" v-model="registerData.primerApellido" />
              </div>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label>Correo institucional:</label>
                <input type="email" v-model="registerData.correo" />
              </div>
              <div class="form-group">
                <label>Dirección:</label>
                <input type="text" v-model="registerData.direccion" />
              </div>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label>Contraseña:</label>
                <input type="password" v-model="registerData.contrasena" />
              </div>
              <div class="form-group">
                <label>Fecha de nacimiento:</label>
                <input type="date" v-model="registerData.fechaNacimiento" />
              </div>
            </div>

            <div class="form-row">
              <div class="form-group gender-group">
                <label>Sexo:</label>
                <div class="radio-options">
                  <label><input type="radio" value="F" v-model="registerData.sexo"> F</label>
                  <label><input type="radio" value="M" v-model="registerData.sexo"> M</label>
                </div>
              </div>

              <div class="form-group">
                <label>Categoría:</label>
                <select v-model="registerData.categoria">
                  <option value="frecuente">Frecuente</option>
                  <option value="preferencial">Preferencial</option>
                </select>
              </div>
            </div>

            <div class="form-row">
              <div class="form-group">
                <button type="submit" class="btn-blue register-btn">Registrarme</button>
              </div>
              <div class="form-group">
                <p class="register-status" v-if="registerError">{{ registerError }}</p>
                <p class="register-success" v-if="registerSuccess">{{ registerSuccess }}</p>
              </div>
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

.btn-secondary {
  background-color: white;
  color: #0b7c3e;
  border: 2px solid #0b7c3e;
  border-radius: 20px;
  padding: 8px 25px;
  cursor: pointer;
  font-weight: bold;
  margin-top: 20px;
}

.btn-secondary:hover {
  background-color: #f0f7f0;
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
  background-color: rgba(226, 214, 150, 0.92); /* Amarillo/Beige translúcido */
  padding: 24px 28px;
  border-radius: 16px;
  width: 420px;
  box-shadow: 0 10px 18px rgba(0,0,0,0.18);
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
  margin-bottom: 12px;
  width: 100%;
}

.form-group label {
  font-weight: bold;
  font-size: 0.9rem;
  margin-bottom: 5px;
}

.form-group input[type="text"],
.form-group input[type="email"],
.form-group input[type="password"],
.form-group input[type="date"],
.form-group select {
  width: 100%;
  padding: 10px 12px;
  border-radius: 14px;
  border: none;
  background-color: #f3f3f3;
  outline: none;
  box-sizing: border-box;
}

.form-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.form-row .form-group {
  flex: 1;
  min-width: 150px;
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