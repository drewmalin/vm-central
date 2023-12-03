CREATE TABLE users
(
    user_pk         SERIAL        PRIMARY KEY,
    user_id         VARCHAR(50)   UNIQUE NOT NULL,
    username        VARCHAR(50)   UNIQUE NOT NULL,
    first_name      VARCHAR(50),
    last_name       VARCHAR(50),
    hashed_password BYTEA,
    salt            BYTEA,
    role_id         VARCHAR(50)   NOT NULL
);

CREATE TABLE virtual_machines
(
    virtual_machine_pk  SERIAL        PRIMARY KEY,
    virtual_machine_id  VARCHAR(50)   UNIQUE NOT NULL,
    provider            VARCHAR(50),
    status              VARCHAR(50),
    user_fk             INT           NOT NULL,
    CONSTRAINT          fk_user       FOREIGN KEY(user_fk) REFERENCES users(user_pk)
);