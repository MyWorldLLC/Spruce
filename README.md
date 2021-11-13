# Spruce :evergreen_tree:
Fresh, trim, &amp; lightweight JDBC wrapper

## Why use Spruce?
The JDBC API was originally developed in 1997 and has changed
little since then. The Java language has changed a great deal
since then, as have other contemporary database APIs. Spruce
provides a tiny (~300 lines of code), modern API wrapper around
JDBC that greatly simplifies most common access patterns. When
you want to do something that Spruce doesn't support, it's
trivially easy to directly access JDBC directly.

Note that Spruce is not (and does not aspire to be) an ORM.
It does not have any features for mapping objects to/from
the database, and support for this is not planned.

## Features
- Modern, clean API
- Concise code
- `ResultSet` unpacking to single/list/map
- Organize related SQL code into modules
- Bring your own connection pool
- Transactions
- Batching

## Usage

**Setting up a Database**
```java
var ds = /* Acquire a JDBC DataSource - typically a connection pool */;
var db = new Database(ds);

// If using modules
db.register(MyModule.class, MyModule.factory());
db.initFactories();

// That's it! Database is now ready for use.
```

**Acquiring & using a DBContext**
```java
/* The main point of entry into Spruce is the DBContext,
 * which wraps a JDBC connection. Note that the DBContext
 * must have close() called immediately after you're done
 * using it to prevent resource leaks, hence the Spruce
 * idiom of acquiring and using contexts in a try-with-resource
 * block.
 */
try(var ctx = db.getContext()){
    return ctx.prepare("SELECT val1 FROM my_table WHERE id = ?")
        .bind(id)
        .querySingle(Integer.class);
}
```

**Unpack results as a map**
```java
try(var ctx = db.getContext()){
    return ctx.prepare("SELECT key, val1, val2 FROM my_table WHERE id = ?")
        .bind(id)
        .queryMap(r -> r.getString("key"), this::unpackMapValue);
}
```

**Insert/Update/Delete (Queries that don't return results)**
```java
try(var ctx = db.getContext()){
    ctx.execute(
        "INSERT INTO my_table (id, val1, val2) VALUES (?, ?, ?)",
        id, value1, value2);
}
```

## Contributing
Spruce is in active use at [MyWorld](https://myworldvw.com), but our
documentation and tests are lacking in this repository. If you use Spruce
or you're looking to kick off your open source career by writing some
documentation, unit tests, or a new feature, we'd value your contribution!

Note about features: Our goal with Spruce is to provide a lightweight, clean,
and modern API around JDBC. We welcome all contributions in furtherance of
that goal, but we have no interest in Spruce becoming an ORM.
