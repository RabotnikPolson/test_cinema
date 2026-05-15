-- V1__init.sql — базовая схема под ваши Entity

-- пользователи
create table if not exists app_users (
                                         id bigserial primary key,
                                         username text not null unique,
                                         password_hash text not null,
                                         role text not null
);

-- жанры
create table if not exists genres (
                                      id bigserial primary key,
                                      name text not null unique
);

-- фильмы
create table if not exists movies (
                                      id bigserial primary key,
                                      title text not null,
                                      year int,                              -- у вас Long, но для года хватает int
                                      imdb_id text unique,
                                      description varchar(2000),
    poster_url text,
    director text,
    actors varchar(1000),
    genre_text text,                        -- строка из API, напр. "Action, Comedy"
    language text,
    country text,
    imdb_rating text,
    runtime text,
    released text,
    rotten_tomatoes_rating text,
    metacritic_rating text,
    imdb_votes text,

    genre_id bigint,
    constraint fk_movies_genre
    foreign key (genre_id) references genres(id)
    on update cascade
    on delete set null                    -- удалили жанр → у фильмов genre_id = null
    );

-- полезные индексы
create index if not exists idx_movies_title_lower on movies (lower(title));
create index if not exists idx_movies_year on movies (year);
