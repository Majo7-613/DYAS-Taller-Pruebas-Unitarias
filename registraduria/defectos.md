# Registro de Defectos — Registraduría

Documento de gestión de defectos del proyecto `registraduria`. Recoge los defectos detectados durante el desarrollo dirigido por pruebas del caso de uso `registerVoter(Person)`.

Cada defecto se detectó **ejecutando una prueba que falló** (barra roja del ciclo TDD), no por inspección visual del código. Por eso todos tienen una prueba asociada: esa prueba es a la vez la evidencia del defecto y la garantía de que no vuelve a aparecer (prueba de regresión).

> El archivo [`defectos.md` de la raíz](../defectos.md) es el ejemplo resuelto del profesor. Este es el registro del equipo.

---

## Formato 1: Lista detallada (narrativa)

### Defecto 01 — El documento no positivo se acepta

- **Caso de prueba**: persona viva y mayor de edad, con `id = 0`.
- **Entrada**: `Person(name="Beatriz", id=0, age=25, gender=FEMALE, alive=true)`
- **Resultado esperado**: `INVALID` (regla R2)
- **Resultado obtenido**: `VALID`
- **Causa probable**: `registerVoter` no validaba el número de documento. Tras la iteración 2 solo existían las guardas de nulidad y de estado de vida.
- **Detectado por**: `shouldRejectWhenIdIsZeroOrNegative`, que además ejercita `-1` y `-5`.
- **Estado**: **Resuelto** — iteración 3, guarda `p.getId() < MIN_VALID_ID` insertada antes de la validación de estado de vida.

---

### Defecto 02 — El menor de edad queda registrado

- **Caso de prueba**: persona viva de 17 años con documento válido.
- **Entrada**: `Person(name="Elena", id=3, age=17, gender=FEMALE, alive=true)`
- **Resultado esperado**: `UNDERAGE` (regla R5)
- **Resultado obtenido**: `VALID`
- **Causa probable**: la edad no participaba en ninguna decisión; `registerVoter` nunca llamaba a `getAge()`.
- **Detectado por**: `shouldRejectUnderageAt17` y, sobre el rango completo `[0,17]`, por la propiedad `todoMenorDeEdadEsRechazado`.
- **Observación**: la propiedad de jqwik falló con una muestra aleatoria (`edad = 10`, `id = 29442`) y la **redujo** a la mínima que rompe la regla: `nombre = ""`, `id = 1`, `edad = 0`. Ver [evidencia del shrinking](../wiki/evidencia/shrinking-todoMenorDeEdadEsRechazado.txt).
- **Estado**: **Resuelto** — iteración 4.

---

### Defecto 03 — Las edades biológicamente imposibles no se distinguen

- **Caso de prueba**: persona viva con edad `-1`, y persona viva con edad `121`.
- **Entradas**:
  - `Person(name="Gloria", id=5, age=-1, gender=FEMALE, alive=true)`
  - `Person(name="Jorge", id=8, age=121, gender=MALE, alive=true)`
- **Resultado esperado**: `INVALID_AGE` en ambos casos (regla R4)
- **Resultado obtenido**:
  - edad `-1` → `UNDERAGE`
  - edad `121` → `VALID`
- **Causa probable**: este es el defecto **más interesante del taller**, porque no era una regla ausente sino una regla mal ordenada. La guarda `edad < 18` implementada en la iteración 4 capturaba también las edades negativas y las clasificaba como "menor de edad", que es una respuesta plausible pero incorrecta: `-1` no es un menor, es un dato imposible. El fallo `expected: <INVALID_AGE> but was: <UNDERAGE>` fue la señal de que la validación de rango tenía que evaluarse **antes**, no después.
- **Detectado por**: `shouldRejectNegativeAge`, `shouldRejectInvalidAgeOver120` y la propiedad `todaEdadFueraDeRangoEsInvalida`.
- **Estado**: **Resuelto** — iteración 5, guarda de rango insertada entre R3 y R5.

---

### Defecto 04 — El mismo documento se puede inscribir dos veces

- **Caso de prueba**: dos personas distintas que comparten el número de documento.
- **Entradas**:
  - `Person(name="Karla", id=777, age=30, gender=FEMALE, alive=true)`
  - `Person(name="Kevin", id=777, age=25, gender=MALE, alive=true)`
