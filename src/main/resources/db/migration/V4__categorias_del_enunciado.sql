INSERT INTO categoria (nombre, activa) VALUES
    ('Rendimiento Académico', TRUE),
    ('Mejor Investigador', TRUE),
    ('Empatía', TRUE),
    ('Comunicación Asertiva', TRUE),
    ('Orientación al Logro', TRUE),
    ('Trabajo en Equipo', TRUE),
    ('Trabajo Bajo Presión', TRUE),
    ('Visión Estratégica', TRUE),
    ('Innovación', TRUE),
    ('Liderazgo', TRUE),
    ('Transformación Digital', TRUE),
    ('Mentor Inspirador', TRUE),
    ('Espíritu UCO', TRUE),
    ('Servicio a la Comunidad', TRUE),
    ('Compromiso Institucional', TRUE)
ON CONFLICT ((lower(nombre))) DO UPDATE
    SET nombre = EXCLUDED.nombre,
        activa = TRUE;

UPDATE categoria
SET activa = FALSE
WHERE lower(nombre) NOT IN (
    lower('Rendimiento Académico'),
    lower('Mejor Investigador'),
    lower('Empatía'),
    lower('Comunicación Asertiva'),
    lower('Orientación al Logro'),
    lower('Trabajo en Equipo'),
    lower('Trabajo Bajo Presión'),
    lower('Visión Estratégica'),
    lower('Innovación'),
    lower('Liderazgo'),
    lower('Transformación Digital'),
    lower('Mentor Inspirador'),
    lower('Espíritu UCO'),
    lower('Servicio a la Comunidad'),
    lower('Compromiso Institucional')
);
