-- =====================================================================
-- MIGRATION V001 — Cohort / Programme / Semester / Course restructure
-- ---------------------------------------------------------------------
BEGIN;

-- ---------------------------------------------------------------------
-- STEP 1 — Cohorts become university-wide INTAKE SEASONS
--   Add season / academic_year / university_id. We add them NULL-able first,
--   fill them in, and only THEN mark them required — because Postgres refuses
--   to add a NOT NULL column to a table that already has rows.
-- ---------------------------------------------------------------------
ALTER TABLE cohorts ADD COLUMN IF NOT EXISTS season        varchar(20);
ALTER TABLE cohorts ADD COLUMN IF NOT EXISTS academic_year integer;
ALTER TABLE cohorts ADD COLUMN IF NOT EXISTS university_id bigint;

-- 1a) Backfill university_id from the cohort's OLD programme link,
--     but only while that old column still exists.
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns
             WHERE table_name = 'cohorts' AND column_name = 'programme_id') THEN
    UPDATE cohorts c
       SET university_id = p.university_id
      FROM programmes p
     WHERE c.programme_id = p.id
       AND c.university_id IS NULL;
  END IF;
END $$;

-- 1b) Dev fallback: any cohort still without a university gets the first one.
UPDATE cohorts
   SET university_id = (SELECT id FROM universities ORDER BY id LIMIT 1)
 WHERE university_id IS NULL;

-- 1c) Sensible defaults for existing rows (adjust later in the UI if needed).
UPDATE cohorts SET season        = 'FALL' WHERE season IS NULL;
UPDATE cohorts SET academic_year = 2026   WHERE academic_year IS NULL;

-- 1d) Now the columns are populated, mark them required (no-op if already set).
ALTER TABLE cohorts ALTER COLUMN season        SET NOT NULL;
ALTER TABLE cohorts ALTER COLUMN academic_year SET NOT NULL;
ALTER TABLE cohorts ALTER COLUMN university_id SET NOT NULL;

-- 1e) Foreign key cohorts.university_id → universities.id (re-runnable).
ALTER TABLE cohorts DROP CONSTRAINT IF EXISTS fk_cohort_university;
ALTER TABLE cohorts ADD  CONSTRAINT fk_cohort_university
  FOREIGN KEY (university_id) REFERENCES universities(id);

-- ---------------------------------------------------------------------
-- STEP 2 — Programme ↔ Cohort many-to-many (a programme can run in many
--          intakes; an intake can host many programmes)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS programme_cohorts (
  programme_id bigint NOT NULL REFERENCES programmes(id) ON DELETE CASCADE,
  cohort_id    bigint NOT NULL REFERENCES cohorts(id)    ON DELETE CASCADE,
  PRIMARY KEY (programme_id, cohort_id)
);

-- 2a) Preserve every OLD cohort→programme link as a row in the new join table.
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns
             WHERE table_name = 'cohorts' AND column_name = 'programme_id') THEN
    INSERT INTO programme_cohorts (programme_id, cohort_id)
    SELECT programme_id, id FROM cohorts
     WHERE programme_id IS NOT NULL
    ON CONFLICT DO NOTHING;
  END IF;
END $$;

-- 2b) The old single-programme column on cohorts is no longer needed.
ALTER TABLE cohorts DROP COLUMN IF EXISTS programme_id;