- **Resultado esperado**: 1ª → `VALID`, 2ª → `DUPLICATED` (regla R6)
- **Resultado obtenido**: 1ª → `VALID`, 2ª → `VALID`
- **Causa probable**: `Registry` no tenía memoria. Era un objeto sin estado, así que no podía saber qué documentos ya había inscrito.
- **Detectado por**: `shouldReturnDuplicatedWhenSameIdRegisteredTwice` y la propiedad `registrarDosVecesElMismoIdSiempreDaDuplicated`.
- **Estado**: **Resuelto** — iteración 6, con un `Set<Integer>` **de instancia** (nunca estático, para no filtrar estado entre pruebas).

---

### Defecto 05 — Los intentos rechazados consumen el documento *(defecto simulado)*

> Este defecto **no apareció espontáneamente**: la regla R6 se implementó correctamente a la primera. Lo **inyectamos a propósito** en el código para comprobar que la suite lo detecta. Es un ejercicio de *fault injection*, la versión manual de lo que hace PIT: en vez de preguntarnos si la prueba pasa, preguntamos si la prueba **fallaría** ante un error plausible.

- **Fallo inyectado**: mover el registro del documento desde el final del método hasta justo después de validar su formato, de modo que el `id` se marca como usado antes de saber si el registro va a ser exitoso.

  ```java
  // version defectuosa
  if (p.getId() < MIN_VALID_ID) return RegisterResult.INVALID;
  if (!registeredIds.add(p.getId())) return RegisterResult.DUPLICATED;  // <-- demasiado pronto
  if (!p.isAlive()) return RegisterResult.DEAD;
  ```

- **Caso de prueba**: registrar a un menor con `id = 9` y luego a una persona válida con el mismo `id`.
- **Entradas**:
  - `Person(name="Nora", id=9, age=15, gender=FEMALE, alive=true)` → `UNDERAGE`
  - `Person(name="Nora", id=9, age=20, gender=FEMALE, alive=true)`
- **Resultado esperado**: `VALID` — un intento rechazado no inscribe a nadie, así que el documento sigue libre.
- **Resultado obtenido**: `DUPLICATED`
- **Detectado por**: `shouldNotRegisterIdWhenPersonIsRejected`, con `expected: <VALID> but was: <DUPLICATED>`. Ver [evidencia](../wiki/evidencia/defecto05-inyectado.txt).
- **Hallazgo importante**: de las **30 pruebas de la suite, exactamente una** detectó el fallo. Las otras 29 siguieron en verde, porque todas registran personas que pasan todas las reglas. Es la mejor justificación posible para la existencia de esa prueba: sin ella, este error habría llegado a producción con la suite completa en verde y el 100 % de cobertura.
- **Estado**: **Resuelto** — se revirtió la inyección. En el código final `registeredIds.add(...)` es la última instrucción antes de devolver `VALID`.

---

### Defecto 06 — La clase `Registry` estaba duplicada en el árbol de pruebas

- **Caso de prueba**: no aplica; es un defecto de configuración del proyecto, no del dominio.
- **Síntoma**: existían dos archivos `Registry.java` con el mismo paquete `edu.unisabana.tyvs.domain.service`, uno en `src/main` y otro en `src/test`.
- **Impacto**: la copia del árbol de pruebas **sombreaba** a la de producción en el classpath de pruebas. Las pruebas ejercitaban la copia mientras PIT mutaba la clase real de `target/classes`, de modo que el *mutation score* habría medido código que nadie estaba probando. Un `mvn test` en verde no significaba nada.
- **Causa probable**: un archivo creado por error en la carpeta equivocada al arrancar el taller.
- **Estado**: **Resuelto** — copia eliminada. Se verificó con `git ls-files` que solo queda un `Registry.java`.

---

### Defecto 07 — Una prueba anulaba su propio `@BeforeEach`

- **Caso de prueba**: `shouldRegisterValidPerson`.
- **Síntoma**: la prueba declaraba una variable local `Registry registry = new Registry();` que **sombreaba** el campo inicializado en el `@BeforeEach`.
- **Impacto**: latente en ese momento, grave más adelante. Mientras `Registry` no tuvo estado, la prueba pasaba igual. Al implementar R6, una prueba que se salta el `@BeforeEach` puede quedar acoplada al orden de ejecución de la suite, que es la clase de defecto más difícil de diagnosticar.
- **Estado**: **Resuelto** — se eliminó la variable local; la prueba usa la instancia limpia del `@BeforeEach`.

---

### Defecto 08 — Dos mutantes sobrevivían pese al 100 % de cobertura

