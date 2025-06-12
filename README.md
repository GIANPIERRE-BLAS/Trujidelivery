# 🚀 Trujidelivery

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green?style=for-the-badge&logo=android" alt="Android">
  <img src="https://img.shields.io/badge/Language-Java%2FKotlin-orange?style=for-the-badge&logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/Database-Firebase-yellow?style=for-the-badge&logo=firebase" alt="Firebase">
  <img src="https://img.shields.io/badge/API-21+-lightblue?style=for-the-badge&logo=android" alt="API">
  <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="License">
</p>

<p align="center">
  <strong>Una aplicación moderna de delivery que conecta usuarios con negocios locales</strong>
</p>

<p align="center">
  <a href="#características">Características</a> •
  <a href="#tecnologías">Tecnologías</a> •
  <a href="#instalación">Instalación</a> •
  <a href="#arquitectura">Arquitectura</a> •
  <a href="#contribución">Contribución</a>
</p>

---

## 📖 Descripción

**Trujidelivery** es una aplicación móvil nativa de Android desarrollada con Java/Kotlin que ofrece una experiencia completa de delivery. Permite a los usuarios explorar negocios cercanos, realizar pedidos y hacer seguimiento de entregas en tiempo real con una interfaz intuitiva y moderna.

### 🎯 Objetivo
Facilitar el acceso a productos y servicios locales mediante una plataforma de delivery eficiente que beneficie tanto a consumidores como a comerciantes.

## ✨ Características

### 🏪 **Exploración Inteligente de Negocios**
- Catálogo completo de restaurantes y tiendas locales
- Sistema de filtros avanzados (categoría, ubicación, valoraciones, precio)
- Información detallada con fotos, horarios y políticas de cada establecimiento
- Búsqueda en tiempo real con sugerencias inteligentes

### 🛒 **Gestión Completa de Pedidos**
- Carrito de compras con persistencia de datos
- Múltiples métodos de pago integrados
- Seguimiento en tiempo real con notificaciones push
- Historial completo de compras con detalles de transacciones

### 📍 **Geolocalización Avanzada**
- Detección automática GPS con alta precisión
- Selección manual con autocompletado de direcciones
- Integración nativa con OSMDroid para mapas offline
- Cálculo inteligente de tiempos y costos de entrega
- Múltiples direcciones guardadas por usuario

### 🔐 **Autenticación y Seguridad**
- Sistema de registro e inicio de sesión multifactor
- Integración OAuth 2.0 con Google Sign-In
- Encriptación de datos sensibles
- Gestión segura de sesiones con tokens JWT
- Verificación por email y SMS

### 💳 **Sistema de Pagos**
- **Efectivo:** Pago contra entrega con monto exacto
- **Yape BCP:** Integración con la plataforma de pagos móviles más popular del Perú
- Procesamiento seguro de transacciones
- Confirmación automática de pagos digitales
- Historial detallado de todas las transacciones

### 💬 **Comunicación en Tiempo Real**
- Chat integrado con sistema de mensajería instantánea
- Comunicación directa usuario-repartidor sin revelar datos personales
- Notificaciones push contextuales y personalizadas
- Seguimiento GPS en vivo con actualizaciones cada 30 segundos
- Sistema de calificación y comentarios post-entrega

### 🎟️ **Motor de Promociones Inteligente**
- Cupones dinámicos basados en comportamiento del usuario
- Descuentos progresivos por fidelidad
- Ofertas geo-específicas y por horarios
- Códigos promocionales compartibles en redes sociales
- Gamificación con sistema de puntos y recompensas

## 🛠️ Tecnologías

### **Frontend Mobile**
```
• Android SDK 21+ (Android 5.0 Lollipop)
• Java 8 + Kotlin 1.8.0
• Android Jetpack Components
• Material Design 3
• ViewBinding & DataBinding
• Navigation Component
```

### **Backend & Cloud Services**
```
• Firebase Suite:
  ├── Firestore (Base de datos NoSQL)
  ├── Authentication (Gestión de usuarios)
  ├── Realtime Database (Chat en tiempo real)
  ├── Cloud Functions (Lógica serverless)
  ├── Cloud Messaging (Push notifications)
  └── Analytics & Crashlytics
```

### **APIs y SDKs Externos**
```
• Google Services:
  ├── Maps SDK & Places API
  ├── Location Services
  └── Sign-In SDK
• Payment Gateway:
  ├── Yape BCP API
  └── Pago en efectivo (contra entrega)
• OSMDroid (Mapas offline)
```

