# Strings schema

# --- !Ups

CREATE TABLE tweet (
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    content TEXT NOT NULL,
    date_time TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);

# --- !Downs

DROP TABLE tweet;