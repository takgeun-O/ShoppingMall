drop table if exists product;
drop table if exists category;
drop table if exists member;

create table category (
    id bigint auto_increment primary key,
    name varchar(50) not null,
    name_key varchar(50) not null,
    slug varchar(100) not null,
    parent_id bigint,
    status varchar(20) not null,

    constraint uq_category_name_key unique (name_key),
    constraint uq_category_slug unique (slug),

    constraint ck_category_name_not_blank check (trim(name) <> ''),
    constraint ck_category_name_key_not_blank check (trim(name_key) <> ''),
    constraint ck_category_slug_not_blank check (trim(slug) <> ''),
    constraint ck_category_parent_id_positive check (parent_id is null or parent_id > 0),
    constraint ck_category_status check (status in ('ACTIVE', 'INACTIVE'))
);

create table product (
    id bigint auto_increment primary key,
    category_id bigint not null,

    name varchar(100) not null,
    price integer not null,
    stock integer not null,
    description varchar(2000),
    status varchar(20) not null,

    image_url varchar(500),
    rating double not null default 0.0,
    original_price integer,

    constraint fk_product_category
        foreign key (category_id) references category(id),

    constraint ck_product_category_id_positive check (category_id > 0),
    constraint ck_product_name_not_blank check (trim(name) <> ''),
    constraint ck_product_price_not_negative check (price >= 0),
    constraint ck_product_stock_not_negative check (stock >= 0),
    constraint ck_product_status check (
        status in ('READY', 'ON_SALE', 'SOLD_OUT','HIDDEN', 'DISCONTINUED')),
    constraint ck_product_original_price_positive check
        (original_price is null or original_price >= 0)
);

create table member (
    id bigint auto_increment primary key,

    email varchar(320) not null,
    password varchar(255) not null,
    name varchar(50) not null,
    phone varchar(20) not null,

    role varchar(20) not null,
    status varchar(20) not null,

    created_at timestamp not null,
    last_login_at timestamp,

    constraint uq_member_email unique (email),

    constraint ck_member_email_not_blank check (trim(email) <> ''),
    constraint ck_member_name_not_blank check (trim(name) <> ''),
    constraint ck_member_password_not_blank check (trim(password) <> ''),
    constraint ck_member_phone_not_blank check (trim(phone) <> ''),
    constraint ck_member_role check (role in ('USER', 'ADMIN')),
    constraint ck_member_status check (status in ('ACTIVE', 'INACTIVE', 'WITHDRAWN'))
);