### **Arquitectura y Patrones**
```
• MVVM (Model-View-ViewModel)
• Repository Pattern
• Dependency Injection (Hilt)
• Clean Architecture
• LiveData & ViewModel
• Coroutines para programación asíncrona
```

## 📦 Instalación

### **Prerrequisitos**
- Android Studio 4.2 o superior
- JDK 8 o superior
- Android SDK 21+ (API Level 21)
- Dispositivo Android 5.0+ o emulador
- Conexión a Internet estable
- Cuenta de Firebase (para desarrolladores)

### **Configuración del Entorno**

1. **Clonar el Repositorio**
   ```bash
   git clone https://github.com/GIANPIERRE-BLAS/Trujidelivery.git
   cd Trujidelivery
   ```

2. **Configuración de Firebase**
   ```bash
   # Crear proyecto en Firebase Console
   # Habilitar Authentication, Firestore y Realtime Database
   # Descargar google-services.json
   cp google-services.json app/
   ```

3. **Configuración de APIs Externas**
   ```kotlin
   // En local.properties agregar:
   YAPE_API_KEY=your_yape_api_key
   GOOGLE_MAPS_API_KEY=your_google_maps_key
   ```

4. **Instalación de Dependencias**
   ```bash
   # Abrir Android Studio
   # File -> Open -> Seleccionar carpeta del proyecto
   # Gradle sync automático
   ```

5. **Ejecutar la Aplicación**
   ```bash
   # Conectar dispositivo Android o iniciar emulador
   # Run -> Run 'app' (Shift + F10)
   ```

## 🏗️ Arquitectura

### **Estructura del Proyecto**
```
Trujidelivery/
├── 📱 app/
│   ├── 📁 src/main/java/com/trujidelivery/
│   │   ├── 🎭 activities/          # Actividades principales
│   │   ├── 🔧 adapters/           # Adaptadores RecyclerView
│   │   ├── 🧩 fragments/          # Fragmentos UI
│   │   ├── 📊 models/             # Modelos de datos
│   │   ├── 🗄️ repositories/       # Capa de datos
│   │   ├── 🌐 services/           # APIs y servicios
│   │   ├── 🛠️ utils/              # Utilidades generales
│   │   └── 🏛️ viewmodels/         # Lógica de presentación
│   ├── 📁 src/main/res/
│   │   ├── 🎨 drawable/           # Recursos gráficos
│   │   ├── 📐 layout/             # Layouts XML
│   │   ├── 🎯 values/             # Strings, colores, estilos
│   │   └── 📋 menu/               # Menús de navegación
│   └── 📄 AndroidManifest.xml     # Configuración de la app
├── 🔧 gradle/                     # Scripts de Gradle
├── 📋 build.gradle                # Configuración global
└── 📖 README.md                   # Documentación
```

### **Patrón MVVM Implementado**
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│     View        │────│    ViewModel     │────│   Repository    │
│  (Activities/   │    │   (Lógica de     │    │  (Fuente de     │
│   Fragments)    │    │  Presentación)   │    │     Datos)      │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                        │                        │
         │                        │                        │
    ┌─────────┐              ┌─────────┐              ┌─────────┐
    │LiveData │              │LiveData │              │Firebase │
    │Observer │              │ State   │              │ & APIs  │
    └─────────┘              └─────────┘              └─────────┘
```

## 🚀 Funcionalidades

### ✅ **Implementadas**
- [x] Sistema de autenticación completo (Email + Google)
- [x] Exploración de negocios con filtros avanzados
- [x] Carrito de compras persistente
- [x] Geolocalización con mapas OSMDroid
- [x] Gestión completa de pedidos
- [x] Perfil de usuario editable
- [x] Notificaciones push en tiempo real
- [x] Chat integrado con repartidores
- [x] Sistema de cupones y descuentos
- [x] Pagos con Yape BCP y efectivo
- [x] Seguimiento GPS de entregas
- [x] Historial de pedidos
- [x] Sistema de calificaciones

### 🚧 **En Desarrollo**
- [ ] Programa de fidelización con puntos
- [ ] Integración con más métodos de pago peruanos
- [ ] Sistema de recomendaciones con IA
- [ ] Modo offline para consultas básicas
- [ ] Dashboard para comerciantes
- [ ] Sistema de reportes avanzados

### 🔮 **Roadmap Futuro**
- [ ] Expansión a otras ciudades del Perú
- [ ] Integración con billeteras digitales locales
- [ ] Sistema de suscripciones premium
- [ ] Marketplace para productos especializados
- [ ] App para repartidores independientes

## 📊 Métricas y Rendimiento

### **Optimizaciones Implementadas**
- ⚡ Carga lazy de imágenes con Glide
- 🗃️ Caché inteligente de datos con Room
- 🔄 Sincronización incremental con Firebase
- 📱 Soporte para modo oscuro automático
- 🌐 Manejo offline con WorkManager
- 🔋 Optimización de batería con JobScheduler

## 🧪 Testing

### **Cobertura de Pruebas**
```
Unit Tests:     85% cobertura
Integration:    70% cobertura
UI Tests:       60% cobertura
```

### **Ejecutar Tests**
```bash
# Tests unitarios
./gradlew test

