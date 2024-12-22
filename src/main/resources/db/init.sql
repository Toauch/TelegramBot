CREATE TABLE IF NOT EXISTS schedules (
    id BIGSERIAL PRIMARY KEY,
    group_number VARCHAR(10) NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    week_number INTEGER NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    subject VARCHAR(100) NOT NULL,
    teacher VARCHAR(100) NOT NULL,
    classroom VARCHAR(20) NOT NULL
);

-- Пример данных для тестирования (группа ИС-201)
INSERT INTO schedules (group_number, day_of_week, week_number, start_time, end_time, subject, teacher, classroom) VALUES
-- Понедельник, 1-я неделя
('ИС-201', 'MONDAY', 1, '08:30', '10:00', 'Математический анализ', 'Иванов И.И.', '301'),
('ИС-201', 'MONDAY', 1, '10:10', '11:40', 'Программирование', 'Петров П.П.', '404'),
('ИС-201', 'MONDAY', 1, '12:10', '13:40', 'Физика', 'Сидоров С.С.', '201'),

-- Вторник, 1-я неделя
('ИС-201', 'TUESDAY', 1, '08:30', '10:00', 'Английский язык', 'Смирнова А.А.', '308'),
('ИС-201', 'TUESDAY', 1, '10:10', '11:40', 'База данных', 'Козло�� К.К.', '405'),

-- Понедельник, 2-я неделя
('ИС-201', 'MONDAY', 2, '08:30', '10:00', 'Математический анализ', 'Иванов И.И.', '301'),
('ИС-201', 'MONDAY', 2, '10:10', '11:40', 'Программирование', 'Петров П.П.', '404'),

-- Вторник, 2-я неделя
('ИС-201', 'TUESDAY', 2, '10:10', '11:40', 'База данных', 'Козлов К.К.', '405'),
('ИС-201', 'TUESDAY', 2, '12:10', '13:40', 'Физика', 'Сидоров С.С.', '201'); 