# 03 — Clases de equivalencia y valores límite

La idea es simple: si el sistema trata igual a todos los valores de un grupo, basta con probar un representante. La consecuencia práctica es que se pueden cubrir infinitas entradas con una docena de pruebas — **siempre que los grupos estén bien definidos**.

Y como los errores se concentran en las fronteras entre grupos, cada partición se prueba en su borde, no en su centro.

---

## Particiones del dominio de entrada

`registerVoter(Person)` recibe un objeto con cinco atributos, pero solo tres participan en las decisiones: `id`, `alive` y `age`. `name` y `gender` no afectan el resultado — un hecho que resultó tener consecuencias, ver [Resultados](06-Resultados).

### Nulidad del parámetro

| Clase | Representante | Resultado |
|---|---|---|
| Persona nula | `null` | `INVALID` |
| Persona no nula | cualquier `Person` | continúa la evaluación |

### Número de documento (`id`)

| Clase | Rango | Valores límite | Resultado |
|---|---|---|---|
| Inválida | `id ≤ 0` | **`0`** (borde), `-1`, `-5` | `INVALID` |
| Válida y libre | `id ≥ 1`, no inscrito | **`1`** (borde inferior) | continúa la evaluación |
| Válida y ocupada | `id ≥ 1`, ya inscrito | — | `DUPLICATED` |

### Estado de vida (`alive`)

| Clase | Valor | Resultado |
|---|---|---|
| No viva | `false` | `DEAD` |
| Viva | `true` | continúa la evaluación |

Atributo booleano: dos clases, sin bordes intermedios. Ambas se prueban.

### Edad (`age`)

Es el atributo con más particiones, y el único con **cuatro fronteras**:

```
        INVALID_AGE  │      UNDERAGE      │          VALID          │  INVALID_AGE
    ─────────────────┼────────────────────┼─────────────────────────┼──────────────
                  -1 │ 0              17  │ 18                 120  │ 121
                     ↑                    ↑                         ↑
                 frontera             frontera                  frontera
```

| Clase | Rango | Valores límite probados | Resultado |
|---|---|---|---|
| Edad imposible por defecto | `edad < 0` | **`-1`** | `INVALID_AGE` |
| Menor de edad | `0 ≤ edad ≤ 17` | **`0`**, **`17`** | `UNDERAGE` |
| Adulto válido | `18 ≤ edad ≤ 120` | **`18`**, **`120`** | `VALID` |
| Edad imposible por exceso | `edad > 120` | **`121`** | `INVALID_AGE` |

Las cuatro fronteras se prueban **por ambos lados**: `-1`/`0`, `17`/`18` y `120`/`121`. Probar un solo lado deja pasar el error clásico de confundir `<` con `<=`.

---

## Matriz de pruebas

Cada fila enlaza una clase de equivalencia con la entrada que la representa y **el método de prueba que la cubre**.

