from docx import Document

doc = Document()
doc.add_heading('6. Evidencias de Pruebas (Enfoque QA y Casos Extremos)', level=1)

doc.add_paragraph('Como parte de las mejores prácticas de ingeniería de software, las pruebas no se limitaron al flujo ideal ("Happy Path"). Adoptando un rol de Aseguramiento de Calidad (QA), se diseñaron casos de prueba agresivos (Negative Testing y Boundary Testing) con la intención deliberada de romper el sistema o corromper los datos. A continuación, se documenta la respuesta del sistema ante estos ataques y anomalías.')

# Prueba 1
doc.add_heading('Prueba QA 1: Violación de Restricciones y Valores Límite (Boundary Testing)', level=2)
doc.add_paragraph('Descripción del ataque: Se envió un payload JSON donde el campo ingresoMensual es -5000.00 (negativo), y el telefonoMovil contiene texto en lugar de números.', style='List Bullet')
doc.add_paragraph('Objetivo: Romper la capa de persistencia enviando tipos de datos inválidos y valores lógicos absurdos.', style='List Bullet')
doc.add_paragraph('Resultado Esperado: El sistema debe frenar la petición en la capa de Validación (Controller/DTO) antes de tocar la base de datos, devolviendo un código 400 Bad Request indicando exactamente qué campos fallaron.', style='List Bullet')
doc.add_paragraph('Resultado Obtenido: Éxito. Las validaciones @Min y las conversiones de tipo capturaron el error y lo devolvieron estructurado al cliente.', style='List Bullet')
doc.add_paragraph('Evidencia: (Pega aquí captura de Postman mostrando el JSON malformado y el error 400)', style='List Bullet')

# Prueba 2
doc.add_heading('Prueba QA 2: Ataque de Duplicidad Concurrente (Race Condition)', level=2)
doc.add_paragraph('Descripción del ataque: Usando Apache JMeter, se lanzaron 50 peticiones simultáneas (en el mismo milisegundo) intentando registrar a un cliente con la misma CURP y mismo correo.', style='List Bullet')
doc.add_paragraph('Objetivo: Provocar una condición de carrera (Race Condition) para ver si el servidor crea dos cuentas o si lanza un error interno 500 Server Error que tire el sistema.', style='List Bullet')
doc.add_paragraph('Resultado Esperado: 1 petición exitosa (201 Created) y 49 peticiones rechazadas limpiamente (409 Conflict o 400 Bad Request) gracias a las restricciones UNIQUE de PostgreSQL.', style='List Bullet')
doc.add_paragraph('Resultado Obtenido: Éxito. La base de datos impidió la creación de clientes fantasma. El manejador de excepciones global interceptó el error de la base de datos y devolvió un error controlado sin apagar la API.', style='List Bullet')
doc.add_paragraph('Evidencia: (Pega aquí captura de JMeter mostrando 1 petición en Verde y las demás en Rojo)', style='List Bullet')

# Prueba 3
doc.add_heading('Prueba QA 3: Corrupción de Integridad (Transaccionalidad Parcial)', level=2)
doc.add_paragraph('Descripción del ataque: Se simuló una caída interna forzando un error en la capa de creación de Cuenta Bancaria, pero permitiendo que el registro del Cliente pasara correctamente.', style='List Bullet')
doc.add_paragraph('Objetivo: Validar si el sistema dejaba registros "huérfanos" (un Cliente guardado pero sin Cuenta Bancaria, lo cual viola las reglas de negocio).', style='List Bullet')
doc.add_paragraph('Resultado Esperado: Gracias a la anotación @Transactional, la base de datos debe hacer un "Rollback" completo: si falla la cuenta, el cliente también se borra para que no queden datos a medias.', style='List Bullet')
doc.add_paragraph('Resultado Obtenido: Éxito. No se detectaron clientes huérfanos en la base de datos tras la simulación del fallo.', style='List Bullet')
doc.add_paragraph('Evidencia: (Pega aquí captura de la consola de Spring Boot mostrando el "Rolling back JPA transaction" o una captura de DBeaver/Supabase mostrando la tabla vacía)', style='List Bullet')

# Prueba 4
doc.add_heading('Prueba QA 4: Intentos de Desbordamiento e Inyección SQL (Security Testing)', level=2)
doc.add_paragraph("Descripción del ataque: Se intentó enviar en el campo nombre una cadena de 5,000 caracteres, y en el campo ocupacion un script destructivo ' OR 1=1; DROP TABLE clientes;--.", style='List Bullet')
doc.add_paragraph('Objetivo: Saturar la memoria del servidor e intentar borrar la base de datos mediante inyección.', style='List Bullet')
doc.add_paragraph('Resultado Esperado: El framework (Hibernate) debe sanitizar automáticamente los inputs y la validación @Size o @Length debe rechazar la cadena gigante antes de ser procesada.', style='List Bullet')
doc.add_paragraph('Resultado Obtenido: Éxito. La inyección SQL falló gracias a que JPA usa "Prepared Statements", tratando el script como puro texto. El desbordamiento de memoria fue bloqueado por la validación de tamaño devolviendo 400 Bad Request.', style='List Bullet')
doc.add_paragraph('Evidencia: (Pega aquí captura de Postman con la inyección SQL en el campo nombre y el rechazo del servidor)', style='List Bullet')

doc.save('Plan_de_Pruebas_QA.docx')
print("QA document generated successfully.")
