package edu.unisabana.tyvs.domain.service;

import edu.unisabana.tyvs.domain.model.Person;
import edu.unisabana.tyvs.domain.model.RegisterResult;

/**
 * PUNTO DE PARTIDA DEL TALLER - no es la solucion final.
 *
 * Esta clase es el estado del codigo al terminar la ITERACION 2 del README
 * (regla "persona muerta"). Las reglas que faltan son las que usted debe
 * construir con TDD (Red -> Green -> Refactor):
 *
 *   - id <= 0                -> INVALID
 *   - edad < 0 o edad > 120  -> INVALID_AGE
 *   - 0 <= edad < 18         -> UNDERAGE
 *   - id ya registrado antes -> DUPLICATED
 *
 * Escriba PRIMERO la prueba que falla, luego la implementacion minima.
 */
public class Registry {

    public RegisterResult registerVoter(Person p) {
        if (p == null) {
            return RegisterResult.INVALID; // regla defensiva
        }
        if (p.getId() <= 0) {
            return RegisterResult.INVALID;
        }
        if (!p.isAlive()) {
            return RegisterResult.DEAD;
        }
        if (p.getAge() < 0 || p.getAge() > 120) {
            return RegisterResult.INVALID_AGE;
        }
        if (p.getAge() < 18) {
            return RegisterResult.UNDERAGE;
        }
        // Implementacion minima para pasar las pruebas de la iteracion 2.
        // TODO iteracion 3 en adelante: validar id, edad y duplicados.
        return RegisterResult.VALID;
    }
}
