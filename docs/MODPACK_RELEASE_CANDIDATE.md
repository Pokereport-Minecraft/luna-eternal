# POKEREPORT NETWORK — MODPACK BASE RELEASE CANDIDATE

**Fecha de Generación:** 08/10/2026  
**Versión Base:** `0.2.0`  
**Nombre del Archivo:** `PokeReport-Base-0.2.0.mrpack`  
**Ubicación Staging:** `build/release-candidate/client/PokeReport-Base-0.2.0.mrpack`  
**Estado:** `STAGED — NOT PUBLISHED TO CDN`

---

## 1. ESPECIFICACIÓN DEL ARTEFACTO .MRPACK

El modpack base empaqueta las dependencias de cliente optimizadas bajo el estándar Modrinth Modpack Specification v1:

- **Tamaño:** `153,883,298 bytes` (~146.7 MB)
- **SHA-256:** `69d1d47eca30a541745f52b524db6c5d4b521370f35b664d066006f21e1fb45a`
- **Juego Base:** Minecraft 1.21.1
- **Cargador:** Fabric Loader 0.19.5
- **Format Version:** 1

---

## 2. COMPOSICIÓN DE DEPENDENCIAS Y OVERRIDES

### Dependencias Principales Gestionadas en `modrinth.index.json`:
- **Cobblemon 1.8.0+1.21.1**
- **Fabric API 0.116.15+1.21.1**
- **ImmediatelyFast, ModernFix, FerriteCore, Lithium, Krypton** (Optimización extrema de cliente)
- **Roughly Enough Items (REI) & LumyREI**
- **Xaero's Minimap & Worldmap**
- **Sound Physics Remastered, 3D Skin Layers, Better Third Person**
- **Cobbleverse Badges & Packs Cosméticos**

### Overrides Incluidos en el Paquete:
- `config/` preconfigurados para rendimiento óptimo de shaders y renderizado.
- `servers.dat` con la entrada preestablecida de PokeReport Network (`15.235.16.131`).
- `resourcepacks/` oficiales del servidor.

---

## 3. PRUEBA DE INSTALACIÓN SIMULADA

La herramienta `tools/release_client.py` ejecutó una prueba de instalación simulada en un entorno temporal aislado:
- Descompresión completa de la estructura de carpetas.
- Verificación de directorios obligatorios: `mods/`, `config/`, `resourcepacks/`.
- Verificación del archivo `servers.dat`.
- **Resultado:** PASS (Instalación limpia y válida al 100%).
