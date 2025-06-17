-- Actors Table
CREATE TABLE Actors (
    actor_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    birthday DATE
);

-- Movies Table
CREATE TABLE Movies (
    movie_id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    release_date TEXT,
    running_time INTEGER,
    genre TEXT,
    plot TEXT,
    ratings REAL
);

-- Directors Table
CREATE TABLE Directors (
    director_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    birthday DATE
);

-- Awards Table
CREATE TABLE Awards (
    award_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    category TEXT
);

-- Movie_Actors Table
CREATE TABLE Movie_Actors (
    movie_actor_id INTEGER PRIMARY KEY AUTOINCREMENT,
    movie_id INTEGER,
    actor_id INTEGER,
    FOREIGN KEY (movie_id) REFERENCES Movies(movie_id),
    FOREIGN KEY (actor_id) REFERENCES Actors(actor_id)
);

-- Movie_Director Table
CREATE TABLE Movie_Director (
    movie_director_id INTEGER PRIMARY KEY AUTOINCREMENT,
    movie_id INTEGER,
    director_id INTEGER,
    FOREIGN KEY (movie_id) REFERENCES Movies(movie_id),
    FOREIGN KEY (director_id) REFERENCES Directors(director_id)
);

-- Movie_Awards Table
CREATE TABLE Movie_Awards (
    movie_award_id INTEGER PRIMARY KEY AUTOINCREMENT,
    movie_id INTEGER,
    award_id INTEGER,
    FOREIGN KEY (movie_id) REFERENCES Movies(movie_id),
    FOREIGN KEY (award_id) REFERENCES Awards(award_id)
);

-- Actor_Awards Table
CREATE TABLE Actor_Awards (
    actor_award_id INTEGER PRIMARY KEY AUTOINCREMENT,
    actor_id INTEGER,
    award_id INTEGER,
    FOREIGN KEY (actor_id) REFERENCES Actors(actor_id),
    FOREIGN KEY (award_id) REFERENCES Awards(award_id)
);

-- Director_Awards Table
CREATE TABLE Director_Awards (
    director_award_id INTEGER PRIMARY KEY AUTOINCREMENT,
    director_id INTEGER,
    award_id INTEGER,
    FOREIGN KEY (director_id) REFERENCES Directors(director_id),
    FOREIGN KEY (award_id) REFERENCES Awards(award_id)
);