| # | Clase de equivalencia | Entrada representativa | Esperado | Prueba que lo cubre |
|---|---|---|---|---|
| 1 | Persona nula | `null` | `INVALID` | `shouldReturnInvalidWhenPersonIsNull` |
| 2 | Documento `= 0` *(borde)* | `id=0, age=25, alive=true` | `INVALID` | `shouldRejectWhenIdIsZeroOrNegative[1]` |
| 3 | Documento negativo | `id=-1` / `id=-5` | `INVALID` | `shouldRejectWhenIdIsZeroOrNegative[2][3]` |
| 4 | Documento `= 1` *(borde)* | `id=1, age=25, alive=true` | `VALID` | `shouldAcceptMinimumValidId` |
| 5 | Persona no viva | `age=40, alive=false` | `DEAD` | `shouldRejectDeadPerson` |
| 6 | Edad `= -1` *(borde)* | `age=-1, alive=true` | `INVALID_AGE` | `shouldRejectNegativeAge` |
| 7 | Edad `= 0` *(borde)* | `age=0, alive=true` | `UNDERAGE` | `shouldRejectAgeZeroAsUnderage` |
| 8 | Edad `= 17` *(borde)* | `age=17, alive=true` | `UNDERAGE` | `shouldRejectUnderageAt17` |
| 9 | Edad `= 18` *(borde)* | `age=18, alive=true` | `VALID` | `shouldAcceptAdultAt18` |
| 10 | Edad en el interior de la clase válida | `age=30, alive=true` | `VALID` | `shouldRegisterValidPerson` |
| 11 | Edad `= 120` *(borde)* | `age=120, alive=true` | `VALID` | `shouldAcceptMaxAge120` |
| 12 | Edad `= 121` *(borde)* | `age=121, alive=true` | `INVALID_AGE` | `shouldRejectInvalidAgeOver120` |
| 13 | Documento ya inscrito | `id=777` dos veces | `DUPLICATED` | `shouldReturnDuplicatedWhenSameIdRegisteredTwice` |
| 14 | Documentos distintos | `id=10` y `id=11` | `VALID` + `VALID` | `shouldAllowDifferentIds` |
| 15 | Documento liberado tras un rechazo | `id=9, age=15` y luego `id=9, age=20` | `UNDERAGE` + `VALID` | `shouldNotRegisterIdWhenPersonIsRejected` |
| 16 | Conflicto R3 vs R5 | `age=15, alive=false` | `DEAD` | `shouldPrioritizeDeadOverUnderage` |
| 17 | Conflicto R2 vs R3 | `id=0, alive=false` | `INVALID` | `shouldPrioritizeInvalidIdOverDead` |

**17 clases cubiertas** — la entrega pedía al menos 5.

Todas viven en [`RegistryTest.java`](https://github.com/Majo7-613/DYAS-Taller-Pruebas-Unitarias/blob/main/registraduria/src/test/java/edu/unisabana/tyvs/domain/service/RegistryTest.java).

---

## Las tres decisiones que no eran obvias

### Por qué `0` es `UNDERAGE` y no `INVALID_AGE` (fila 7)

`0` es la frontera entre dos clases inválidas por motivos distintos, y es fácil equivocarse. Un recién nacido tiene una edad **biológicamente posible**: no es un dato corrupto, es una persona que todavía no puede votar. Por eso `0` cae en la clase "menor de edad".

Sin esta prueba, alguien podría implementar R4 como `edad <= 0` y el resultado seguiría pareciendo correcto en todos los demás casos.

### Por qué las filas 16 y 17 existen

Son las únicas entradas que pertenecen a **dos clases inválidas a la vez**. Una persona muerta de 15 años incumple R3 y R5; el sistema debe responder `DEAD`, porque R3 se evalúa primero.

Estas dos filas no cubren una regla nueva: cubren la **interacción** entre reglas, que es donde el orden de evaluación deja de ser un detalle interno y se vuelve comportamiento observable.

### Por qué la fila 15 no es redundante con la 13

La 13 pregunta "¿se rechaza un documento repetido?". La 15 pregunta algo más fino: "¿qué cuenta como *ocupar* un documento?". La respuesta correcta es que solo un registro **exitoso** lo consume — un intento rechazado deja el documento libre.

Es una distinción que ninguna otra prueba de la suite verifica. Lo comprobamos inyectando el error a propósito: de las 30 pruebas, **solo esta falló**. Ver el defecto 05 en [Gestión de defectos](07-Gestion-de-Defectos).

---

## El límite de esta técnica

Las clases de equivalencia descansan en un supuesto: **que acertamos al definir los grupos**. Si el código tratara distinto al `17` y al `0` aunque los dos sean "menores de edad", el representante que elegimos a mano jamás lo revelaría.

Por eso cada partición importante tiene además una **propiedad** que recorre el rango completo con 1000 casos generados. Las dos técnicas se complementan: la tabla de arriba documenta y explica, las propiedades exploran. Ver [Pruebas de propiedades](05-Pruebas-de-Propiedades).
