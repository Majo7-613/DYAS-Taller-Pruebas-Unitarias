# 06 — Resultados: cobertura y calidad de las pruebas

Dos métricas que responden preguntas distintas:

- **JaCoCo** responde *¿qué código se ejecutó?*
- **PIT** responde *¿si el código cambiara, se enteraría alguna prueba?*

La primera se puede inflar sin escribir una sola aserción. La segunda no.

---

## Resumen

| Métrica | Resultado | Exigido | Verificado automáticamente |
|---|---|---|---|
| Cobertura de línea | **100 %** | ≥ 80 % | ✅ el build falla si baja |
| Cobertura de rama | **100 %** | ≥ 80 % | ✅ el build falla si baja |
| Cobertura de método | **100 %** | — | — |
| *Mutation score* | **100 %** (24/24) | ≥ 60 % | ✅ el build falla si baja |
| Pruebas | 30 en verde | — | — |

Ninguno de estos números es una declaración de intenciones: los tres umbrales están configurados en el [`pom.xml`](https://github.com/Majo7-613/DYAS-Taller-Pruebas-Unitarias/blob/main/registraduria/pom.xml) y **rompen el build** si se incumplen. Se comprueban además en cada push mediante [integración continua](https://github.com/Majo7-613/DYAS-Taller-Pruebas-Unitarias/blob/main/.github/workflows/ci.yml).

---

## 1. Cobertura de código (JaCoCo)

```bash
mvn clean verify     # -> target/site/jacoco/index.html
```

### Por clase

| Clase | Instrucciones | Ramas | Líneas | Métodos |
|---|---|---|---|---|
| `Registry` | 56 / 56 (100 %) | 14 / 14 (100 %) | 16 / 16 (100 %) | 2 / 2 |
| `Person` | 33 / 33 (100 %) | — | 12 / 12 (100 %) | 6 / 6 |
| `RegisterResult` | 39 / 39 (100 %) | — | 7 / 7 (100 %) | 1 / 1 |
| `Gender` | 21 / 21 (100 %) | — | 2 / 2 (100 %) | 1 / 1 |
| **Total** | **149 / 149** | **14 / 14** | **37 / 37** | **10 / 10** |

### Por paquete

| Paquete | Líneas | Ramas |
|---|---|---|
| `edu.unisabana.tyvs.domain.service` | 100 % | 100 % |
| `edu.unisabana.tyvs.domain.model` | 100 % | — |

### Qué quedó sin cubrir

**Nada.** No hay líneas ni ramas sin ejecutar, por dos razones concretas:

1. **El proyecto es dominio puro.** No hay controladores, ni configuración de framework, ni adaptadores de base de datos — que es donde normalmente se acumula el código difícil de cubrir.
2. **Se eliminó el código muerto del arquetipo.** El proyecto de calentamiento con `App` y `AppTest` (`assertTrue(true)`) se borró, tal como pide la sección 8 del README. Sin ese paso, `App.main()` habría quedado como código nunca ejecutado, y habría hecho falta excluirlo del reporte para no arrastrar el porcentaje hacia abajo.

Las 14 ramas de `Registry` son las siete guardas `if` con sus dos salidas cada una. Todas se recorren por ambos lados, que es exactamente lo que garantizan los valores límite de la [matriz de clases de equivalencia](03-Clases-de-Equivalencia#matriz-de-pruebas).

### La puerta de calidad

```xml
<execution>
  <id>check-coverage</id>
  <phase>verify</phase>
  <goals><goal>check</goal></goals>
  <configuration>
    <rules><rule>
      <element>BUNDLE</element>
      <limits>
        <limit><counter>LINE</counter>  <value>COVEREDRATIO</value><minimum>0.80</minimum></limit>
        <limit><counter>BRANCH</counter><value>COVEREDRATIO</value><minimum>0.80</minimum></limit>
      </limits>
    </rule></rules>
  </configuration>
</execution>
```

Salida de `mvn clean verify`:

```
[INFO] --- jacoco:0.8.12:check (check-coverage) @ registraduria ---
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS
```

Añadimos esta regla porque un umbral que solo vive en un documento no es un umbral: es una intención. Configurado así, es una condición del build.

---

## 2. Por qué la cobertura no basta

Antes de celebrar el 100 %, conviene entender qué **no** significa.

Considere esta prueba:

```java
@Test
void pruebaInutil() {
    new Registry().registerVoter(new Person("X", 1, 30, Gender.FEMALE, true));
    // sin ningun assert
}
```

La cobertura **sube**: la línea se ejecutó y JaCoCo la cuenta. Pero la prueba no verifica nada — si `registerVoter` devolviera `DEAD` en vez de `VALID`, seguiría pasando.

Esa es la limitación de fondo: la cobertura mide **qué código se ejecutó**, no **qué comportamiento se verificó**. Y como es la métrica que casi todas las organizaciones exigen, es también la más fácil de inflar sin mejorar nada. En la industria se le llama *coverage theater*.

**Respuesta a la pregunta del README — ¿puede tenerse 100 % de cobertura y 0 % de mutación?** Sí, y construir el ejemplo es trivial: basta con una suite formada únicamente por pruebas como la de arriba. Llamarían a todos los métodos y recorrerían todas las ramas —cobertura del 100 %— y sin una sola aserción, **ningún** mutante moriría. Mutación 0 %. Las dos métricas son ortogonales: la cobertura es condición necesaria para matar un mutante, pero no suficiente.

---

## 3. Pruebas de mutación (PIT)

```bash
mvn test-compile org.pitest:pitest-maven:mutationCoverage    # -> target/pit-reports/index.html
```

PIT introduce cambios pequeños y deliberados en el bytecode —cambiar `>=` por `>`, invertir un `if`, reemplazar un retorno por `0` o `null`— y ejecuta la suite contra cada versión mutada. Si alguna prueba falla, el mutante está **eliminado**; si todas siguen pasando, el mutante **sobrevive**, y eso significa que ese comportamiento no lo está verificando nadie.

### Resultado final

| Clase | Mutantes | Eliminados | Score |
|---|---|---|---|
| `Registry` | 18 | 18 | **100 %** |
| `Person` | 6 | 6 | **100 %** |
| **Total** | **24** | **24** | **100 %** |

### Mutadores aplicados

| Mutador | Cantidad | Qué cambia |
|---|---|---|
| `NullReturnValsMutator` | 8 | Reemplaza el retorno por `null` |
| `NegateConditionalsMutator` | 7 | Invierte una condición (`==` ↔ `!=`, `<` ↔ `>=`) |
| `ConditionalsBoundaryMutator` | 4 | Mueve el borde (`<` → `<=`, `>` → `>=`) |
| `PrimitiveReturnsMutator` | 2 | Reemplaza el retorno numérico por `0` |
| `BooleanTrue/FalseReturnVals` | 2 | Fuerza el retorno booleano |
| `EmptyObjectReturnValsMutator` | 1 | Reemplaza el retorno por `""` |

Los cuatro `ConditionalsBoundaryMutator` son los más informativos: son exactamente los que conviertan `edad < 18` en `edad <= 18`, o `edad > 120` en `edad >= 120`. Mueren gracias a los valores límite `17`/`18` y `120`/`121` de la matriz de equivalencia. **Sin las pruebas de borde, esos cuatro mutantes habrían sobrevivido** aunque la cobertura siguiera en 100 %.

---

## 4. Análisis del mutante sobreviviente

Este es el hallazgo más instructivo del taller.

### El punto de partida

Con las 27 pruebas de `Registry` en verde y **cobertura del 100 %**, PIT reportó:

```
Mutantes generados : 24
Mutantes eliminados: 22
Mutation score     : 91,67 %

MUTANTES SOBREVIVIENTES
-----------------------
[NO_COVERAGE] Person.getGender(), linea 31
   mutador: NullReturnValsMutator
   cambio : replaced return value with null for Person::getGender

[NO_COVERAGE] Person.getName(), linea 19
   mutador: EmptyObjectReturnValsMutator
   cambio : replaced return value with "" for Person::getName
```

[Evidencia completa](https://github.com/Majo7-613/DYAS-Taller-Pruebas-Unitarias/blob/main/wiki/evidencia/pit-mutantes-sobrevivientes.txt)

### Qué comportamiento no estaba verificado

Podíamos romper `Person.getName()` para que devolviera siempre la cadena vacía, y **ninguna de las 27 pruebas se enteraba**.

La causa es directa: `registerVoter` consulta `getId()`, `isAlive()` y `getAge()`, pero **nunca consulta `getName()` ni `getGender()`**. Ninguna regla de negocio depende del nombre ni del género de la persona.

Los dos getters se ejecutaban de todos modos —al construir el objeto en cada `Arrange`— y por eso JaCoCo los contaba como cubiertos. Pero su **valor** no influía en ningún resultado que alguien estuviera verificando.

> Es la diferencia entre cobertura y mutación, en un caso concreto y no en una explicación abstracta: **código ejecutado no es código verificado**. Teníamos el 100 % de cobertura sobre `Person` y aun así dos de sus seis métodos no estaban probados en ningún sentido útil.

Vale la pena notar el estado que reporta PIT: `NO_COVERAGE`, no `SURVIVED`. PIT distingue "hay pruebas que ejecutan esta línea pero no la verifican" de "ninguna prueba que ejercite esta línea tiene forma de detectar el cambio". Estos getters caían en el segundo caso.

### La prueba que los elimina

```java
@Test
@DisplayName("Dado los datos de una persona, "
        + "cuando construyo el objeto, entonces cada getter devuelve el valor recibido")
void shouldExposeEveryValueGivenToTheConstructor() {
    // Arrange: un valor distinto y reconocible por cada atributo
    String nombre = "Ana Maria";
    int id = 1234;
    int edad = 30;
    Gender genero = Gender.FEMALE;

    // Act: construir el objeto bajo prueba
    Person person = new Person(nombre, id, edad, genero, true);

    // Assert: ningun atributo se pierde ni se confunde con otro
    assertEquals(nombre, person.getName());
    assertEquals(id, person.getId());
    assertEquals(edad, person.getAge());
    assertEquals(genero, person.getGender());
    assertTrue(person.isAlive());
}
```

Con el mutante `getName() → ""` activo, la primera aserción falla: `expected: <"Ana Maria"> but was: <"">`. Mutante eliminado.

Añadimos también `shouldKeepUnidentifiedGender`, que verifica que `Gender.UNIDENTIFIED` sobrevive al viaje por el constructor. Es el valor más fácil de confundir con `null` —los dos significan informalmente "no sé"— y la prueba deja explícito que el modelo distingue **"sin identificar"** de **"ausente"**.

Archivo: [`PersonTest.java`](https://github.com/Majo7-613/DYAS-Taller-Pruebas-Unitarias/blob/main/registraduria/src/test/java/edu/unisabana/tyvs/domain/model/PersonTest.java)

### El resultado

| | Antes de `PersonTest` | Después |
|---|---|---|
| Pruebas | 27 | 30 |
| Cobertura de línea | 100 % | 100 % |
| **Mutation score** | **91,67 %** | **100 %** |
| Mutantes vivos | 2 | 0 |

La cobertura **no se movió**, porque ya estaba al máximo. El *mutation score* subió ocho puntos. Es la demostración más limpia posible de que las dos métricas miden cosas distintas.

---

## 5. Comparación de las dos cifras

| Momento del proyecto | Cobertura | Mutación | Diferencia | Lectura |
|---|---|---|---|---|
| Iteraciones 1–2 *(estado inicial)* | ~89 % | ~64 % | 25 pts | Muchas reglas sin implementar; los getters de `Person` sin verificar |
| Tras implementar R2, R4, R5, R6 | 100 % | 91,67 % | 8,33 pts | Las reglas de negocio quedan verificadas; sobreviven los getters no usados |
| Tras añadir `PersonTest` | 100 % | 100 % | 0 pts | Todo comportamiento del dominio está verificado |

**Por qué difieren las dos métricas.** Porque miden eslabones distintos de la misma cadena. Para que una prueba detecte un cambio en una línea hacen falta tres cosas: que la línea **se ejecute**, que su resultado **se propague** hasta un valor observable, y que alguna aserción **compruebe** ese valor. La cobertura solo verifica la primera. Los getters de `Person` cumplían la primera y fallaban las otras dos.

**Cómo cerramos la brecha.** No inflando números, sino en dos movimientos que atacan cada eslabón:

1. **Implementando las reglas que faltaban.** A medida que `registerVoter` empezó a consultar `getAge()` y `getId()`, esos getters pasaron a influir en resultados verificados y sus mutantes murieron solos. Como anticipa el README, *el mutation score mide el avance real del TDD*.
2. **Añadiendo aserciones sobre lo que nadie observaba.** Los dos getters que ninguna regla consulta necesitaban una prueba propia. No había forma de matarlos "de rebote".

**La conclusión que nos llevamos:** un salto grande entre cobertura y mutación es la señal de que hay pruebas que ejecutan código sin verificarlo. La cobertura es un buen detector de código *olvidado*; la mutación es el único de los dos que detecta código *mal probado*. Reportar solo la primera, como hace la mayoría de organizaciones, deja fuera precisamente el riesgo que importa.

---

## 6. Capturas de los reportes

> **Pendiente de adjuntar.** Genere los reportes y capture estas dos páginas:
>
> | Reporte | Archivo a abrir | Guardar la captura como |
> |---|---|---|
> | Cobertura JaCoCo | `registraduria/target/site/jacoco/index.html` | `wiki/img/jacoco.png` |
> | Mutación PIT | `registraduria/target/pit-reports/index.html` | `wiki/img/pit.png` |
>
> Para incrustarlas en el Wiki de GitHub, arrastre los PNG al editor de esta página; GitHub las sube y genera el enlace. Después reemplace este bloque por:
>
> ```markdown
> ![Cobertura JaCoCo](ruta-generada-por-github/jacoco.png)
> ![Mutación PIT](ruta-generada-por-github/pit.png)
> ```
>
> Los datos numéricos de este documento ya están extraídos de `jacoco.csv` y `mutations.xml`, así que las capturas son evidencia visual, no la fuente de las cifras.

---

## 7. Cómo reproducir estos números

```bash
cd registraduria

mvn clean verify
#   Tests run: 30, Failures: 0, Errors: 0, Skipped: 0
#   All coverage checks have been met.
#   BUILD SUCCESS

mvn test-compile org.pitest:pitest-maven:mutationCoverage
#   >> Generated 24 Killed 24 (100%)
#   BUILD SUCCESS
```

Requiere JDK 17. Los reportes quedan en `target/site/jacoco/index.html` y `target/pit-reports/index.html`.
