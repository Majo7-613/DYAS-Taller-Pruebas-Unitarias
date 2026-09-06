# 04 — Escenarios BDD (Given – When – Then)

BDD expresa las pruebas en el lenguaje del negocio, no en el de la implementación. En este proyecto no mantenemos los escenarios en un archivo aparte: **viven dentro de las pruebas**, en el `@DisplayName` de cada método.

La razón es de mantenimiento. Un archivo `.feature` separado del código se desactualiza en cuanto alguien cambia una regla y olvida abrirlo. Un `@DisplayName` se lee en el mismo lugar donde se edita la prueba, y aparece en el reporte de ejecución — si la regla cambia, la redacción se corrige en el acto.

---

## Cómo se lee el reporte de pruebas

Al ejecutar la suite, esto es lo que produce — la especificación funcional del sistema, generada por el propio código:

```
RegistryTest
  ✔ Dado una persona viva de 30 anios con id valido, cuando la registro, entonces el resultado es VALID
  ✔ Dado una persona no viva, cuando la registro, entonces el resultado es DEAD
  ✔ Dado una persona nula, cuando la registro, entonces el resultado es INVALID
  ✔ Dado una persona viva con documento no positivo, cuando la registro, entonces el resultado es INVALID
      ✔ id = 0
      ✔ id = -1
      ✔ id = -5
  ✔ Dado una persona viva con el documento positivo mas pequenio (id = 1), cuando la registro, entonces el resultado es VALID
  ✔ Dado una persona viva de 17 anios con id valido, cuando la registro, entonces el resultado es UNDERAGE
  ✔ Dado una persona viva de 18 anios con id valido, cuando la registro, entonces el resultado es VALID
  ✔ Dado una persona viva con edad negativa (-1), cuando la registro, entonces el resultado es INVALID_AGE
  ✔ Dado una persona viva de 0 anios, cuando la registro, entonces el resultado es UNDERAGE y no INVALID_AGE
  ✔ Dado una persona viva de 120 anios con id valido, cuando la registro, entonces el resultado es VALID
  ✔ Dado una persona viva de 121 anios, cuando la registro, entonces el resultado es INVALID_AGE
  ✔ Dado un documento ya inscrito, cuando registro otra persona con el mismo documento, entonces el resultado es DUPLICATED
  ✔ Dado dos personas con documentos distintos, cuando las registro, entonces ambas quedan registradas
  ✔ Dado un intento de registro rechazado, cuando vuelvo a usar ese documento con una persona valida, entonces el resultado es VALID y no DUPLICATED
  ✔ Dado una persona no viva y menor de edad, cuando la registro, entonces el resultado es DEAD y no UNDERAGE
  ✔ Dado una persona no viva y con documento invalido, cuando la registro, entonces el resultado es INVALID y no DEAD
```

Un analista funcional puede revisar esa lista sin abrir un solo archivo `.java`.

---

## Escenarios por regla de negocio

### R1 — La persona no puede ser nula

```gherkin
Escenario: Rechazar una referencia nula
  Dado    que no se recibió ninguna persona
  Cuando  intento registrarla
  Entonces el resultado debe ser INVALID
```
→ `shouldReturnInvalidWhenPersonIsNull`

---

### R2 — El documento debe ser positivo

```gherkin
Esquema del escenario: Rechazar documentos no positivos
  Dado    que existe una persona viva de 25 años con documento <id>
  Cuando  intento registrarla
  Entonces el resultado debe ser INVALID

  Ejemplos:
    | id |
    |  0 |
    | -1 |
    | -5 |

Escenario: Aceptar el documento positivo más pequeño
  Dado    que existe una persona viva de 25 años con documento 1
  Cuando  intento registrarla
  Entonces el resultado debe ser VALID
```
→ `shouldRejectWhenIdIsZeroOrNegative`, `shouldAcceptMinimumValidId`

---

### R3 — La persona debe estar viva

```gherkin
Escenario: Rechazar a una persona fallecida
  Dado    que existe una persona de 40 años que no está viva
  Cuando  intento registrarla
  Entonces el resultado debe ser DEAD
```
→ `shouldRejectDeadPerson`

---

### R4 — La edad debe ser biológicamente posible

```gherkin
Escenario: Rechazar una edad negativa
  Dado    que existe una persona viva con edad -1
  Cuando  intento registrarla
  Entonces el resultado debe ser INVALID_AGE
  Y       no debe ser UNDERAGE, porque -1 no describe a un menor sino a un dato imposible

Escenario: Rechazar una edad superior al máximo biológico
  Dado    que existe una persona viva de 121 años
  Cuando  intento registrarla
  Entonces el resultado debe ser INVALID_AGE

Escenario: Aceptar el máximo biológico
  Dado    que existe una persona viva de 120 años con documento válido
  Cuando  intento registrarla
  Entonces el resultado debe ser VALID
```
→ `shouldRejectNegativeAge`, `shouldRejectInvalidAgeOver120`, `shouldAcceptMaxAge120`

---

### R5 — La persona debe ser mayor de edad