# Tests de integración
./gradlew connectedAndroidTest

# Reporte de cobertura
./gradlew jacocoTestReport
```

## 🤝 Contribución

### **Cómo Contribuir**
1. 🍴 Fork el repositorio
2. 🌿 Crear branch de feature (`git checkout -b feature/nueva-funcionalidad`)
3. ✅ Asegurar que los tests pasen
4. 📝 Commit con mensaje descriptivo
5. 🚀 Push al branch (`git push origin feature/nueva-funcionalidad`)
6. 🔄 Crear Pull Request

### **Estándares de Código**
- 📏 Seguir convenciones de Kotlin/Java
- 📖 Documentar funciones públicas
- ✅ Incluir tests para nuevas funcionalidades
- 🎨 Seguir Material Design Guidelines
- 🔍 Code review obligatorio antes de merge

## 📄 Licencia

Este proyecto está licenciado bajo la **Licencia MIT**. Ver [LICENSE](LICENSE) para más detalles.

```
MIT License - Uso libre para fines educativos y comerciales
```

## 👨‍💻 Desarrollador

<div align="center">

### **Gianpierre Blas Flores**
*Desarrollador Android | Computación e Informática*

[![GitHub](https://img.shields.io/badge/GitHub-@GIANPIERRE--BLAS-black?style=flat-square&logo=github)](https://github.com/GIANPIERRE-BLAS)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-Gianpierre%20Blas-blue?style=flat-square&logo=linkedin)](https://www.linkedin.com/in/justo-gianpierre-blas-flores-5ba671302/)
[![Email](https://img.shields.io/badge/Email-gianpierreblasflores235@gmail.com-red?style=flat-square&logo=gmail)](mailto:gianpierreblasflores235@gmail.com)

</div>

---

## 🙏 Agradecimientos

- 🍕 **Inspiración:** Rappi, Glovo, PedidosYa
- 👥 **Comunidad:** Desarrolladores Android Perú
- 📚 **Recursos:** Google Developers, Firebase Documentation
- 🏛️ **Instituciones:** Instituto Superior Tecnológico Cibertec
- 💡 **Mentores:** Comunidad de Stack Overflow en Español

## 📞 Soporte y Contacto

### **¿Necesitas Ayuda?**
- 🐛 **Reportar Bug:** [Crear Issue](https://github.com/GIANPIERRE-BLAS/Trujidelivery/issues)
- 💡 **Sugerir Feature:** [Discussions](https://github.com/GIANPIERRE-BLAS/Trujidelivery/discussions)
- 📧 **Contacto Directo:** gianpierreblasflores235@gmail.com
- 📖 **Documentación:** [Wiki del Proyecto](https://github.com/GIANPIERRE-BLAS/Trujidelivery/wiki)

### **Tiempos de Respuesta**
- 🔥 Bugs críticos: 24-48 horas
- 🐛 Bugs menores: 3-5 días hábiles
- 💡 Features: 1-2 semanas
- ❓ Preguntas generales: 1-3 días hábiles

---

<div align="center">

### 🌟 **¡Dale una estrella si te gusta el proyecto!** ⭐

[![Stargazers](https://img.shields.io/github/stars/GIANPIERRE-BLAS/Trujidelivery?style=social)](https://github.com/GIANPIERRE-BLAS/Trujidelivery/stargazers)
[![Forks](https://img.shields.io/github/forks/GIANPIERRE-BLAS/Trujidelivery?style=social)](https://github.com/GIANPIERRE-BLAS/Trujidelivery/network/members)
[![Issues](https://img.shields.io/github/issues/GIANPIERRE-BLAS/Trujidelivery)](https://github.com/GIANPIERRE-BLAS/Trujidelivery/issues)

**Hecho con ❤️ en Perú 🇵🇪**

</div>
