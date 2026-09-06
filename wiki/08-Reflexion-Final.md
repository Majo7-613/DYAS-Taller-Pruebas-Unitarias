# 08 — Reflexión final

---

## 1. ¿Qué escenarios no se cubrieron y por qué?

Con 100 % de cobertura y 100 % de *mutation score*, la pregunta interesante no es qué línea quedó sin ejecutar —ninguna— sino **qué preguntas no se le hicieron al sistema**.

### Concurrencia

`Registry` no es *thread-safe*. Dos hilos que registren el mismo documento a la vez pueden pasar los dos por `registeredIds.contains(id)` antes de que ninguno haya hecho el `add`, y ambos obtendrían `VALID`. Es una condición de carrera real.

No la probamos, y creemos que fue correcto: el enunciado describe un servicio de dominio de un solo hilo, y probar concurrencia exige otras herramientas (`CountDownLatch`, ejecuciones repetidas, detectores de carreras). Sería resolver un problema que nadie planteó.

Lo que sí hicimos fue **dejar constancia**: está registrado aquí y en el punto 3 de esta página, con la solución que aplicaríamos si el requisito apareciera.

### Persistencia y volumen

El `Set` vive en memoria y desaparece al terminar el proceso. Un registro electoral real necesitaría una base de datos, y con ella llegarían escenarios que hoy no existen: qué pasa si la escritura falla a medias, qué pasa con millones de documentos. Están fuera del alcance de un taller de pruebas unitarias de dominio — y de hecho ya no serían pruebas unitarias.

### Validaciones sobre el nombre

`registerVoter` acepta un nombre vacío, con espacios o con caracteres extraños. **No es un hueco de pruebas, es una ausencia de requisito**: las siete reglas del enunciado no mencionan el nombre. Añadir una validación no pedida habría sido inventar negocio.

Este es justamente el punto donde la mutación nos enseñó algo (ver el defecto 08): al no participar el nombre en ninguna regla, `Person.getName()` no estaba verificado por nadie. La solución no fue inventar una regla, sino probar el contrato del modelo en `PersonTest`.

### `Integer.MIN_VALUE` y los desbordamientos

Estos **sí** están cubiertos, aunque no con una prueba por ejemplo. La propiedad `elResultadoSiempreEsUnValorDelEnum` genera `id` y `edad` sobre el rango completo de `int`, e incluye deliberadamente los extremos. Es un caso donde la propiedad hizo un trabajo que habría sido tedioso enumerar a mano.

---

## 2. ¿Qué defectos reales detectaron los tests?

Los ocho están en [Gestión de defectos](07-Gestion-de-Defectos). Tres merecen destacarse por lo que enseñaron, más allá de haberse corregido.

### El que cambió cómo entendemos "implementar una regla"

El **defecto 03** no fue una regla ausente: fue una regla *presente pero mal ubicada*. Con R5 implementada y R4 todavía no, una edad de `-1` devolvía `UNDERAGE` — coherente con el código y equivocado para el negocio.

Lo aprendido: **el orden de evaluación es comportamiento observable, no un detalle interno**. Por eso terminó fijado con dos pruebas de caracterización y una propiedad, en vez de con un comentario.

### El que solo una prueba de treinta pudo ver

El **defecto 05**, que inyectamos a propósito, falló en exactamente **una** de las 30 pruebas. Las otras 29 registran personas que superan todas las reglas, así que ninguna podía notar que un intento rechazado consumía el documento.

Lo aprendido: la cobertura y el número de pruebas no dicen nada sobre si existe la prueba **específica** que cubre un caso límite del negocio. Solo el análisis del dominio lo dice.

### El que ninguna prueba encontró

El **defecto 08** —los dos mutantes vivos en `Person`— no lo encontró ninguna prueba. Lo encontró PIT. Con 27 pruebas en verde y cobertura del 100 %, podíamos romper `getName()` y nadie se enteraba.

Lo aprendido: **la cobertura es un detector de código olvidado; la mutación es el único que detecta código mal probado.**

---

## 3. ¿Cómo mejorarías la clase `Registry` para facilitar su prueba?

`Registry` es fácil de probar hoy: sin dependencias externas, sin *mocks*, ejecución en milisegundos. Pero tiene una limitación de diseño que se nota apenas se piensa en producción.

### El problema: una responsabilidad de infraestructura escondida en el dominio

```java
private final Set<Integer> registeredIds = new HashSet<>();
```

Ese `HashSet` **es** la decisión de persistencia, tomada dentro del dominio y no negociable desde afuera. Hoy funciona porque el almacenamiento es trivial. El día que los documentos deban vivir en una base de datos, `Registry` tendrá que conocerla — y el dominio dejará de ser puro.

Desde el punto de vista de las pruebas, además, **no hay forma de preparar un registro con estado previo** sin llamar a `registerVoter` varias veces. La preparación de un escenario tiene que pasar por el método que se está probando, lo cual es un olor conocido.

