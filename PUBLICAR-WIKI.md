# Cómo publicar el Wiki

Las páginas del Wiki están escritas en la carpeta [`wiki/`](wiki/) de este repositorio. GitHub guarda el Wiki en un **repositorio git aparte**, así que hay que copiarlas allí.

---

## Paso 1 — Habilitar el Wiki (solo la primera vez)

1. Ir a **Settings → General → Features** del repositorio.
2. Marcar la casilla **Wikis**.
3. Ir a la pestaña **Wiki** y pulsar **Create the first page**. Guardar cualquier contenido: hace falta para que el repositorio del Wiki exista.

---

## Paso 2 — Clonar el repositorio del Wiki y copiar las páginas

Desde una carpeta **fuera** de este repositorio:

```bash
git clone https://github.com/Majo7-613/DYAS-Taller-Pruebas-Unitarias.wiki.git
cd DYAS-Taller-Pruebas-Unitarias.wiki

# copiar solo las paginas (sin las subcarpetas evidencia/ e img/)
cp ../TYVS-Taller_Pruebas_Unitarias/wiki/*.md .

git add .
git commit -m "docs: documentacion del taller de pruebas unitarias"
git push
```

En PowerShell, el `cp` equivalente es:

```powershell
Copy-Item ..\TYVS-Taller_Pruebas_Unitarias\wiki\*.md .
```

> **Copie solo los `.md` de la raíz de `wiki/`.** Las carpetas `evidencia/` e `img/` se quedan en el repositorio principal: las páginas del Wiki las enlazan por URL de GitHub, y así la evidencia queda versionada junto al código que la produjo.

---

## Paso 3 — Adjuntar las capturas

La página **06 — Resultados** tiene un bloque marcado como *pendiente de adjuntar*. Genere los reportes, capture las dos páginas HTML y arrastre los PNG al editor del Wiki de GitHub, que los sube y genera el enlace. Las instrucciones detalladas están en [`wiki/img/LEEME.md`](wiki/img/LEEME.md).

---

## Verificación

Al terminar, el Wiki debe mostrar nueve páginas y una barra lateral de navegación:

```
Home                          _Sidebar
01-Historia-TDD               05-Pruebas-de-Propiedades
02-Patron-AAA                 06-Resultados
03-Clases-de-Equivalencia     07-Gestion-de-Defectos
04-BDD-Escenarios             08-Reflexion-Final
```

Revise que los enlaces entre páginas funcionen: GitHub los resuelve por el **nombre del archivo sin extensión**, que es como están escritos.
