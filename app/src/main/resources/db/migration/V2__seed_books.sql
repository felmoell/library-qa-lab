-- Seed data for manual exploration, Postman and JMeter. Tests rely on some of it:
--   author "martin" -> 2 books, title "test" -> 6 books, "Frankenstein" has 0 copies, 21 books in total.
INSERT INTO book (isbn, title, author, published_year, copies_total) VALUES
    ('9780132350884', 'Clean Code', 'Robert C. Martin', 2008, 3),
    ('9780201616224', 'The Pragmatic Programmer', 'Andrew Hunt, David Thomas', 1999, 2),
    ('9780201485677', 'Refactoring', 'Martin Fowler', 1999, 2),
    ('9780321146533', 'Test Driven Development: By Example', 'Kent Beck', 2002, 4),
    ('9780471081128', 'Lessons Learned in Software Testing', 'Cem Kaner, James Bach, Bret Pettichord', 2001, 1),
    ('9780321534460', 'Agile Testing', 'Lisa Crispin, Janet Gregory', 2008, 2),
    ('9781937785024', 'Explore It!', 'Elisabeth Hendrickson', 2013, 1),
    ('9780321503626', 'Growing Object-Oriented Software, Guided by Tests', 'Steve Freeman, Nat Pryce', 2009, 1),
    ('9780321125217', 'Domain-Driven Design', 'Eric Evans', 2003, 2),
    ('9780201633610', 'Design Patterns', 'Erich Gamma, Richard Helm, Ralph Johnson, John Vlissides', 1994, 3),
    ('9780321601919', 'Continuous Delivery', 'Jez Humble, David Farley', 2010, 2),
    ('9781118031964', 'The Art of Software Testing', 'Glenford J. Myers', 2011, 1),
    ('9780134685991', 'Effective Java', 'Joshua Bloch', 2018, 5),
    ('9780131177055', 'Working Effectively with Legacy Code', 'Michael Feathers', 2004, 2),
    ('9780131495050', 'xUnit Test Patterns', 'Gerard Meszaros', 2007, 1),
    ('9781680502398', 'Release It!', 'Michael T. Nygard', 2018, 2),
    ('9781942788331', 'Accelerate', 'Nicole Forsgren, Jez Humble, Gene Kim', 2018, 3),
    ('9780201835953', 'The Mythical Man-Month', 'Frederick P. Brooks Jr.', 1995, 1),
    ('9780141439471', 'Frankenstein', 'Mary Shelley', 1818, 0),
    ('9780141439518', 'Pride and Prejudice', 'Jane Austen', 1813, 4),
    ('9791090636071', 'Le Petit Prince', 'Antoine de Saint-Exupéry', 1943, 1);