### La mejora: invertir la dependencia con un puerto

```java
// dominio: define QUÉ necesita, no CÓMO se implementa
public interface VoterRepository {
    boolean exists(int id);
    void save(int id);
}

public class Registry {
    private final VoterRepository repository;

    public Registry(VoterRepository repository) {
        this.repository = repository;
    }

    public Registry() {                       // conveniencia: implementación en memoria
        this(new InMemoryVoterRepository());
    }

    public RegisterResult registerVoter(Person p) {
        ...
        if (repository.exists(p.getId())) return RegisterResult.DUPLICATED;
        repository.save(p.getId());
        return RegisterResult.VALID;
    }
}
```

Qué gana el proyecto:

| Ventaja | Detalle |
|---|---|
| **Pruebas más directas** | Se puede construir un `Registry` con documentos ya inscritos sin llamar a `registerVoter`. El *Arrange* deja de depender del *Act*. |
| **Dominio verdaderamente puro** | La persistencia queda del otro lado de la frontera; el dominio depende de una interfaz que él mismo define (regla de dependencias de Arquitectura Limpia). |
| **Escenarios hoy imposibles** | Un doble de prueba puede simular un fallo del almacenamiento y verificar cómo responde `Registry`. Con el `HashSet` no hay manera de provocarlo. |
| **Producción sin tocar el dominio** | Cambiar a base de datos es escribir otra implementación de `VoterRepository`. `Registry` no se entera. |

**El precio** es un poco más de ceremonia: una interfaz, una implementación en memoria y un constructor adicional. Para el alcance de este taller no valía la pena — habría sido complejidad especulativa, justo lo que TDD desaconseja. Pero es el primer refactor que haríamos si el proyecto creciera.

### Otras dos mejoras menores

**Hacer `Registry` seguro para concurrencia.** Cambiar el `Set` por un `ConcurrentHashMap.newKeySet()` y usar el valor de retorno de `add` como comprobación atómica:

```java
if (!registeredIds.add(p.getId())) return RegisterResult.DUPLICATED;
```

Es una línea, y elimina la condición de carrera. La dejamos fuera a propósito: sin un requisito de concurrencia, `contains` seguido de `add` se lee mejor y expresa con más claridad que el documento solo se consume al final. Sería un cambio guiado por una necesidad especulativa.

**Devolver información, no solo un enum.** Hoy `registerVoter` responde `INVALID` tanto si la persona es nula como si el documento es negativo. Un `RegisterResult` con mensaje o un `Result<T>` diría *por qué*. Otra vez: el enunciado no lo pide, y el enum mantiene el dominio simple.

---

## 4. Lo que nos llevamos del taller

### Sobre TDD

**La barra roja es información, no un trámite.** El fallo `expected: <INVALID_AGE> but was: <UNDERAGE>` del defecto 03 no dijo solo "esto falla": dijo *qué* estaba respondiendo en lugar de la regla correcta, y eso resolvió la mitad del diagnóstico. Una prueba que nunca estuvo en rojo no ha demostrado que pueda fallar.

**El refactor no es opcional.** El `@BeforeEach` que introdujimos por legibilidad en la iteración 2 se volvió la condición que hizo segura la iteración 6. Ninguno de los dos momentos tenía cómo anticipar al otro.

**La implementación mínima es una disciplina, no pereza.** Devolver `VALID` para todo en la iteración 1 se siente como hacer trampa. Es lo correcto: obliga a que sea la *siguiente* prueba la que justifique cada generalización, y evita escribir código que nadie pidió.

### Sobre las técnicas de prueba

Cada una encontró algo que las otras no habrían visto:

| Técnica | Encontró |
|---|---|
| Clases de equivalencia y valores límite | Las cuatro fronteras de la edad, incluida la distinción sutil entre `0` → `UNDERAGE` y `-1` → `INVALID_AGE` |
| Pruebas basadas en propiedades | El contraejemplo reducido que aisló la variable culpable en cuatro pasos |
| Pruebas de mutación | Dos getters sin verificar que la cobertura del 100 % daba por buenos |
| Inyección de fallos | Que solo 1 de 30 pruebas cubría un caso límite real del negocio |

**No son alternativas, son capas.** Y ninguna sustituye al análisis del dominio: la prueba que detectó el defecto 05 no salió de una herramienta, salió de preguntarse qué significa exactamente "ocupar un documento".

### La conclusión

Empezamos el taller con una intuición común —que un buen porcentaje de cobertura significa un buen conjunto de pruebas— y la terminamos habiéndola visto fallar en un caso concreto: **100 % de cobertura con dos métodos del dominio sin verificar por nadie**.

La cobertura sigue siendo útil, pero como detector de olvidos, no como certificado de calidad. La pregunta que de verdad importa es la que hace la mutación: *si el código cambiara, ¿se enteraría alguna prueba?* Es la única que no se puede responder sin escribir aserciones de verdad.
