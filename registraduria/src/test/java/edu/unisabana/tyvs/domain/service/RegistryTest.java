package edu.unisabana.tyvs.domain.service;

import edu.unisabana.tyvs.domain.model.Gender;
import edu.unisabana.tyvs.domain.model.Person;
import edu.unisabana.tyvs.domain.model.RegisterResult;
import edu.unisabana.tyvs.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pruebas por EJEMPLO del dominio: cada prueba fija una entrada concreta y su
 * resultado esperado. Estado al terminar la ITERACION 2 del README.
 *
 * Complemento: RegistryPropertiesTest expresa las mismas reglas como
 * PROPIEDADES sobre rangos completos de entradas, en vez de ejemplos sueltos.
 */
public class RegistryTest {

    private Registry registry;

    /**
     * Un Registry NUEVO antes de cada prueba.
     *
     * Importante: cuando implemente DUPLICATED, el Registry guardara estado
     * (los ids ya registrados). Si compartiera la misma instancia entre
     * pruebas, una prueba podria "ensuciar" a la siguiente y los resultados
     * dependerian del orden de ejecucion. Cada prueba debe ser independiente.
     */
    @BeforeEach
    void setUp() {
        registry = new Registry();
    }

    @Test
    @DisplayName("Una persona viva y mayor de edad queda registrada")
    void shouldRegisterValidPerson() {
        Registry registry = new Registry();

        // Arrange: preparar los datos
        Person person = new Person("Ana", 1, 30, Gender.FEMALE, true);

        // Act: ejecutar la accion que queremos probar
        RegisterResult result = registry.registerVoter(person);

        // Assert: verificar el resultado esperado
        assertEquals(RegisterResult.VALID, result);
    }

    @Test
    @DisplayName("Una persona no viva se rechaza con DEAD")
    void shouldRejectDeadPerson() {
        // Arrange: preparar los datos
        Person dead = new Person("Carlos", 2, 40, Gender.MALE, false);

        // Act: ejecutar la accion que queremos probar
        RegisterResult result = registry.registerVoter(dead);

        // Assert: verificar el resultado esperado
        assertEquals(RegisterResult.DEAD, result);
    }

    @Test
    @DisplayName("Una persona nula se rechaza con INVALID")
    void shouldReturnInvalidWhenPersonIsNull() {
        // Arrange (Preparación)
        Person person = null;

        // Act (Ejecución)
        RegisterResult result = registry.registerVoter(person);

        // Assert (Verificación)
        assertEquals(RegisterResult.INVALID, result);
    }
}