CREATE TABLE applicant (
                           id UUID PRIMARY KEY,
                           full_name TEXT NOT NULL,
                           email TEXT NOT NULL UNIQUE,
                           kyc_status TEXT NOT NULL DEFAULT 'PENDING'
);


CREATE TABLE application (
                             id UUID PRIMARY KEY,
                             applicant_id UUID NOT NULL REFERENCES applicant(id),
                             amount NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
                             status TEXT NOT NULL DEFAULT 'PENDING',
                             created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);


CREATE INDEX idx_application_status ON application(status);
CREATE INDEX idx_application_created_at ON application(created_at);


CREATE TABLE decision (
                          id UUID PRIMARY KEY,
                          application_id UUID NOT NULL REFERENCES application(id),
                          score NUMERIC(6,2) NOT NULL,
                          outcome TEXT NOT NULL,
                          reason TEXT,
                          decided_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
