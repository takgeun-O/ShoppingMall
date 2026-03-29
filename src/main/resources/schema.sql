drop table if exists order_items;
drop table if exists orders;
drop table if exists product;
drop table if exists category;
drop table if exists member;

create table category
(
    id        bigint auto_increment primary key,
    name      varchar(50)  not null,
    name_key  varchar(50)  not null,
    slug      varchar(100) not null,
    parent_id bigint,
    status    varchar(20)  not null,

    constraint uq_category_name_key unique (name_key),
    constraint uq_category_slug unique (slug),

    constraint ck_category_name_not_blank check (trim(name) <> ''),
    constraint ck_category_name_key_not_blank check (trim(name_key) <> ''),
    constraint ck_category_slug_not_blank check (trim(slug) <> ''),
    constraint ck_category_parent_id_positive check (parent_id is null or parent_id > 0),
    constraint ck_category_status check (status in ('ACTIVE', 'INACTIVE'))
);

create table product
(
    id             bigint auto_increment primary key,
    category_id    bigint       not null,

    name           varchar(100) not null,
    price          integer      not null,
    stock          integer      not null,
    description    varchar(2000),
    status         varchar(20)  not null,

    image_url      varchar(500),
    rating double not null default 0.0,
    original_price integer,

    constraint fk_product_category
        foreign key (category_id) references category (id),

    constraint ck_product_category_id_positive check (category_id > 0),
    constraint ck_product_name_not_blank check (trim(name) <> ''),
    constraint ck_product_price_not_negative check (price >= 0),
    constraint ck_product_stock_not_negative check (stock >= 0),
    constraint ck_product_status check (
        status in ('READY', 'ON_SALE', 'SOLD_OUT', 'HIDDEN', 'DISCONTINUED')),
    constraint ck_product_original_price_positive check
        (original_price is null or original_price >= 0)
);

create table member
(
    id            bigint auto_increment primary key,

    email         varchar(320) not null,
    password      varchar(255) not null,
    name          varchar(50)  not null,
    phone         varchar(20)  not null,

    role          varchar(20)  not null,
    status        varchar(20)  not null,

    created_at    timestamp    not null,
    last_login_at timestamp,

    constraint uq_member_email unique (email),

    constraint ck_member_email_not_blank check (trim(email) <> ''),
    constraint ck_member_name_not_blank check (trim(name) <> ''),
    constraint ck_member_password_not_blank check (trim(password) <> ''),
    constraint ck_member_phone_not_blank check (trim(phone) <> ''),
    constraint ck_member_role check (role in ('USER', 'ADMIN')),
    constraint ck_member_status check (status in ('ACTIVE', 'INACTIVE', 'WITHDRAWN'))
);

create table orders
(
    id                      bigint auto_increment primary key,
    order_number             varchar(50)  not null,
    member_id               bigint       not null,
    status                  varchar(30)  not null,
    request_key             varchar(100) not null,

    recipient_name          varchar(50)  not null,
    recipient_phone         varchar(30)  not null,
    shipping_zip_code       varchar(20)  not null,
    shipping_address        varchar(200) not null,
    shipping_address_detail varchar(200) not null,
    request_message         varchar(200) null,

    subtotal                int          not null,
    shipping_fee            int          not null,
    total_price             int          not null,

    ordered_at              datetime     not null,
    canceled_at             datetime null,
    updated_at              datetime     not null,

    constraint uk_orders_order_number unique (order_number),
    constraint uk_orders_request_key unique (request_key),

    constraint ck_orders_status
        check (status in (
                          'ORDERED',
                          'PAYMENT_COMPLETED',
                          'PREPARING',
                          'SHIPPING',
                          'DELIVERED',
                          'CANCELED'
            )),

    constraint ck_orders_member_id_positive check (member_id > 0),
    constraint ck_orders_subtotal_non_negative check (subtotal >= 0),
    constraint ck_orders_shipping_fee_non_negative check (shipping_fee >= 0),
    constraint ck_orders_total_price_non_negative check (total_price >= 0),

    constraint fk_orders_member
        foreign key (member_id) references member (id)
);

create table order_items
(
    id                      bigint auto_increment primary key,
    order_id                bigint       not null,
    product_id              bigint       not null,

    product_name_snapshot   varchar(200) not null,
    unit_price_snapshot     int          not null,
    original_price_snapshot int null,
    quantity                int          not null,
    image_url_snapshot      varchar(500) not null,

    constraint fk_order_items_order
        foreign key (order_id) references orders (id) on delete cascade,
    constraint fk_order_items_product
        foreign key (product_id) references product (id),

    constraint ck_order_items_product_name_not_blank
        check (trim(product_name_snapshot) <> '' ),

    constraint ck_order_items_order_id_positive check (order_id > 0),
    constraint ck_order_items_product_id_positive check (product_id > 0),
    constraint ck_order_items_price_non_negative
        check (unit_price_snapshot >= 0 ),
    constraint ck_order_items_original_price_positive
        check (original_price_snapshot is null or original_price_snapshot > 0 ),
    constraint ck_order_items_original_price_gt_unit_price
        check (original_price_snapshot is null or original_price_snapshot >= unit_price_snapshot),

    constraint ck_order_items_quantity_positive
        check ( quantity >= 1 ),
    constraint ck_order_items_image_url_not_blank
        check ( trim(image_url_snapshot) <> '' )
);