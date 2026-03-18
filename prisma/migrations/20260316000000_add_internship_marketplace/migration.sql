-- Migration: add_internship_marketplace
-- Adds city/state to student_profiles, creates law_firms,
-- internship_postings, and student_internship_notifications tables.

-- ── 1. Add city / state to student_profiles ─────────────────────────────────

ALTER TABLE "student_profiles" ADD COLUMN "city" TEXT;
ALTER TABLE "student_profiles" ADD COLUMN "state" TEXT;

-- ── 2. Create law_firms ──────────────────────────────────────────────────────

CREATE TABLE "law_firms" (
    "id"                   TEXT NOT NULL,
    "official_name"        TEXT NOT NULL,
    "contact_person_name"  TEXT NOT NULL,
    "official_email"       TEXT NOT NULL,
    "website_url"          TEXT,
    "city"                 TEXT NOT NULL,
    "state"                TEXT NOT NULL,
    "specialties"          TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    "firm_token"           TEXT NOT NULL,
    "terms_accepted"       BOOLEAN NOT NULL DEFAULT false,
    "status"               TEXT NOT NULL DEFAULT 'active',
    "created_at"           TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at"           TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "law_firms_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "law_firms_official_email_key" ON "law_firms"("official_email");
CREATE UNIQUE INDEX "law_firms_firm_token_key"     ON "law_firms"("firm_token");
CREATE INDEX "law_firms_city_state_idx"            ON "law_firms"("city", "state");

-- ── 3. Create internship_postings ────────────────────────────────────────────

CREATE TABLE "internship_postings" (
    "id"                    TEXT NOT NULL,
    "firm_id"               TEXT NOT NULL,
    "title"                 TEXT NOT NULL,
    "description"           TEXT NOT NULL,
    "specialty_areas"       TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    "year_of_study_min"     INTEGER,
    "eligibility_criteria"  TEXT,
    "application_deadline"  TIMESTAMP(3),
    "notification_radius"   TEXT NOT NULL DEFAULT 'city',
    "status"                TEXT NOT NULL DEFAULT 'active',
    "notifications_sent"    BOOLEAN NOT NULL DEFAULT false,
    "created_at"            TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at"            TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "internship_postings_pkey" PRIMARY KEY ("id")
);

CREATE INDEX "internship_postings_firm_id_idx"            ON "internship_postings"("firm_id");
CREATE INDEX "internship_postings_status_created_at_idx"  ON "internship_postings"("status", "created_at" DESC);

ALTER TABLE "internship_postings"
    ADD CONSTRAINT "internship_postings_firm_id_fkey"
    FOREIGN KEY ("firm_id") REFERENCES "law_firms"("id")
    ON DELETE RESTRICT ON UPDATE CASCADE;

-- ── 4. Create student_internship_notifications ───────────────────────────────

CREATE TABLE "student_internship_notifications" (
    "id"         TEXT NOT NULL,
    "user_id"    TEXT NOT NULL,
    "posting_id" TEXT NOT NULL,
    "is_read"    BOOLEAN NOT NULL DEFAULT false,
    "tier"       INTEGER NOT NULL,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "student_internship_notifications_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "student_internship_notifications_user_id_posting_id_key"
    ON "student_internship_notifications"("user_id", "posting_id");
CREATE INDEX "student_internship_notifications_user_id_idx"
    ON "student_internship_notifications"("user_id", "is_read");

ALTER TABLE "student_internship_notifications"
    ADD CONSTRAINT "student_internship_notifications_user_id_fkey"
    FOREIGN KEY ("user_id") REFERENCES "users"("id")
    ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE "student_internship_notifications"
    ADD CONSTRAINT "student_internship_notifications_posting_id_fkey"
    FOREIGN KEY ("posting_id") REFERENCES "internship_postings"("id")
    ON DELETE CASCADE ON UPDATE CASCADE;