-- ---------------------------------------------------------------------
-- STEP 3 — Semesters (structure a programme into ordered semesters)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS semesters (
  id           bigserial   PRIMARY KEY,
  name         varchar     NOT NULL,
  order_index  integer     NOT NULL DEFAULT 1,
  programme_id bigint      NOT NULL REFERENCES programmes(id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- STEP 4 — Courses (belong to a semester + programme, optional lecturer)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS courses (
  id           bigserial    PRIMARY KEY,
  name         varchar      NOT NULL,
  code         varchar,
  description  varchar(2000),
  semester_id  bigint       NOT NULL REFERENCES semesters(id) ON DELETE CASCADE,
  programme_id bigint       NOT NULL REFERENCES programmes(id),
  lecturer_id  bigint       REFERENCES lecturers(id),
  status       varchar(50)  NOT NULL DEFAULT 'ACTIVE'
);

-- ---------------------------------------------------------------------
-- STEP 5 — Assignments (submissions) move from a cohort onto a COURSE
-- ---------------------------------------------------------------------
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS course_id bigint;

-- 5a) Preserve OLD assignments: give each programme a placeholder
--     "General (migrated)" course and point old assignments at it
--     (via the cohort→programme link we saved in step 2). Only runs while
--     the old submissions.cohort_id column still exists.
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns
             WHERE table_name = 'submissions' AND column_name = 'cohort_id') THEN

    -- one "Migrated" semester per programme
    INSERT INTO semesters (name, order_index, programme_id)
    SELECT 'Migrated', 1, p.id FROM programmes p
    WHERE NOT EXISTS (SELECT 1 FROM semesters s
                       WHERE s.programme_id = p.id AND s.name = 'Migrated');

    -- one "General (migrated)" course per programme
    INSERT INTO courses (name, code, semester_id, programme_id, status)
    SELECT 'General (migrated)', 'GEN', s.id, s.programme_id, 'ACTIVE'
      FROM semesters s
     WHERE s.name = 'Migrated'
       AND NOT EXISTS (SELECT 1 FROM courses c
                        WHERE c.programme_id = s.programme_id
                          AND c.name = 'General (migrated)');

    -- attach each old assignment to the migrated course of its programme
    UPDATE submissions sub
       SET course_id = co.id
      FROM programme_cohorts pc
      JOIN courses co ON co.programme_id = pc.programme_id
                     AND co.name = 'General (migrated)'
     WHERE sub.cohort_id = pc.cohort_id
       AND sub.course_id IS NULL;
  END IF;
END $$;

-- 5b) Remove any assignment we genuinely could not map (and its children),
--     so course_id can become required. Guarded by to_regclass so unknown
--     child tables are simply skipped.
DO $$
BEGIN
  IF to_regclass('public.student_grades') IS NOT NULL THEN
    DELETE FROM student_grades
     WHERE submission_id IN (SELECT id FROM submissions WHERE course_id IS NULL);
  END IF;
  IF to_regclass('public.submission_uploads') IS NOT NULL THEN
    DELETE FROM submission_uploads
     WHERE submission_id IN (SELECT id FROM submissions WHERE course_id IS NULL);
  END IF;
  IF to_regclass('public.notifications') IS NOT NULL THEN
    DELETE FROM notifications
     WHERE submission_id IN (SELECT id FROM submissions WHERE course_id IS NULL);
  END IF;
END $$;
DELETE FROM submissions WHERE course_id IS NULL;

-- 5c) Make course_id required, add the FK, drop the old cohort_id column.
ALTER TABLE submissions ALTER COLUMN course_id SET NOT NULL;
ALTER TABLE submissions DROP CONSTRAINT IF EXISTS fk_submission_course;
ALTER TABLE submissions ADD  CONSTRAINT fk_submission_course
  FOREIGN KEY (course_id) REFERENCES courses(id);
ALTER TABLE submissions DROP COLUMN IF EXISTS cohort_id;

-- ---------------------------------------------------------------------
-- STEP 6 — Teaching now lives on courses, so drop the old cohort↔lecturer join
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS cohort_lecturers;

-- ---------------------------------------------------------------------
-- STEP 7 — Drop stale enum CHECK constraints left by earlier runs
--          (the app also clears these on startup via SchemaConstraintFixer)
-- ---------------------------------------------------------------------
ALTER TABLE cohorts     DROP CONSTRAINT IF EXISTS cohorts_status_check;
ALTER TABLE courses     DROP CONSTRAINT IF EXISTS courses_status_check;
ALTER TABLE submissions DROP CONSTRAINT IF EXISTS submissions_status_check;

COMMIT;

-- Done. Start the backend normally afterwards; DemoDataSeeder fills in any
-- missing demo universities, and your existing data is preserved.
