package edu.unisabana.tyvs.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del modelo Person.
 *
 * POR QUE EXISTE ESTA CLASE
 * -------------------------
 * No nacio de una regla de negocio, sino de un reporte de PIT. Con las pruebas
 * de Registry, la cobertura de JaCoCo ya era alta, pero dos mutantes seguian
 * SOBREVIVIENDO:
 *
 *   Person.getName()   -> reemplazar el retorno por ""
 *   Person.getGender() -> reemplazar el retorno por null
 *
 * La razon es que registerVoter() nunca consulta el nombre ni el genero: los
 * campos se ejecutan al construir el objeto (por eso JaCoCo los daba por
 * cubiertos) pero su valor no influye en ningun resultado verificado.
 *
 * Es el ejemplo exacto de la diferencia entre cobertura y mutacion: codigo
 * ejecutado no es codigo verificado. Estas pruebas cierran ese hueco afirmando
 * el contrato real del modelo: cada getter devuelve lo que recibio el
 * constructor.
 */
class PersonTest {

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

    @Test
    @DisplayName("Dado una persona no viva, "
            + "cuando consulto isAlive, entonces devuelve false")
    void shouldReportNotAliveWhenBuiltAsNotAlive() {
        // Arrange: la otra rama del atributo booleano
        Person person = new Person("Carlos", 2, 40, Gender.MALE, false);

        // Act: consultar el estado de vida
        boolean alive = person.isAlive();

        // Assert: verificar el resultado esperado
        assertFalse(alive);
    }

    @Test
    @DisplayName("Dado una persona con genero UNIDENTIFIED, "
            + "cuando consulto getGender, entonces devuelve UNIDENTIFIED y no null")
    void shouldKeepUnidentifiedGender() {
        // Arrange: UNIDENTIFIED es el valor mas facil de confundir con null
        Person person = new Person("", 3, 25, Gender.UNIDENTIFIED, true);

        // Act: consultar el genero
        Gender genero = person.getGender();

        // Assert: el modelo distingue "sin identificar" de "ausente"
        assertEquals(Gender.UNIDENTIFIED, genero);
    }
}
