CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE appuser
(
    user_id UUID NOT NULL DEFAULT uuid_generate_v4(),
    account_non_expired     BOOLEAN      NOT NULL,
    account_non_locked      BOOLEAN      NOT NULL,
    credentials_non_expired BOOLEAN      NOT NULL,
    enabled                 BOOLEAN      NOT NULL,
    passwordHash            VARCHAR(255) NOT NULL,
    username                VARCHAR(255) NOT NULL UNIQUE,
    PRIMARY KEY (user_id)
);


CREATE TABLE appuser_profile
(
    user_id UUID NOT NULL DEFAULT uuid_generate_v4(),
    first_name VARCHAR(32)  NOT NULL,
    last_name  VARCHAR(32)  NOT NULL,
    email      VARCHAR(255) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES appuser (user_id),
    PRIMARY KEY (user_id)
);

-- TODO add platform type (browser-based application, web application, native application)
-- TODO add security level public, credentialed, confidential
CREATE TABLE client_registration
(
    client_registration_id uuid NOT NULL DEFAULT uuid_generate_v4(),
    client_id                 VARCHAR(255) NOT NULL UNIQUE,
    secret                    VARCHAR(255) NOT NULL,
    redirect_uris             text[] NOT NULL,
    authorization_grant_types text[] NOT NULL,
    scopes                    text[] NOT NULL,
    access_token_lifetime     INTEGER      NOT NULL default 3600,
    refresh_token_lifetime    INTEGER      NOT NULL default 1209600,
    PRIMARY KEY (client_registration_id)
);

CREATE TABLE refresh_token
(
    user_id UUID NOT NULL,
    client_registration_id uuid NOT NULL,
    scopes        text[] NOT NULL,
    refresh_token VARCHAR(255) NOT NULL,
    PRIMARY KEY (client_registration_id, refresh_token),
    FOREIGN KEY (client_registration_id) REFERENCES client_registration (client_registration_id),
    FOREIGN KEY (user_id) REFERENCES appuser (user_id)
);

create table resource
(
    resource_id uuid NOT NULL DEFAULT uuid_generate_v4(),
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    host        VARCHAR(255) NOT NULL,
    client_id   VARCHAR(255) NOT NULL,
    PRIMARY KEY (resource_id),
    FOREIGN KEY (client_id) REFERENCES client_registration (client_id)
);

CREATE TABLE resource_endpoint
(
    resource_endpoint_id uuid NOT NULL,
    resource_id uuid NOT NULL,
    endpoint  VARCHAR(255) NOT NULL,
    operation VARCHAR(255) NOT NULL,
    PRIMARY KEY (resource_endpoint_id),
    UNIQUE (resource_id, endpoint, operation),
    FOREIGN KEY (resource_id) REFERENCES resource (resource_id)
);

CREATE TABLE scope
(
    scope_name         VARCHAR(255) NOT NULL,
    scope_desc         VARCHAR(255) NOT NULL,
    protocol           VARCHAR(255) NOT NULL,
    display_on_consent VARCHAR(255) NOT NULL,
    consent_text       VARCHAR(255) NOT NULL,
    resource_id UUID NOT NULL,
    PRIMARY KEY (scope_name),
    FOREIGN KEY (resource_id) REFERENCES resource (resource_id)
);

create table resource_endpoints_scopes
(
    resource_endpoint_id uuid NOT NULL,
    scope_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (resource_endpoint_id, scope_name),
    FOREIGN KEY (resource_endpoint_id) REFERENCES resource_endpoint (resource_endpoint_id),
    FOREIGN KEY (scope_name) REFERENCES scope (scope_name)
);

create table grant_request
(
    id uuid not null DEFAULT uuid_generate_v4(),
    redirect_uri          varchar(255) not null,
    scopes                text[] NOT NULL,
    response_type         varchar(32)  not null,
    client_id             varchar(255) not null,
    code                  varchar(255) not null,
    state                 varchar(255) not null,
    code_created_at       timestamp    not null,
    request_state         varchar(32)  not null,
    accepted_scopes       text[] not null,
    associated_user_id uuid not null,
    processed_at          timestamp    not null,
    consent_requested_at  timestamp    not null,
    code_challenge        varchar(128) not null,
    code_challenge_method varchar(10)  not null,
    primary key (id),
    foreign key (associated_user_id) references appuser (user_id),
    foreign key (client_id) references client_registration (client_id)
);
