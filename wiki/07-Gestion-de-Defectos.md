# 07 — Gestión de defectos

Registro completo: [`registraduria/defectos.md`](https://github.com/Majo7-613/DYAS-Taller-Pruebas-Unitarias/blob/main/registraduria/defectos.md)

Se registraron **8 defectos**. Ninguno quedó abierto, y cada uno tiene una prueba automatizada asociada que actúa como prueba de regresión: si el defecto reaparece, la barra vuelve a ponerse roja.

---

## Tabla de defectos

| ID | Defecto | Esperado | Obtenido | Detectado por | Estado |
|----|---------|----------|----------|---------------|--------|
| 01 | Documento no positivo aceptado | `INVALID` | `VALID` | `shouldRejectWhenIdIsZeroOrNegative` | Resuelto (iter. 3) |
| 02 | Menor de edad registrado | `UNDERAGE` | `VALID` | `shouldRejectUnderageAt17` | Resuelto (iter. 4) |
| 03 | Edad imposible clasificada como minoría de edad | `INVALID_AGE` | `UNDERAGE` / `VALID` | `shouldRejectNegativeAge`, `shouldRejectInvalidAgeOver120` | Resuelto (iter. 5) |
| 04 | Documento duplicado aceptado | `DUPLICATED` | `VALID` | `shouldReturnDuplicatedWhenSameIdRegisteredTwice` | Resuelto (iter. 6) |
| 05 | Un rechazo consume el documento *(simulado)* | `VALID` | `DUPLICATED` | `shouldNotRegisterIdWhenPersonIsRejected` | Resuelto (revertido) |
| 06 | `Registry` duplicado en el árbol de pruebas | Una sola clase | Dos, sombreándose | `git ls-files` | Resuelto |
| 07 | Una prueba anulaba su `@BeforeEach` | Instancia limpia | Variable local que la sombrea | Revisión de código | Resuelto |
| 08 | Dos mutantes vivos con 100 % de cobertura | Score ≥ 60 % | 91,67 %, 2 vivos | Reporte de PIT | Resuelto (`PersonTest`) |

---

## Los defectos no vinieron todos del mismo sitio

Es lo más interesante del registro: **cada grupo se detectó con un mecanismo distinto**, y ninguno de los tres mecanismos habría encontrado los defectos de los otros.

| Origen | Defectos | Cómo se detectaron |
|---|---|---|
| **Dominio** | 01 – 04 | La barra roja del ciclo TDD |
| **Simulado** | 05 | Inyección deliberada de un fallo |
| **Configuración** | 06, 07 | Revisión del proyecto |
| **Herramienta** | 08 | Reporte de mutación de PIT |

Los defectos 06 y 07 no estaban en el código productivo, pero **comprometían la validez de todo lo demás**: con `Registry` duplicado, las pruebas ejercitaban una clase distinta de la que PIT mutaba. Cualquier métrica calculada en ese estado habría sido falsa.

---

## Tres defectos que vale la pena mirar de cerca

### Defecto 03 — Una regla implementada puede ser incorrecta

El único defecto del taller que **no** fue una regla ausente, sino una regla *mal ordenada*.

Tras implementar R5 (`edad < 18 → UNDERAGE`), una edad de `-1` devolvía `UNDERAGE`. Técnicamente coherente —`-1 < 18`— y funcionalmente equivocado: `-1` no describe a un menor de edad, describe un dato imposible. El sistema estaba ocultando un error de captura de datos detrás de una respuesta de negocio plausible.

El mensaje de fallo lo dijo con precisión:

```
expected: <INVALID_AGE> but was: <UNDERAGE>
```

No `but was: <VALID>`, que habría indicado una regla faltante. `UNDERAGE` señalaba que **otra regla estaba respondiendo en su lugar**.

> La corrección no fue añadir código, sino insertarlo en el punto correcto de la secuencia. De ahí que el orden de evaluación esté fijado con pruebas explícitas y no solo documentado.

### Defecto 05 — Comprobar que la prueba sirve

Este defecto lo **inyectamos a propósito**. La regla R6 se implementó bien a la primera, así que no había forma natural de saber si `shouldNotRegisterIdWhenPersonIsRejected` estaba haciendo algo útil.

Movimos el registro del documento desde el final del método hasta justo después de validar su formato:

```java
if (p.getId() < MIN_VALID_ID) return RegisterResult.INVALID;
if (!registeredIds.add(p.getId())) return RegisterResult.DUPLICATED;  // <-- demasiado pronto
if (!p.isAlive()) return RegisterResult.DEAD;
```

Es un error realista: parece una simplificación elegante —una sola operación en vez de `contains` más `add`— y es incorrecta, porque marca el documento como usado antes de saber si el registro va a prosperar.

**Resultado: de las 30 pruebas, falló exactamente una.**

```
edu.unisabana.tyvs.domain.service.RegistryTest.shouldNotRegisterIdWhenPersonIsRejected <<< FAILURE!
org.opentest4j.AssertionFailedError: expected: <VALID> but was: <DUPLICATED>
```

Las otras 29 siguieron en verde, porque todas registran personas que superan todas las reglas. Es la mejor justificación posible para la existencia de esa prueba: sin ella, este error habría llegado a producción con la suite completa en verde y el 100 % de cobertura.

[Evidencia](https://github.com/Majo7-613/DYAS-Taller-Pruebas-Unitarias/blob/main/wiki/evidencia/defecto05-inyectado.txt) · Inyección revertida; el código final tiene el `add` como última instrucción.

### Defecto 06 — La suite en verde que no probaba nada

Existían dos archivos `Registry.java` con el mismo paquete: uno en `src/main` y otro en `src/test`. La copia del árbol de pruebas **sombreaba** a la de producción en el classpath de pruebas.

El efecto era silencioso y grave:

- Las pruebas ejercitaban la copia de `src/test`.
- PIT mutaba la clase real de `target/classes`.
- El *mutation score* habría medido una clase que ninguna prueba cargaba.
- `mvn test` en verde no decía nada sobre el código a entregar.

No hubo ningún error de compilación ni advertencia. El proyecto compilaba, las pruebas pasaban y las métricas salían — todas mintiendo.

> **Lección**: antes de medir, hay que asegurarse de estar midiendo lo correcto. Una suite en verde sobre el archivo equivocado es peor que una suite en rojo, porque produce confianza injustificada.

---

## Qué nos llevamos de la gestión de defectos

**Un defecto sin prueba no está cerrado.** Los ocho tienen una prueba asociada. Cerrar un defecto arreglando el código y siguiendo adelante deja abierta la puerta a que vuelva; la prueba es lo que convierte la corrección en permanente.

**El mensaje de fallo es información, no ruido.** `expected: <INVALID_AGE> but was: <UNDERAGE>` y `but was: <VALID>` describen dos problemas distintos: en el primero otra regla está respondiendo, en el segundo no hay regla. Leer con atención el valor obtenido ahorró la mitad del diagnóstico del defecto 03.

**Ninguna herramienta encuentra todo.** El ciclo TDD encontró los defectos de dominio, pero no vio los getters sin verificar. PIT encontró esos, pero no habría detectado el `Registry` duplicado. La revisión del proyecto encontró ese, pero no sustituye a ninguna de las otras dos.