```gherkin
Escenario: Rechazar a un menor de edad
  Dado    que existe una persona viva de 17 años con documento válido
  Cuando  intento registrarla
  Entonces el resultado debe ser UNDERAGE

Escenario: Aceptar a quien acaba de alcanzar la mayoría de edad
  Dado    que existe una persona viva de 18 años con documento válido
  Cuando  intento registrarla
  Entonces el resultado debe ser VALID

Escenario: Un recién nacido es menor de edad, no un dato inválido
  Dado    que existe una persona viva de 0 años
  Cuando  intento registrarla
  Entonces el resultado debe ser UNDERAGE
  Y       no debe ser INVALID_AGE, porque 0 es una edad biológicamente posible
```
→ `shouldRejectUnderageAt17`, `shouldAcceptAdultAt18`, `shouldRejectAgeZeroAsUnderage`

---

### R6 — Una sola inscripción por documento

```gherkin
Escenario: Rechazar una segunda inscripción con el mismo documento
  Dado    que ya inscribí a una persona con documento 777
  Cuando  intento registrar a otra persona con el documento 777
  Entonces el resultado debe ser DUPLICATED

Escenario: Documentos distintos no se estorban
  Dado    que existen dos personas válidas con documentos 10 y 11
  Cuando  intento registrarlas
  Entonces ambas deben quedar registradas

Escenario: Un intento rechazado no consume el documento
  Dado    que intenté registrar a un menor de edad con documento 9 y fue rechazado
  Cuando  intento registrar a una persona válida con el mismo documento 9
  Entonces el resultado debe ser VALID
  Y       no debe ser DUPLICATED, porque solo una inscripción exitosa ocupa un documento
```
→ `shouldReturnDuplicatedWhenSameIdRegisteredTwice`, `shouldAllowDifferentIds`, `shouldNotRegisterIdWhenPersonIsRejected`

---

### R7 — El camino feliz

```gherkin
Escenario: Registrar a una votante válida
  Dado    que existe una persona viva de 30 años con documento válido y no inscrito
  Cuando  intento registrarla
  Entonces el resultado debe ser VALID
```
→ `shouldRegisterValidPerson`

---

## Escenarios de prioridad entre reglas

Estos no corresponden a una regla, sino a **qué pasa cuando se incumplen dos a la vez**. Son los que hacen explícito el orden de evaluación R1 → R7.

```gherkin
Escenario: La condición de fallecimiento manda sobre la de minoría de edad
  Dado    que existe una persona de 15 años que no está viva
  Cuando  intento registrarla
  Entonces el resultado debe ser DEAD
  Y       no debe ser UNDERAGE, porque R3 se evalúa antes que R5

Escenario: El documento inválido manda sobre la condición de fallecimiento
  Dado    que existe una persona de 40 años que no está viva y cuyo documento es 0
  Cuando  intento registrarla
  Entonces el resultado debe ser INVALID
  Y       no debe ser DEAD, porque R2 se evalúa antes que R3
```
→ `shouldPrioritizeDeadOverUnderage`, `shouldPrioritizeInvalidIdOverDead`

---

## Escenarios generalizados

Las pruebas basadas en propiedades permiten redactar escenarios cuantificados sobre **todo** un rango, no sobre un ejemplo. Es BDD llevado un paso más allá: en vez de "dado que existe una persona de 17 años", se afirma "dada **cualquier** persona de entre 0 y 17 años".

```gherkin
Escenario: Ninguna persona no viva se registra, sean cuales sean sus demás datos
  Dado    cualquier documento válido, cualquier edad posible y cualquier género
  Y       que la persona no está viva
  Cuando  intento registrarla
  Entonces el resultado siempre debe ser DEAD

Escenario: Ningún menor de edad se registra
  Dado    cualquier persona viva con documento válido y edad entre 0 y 17
  Cuando  intento registrarla
  Entonces el resultado siempre debe ser UNDERAGE

Escenario: Todo adulto válido se registra
  Dado    cualquier persona viva con documento válido y edad entre 18 y 120
  Y       que el registro está vacío
  Cuando  intento registrarla
  Entonces el resultado siempre debe ser VALID
```

Cada uno se ejecuta con **1000 combinaciones generadas**. Ver [Pruebas de propiedades](05-Pruebas-de-Propiedades).

---

## Trazabilidad regla → escenario → prueba

| Regla | Escenarios | Pruebas por ejemplo | Propiedades |
|---|---|---|---|
| R1 nulidad | 1 | 1 | `nuncaDevuelveNullNiLanzaExcepcion` |
| R2 documento | 2 | 4 | `todoIdNoPositivoEsInvalido` |
| R3 estado de vida | 1 | 1 | `unaPersonaNoVivaSiempreEsRechazada` |
| R4 rango de edad | 3 | 3 | `todaEdadFueraDeRangoEsInvalida` |
| R5 mayoría de edad | 3 | 3 | `todoMenorDeEdadEsRechazado` |
| R6 duplicados | 3 | 3 | `registrarDosVecesElMismoIdSiempreDaDuplicated` |
| R7 camino feliz | 1 | 1 | `todoAdultoValidoSeRegistra` |
| Orden R1→R7 | 2 | 2 | `todoIdNoPositivoEsInvalido` |

**Las siete reglas tienen escenario, prueba por ejemplo y propiedad.** Ninguna quedó documentada sin verificar, ni verificada sin documentar.
