# 05 — Pruebas basadas en propiedades (jqwik)

Las clases de equivalencia parten de una idea potente: si todos los valores de un grupo se tratan igual, basta probar un representante. Pero esa idea esconde un supuesto — **que acertamos al definir el grupo**.

Las pruebas basadas en propiedades eliminan ese supuesto. En vez de elegir el representante a mano, se declara la regla sobre **todo el rango** y la herramienta genera cientos de casos intentando refutarla.

| Prueba por ejemplo | Propiedad |
|---|---|
| «Carlos, 40 años, no vivo → `DEAD`» | «Para **toda** edad, **todo** género y **todo** documento, si no está viva → `DEAD`» |
| Verifica **un punto** del espacio de entradas | Verifica **el espacio completo**, por muestreo |
| Falla mostrando el caso que usted escribió | Falla mostrando el caso **más simple** que la rompe |

Archivo: [`RegistryPropertiesTest.java`](https://github.com/Majo7-613/DYAS-Taller-Pruebas-Unitarias/blob/main/registraduria/src/test/java/edu/unisabana/tyvs/domain/service/RegistryPropertiesTest.java)

---

## Las 9 propiedades

Tres venían de referencia en el repositorio; **seis las escribimos nosotros**. Cada una se ejecuta con **1000 casos generados**.

### De regla de negocio

| # | Propiedad | Regla | Rango generado |
|---|---|---|---|
| 1 | `unaPersonaNoVivaSiempreEsRechazada` *(referencia)* | R3 | `id ∈ [1, 100000]`, `edad ∈ [0, 120]`, todo género, `alive=false` → `DEAD` |
| 2 | `todoIdNoPositivoEsInvalido` | R2 | `id ∈ [-100000, 0]`, cualquier edad, **cualquier estado de vida** → `INVALID` |
| 3 | `todoMenorDeEdadEsRechazado` | R5 | `edad ∈ [0, 17]`, viva, `id` válido → `UNDERAGE` |
| 4 | `todaEdadFueraDeRangoEsInvalida` | R4 | `edad ∈ [-10000, -1] ∪ [121, 10000]` → `INVALID_AGE` |
| 5 | `todoAdultoValidoSeRegistra` | R7 | `edad ∈ [18, 120]`, viva, `id` válido, registro limpio → `VALID` |
| 6 | `registrarDosVecesElMismoIdSiempreDaDuplicated` | R6 | mismo `Registry`, misma persona dos veces → `VALID` y luego `DUPLICATED` |

### Estructurales

| # | Propiedad | Tipo | Qué garantiza |
|---|---|---|---|
| 7 | `elResultadoNoDependeDeLaInstancia` *(referencia)* | Determinismo | El mismo insumo produce el mismo resultado en dos `Registry` recién creados |
| 8 | `nuncaDevuelveNullNiLanzaExcepcion` *(referencia)* | Totalidad | Ninguna entrada produce `null` ni una excepción |
| 9 | `elResultadoSiempreEsUnValorDelEnum` | Invariante de partición | El resultado siempre es uno de los seis valores de `RegisterResult` |

---

## Las tres que más aportan

### `todoIdNoPositivoEsInvalido` — una propiedad que fija el orden de las reglas

```java
@Property
void todoIdNoPositivoEsInvalido(
        @ForAll("nombres") String nombre,
        @ForAll @IntRange(min = -100_000, max = 0) int id,
        @ForAll @IntRange(min = 0, max = 120) int edad,
        @ForAll("generos") Gender genero,
        @ForAll boolean viva) {                      // <-- incluye alive = false

    Person p = new Person(nombre, id, edad, genero, viva);

    assertEquals(RegisterResult.INVALID, new Registry().registerVoter(p));
}
```

El detalle que la hace valiosa está en el último parámetro: **`viva` también se genera**. La propiedad no dice solo "un documento inválido se rechaza", dice "un documento inválido se rechaza **incluso si la persona está muerta**". Es decir, afirma sobre todo el rango que **R2 domina a R3**.

Una prueba por ejemplo puede afirmar lo mismo (`shouldPrioritizeInvalidIdOverDead`), pero solo para un caso. Esta lo afirma para 1000.

### `elResultadoSiempreEsUnValorDelEnum` — el contrato mínimo

```java
@Property
void elResultadoSiempreEsUnValorDelEnum(
        @ForAll("nombres") String nombre,
        @ForAll int id,          // <-- rango COMPLETO de int
        @ForAll int edad,        // <-- incluido Integer.MIN_VALUE y MAX_VALUE
        @ForAll("generos") Gender genero,
        @ForAll boolean viva) { ... }
```

Sin `@IntRange`, jqwik genera el rango entero de `int` e incluye deliberadamente los casos extremos (`Integer.MIN_VALUE`, `Integer.MAX_VALUE`, `0`, `-1`, `1`). Es donde aparecen los desbordamientos aritméticos y los comportamientos no contemplados.

Nuestra implementación los soporta porque compara en vez de calcular. Pero eso es una conclusión **verificada**, no una suposición: si alguien reescribiera R4 como `Math.abs(edad) > 120`, esta propiedad lo detectaría con `Integer.MIN_VALUE`, cuyo valor absoluto es negativo.

### `registrarDosVecesElMismoIdSiempreDaDuplicated` — la única que reutiliza la instancia

```java
Person p = new Person(nombre, id, edad, genero, true);
Registry registro = new Registry();

assertEquals(RegisterResult.VALID, registro.registerVoter(p));
assertEquals(RegisterResult.DUPLICATED, registro.registerVoter(p));
```

Todas las demás propiedades crean un `Registry` nuevo por invocación, precisamente para no depender del estado. Esta hace lo contrario a propósito, porque la regla que verifica **es** la que depende del estado acumulado.

Además comprueba las dos mitades del contrato en una sola afirmación: que la primera inscripción funciona y que la segunda no. Si `registerVoter` devolviera siempre `DUPLICATED`, la primera aserción lo detectaría.

---

## El contraejemplo reducido

Esta es la característica que justifica por sí sola usar property-based testing.

Al escribir `todoMenorDeEdadEsRechazado` durante la [iteración 4](01-Historia-TDD#iteración-4--la-mayoría-de-edad-r5), la propiedad falló. jqwik **no reportó la entrada aleatoria que la rompió**: la redujo al caso mínimo que sigue rompiéndola.

```
timestamp = 2026-09-06, RegistryPropertiesTest:todoMenorDeEdadEsRechazado =
  org.opentest4j.AssertionFailedError:
    expected: <UNDERAGE> but was: <VALID>

                              |-----------------------jqwik-----------------------
tries = 1                     | # of calls to property
checks = 1                    | # of not rejected calls
generation = RANDOMIZED       | parameters are randomly generated
seed = 2234389622065644295    | random seed to reproduce generated values

Shrunk Sample (4 steps)          Original Sample
-----------------------          ---------------
  arg0: ""                         arg0: "a"
  arg1: 1                          arg1: 29442
  arg2: 0                          arg2: 10
  arg3: MALE                       arg3: FEMALE
```

[Evidencia completa](https://github.com/Majo7-613/DYAS-Taller-Pruebas-Unitarias/blob/main/wiki/evidencia/shrinking-todoMenorDeEdadEsRechazado.txt)

### Por qué la versión reducida es mejor

| | Muestra original | Muestra reducida |
|---|---|---|
| nombre | `"a"` | `""` |
| id | `29442` | `1` |
| edad | `10` | `0` |
| género | `FEMALE` | `MALE` |

Ante la muestra original uno se pregunta, inevitablemente: *¿el fallo tiene que ver con que el id sea de cinco cifras? ¿con que el nombre no esté vacío? ¿con el género?* Son cuatro variables moviéndose a la vez, y no hay forma de saber cuál importa sin ponerse a experimentar.

La muestra reducida responde esas preguntas antes de que se formulen. jqwik llevó **cada** parámetro a su valor más simple y comprobó que el fallo persistía: el nombre vacío, el id mínimo posible y el género inicial del enum. Que el fallo sobreviva a esa reducción demuestra que **ninguno de esos tres atributos participa en el defecto**.

Queda una sola variable en pie: `edad = 0`. Y con eso el diagnóstico es inmediato — la regla de edad no está implementada. Cuatro pasos de reducción convirtieron "algo falla con estos cuatro datos" en "la edad no se está validando".

> Hay un beneficio adicional: el `seed` que aparece en el reporte permite **reproducir exactamente** la ejecución fallida. Un fallo intermitente deja de ser intermitente.

---

## Cuándo NO usar property-based

Las propiedades **no reemplazan** a las pruebas por ejemplo, las complementan.

| Pruebas por ejemplo | Propiedades |
|---|---|
| Documentan el comportamiento esperado | Exploran el espacio de entradas |
| Se leen como especificación | Se leen como contrato matemático |
| Explican **por qué** ese caso importa | Verifican **que la regla no tiene huecos** |
| `shouldRejectUnderageAt17` dice que 17 es la frontera | `todoMenorDeEdadEsRechazado` dice que ningún valor de 0 a 17 se escapa |

Nuestro criterio fue: **todo caso de negocio importante merece las dos**. Una propiedad que cubre el rango y un ejemplo con nombre que explica por qué ese rango importa. Por eso las siete reglas tienen propiedad *y* ejemplo — ver la tabla de trazabilidad en [Escenarios BDD](04-BDD-Escenarios#trazabilidad-regla--escenario--prueba).

Donde las propiedades no aportan es en los casos singulares. `shouldReturnInvalidWhenPersonIsNull` no tiene rango que recorrer: `null` es un único valor. Generar 1000 veces `null` no añade nada.

---

## Configuración

```xml
<dependency>
  <groupId>net.jqwik</groupId>
  <artifactId>jqwik</artifactId>
  <version>1.8.0</version>
  <scope>test</scope>
</dependency>
```

jqwik se ejecuta sobre la plataforma de JUnit 5, así que Surefire lo descubre sin configuración adicional: `mvn test` corre las pruebas por ejemplo y las propiedades en la misma pasada.

Los generadores personalizados se declaran con `@Provide`:

```java
@Provide
Arbitrary<Gender> generos() {
    return Arbitraries.of(Gender.values());
}

@Provide
Arbitrary<String> nombres() {
    return Arbitraries.strings().alpha().ofMaxLength(20);   // incluye la cadena vacía
}

@Provide
Arbitrary<Integer> edadesImposibles() {                     // las dos clases inválidas a la vez
    return Arbitraries.oneOf(
            Arbitraries.integers().between(-10_000, -1),
            Arbitraries.integers().between(121, 10_000));
}
```

`edadesImposibles` merece atención: combina las dos clases de equivalencia inválidas de la edad en un solo generador. Una tabla de ejemplos necesitaría dos filas separadas; la propiedad las cubre con una sola declaración, y jqwik reparte los 1000 casos entre ambos rangos.