- **Caso de prueba**: no aplica; detectado por PIT, no por una prueba que falla.
- **Síntoma**: con `mvn clean verify` la cobertura de línea era del 100 %, pero PIT reportaba **2 mutantes vivos** de 24 (score 91,67 %):
  - `Person.getName()` → reemplazar el retorno por `""`
  - `Person.getGender()` → reemplazar el retorno por `null`
- **Causa**: `registerVoter` nunca consulta el nombre ni el género. Ambos getters se ejecutan al construir el objeto —por eso JaCoCo los contaba como cubiertos— pero su valor no influía en ningún resultado verificado. Es el caso de libro de la diferencia entre **código ejecutado** y **código verificado**.
- **Estado**: **Resuelto** — se agregó `PersonTest`, que afirma que cada getter devuelve lo que recibió el constructor. El *mutation score* pasó de **91,67 % a 100 %**.

---

## Formato 2: Tabla de defectos (bug tracking)

| ID | Caso de prueba | Entrada | Esperado | Obtenido | Causa probable | Prueba que lo detecta | Estado |
|----|----------------|---------|----------|----------|----------------|-----------------------|--------|
| 01 | Documento no positivo | `id=0, age=25, alive=true` | `INVALID` | `VALID` | No se validaba el documento | `shouldRejectWhenIdIsZeroOrNegative` | Resuelto (iter. 3) |
| 02 | Menor de edad | `id=3, age=17, alive=true` | `UNDERAGE` | `VALID` | La edad no participaba en ninguna decisión | `shouldRejectUnderageAt17` | Resuelto (iter. 4) |
| 03 | Edad imposible | `age=-1` / `age=121` | `INVALID_AGE` | `UNDERAGE` / `VALID` | Regla de rango ausente y luego mal ordenada | `shouldRejectNegativeAge`, `shouldRejectInvalidAgeOver120` | Resuelto (iter. 5) |
| 04 | Documento duplicado | `id=777` dos veces | `VALID` + `DUPLICATED` | `VALID` + `VALID` | `Registry` no tenía memoria | `shouldReturnDuplicatedWhenSameIdRegisteredTwice` | Resuelto (iter. 6) |
| 05 | Rechazo que consume documento *(simulado)* | `id=9, age=15` y luego `id=9, age=20` | `UNDERAGE` + `VALID` | `UNDERAGE` + `DUPLICATED` | Inyectado: el `id` se guarda antes de terminar de validar | `shouldNotRegisterIdWhenPersonIsRejected` | Resuelto (revertido) |
| 06 | `Registry` duplicado en `src/test` | — | Una sola clase de producción | Dos clases, la de pruebas sombreando | Archivo creado en la carpeta equivocada | `git ls-files` | Resuelto |
| 07 | `@BeforeEach` anulado | — | Instancia limpia por prueba | Variable local que la sombrea | Sombreado de variable | Revisión + riesgo con R6 | Resuelto |
| 08 | Mutantes vivos con 100 % cobertura | — | Score ≥ 60 % | 91,67 %, 2 vivos | Getters ejecutados pero no verificados | Reporte de PIT | Resuelto (`PersonTest`) |

---

## Convenciones de Estado

| Estado | Significado |
|--------|-------------|
| **Abierto** | El defecto fue detectado pero no corregido. |
| **En progreso** | El defecto se encuentra en análisis o corrección. |
| **Resuelto** | El defecto fue corregido y validado mediante pruebas. |

---

## Observaciones

- **Ningún defecto quedó abierto.** Los ocho tienen una prueba automatizada que los cubre, de modo que una regresión futura vuelve a poner la barra en rojo.
- Origen de cada defecto, que no es el mismo en todos los casos:
  - **01 a 04**: defectos **reales de dominio**, detectados por la barra roja del ciclo TDD.
  - **05**: defecto **simulado** (inyectado a propósito) para verificar que la suite lo detecta.
  - **06 y 07**: defectos **de configuración** del proyecto y de las pruebas. No estaban en el código productivo, pero comprometían la validez de todo lo demás.
  - **08**: no lo encontró ninguna prueba, lo encontró la **herramienta de mutación**, que es justamente su razón de ser.
- El defecto 03 muestra que una regla puede estar *implementada* y aun así ser incorrecta si se evalúa en el orden equivocado. Por eso el orden R1 → R7 está fijado con pruebas explícitas (`shouldPrioritizeDeadOverUnderage`, `shouldPrioritizeInvalidIdOverDead`) y no solo documentado en un comentario.
