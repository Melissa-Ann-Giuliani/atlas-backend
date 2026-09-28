-- 1. Insert Tipo de Unidad
INSERT INTO public.tipo_de_unidad (tipo_unidad_nombre) VALUES
('Centro'),
('Departamento'),
('Instituto');

-- The above will likely get IDs 1, 2, 3 assuming the table is empty.
-- We will use subqueries to be safe.

-- 2. Insert Centros
INSERT INTO public.unidades (unidad_nombre, tipo_unidad_id) VALUES
('Creación. Art. Coral', (SELECT tipo_unidad_id FROM public.tipo_de_unidad WHERE tipo_unidad_nombre = 'Centro')),
('Creación. Art. Orq.', (SELECT tipo_unidad_id FROM public.tipo_de_unidad WHERE tipo_unidad_nombre = 'Centro')),
('Tornambé Centro de Creación', (SELECT tipo_unidad_id FROM public.tipo_de_unidad WHERE tipo_unidad_nombre = 'Centro'));

-- 3. Insert Departamentos
INSERT INTO public.unidades (unidad_nombre, tipo_unidad_id) VALUES
('Artes Visuales', (SELECT tipo_unidad_id FROM public.tipo_de_unidad WHERE tipo_unidad_nombre = 'Departamento')),
('Filosofía y Cs. de la Edu.', (SELECT tipo_unidad_id FROM public.tipo_de_unidad WHERE tipo_unidad_nombre = 'Departamento')),
('Física, Química y Tec.', (SELECT tipo_unidad_id FROM public.tipo_de_unidad WHERE tipo_unidad_nombre = 'Departamento')),
('Geografía', (SELECT tipo_unidad_id FROM public.tipo_de_unidad WHERE tipo_unidad_nombre = 'Departamento')),
('Historia', (SELECT tipo_unidad_id FROM public.tipo_de_unidad WHERE tipo_unidad_nombre = 'Departamento')),
('Lengua y Lit. Inglesa', (SELECT tipo_unidad_id FROM public.tipo_de_unidad WHERE tipo_unidad_nombre = 'Departamento')),
('Letras', (SELECT tipo_unidad_id FROM public.tipo_de_unidad WHERE tipo_unidad_nombre = 'Departamento')),
('Matemática', (SELECT tipo_unidad_id FROM public.tipo_de_unidad WHERE tipo_unidad_nombre = 'Departamento')),
('Música', (SELECT tipo_unidad_id FROM public.tipo_de_unidad WHERE tipo_unidad_nombre = 'Departamento')),
('Turismo', (SELECT tipo_unidad_id FROM public.tipo_de_unidad WHERE tipo_unidad_nombre = 'Departamento'));

-- 4. Insert Institutos
INSERT INTO public.unidades (unidad_nombre, tipo_unidad_id) VALUES
('Ciencias Básicas - ICB', (SELECT tipo_unidad_id FROM public.tipo_de_unidad WHERE tipo_unidad_nombre = 'Instituto')),
('Geografía Aplicada', (SELECT tipo_unidad_id FROM public.tipo_de_unidad WHERE tipo_unidad_nombre = 'Instituto')),
('Instituto de Est. Musicales', (SELECT tipo_unidad_id FROM public.tipo_de_unidad WHERE tipo_unidad_nombre = 'Instituto')),
('Instituto de Exp. Visual', (SELECT tipo_unidad_id FROM public.tipo_de_unidad WHERE tipo_unidad_nombre = 'Instituto')),
('Instituto de Filosofía', (SELECT tipo_unidad_id FROM public.tipo_de_unidad WHERE tipo_unidad_nombre = 'Instituto')),
('Instituto de Inv. Ling. y Filolog.', (SELECT tipo_unidad_id FROM public.tipo_de_unidad WHERE tipo_unidad_nombre = 'Instituto')),
('Investig. Aqueológ. y Museo', (SELECT tipo_unidad_id FROM public.tipo_de_unidad WHERE tipo_unidad_nombre = 'Instituto')),
('Investig. en Cs. de la Edu.', (SELECT tipo_unidad_id FROM public.tipo_de_unidad WHERE tipo_unidad_nombre = 'Instituto')),
('Investig. en Ed. en Cs. Exper.', (SELECT tipo_unidad_id FROM public.tipo_de_unidad WHERE tipo_unidad_nombre = 'Instituto')),
('Investig. en Historia Reg. y Arg.', (SELECT tipo_unidad_id FROM public.tipo_de_unidad WHERE tipo_unidad_nombre = 'Instituto')),
('Litertura - Ricardo Güiraldes', (SELECT tipo_unidad_id FROM public.tipo_de_unidad WHERE tipo_unidad_nombre = 'Instituto'));
