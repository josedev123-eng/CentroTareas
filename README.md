# CentroTareas

## Observación del Paso 1

Al pulsar **Tarea larga (MAL)** la pantalla se congela durante 4 segundos: el botón **Tócame** no suma, los botones no responden y nada se mueve. Después de los 4 segundos aparece directamente **Terminó**.

**¿Por qué no se ve "Trabajando..."?** Porque `Thread.sleep(4000)` se ejecuta en el **hilo principal**, que es el mismo que dibuja la pantalla. El código cambia el mensaje a "Trabajando...", pero el hilo principal se queda dormido antes de poder redibujar; cuando despierta, el mensaje ya cambió a "Terminó", así que solo se ve ese.

**¿Y si la espera fuera de 10 segundos?** Android mostraría el diálogo **"La aplicación no responde" (ANR)**, que aparece cuando el hilo principal está bloqueado unos 5 segundos sin atender los toques, y le ofrece al usuario cerrar la app o esperar.
