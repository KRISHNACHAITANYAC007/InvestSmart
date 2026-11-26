-- Back up before applying. This preserves provider listings and stores only owner-admin overrides.
CREATE TABLE admin_listing_overrides (
 asset_type VARCHAR(8) NOT NULL,
 reference_id VARCHAR(180) NOT NULL,
 available BOOLEAN NOT NULL,
 open_date DATE NULL,
 close_date DATE NULL,
 allotment_date DATE NULL,
 listing_date DATE NULL,
 reason VARCHAR(1000) NOT NULL,
 actor_id BIGINT NOT NULL,
 updated_at TIMESTAMP NOT NULL,
 PRIMARY KEY(asset_type,reference_id)
);
