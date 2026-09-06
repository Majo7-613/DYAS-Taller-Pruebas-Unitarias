# Registro de Defectos — EJEMPLO RESUELTO

> 📌 **La entrega del equipo está en [`registraduria/defectos.md`](registraduria/defectos.md)** — 8 defectos documentados, ninguno abierto.

> ℹ️ **Este archivo es un ejemplo del profesor**, no su entrega. Muestra el nivel de detalle y los dos formatos aceptados.
> Para su taller, parta de [`defectos_template.md`](defectos_template.md) y documente los defectos que **usted** encuentre al ejecutar sus propias pruebas.

Este documento recopila los defectos encontrados durante la ejecución de pruebas unitarias del proyecto **Registraduría**.
Cada defecto debe documentarse claramente para facilitar su análisis y corrección.

Los defectos de abajo se detectaron sobre el estado del código **al terminar la iteración 1** del README (cuando `registerVoter` aún devolvía `VALID` para cualquier entrada).

---

## Formato 1: Lista detallada (narrativa)

### Defecto 01

- **Caso de prueba**: Persona con edad -1 (edad inválida).
- **Entrada**: `Person(name="Juan", id=101, age=-1, gender=MALE, alive=true)`
- **Resultado esperado**: `INVALID_AGE`
- **Resultado obtenido**: `VALID`
- **Causa probable**: Falta de validación de edad negativa en `Registry.registerVoter`.
- **Estado**: Abierto

---

### Defecto 02

- **Caso de prueba**: Persona muerta.
- **Entrada**: `Person(name="Ana", id=102, age=45, gender=FEMALE, alive=false)`
- **Resultado esperado**: `DEAD`
- **Resultado obtenido**: `VALID`
- **Causa probable**: No se evalúa la condición `alive=false`.
- **Estado**: **Resuelto** — corregido en la iteración 2 (`if (!p.isAlive()) return RegisterResult.DEAD;`) y verificado con la prueba `shouldRejectDeadPerson`.

---

### Defecto 03

- **Caso de prueba**: Registro duplicado con el mismo `id`.
- **Entradas**:
  - Persona 1: `Person(name="Carlos", id=200, age=30, gender=MALE, alive=true)`
  - Persona 2: `Person(name="Carla", id=200, age=25, gender=FEMALE, alive=true)`
- **Resultado esperado**:
  - Persona 1 → `VALID`
  - Persona 2 → `DUPLICATED`
- **Resultado obtenido**:
  - Persona 1 → `VALID`
  - Persona 2 → `VALID`
- **Causa probable**: No hay verificación de `id` previamente registrado.
- **Estado**: Abierto

---

## Formato 2: Tabla de defectos (bug tracking)

| ID | Caso de Prueba | Entrada | Resultado Esperado | Resultado Obtenido | Causa Probable | Estado |
|-----|---------------------|---------|--------------------|--------------------|----------------|--------|
| 01 | Edad inválida | `Person(id=101, age=-1, alive=true)` | `INVALID_AGE` | `VALID` | No se valida edad negativa | Abierto |
| 02 | Persona muerta | `Person(id=102, age=45, alive=false)` | `DEAD` | `VALID` | No se evalúa condición `alive=false` | Resuelto (iteración 2) |
| 03 | Registro duplicado | `Person(id=200, age=30, alive=true)` + `Person(id=200, age=25, alive=true)` | 1º → `VALID` 2º → `DUPLICATED` | 1º → `VALID` 2º → `VALID` | No hay verificación de `id` duplicado | Abierto |

---

## Convenciones de Estado

| Estado | Significado |
|---------|-------------|
| **Abierto** | El defecto fue detectado pero no corregido. |
| **En progreso** | El defecto se encuentra en análisis o corrección. |
| **Resuelto** | El defecto fue corregido y validado mediante pruebas. |

---

## Observaciones

- Se pueden usar **ambos formatos** o elegir uno como estándar de equipo.
- El objetivo es **gestionar la calidad del software** y **demostrar un proceso sistemático de testing**.
- Mantener este archivo actualizado durante todo el ciclo de desarrollo.
