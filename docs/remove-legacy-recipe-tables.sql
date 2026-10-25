-- Optional cleanup for databases created by an older FreshMind version.
-- Back up the database first. Do not run this if another application still uses these tables.

DROP TABLE IF EXISTS recipe_ingredients;
DROP TABLE IF EXISTS recipes;
