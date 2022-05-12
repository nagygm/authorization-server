alter table client_registration
    add token_endpoint_auth_method varchar(255) default 'NONE' not null;

alter table grant_request alter column code drop not null;
alter table grant_request alter column code_created_at drop not null;
alter table grant_request alter column associated_user_id drop not null;
alter table grant_request alter column processed_at drop not null;
alter table grant_request alter column consent_requested_at drop not null;

alter table grant_request alter column code_challenge drop not null;
alter table grant_request alter column code_challenge_method drop not null;

alter table client_registration
    add type varchar(100) default 'PUBLIC' not null;

alter table appuser
    add mfa boolean default false not null;

alter table appuser
    add totp_seed varchar(100);

create table role
(
    role_id integer      not null
        constraint role_pk
        primary key,
    name    varchar(255) not null
);

create unique index role_name_uindex
    on role (name);

create table appuser_roles
(
    appuser_id uuid not null
        constraint appuser_roles_appuser_user_id_fk
        references appuser,
    role_id int not null
        constraint appuser_roles_role_role_id_fk
        references role,
    constraint appuser_roles_pk
        primary key (appuser_id, role_id)
);


