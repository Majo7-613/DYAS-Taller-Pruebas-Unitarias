# 01 — Historia TDD (Red → Green → Refactor)

Cinco iteraciones, cada una con su barra roja, su implementación mínima y su refactor. Las iteraciones 1 y 2 venían guiadas en el README del taller; de la 3 en adelante son nuestras.

Cada iteración dejó **commits separados para el rojo y el verde**, de modo que el historial de git es en sí mismo la evidencia del ciclo. La salida de consola de cada paso está en [`wiki/evidencia/`](https://github.com/Majo7-613/DYAS-Taller-Pruebas-Unitarias/tree/main/wiki/evidencia).

```
9bc8e4c  chore: elimina Registry duplicado, proyecto de calentamiento y agrega integrantes.txt
b62d143  refactor: corrige la variable local que sombreaba el @BeforeEach ...
232041b  test: rechaza documento no positivo (RED)
bcab777  feat: valida que el documento sea positivo (GREEN)
a6341e1  test: rechaza a los menores de edad, por ejemplo y por propiedad (RED)
cd908ed  feat: valida la mayoria de edad (GREEN)
bcfc1da  test: rechaza edades biologicamente imposibles (RED)
7328135  feat: valida el rango biologico de la edad antes de la mayoria de edad (GREEN)
9388337  test: rechaza documentos ya inscritos (RED)
04a677c  feat: rechaza documentos ya inscritos guardando los ids registrados (GREEN)
e9aa071  test: fija el orden de evaluacion R1-R7 y agrega las propiedades ...
c042dea  refactor: extrae MIN_VALID_ID, MIN_AGE, MAX_AGE y MIN_VOTING_AGE (REFACTOR)
59b078c  test: agrega PersonTest para eliminar los mutantes sobrevivientes de los getters
36916a8  build: agrega puerta de cobertura del 80% en JaCoCo y pipeline de CI
```

---

## Iteración 0 — Limpiar antes de empezar

Antes de la primera prueba nueva encontramos dos problemas heredados que había que resolver, porque invalidaban cualquier medición posterior.

**Existían dos `Registry.java`**, con el mismo paquete `edu.unisabana.tyvs.domain.service`: uno en `src/main` y otro en `src/test`. La copia del árbol de pruebas **sombreaba** a la de producción en el classpath de pruebas. El efecto era silencioso y grave: las pruebas ejercitaban la copia, mientras PIT mutaba la clase real. Un `mvn test` en verde no decía nada sobre el código que de verdad se iba a entregar, y el *mutation score* habría medido una clase que ninguna prueba cargaba.

**Una prueba anulaba su propio `@BeforeEach`**: `shouldRegisterValidPerson` declaraba una variable local `Registry registry = new Registry();` que sombreaba el campo de la clase. Inofensivo mientras `Registry` no tuviera estado, pero una bomba de tiempo para la iteración 6.

Ambos quedaron documentados como defectos 06 y 07 en [Gestión de defectos](07-Gestion-de-Defectos).

> **Lección**: antes de medir, hay que asegurarse de estar midiendo lo correcto. Una suite en verde sobre el archivo equivocado es peor que una suite en rojo, porque genera confianza injustificada.

---

## Iteración 3 — El número de documento (R2)

### 🔴 RED

Escribimos primero la prueba. Usamos `@ParameterizedTest` porque la clase de equivalencia "documento no positivo" tiene tres representantes que merecen el mismo trato: el borde (`0`) y dos negativos (`-1`, `-5`).

```java
@ParameterizedTest(name = "id = {0}")
@ValueSource(ints = {0, -1, -5})
@DisplayName("Dado una persona viva con documento no positivo, "
        + "cuando la registro, entonces el resultado es INVALID")
void shouldRejectWhenIdIsZeroOrNegative(int invalidId) {
    // Arrange
    Person person = new Person("Beatriz", invalidId, 25, Gender.FEMALE, true);
    // Act
    RegisterResult result = registry.registerVoter(person);
    // Assert
    assertEquals(RegisterResult.INVALID, result);
}
```

Y añadimos el borde del otro lado, `id = 1`, que debe seguir siendo válido: sin él, una implementación que rechazara *todos* los documentos también pasaría.

```
[ERROR] shouldRejectWhenIdIsZeroOrNegative(int)[1] expected: <INVALID> but was: <VALID>
[ERROR] shouldRejectWhenIdIsZeroOrNegative(int)[2] expected: <INVALID> but was: <VALID>
[ERROR] shouldRejectWhenIdIsZeroOrNegative(int)[3] expected: <INVALID> but was: <VALID>
Tests run: 10, Failures: 3
```

### 🟢 GREEN

Una guarda, colocada **antes** de la validación de estado de vida porque R2 precede a R3:

```java
if (p.getId() <= 0) {
    return RegisterResult.INVALID;
}
```

`Tests run: 10, Failures: 0` ✅

### 🔵 REFACTOR

Nada que limpiar todavía. Un refactor vacío también es una respuesta válida del ciclo: no se refactoriza por cumplir, se refactoriza cuando hay duplicación o falta de claridad.

---

## Iteración 4 — La mayoría de edad (R5)

### 🔴 RED

Aquí escribimos por primera vez **dos pruebas del mismo hecho a distinto nivel**: un ejemplo concreto y una propiedad sobre todo el rango.

```java
@Test
void shouldRejectUnderageAt17() { ... }   // el representante de la clase

@Property
void todoMenorDeEdadEsRechazado(         // la clase completa, 1000 casos
        @ForAll("nombres") String nombre,
        @ForAll @IntRange(min = 1, max = 100_000) int id,
        @ForAll @IntRange(min = 0, max = 17) int edad,
        @ForAll("generos") Gender genero) { ... }
```

Las dos fallaron:

```
[ERROR] RegistryTest.shouldRejectUnderageAt17           expected: <UNDERAGE> but was: <VALID>
[ERROR] RegistryPropertiesTest.todoMenorDeEdadEsRechazado  expected: <UNDERAGE> but was: <VALID>
```

Pero fallaron de forma distinta, y esa diferencia es el motivo por el que vale la pena tener las dos. jqwik encontró el fallo con una entrada aleatoria y luego la **redujo** al caso mínimo que todavía rompe la regla:

```
Shrunk Sample (4 steps)          Original Sample
-----------------------          ---------------
  arg0: ""                         arg0: "a"
  arg1: 1                          arg1: 29442
  arg2: 0                          arg2: 10
  arg3: MALE                       arg3: FEMALE
```

Ver el análisis completo en [Pruebas de propiedades](05-Pruebas-de-Propiedades#el-contraejemplo-reducido).

### 🟢 GREEN

```java
if (p.getAge() < 18) {
    return RegisterResult.UNDERAGE;
}
```

`Tests run: 13, Failures: 0` ✅

---

## Iteración 5 — El rango biológico de la edad (R4)

**Esta fue la iteración que más nos enseñó**, porque el rojo no reveló una regla ausente sino una regla *mal ubicada*.

### 🔴 RED

Cuatro pruebas que recorren las cuatro fronteras de la edad: `-1`, `0`, `120` y `121`.

```
[ERROR] RegistryTest.shouldRejectNegativeAge          expected: <INVALID_AGE> but was: <UNDERAGE>
[ERROR] RegistryTest.shouldRejectInvalidAgeOver120    expected: <INVALID_AGE> but was: <VALID>
[ERROR] RegistryPropertiesTest.todaEdadFueraDeRangoEsInvalida
                                                     expected: <INVALID_AGE> but was: <UNDERAGE>
Tests run: 18, Failures: 3
```

Fíjese en el primero: **`but was: <UNDERAGE>`**, no `<VALID>`. La guarda `edad < 18` que habíamos escrito en la iteración 4 estaba capturando también las edades negativas y clasificándolas como "menor de edad".

Es una respuesta plausible —técnicamente, `-1 < 18`— y sin embargo incorrecta. Una edad de `-1` no describe a un menor: describe un dato imposible. Confundirlos oculta un error de captura de datos detrás de una respuesta de negocio que parece razonable.

### 🟢 GREEN

La corrección no fue añadir código al final, sino **insertarlo en el punto correcto de la secuencia**, entre R3 y R5:

```java
if (!p.isAlive()) {                                   // R3
    return RegisterResult.DEAD;
}
if (p.getAge() < 0 || p.getAge() > 120) {             // R4  <-- aquí
    return RegisterResult.INVALID_AGE;
}
if (p.getAge() < 18) {                                // R5
    return RegisterResult.UNDERAGE;
}
```

`Tests run: 18, Failures: 0` ✅

Y la prueba `shouldRejectAgeZeroAsUnderage` fija la frontera exacta entre las dos reglas: `0` **sí** es una edad biológicamente posible, así que devuelve `UNDERAGE` y no `INVALID_AGE`. Sin esa prueba, alguien podría "arreglar" R4 usando `edad <= 0` y nadie se enteraría.

> **Lección**: una regla puede estar implementada y aun así ser incorrecta, si se evalúa en el orden equivocado. El orden no es un detalle interno — es comportamiento observable, y por lo tanto hay que probarlo.

---

## Iteración 6 — Los duplicados (R6)

### 🔴 RED

```
[ERROR] RegistryTest.shouldReturnDuplicatedWhenSameIdRegisteredTwice
                                                  expected: <DUPLICATED> but was: <VALID>
[ERROR] RegistryPropertiesTest.registrarDosVecesElMismoIdSiempreDaDuplicated
                                                  expected: <DUPLICATED> but was: <VALID>
Tests run: 22, Failures: 2
```

### 🟢 GREEN

Esta regla es distinta de todas las anteriores: no se puede responder mirando a la persona. Hay que recordar **qué pasó antes**. `Registry` deja de ser una función pura y pasa a ser un objeto con estado:

```java
private final Set<Integer> registeredIds = new HashSet<>();
...
if (registeredIds.contains(p.getId())) {
    return RegisterResult.DUPLICATED;
}
registeredIds.add(p.getId());
return RegisterResult.VALID;
```

Dos decisiones de diseño que parecen menores y no lo son:

**El `Set` es de instancia, nunca estático.** Un `static` haría que el estado se filtrara entre pruebas y el resultado dependiera del orden de ejecución de la suite. La propiedad `elResultadoNoDependeDeLaInstancia` vigila justamente eso.

**El `add` es la última instrucción.** Solo un registro exitoso consume el número de documento; si cualquier regla anterior rechaza a la persona, el documento sigue libre. Lo verificamos inyectando el error a propósito — ver el defecto 05 en [Gestión de defectos](07-Gestion-de-Defectos).

`Tests run: 22, Failures: 0` ✅

### 🔵 REFACTOR — el que ya habíamos hecho

Aquí ocurrió lo más interesante del taller, y no fue un cambio de código: fue darnos cuenta de para qué servía un cambio anterior.

El `@BeforeEach` que introdujimos en la iteración 2 parecía una limpieza estética — evitar repetir `new Registry()` en cada prueba. Al hacer que `Registry` tuviera estado, ese mismo `@BeforeEach` pasó a ser una **condición necesaria para que las pruebas sean independientes**. Sin él, la primera prueba que registrara el `id = 1` contaminaría a todas las siguientes, y los fallos aparecerían y desaparecerían según el orden de ejecución: la clase de defecto más difícil de diagnosticar que existe en una suite.

> **Lección**: el paso *Refactor* no es opcional ni cosmético. Una limpieza hecha por legibilidad terminó siendo la que permitió que la iteración 6 fuera segura.

---

## Iteración 7 — Refactor de calidad

Todo en verde, ninguna regla nueva. Es el momento de mejorar el código sin cambiar su comportamiento.

### Constantes con nombre

Los números mágicos desaparecieron del código de producción **y de las pruebas**:

```java
public static final int MIN_VALID_ID    = 1;
public static final int MIN_AGE         = 0;
public static final int MAX_AGE         = 120;
public static final int MIN_VOTING_AGE  = 18;
```

En las pruebas, el cambio hace que cada valor límite declare **por qué** es un límite:

```java
// antes                                    // después
new Person("Elena", 3, 17, ...)             new Person("Elena", 3, Registry.MIN_VOTING_AGE - 1, ...)
new Person("Jorge", 8, 121, ...)            new Person("Jorge", 8, Registry.MAX_AGE + 1, ...)
```

`MIN_VOTING_AGE - 1` no es solo `17`: dice "el último valor que todavía es menor de edad". Un lector entiende la intención sin ir a buscar la regla.

### Pruebas del orden de evaluación

Añadimos dos pruebas que no cubren ninguna regla nueva, sino la **interacción** entre reglas:

- `shouldPrioritizeDeadOverUnderage` — muerta *y* menor → `DEAD`
- `shouldPrioritizeInvalidIdOverDead` — documento inválido *y* muerta → `INVALID`

Estas pruebas pasaron desde el primer intento, porque el orden ya era el correcto. No son parte de un ciclo rojo-verde: son **pruebas de caracterización**. Su valor no es descubrir un error hoy, sino impedir que mañana alguien reordene las guardas creyendo que da igual.

`Tests run: 27, Failures: 0` ✅

---

## Epílogo — La iteración que no vino de una prueba

Con todo en verde y **100 % de cobertura**, ejecutamos PIT. Sobrevivieron dos mutantes:

```
Person.getName()   -> reemplazar el retorno por ""     SOBREVIVE
Person.getGender() -> reemplazar el retorno por null   SOBREVIVE
```

`registerVoter` nunca consulta el nombre ni el género. Los getters se ejecutaban al construir el objeto —por eso JaCoCo los daba por cubiertos— pero su valor no influía en ningún resultado verificado. Podíamos romperlos y **ninguna de las 27 pruebas se enteraba**.

La respuesta fue [`PersonTest`](https://github.com/Majo7-613/DYAS-Taller-Pruebas-Unitarias/blob/main/registraduria/src/test/java/edu/unisabana/tyvs/domain/model/PersonTest.java), que afirma el contrato del modelo: cada getter devuelve lo que recibió el constructor. El *mutation score* pasó de **91,67 % a 100 %**.

> **Lección final**: la cobertura mide qué código se **ejecutó**; la mutación mide qué comportamiento se **verificó**. Teníamos el 100 % de la primera y aun así había código que nadie estaba probando. Ver el análisis completo en [Resultados](06-Resultados).

---

## Resumen del ciclo

| Iteración | Regla | Rojo | Verde | Refactor |
|---|---|---|---|---|
| 3 | R2 documento | 3 fallos | guarda `id <= 0` | — |
| 4 | R5 mayoría de edad | 2 fallos (ejemplo + propiedad) | guarda `edad < 18` | — |
| 5 | R4 rango biológico | 3 fallos, uno revelando mal orden | guarda insertada entre R3 y R5 | — |
| 6 | R6 duplicados | 2 fallos | `Set` de instancia | el `@BeforeEach` pasa a ser necesario |
| 7 | calidad | — (caracterización) | — | constantes con nombre |
