CREATE TABLE messages
(
  id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  currentTime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  message     TEXT      NOT NULL
);

CREATE TABLE scores
(
  id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  currentTime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  team        TEXT      NOT NULL,
  score       INTEGER
);

CREATE TABLE kills (
  id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  currentTime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  killer      TEXT      NOT NULL,
  victim      TEXT
);

CREATE TABLE puzzels (
  id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  currentTime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  player      TEXT      NOT NULL,
  puzzle      TEXT      NOT NULL
);


CREATE TABLE teams (
  id       INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  userId   INTEGER NOT NULL,
  fullName TEXT    NOT NULL,
  alias    TEXT    NOT NULL
);

