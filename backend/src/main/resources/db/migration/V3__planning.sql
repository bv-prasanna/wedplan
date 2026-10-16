CREATE TABLE IF NOT EXISTS budget_categories(id UUID PRIMARY KEY,wedding_id UUID NOT NULL REFERENCES weddings(id) ON DELETE CASCADE,name VARCHAR(100) NOT NULL,allocated_paise BIGINT NOT NULL CHECK(allocated_paise>=0),spent_paise BIGINT NOT NULL DEFAULT 0 CHECK(spent_paise>=0),UNIQUE(wedding_id,name));
CREATE TABLE IF NOT EXISTS wedding_tasks(id UUID PRIMARY KEY,wedding_id UUID NOT NULL REFERENCES weddings(id) ON DELETE CASCADE,title VARCHAR(200) NOT NULL,due_date DATE,completed BOOLEAN NOT NULL DEFAULT FALSE);
CREATE TABLE IF NOT EXISTS wedding_guests(id UUID PRIMARY KEY,wedding_id UUID NOT NULL REFERENCES weddings(id) ON DELETE CASCADE,full_name VARCHAR(160) NOT NULL,phone VARCHAR(30),rsvp VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK(rsvp IN ('PENDING','ACCEPTED','DECLINED')));
CREATE INDEX IF NOT EXISTS idx_budget_wedding ON budget_categories(wedding_id);
CREATE INDEX IF NOT EXISTS idx_tasks_wedding ON wedding_tasks(wedding_id);
CREATE INDEX IF NOT EXISTS idx_guests_wedding ON wedding_guests(wedding_id);
