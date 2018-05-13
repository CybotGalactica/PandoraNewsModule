create table messages
(
  id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  currentTime TIMESTAMP                         not null,
  message     TEXT                              not null
);

CREATE TABLE scores
(
  id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  currentTime TIMESTAMP                         NOT NULL,
  team        TEXT                              NOT NULL,
  score       INTEGER
);

CREATE TABLE kills (
  id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  currentTime TIMESTAMP                         NOT NULL,
  killer      TEXT                              NOT NULL,
  victim      TEXT
);

CREATE TABLE puzzels (
  id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  currentTime TIMESTAMP                         NOT NULL,
  player      TEXT                              NOT NULL,
  puzzle      TEXT
);

