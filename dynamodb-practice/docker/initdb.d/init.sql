create table event
(
    id         varchar(50) primary key,
    name       varchar(255) not null,
    start_at   timestamp,
    end_at     timestamp,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

create table event_apply
(
    id           varchar(50) primary key,
    event_id     varchar(50),
    user_id      int,
    reward_id    int,
    apply_status varchar(50) default 'PENDING',
    created_at   timestamp   default current_timestamp,
    updated_at   timestamp   default current_timestamp,
    foreign key (event_id) references event (id),
    foreign key (reward_id) references reward (id)
);

create table reward
(
    id          varchar(50) primary key,
    name        varchar(255) not null,
    reward_type varchar(50)  not null,
    event_id    int,
    stock_count int       default 0,
    created_at  timestamp default current_timestamp,
    updated_at  timestamp default current_timestamp,
    valid_from  timestamp,
    valid_to    timestamp,
    foreign key (event_id) references event (id)
);
